package com.rafgittools.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor for authenticated GitHub API requests.
 *
 * The interceptor owns the in-memory fail-closed boundary: once GitHub returns
 * 401 for a request that carried the current credential, that credential is
 * removed from [AuthTokenCache] before this method returns. Persistent cleanup
 * is delegated to [TokenRefreshManager] in the same request cycle.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authTokenCache: AuthTokenCache,
    private val tokenRefreshManager: TokenRefreshManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = authTokenCache.token ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        val response = chain.proceed(authenticatedRequest)

        if (response.code == 401 || response.code == 403) {
            val state = runBlocking(Dispatchers.IO) {
                tokenRefreshManager.handleHttpResponse(
                    responseCode = response.code,
                    rateLimitRemaining = response.header("X-RateLimit-Remaining"),
                    rateLimitReset = response.header("X-RateLimit-Reset")
                )
            }

            if (state is TokenRefreshManager.TokenState.InvalidCredential) {
                // Memory invalidation is unconditional. Even if DataStore cleanup
                // reports failure, this process will not reuse a credential that
                // GitHub just rejected.
                authTokenCache.token = null
            }
        }

        return response
    }
}
