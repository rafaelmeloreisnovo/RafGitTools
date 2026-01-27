package com.rafgittools.data.network

import com.rafgittools.core.security.CredentialManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val credentialManager: CredentialManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = credentialManager.loadToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()

        return chain.proceed(request)
    }
}
