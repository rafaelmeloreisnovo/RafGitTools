package com.rafgittools.presentation.auth

import androidx.lifecycle.ViewModel
import com.rafgittools.core.security.CredentialManager

class CredentialViewModel(private val manager: CredentialManager): ViewModel() {
    fun saveToken(token: String) = manager.saveToken(token)
    fun loadToken(): String? = manager.loadToken()
}
