package com.rafgittools.data.network

import com.rafgittools.core.security.CredentialManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    fun create(baseUrl: String, credentialManager: CredentialManager): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(credentialManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
