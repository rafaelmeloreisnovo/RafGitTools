package com.rafgittools.core.security

class CredentialManager(private val storage: SecureStorage) {
    fun saveToken(token: String) = storage.put("auth_token", token)
    fun loadToken(): String? = storage.get("auth_token")
    fun clearToken() = storage.remove("auth_token")
}
