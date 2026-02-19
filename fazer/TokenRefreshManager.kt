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
    }

    /** State of the current token health */
    sealed class TokenState {
        object Valid : TokenState()
        object Expired : TokenState()
        object Revoked : TokenState()
        data class RateLimited(val resetEpoch: Long) : TokenState()
        object Unknown : TokenState()
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
     * For OAuth apps with refresh_token support (future use):
     * Exchange refresh token for new access token.
     *
     * Note: GitHub PATs do NOT support this — only GitHub Apps / OAuth Apps
     * that implement the device flow or web flow get refresh tokens.
     * This method is a stub for when the app evolves to use OAuth App flow.
     */
    @Suppress("unused")
    suspend fun refreshOAuthToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String
    ): Result<String> = runCatching {
        // Future implementation when OAuthDeviceFlowManager returns refresh_token
        // val response = oauthApi.refreshToken(clientId, clientSecret, refreshToken)
        // authRepository.savePat(response.access_token, authRepository.getUsername() ?: "")
        // return@runCatching response.access_token
        throw UnsupportedOperationException(
            "OAuth token refresh not yet supported. GitHub PATs don't use refresh tokens. " +
            "Use OAuthDeviceFlowManager.startDeviceFlow() to re-authenticate."
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
