package com.rafgittools.data.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that adds authentication headers to GitHub API requests
 * 
 * Automatically retrieves the stored PAT and adds it as a Bearer token
 * to all outgoing requests to the GitHub API.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authTokenCache: AuthTokenCache
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val token = authTokenCache.token ?: return chain.proceed(originalRequest)
        
        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
