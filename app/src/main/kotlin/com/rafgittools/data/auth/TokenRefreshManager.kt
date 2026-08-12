package com.rafgittools.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token lifecycle manager — P33-25.
 *
 * RafGitTools currently authenticates GitHub API requests with PATs or an OAuth
 * App Device Flow access token. Neither path exposes a refresh-token contract in
 * the current app model. Therefore this class does not pretend to "refresh" a
 * token or accept a client secret inside the Android application.
 *
 * Its real responsibility is deterministic session lifecycle handling:
 * - an authenticated 401 invalidates the local session and requires re-auth;
 * - a proven rate-limit 403 is reported without destroying credentials;
 * - a non-rate-limit 403 is reported as forbidden;
 * - OAuth scope response metadata can be parsed when GitHub provides it.
 */
@Singleton
class TokenRefreshManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    sealed class TokenState {
        object Valid : TokenState()

        /**
         * GitHub rejected the attached credential. HTTP 401 does not prove
         * whether it was expired, revoked, deleted or otherwise invalid, so the
         * app deliberately does not invent a more specific cause.
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
     * Interpret one GitHub API response using only stable HTTP/header evidence.
     *
     * On 401 the persisted auth state is cleared immediately. The in-memory
     * cache is cleared by [AuthInterceptor] in the same request cycle, regardless
     * of whether persistent cleanup succeeds.
     */
    suspend fun handleHttpResponse(
        responseCode: Int,
        rateLimitRemaining: String?,
        rateLimitReset: String?
    ): TokenState {
        return when (responseCode) {
            401 -> TokenState.InvalidCredential(
                persistentStateCleared = authRepository.clearAuthState().isSuccess
            )

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
     * Parses GitHub's OAuth-scope response header when present.
     * Fine-grained PATs may not expose classic OAuth scopes, so absence is not
     * treated as authentication failure.
     */
    fun parseScopesFromHeader(headerValue: String?): Set<String> {
        if (headerValue.isNullOrBlank()) return emptySet()
        return headerValue
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
