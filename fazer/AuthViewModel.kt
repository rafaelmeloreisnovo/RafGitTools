package com.rafgittools.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.DeviceFlowState
import com.rafgittools.data.auth.OAuthDeviceFlowManager
import com.rafgittools.data.github.GithubApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubApiService: GithubApiService,
    private val oauthDeviceFlowManager: OAuthDeviceFlowManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _oauthState = MutableStateFlow<OAuthUiState>(OAuthUiState.Idle)
    val oauthState: StateFlow<OAuthUiState> = _oauthState.asStateFlow()

    private var oauthJob: Job? = null

    init {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.isAuthenticated()
            _username.value = authRepository.getUsername()
        }
    }

    /** Save PAT and validate it against GitHub API */
    fun saveToken(token: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // Validate token by fetching user info
                val response = runCatching {
                    // Temporary header injection for validation
                    val user = githubApiService.getAuthenticatedUser()
                    user
                }
                val user = response.getOrElse {
                    _uiState.value = AuthUiState.Error("Invalid token or network error: ${it.message}")
                    return@launch
                }
                // Token valid — save it
                authRepository.savePat(token, user.login)
                    .onSuccess {
                        _isAuthenticated.value = true
                        _username.value = user.login
                        _uiState.value = AuthUiState.Success
                    }
                    .onFailure { e ->
                        _uiState.value = AuthUiState.Error(e.message ?: "Failed to save token")
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    /** Start OAuth Device Flow (P33-23) */
    fun startOAuthFlow() {
        oauthJob?.cancel()
        _oauthState.value = OAuthUiState.Idle
        oauthJob = viewModelScope.launch {
            oauthDeviceFlowManager.startDeviceFlow().collect { state ->
                when (state) {
                    is DeviceFlowState.Requesting -> {
                        _oauthState.value = OAuthUiState.Polling(0, 60)
                    }
                    is DeviceFlowState.PendingUserAction -> {
                        _oauthState.value = OAuthUiState.PendingUserAction(
                            userCode = state.userCode,
                            verificationUri = state.verificationUri,
                            expiresInSeconds = state.expiresInSeconds
                        )
                    }
                    is DeviceFlowState.Polling -> {
                        _oauthState.value = OAuthUiState.Polling(state.attempt, state.max)
                    }
                    is DeviceFlowState.Authorized -> {
                        _isAuthenticated.value = true
                        _username.value = state.username
                        _oauthState.value = OAuthUiState.Authorized(state.username)
                    }
                    is DeviceFlowState.Error -> {
                        _oauthState.value = OAuthUiState.Error(state.message)
                    }
                }
            }
        }
    }

    fun cancelOAuth() {
        oauthJob?.cancel()
        _oauthState.value = OAuthUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isAuthenticated.value = false
            _username.value = null
            _uiState.value = AuthUiState.Idle
            _oauthState.value = OAuthUiState.Idle
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class OAuthUiState {
    object Idle : OAuthUiState()
    data class PendingUserAction(val userCode: String, val verificationUri: String, val expiresInSeconds: Int) : OAuthUiState()
    data class Polling(val attempt: Int, val max: Int) : OAuthUiState()
    data class Authorized(val username: String) : OAuthUiState()
    data class Error(val message: String) : OAuthUiState()
}
