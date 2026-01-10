package com.rafgittools.data.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    private val authRepository: AuthRepository
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only add auth header if user is authenticated
        val isAuthenticated = runBlocking { authRepository.isAuthenticated() }
        
        if (!isAuthenticated) {
            return chain.proceed(originalRequest)
        }
        
        // Get the PAT
        val patResult = runBlocking { authRepository.getPat() }
        
        val token = patResult.getOrNull()
        if (token == null) {
            return chain.proceed(originalRequest)
        }
        
        // Add authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        
        return chain.proceed(authenticatedRequest)
    }
}
