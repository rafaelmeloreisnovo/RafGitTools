package com.rafgittools.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor for authenticated GitHub API requests.
 *
 * A 401 may trigger exactly one refresh attempt when the current session owns a
 * Device-Flow refresh token. Successful rotation retries the original request
 * once with the new access token. A second 401 invalidates the session without
 * attempting another refresh, preventing retry loops.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authTokenCache: AuthTokenCache,
    private val tokenRefreshManager: TokenRefreshManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = authTokenCache.token ?: return chain.proceed(originalRequest)

        val firstResponse = chain.proceed(authenticatedRequest(originalRequest, token))
        if (firstResponse.code != 401 && firstResponse.code != 403) {
            return firstResponse
        }

        val firstState = boundedLifecycle(
            response = firstResponse,
            rejectedAccessToken = if (firstResponse.code == 401) token else null
        )
        if (firstState is TokenRefreshManager.TokenState.Refreshed) {
            authTokenCache.token = firstState.accessToken
            firstResponse.close()

            val retryResponse = chain.proceed(
                authenticatedRequest(originalRequest, firstState.accessToken)
            )

            when (retryResponse.code) {
                401 -> {
                    // The newly rotated token was also rejected. Never refresh twice
                    // in one call chain: invalidate persistent + in-memory session.
                    boundedInvalidation()
                    authTokenCache.token = null
                }

                403 -> {
                    // Preserve the refreshed token; classify rate-limit/forbidden
                    // without attempting another refresh.
                    boundedLifecycle(retryResponse, rejectedAccessToken = null)
                }
            }
            return retryResponse
        }

        if (firstState is TokenRefreshManager.TokenState.InvalidCredential) {
            // Memory invalidation is unconditional. Even if persistence cleanup
            // failed or timed out, this process cannot reuse the rejected token.
            authTokenCache.token = null
        }

        return firstResponse
    }

    private fun authenticatedRequest(original: Request, token: String): Request {
        return original.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
    }

    private fun boundedLifecycle(
        response: Response,
        rejectedAccessToken: String?
    ): TokenRefreshManager.TokenState {
        return runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(LIFECYCLE_TIMEOUT_MS) {
                tokenRefreshManager.handleHttpResponse(
                    responseCode = response.code,
                    rateLimitRemaining = response.header("X-RateLimit-Remaining"),
                    rateLimitReset = response.header("X-RateLimit-Reset"),
                    rejectedAccessToken = rejectedAccessToken
                )
            } ?: if (response.code == 401) {
                TokenRefreshManager.TokenState.InvalidCredential(
                    persistentStateCleared = false
                )
            } else {
                TokenRefreshManager.TokenState.Forbidden
            }
        }
    }

    private fun boundedInvalidation() {
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(LIFECYCLE_TIMEOUT_MS) {
                tokenRefreshManager.invalidateSession()
            }
        }
    }

    companion object {
        // Long enough for one mobile-network refresh round trip; still bounded so
        // a storage/network stall cannot hold the OkHttp worker indefinitely.
        private const val LIFECYCLE_TIMEOUT_MS = 15_000L
    }
}
