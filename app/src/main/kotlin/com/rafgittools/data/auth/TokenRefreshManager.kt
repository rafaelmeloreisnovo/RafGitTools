package com.rafgittools.data.auth

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenRefreshManager — P33-25
 *
 * GitHub Personal Access Tokens (PAT) don't use OAuth refresh tokens —
 * they are long-lived credentials that users set expiry for manually.
 *
 * This manager handles:
 * 1. Token expiration detection via API 401/403 responses
 * 2. Proactive expiry checking (GitHub sends X-OAuth-Scopes and expiry hints)
 * 3. Notifying the UI to re-authenticate when a token is expired/revoked
 * 4. OAuth token refresh when a refresh_token IS available (future OAuth app flow)
 *
 * Integration point: AuthInterceptor calls [checkAndHandleTokenError] on 401/403.
 */
@Singleton
class TokenRefreshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    companion object {
        // GitHub PATs can carry an expiration date in X-GitHub-Token-Expiry header
        // or return 401 with "Bad credentials" message when expired/revoked
        private const val EXPIRY_HEADER = "GitHub-Authentication-Token-Expiry"
        private const val RATE_LIMIT_HEADER = "X-RateLimit-Remaining"
        private const val SCOPES_HEADER = "X-OAuth-Scopes"
        private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    }

    /** State of the current token health */
    sealed class TokenState {
        object Valid : TokenState()
        object Expired : TokenState()
        object Revoked : TokenState()
        data class RateLimited(val resetEpoch: Long) : TokenState()
        object Unknown : TokenState()
    }

    /** PAT expiry warning states */
    sealed class PatExpiryState {
        object NoExpiryInfo : PatExpiryState()
        object Ok : PatExpiryState()
        data class WarningSoon(val expiresAt: Long, val daysLeft: Long) : PatExpiryState()
        object AlreadyExpired : PatExpiryState()
    }

    /**
     * Called by AuthInterceptor when a 401/403 response is received.
     * Returns true if the error is due to token expiry/revocation
     * (in which case the UI should prompt re-authentication).
     */
    suspend fun handleUnauthorizedResponse(
        responseCode: Int,
        responseHeaders: Map<String, String>,
        responseBody: String?
    ): TokenState {
        return when (responseCode) {
            401 -> {
                // 401 = "Bad credentials" — token revoked or expired
                when {
                    responseBody?.contains("Bad credentials", ignoreCase = true) == true -> {
                        clearExpiredToken()
                        TokenState.Revoked
                    }
                    responseBody?.contains("token expired", ignoreCase = true) == true -> {
                        clearExpiredToken()
                        TokenState.Expired
                    }
                    else -> TokenState.Unknown
                }
            }
            403 -> {
                // 403 can be rate-limiting
                val remaining = responseHeaders[RATE_LIMIT_HEADER]?.toIntOrNull()
                val resetAt = responseHeaders["X-RateLimit-Reset"]?.toLongOrNull()
                if (remaining == 0 && resetAt != null) {
                    TokenState.RateLimited(resetAt)
                } else {
                    TokenState.Unknown
                }
            }
            else -> TokenState.Valid
        }
    }

    /**
     * Inspect response headers proactively to detect upcoming expiry.
     * GitHub may return X-GitHub-Token-Expiry or similar hints.
     */
    fun extractExpiryFromHeaders(headers: Map<String, String>): Long? {
        val expiryStr = headers[EXPIRY_HEADER] ?: return null
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .parse(expiryStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if stored token will expire within [withinMs] milliseconds.
     * Returns null if no expiry information is available.
     */
    fun isTokenExpiringSoon(expiryEpoch: Long, withinMs: Long = 86_400_000): Boolean {
        val now = System.currentTimeMillis()
        return expiryEpoch - now < withinMs
    }

    /**
     * Derives the PAT expiry state from response headers.
     *
     * Returns [PatExpiryState.WarningSoon] when the token expires within 7 days,
     * [PatExpiryState.AlreadyExpired] if already past, [PatExpiryState.Ok] otherwise,
     * and [PatExpiryState.NoExpiryInfo] when the header is absent (e.g. classic PATs
     * with no configured expiry).
     */
    fun checkPATExpiry(headers: Map<String, String>): PatExpiryState {
        val expiryEpoch = extractExpiryFromHeaders(headers) ?: return PatExpiryState.NoExpiryInfo
        val now = System.currentTimeMillis()
        val remaining = expiryEpoch - now
        return when {
            remaining <= 0 -> PatExpiryState.AlreadyExpired
            remaining < SEVEN_DAYS_MS -> PatExpiryState.WarningSoon(
                expiresAt = expiryEpoch,
                daysLeft = remaining / (24 * 60 * 60 * 1000)
            )
            else -> PatExpiryState.Ok
        }
    }

    /**
     * Parses the `X-OAuth-Scopes` header into a set of granted scope strings.
     *
     * Returns an empty set when the header is absent (fine-grained PATs don't
     * send this header — they use repository permissions instead).
     */
    fun parseScopesFromHeader(headers: Map<String, String>): Set<String> {
        val raw = headers[SCOPES_HEADER] ?: return emptySet()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * For OAuth apps with refresh_token support (future use):
     * Exchange refresh token for new access token.
     *
     * Note: GitHub PATs do NOT support this — only GitHub Apps / OAuth Apps
     * that implement the device flow or web flow get refresh tokens.
     * This method is a stub for when the app evolves to use OAuth App flow.
     *
     * Caller behavior: when this returns a failed [Result], trigger the
     * re-authentication flow via [OAuthDeviceFlowManager.startDeviceFlow].
     */
    @Suppress("unused")
    suspend fun refreshOAuthToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String
    ): Result<String> {
        // Future implementation when OAuthDeviceFlowManager returns refresh_token
        // val response = oauthApi.refreshToken(clientId, clientSecret, refreshToken)
        // authRepository.savePat(response.access_token, authRepository.getUsername() ?: "")
        // return Result.success(response.access_token)
        return Result.failure(
            UnsupportedOperationException(
                "OAuth token refresh not yet supported. GitHub PATs don't use refresh tokens. " +
                    "Use OAuthDeviceFlowManager.startDeviceFlow() to re-authenticate."
            )
        )
    }

    private suspend fun clearExpiredToken() {
        try {
            authRepository.logout()
        } catch (e: Exception) {
            // Ignore — best effort cleanup
        }
    }
}
