package com.rafgittools.data.network

import com.rafgittools.core.security.CredentialManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    fun create(baseUrl: String, credentialManager: CredentialManager): Retrofit {
        val networkGuard = RafNetworkGuardInterceptor(
            policy = RafNetworkGuardPolicy.forBaseUrl(
                baseUrl = baseUrl,
                mode = RafNetworkGuardMode.ENFORCE
            ),
            sink = AndroidRafNetworkAuditSink
        )
        val client = OkHttpClient.Builder()
            // Keep the destination gate before bearer-token injection.
            .addInterceptor(networkGuard)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                credentialManager.loadToken()?.takeIf { it.isNotBlank() }?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
