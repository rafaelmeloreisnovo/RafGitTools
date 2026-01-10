package com.rafgittools.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.github.GithubDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screen
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubDataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()
    
    init {
        checkAuthenticationStatus()
    }
    
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.isAuthenticated()
            _username.value = authRepository.getUsername()
        }
    }
    
    /**
     * Authenticate with Personal Access Token
     * 
     * @param token The GitHub Personal Access Token
     */
    fun authenticateWithPat(token: String) {
        if (token.isBlank()) {
            _uiState.value = AuthUiState.Error("Token cannot be empty")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // First, save the token temporarily to make the API call
            authRepository.savePat(token, "temp")
            
            // Verify the token by making an API call
            githubRepository.getAuthenticatedUser().first()
                .onSuccess { user ->
                    // Save the token with the actual username
                    authRepository.savePat(token, user.login)
                        .onSuccess {
                            _isAuthenticated.value = true
                            _username.value = user.login
                            _uiState.value = AuthUiState.Success(user.login)
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState.Error(
                                error.message ?: "Failed to save credentials"
                            )
                        }
                }
                .onFailure { error ->
                    // Clear the temporary token
                    authRepository.logout()
                    _uiState.value = AuthUiState.Error(
                        when {
                            error.message?.contains("401") == true -> 
                                "Invalid token. Please check your PAT and try again."
                            error.message?.contains("403") == true ->
                                "Token doesn't have required permissions."
                            else -> error.message ?: "Authentication failed"
                        }
                    )
                }
        }
    }
    
    /**
     * Logout and clear credentials
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
                .onSuccess {
                    _isAuthenticated.value = false
                    _username.value = null
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.message ?: "Failed to logout"
                    )
                }
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * UI state for authentication
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
