package com.rafgittools.data.auth

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token lifecycle manager — P33-25.
 *
 * Credential classes are handled by capability:
 * - PAT / OAuth App access token: no stored refresh token -> 401 invalidates session;
 * - GitHub App user token produced by Device Flow with expiring-token support:
 *   stored refresh token -> one serialized rotation attempt -> refreshed session or invalidation;
 * - concurrent 401s coalesce safely;
 * - refresh failure and persistent invalidation are serialized in the same
 *   recovery transaction so a late clear cannot erase a newly rotated session;
 * - 403 rate-limit/forbidden never destroys a still-valid credential.
 */
@Singleton
class TokenRefreshManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val oauthDeviceFlowManager: OAuthDeviceFlowManager
) {
    private val recoveryMutex = Mutex()

    sealed class TokenState {
        object Valid : TokenState()

        data class Refreshed(
            val accessToken: String
        ) : TokenState()

        data class InvalidCredential(
            val persistentStateCleared: Boolean
        ) : TokenState()

        data class RateLimited(
            val resetEpoch: Long?
        ) : TokenState()

        object Forbidden : TokenState()
    }

    suspend fun handleHttpResponse(
        responseCode: Int,
        rateLimitRemaining: String?,
        rateLimitReset: String?,
        rejectedAccessToken: String? = null
    ): TokenState {
        return when (responseCode) {
            401 -> recoveryMutex.withLock {
                oauthDeviceFlowManager.refreshStoredSession(rejectedAccessToken)
                    .fold(
                        onSuccess = { TokenState.Refreshed(it.accessToken) },
                        onFailure = { invalidateSessionUnlocked() }
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
     * It shares the recovery lock with refresh, so it cannot race a successful
     * rotation from another request.
     */
    suspend fun invalidateSession(): TokenState.InvalidCredential = recoveryMutex.withLock {
        invalidateSessionUnlocked()
    }

    private suspend fun invalidateSessionUnlocked(): TokenState.InvalidCredential {
        return TokenState.InvalidCredential(
            persistentStateCleared = authRepository.clearAuthState().isSuccess
        )
    }

    fun parseScopesFromHeader(headerValue: String?): Set<String> {
        if (headerValue.isNullOrBlank()) return emptySet()
        return headerValue
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
