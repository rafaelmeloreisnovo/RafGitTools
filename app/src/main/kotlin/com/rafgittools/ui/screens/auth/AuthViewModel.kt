package com.rafgittools.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthMethod
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.github.GithubDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubDataRepository,
    private val authTokenCache: AuthTokenCache
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _selectedMethod = MutableStateFlow<AuthMethod?>(null)
    val selectedMethod: StateFlow<AuthMethod?> = _selectedMethod.asStateFlow()

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.isAuthenticated()
            _username.value = authRepository.getUsername()
            authTokenCache.token = authRepository.getPat().getOrNull()
            _selectedMethod.value = authRepository.getAuthMethod().getOrNull()
        }
    }

    fun selectMethod(method: AuthMethod) {
        _selectedMethod.value = method
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun clearSelectedMethod() {
        _selectedMethod.value = null
        _uiState.value = AuthUiState.Idle
    }

    fun startDeviceCodeLogin() {
        selectMethod(AuthMethod.DEVICE_CODE)
        _uiState.value = AuthUiState.Error("Fluxo Device Code em preparação. Você pode usar PAT agora.")
    }

    fun completeDeviceCodeLogin() {
        _uiState.value = AuthUiState.Error("Nenhum device code ativo para concluir.")
    }

    fun authenticateWithOAuthCallback(code: String) {
        selectMethod(AuthMethod.OAUTH_WEB)
        if (code.isBlank()) {
            _uiState.value = AuthUiState.Error("Código OAuth inválido")
            return
        }
        _uiState.value = AuthUiState.Error("Fluxo OAuth Web em preparação. Você pode usar PAT agora.")
    }

    fun importFromGhCli() {
        selectMethod(AuthMethod.GH_CLI_IMPORT)
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val hasGhCli = runCatching {
                val process = ProcessBuilder("gh", "--version").start()
                process.waitFor() == 0
            }.getOrDefault(false)

            _uiState.value = if (!hasGhCli) {
                AuthUiState.Error("gh CLI não encontrado. Use PAT ou Device Code.")
            } else {
                AuthUiState.Error("Importação via gh CLI em preparação. Você pode usar PAT agora.")
            }
        }
    }

    fun continueOffline() {
        selectMethod(AuthMethod.OFFLINE)
        viewModelScope.launch {
            authRepository.saveAuthMethod(AuthMethod.OFFLINE)
            _isAuthenticated.value = false
            _username.value = null
            authTokenCache.token = null
            _uiState.value = AuthUiState.SuccessOffline
        }
    }

    fun authenticateWithPat(token: String) {
        if (token.isBlank()) {
            _uiState.value = AuthUiState.Error("Token cannot be empty")
            return
        }

        viewModelScope.launch {
            selectMethod(AuthMethod.PAT)
            _uiState.value = AuthUiState.Loading

            authRepository.savePat(token, "temp")
                .onFailure {
                    _uiState.value = AuthUiState.Error(it.message ?: "Failed to validate token format")
                    return@launch
                }

            githubRepository.getAuthenticatedUserSync()
                .onSuccess { user ->
                    authRepository.savePat(token, user.login)
                        .onSuccess {
                            authRepository.saveAuthMethod(AuthMethod.PAT)
                            authTokenCache.token = token
                            _isAuthenticated.value = true
                            _username.value = user.login
                            _uiState.value = AuthUiState.Success(user.login)
                        }
                        .onFailure { error ->
                            _uiState.value = AuthUiState.Error(error.message ?: "Failed to save credentials")
                        }
                }
                .onFailure { error ->
                    authRepository.logout()
                    authTokenCache.token = null
                    _uiState.value = AuthUiState.Error(
                        when {
                            error.message?.contains("401") == true -> "Invalid token. Please check your PAT and try again."
                            error.message?.contains("403") == true -> "Token doesn't have required permissions."
                            else -> error.message ?: "Authentication failed"
                        }
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
                .onSuccess {
                    authTokenCache.token = null
                    _isAuthenticated.value = false
                    _username.value = null
                    _selectedMethod.value = null
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(error.message ?: "Failed to logout")
                }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val username: String) : AuthUiState()
    object SuccessOffline : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
