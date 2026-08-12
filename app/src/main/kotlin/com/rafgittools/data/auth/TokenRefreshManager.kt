package com.rafgittools.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token lifecycle manager — P33-25.
 *
 * Credential classes are handled by capability, not by wishful inference:
 * - PAT / OAuth App access token: no stored refresh token -> 401 invalidates session;
 * - GitHub App user token produced by Device Flow with expiring-token support:
 *   stored refresh token -> one rotation attempt -> refreshed session or invalidation;
 * - 403 rate-limit/forbidden never destroys a still-valid credential.
 */
@Singleton
class TokenRefreshManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val oauthDeviceFlowManager: OAuthDeviceFlowManager
) {
    sealed class TokenState {
        object Valid : TokenState()

        data class Refreshed(
            val accessToken: String
        ) : TokenState()

        /**
         * GitHub rejected the attached credential and no usable refresh path
         * recovered it. HTTP 401 does not prove whether the credential expired,
         * was revoked, deleted or became invalid for another reason.
         */
        data class InvalidCredential(
            val persistentStateCleared: Boolean
        ) : TokenState()

        data class RateLimited(
            val resetEpoch: Long?
        ) : TokenState()

        object Forbidden : TokenState()
    }

    /**
     * Interpret one GitHub API response.
     *
     * On 401, try exactly one stored Device-Flow refresh capability. If no
     * refresh token exists or rotation fails, invalidate the persistent session.
     * The interceptor independently clears the in-memory token fail-closed.
     */
    suspend fun handleHttpResponse(
        responseCode: Int,
        rateLimitRemaining: String?,
        rateLimitReset: String?
    ): TokenState {
        return when (responseCode) {
            401 -> {
                oauthDeviceFlowManager.refreshStoredSession()
                    .fold(
                        onSuccess = { TokenState.Refreshed(it.accessToken) },
                        onFailure = { invalidateSession() }
                    )
            }

            403 -> {
                val remaining = rateLimitRemaining?.toLongOrNull()
                if (remaining == 0L) {
                    TokenState.RateLimited(rateLimitReset?.toLongOrNull())
                } else {
                    TokenState.Forbidden
                }
            }

            else -> TokenState.Valid
        }
    }

    /**
     * Explicit invalidation used after a refreshed request is rejected again.
     * This method never attempts a second refresh.
     */
    suspend fun invalidateSession(): TokenState.InvalidCredential {
        return TokenState.InvalidCredential(
            persistentStateCleared = authRepository.clearAuthState().isSuccess
        )
    }

    /** Parse GitHub's OAuth scope response header when present. */
    fun parseScopesFromHeader(headerValue: String?): Set<String> {
        if (headerValue.isNullOrBlank()) return emptySet()
        return headerValue
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
