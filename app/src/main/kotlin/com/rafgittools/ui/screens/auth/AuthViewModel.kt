package com.rafgittools.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthMethod
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.auth.DeviceFlowState
import com.rafgittools.data.auth.GhCliAuthImporter
import com.rafgittools.data.auth.OAuthDeviceFlowManager
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.core.security.SshKeyManager
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
    private val authTokenCache: AuthTokenCache,
    private val deviceFlowManager: OAuthDeviceFlowManager,
    private val ghCliAuthImporter: GhCliAuthImporter,
    private val sshKeyManager: SshKeyManager
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.ChoosingMethod)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()
    private val _selectedMethod = MutableStateFlow<AuthMethod?>(null)
    val selectedMethod: StateFlow<AuthMethod?> = _selectedMethod.asStateFlow()

    init { checkAuthenticationStatus() }

    private fun checkAuthenticationStatus() { viewModelScope.launch {
        _isAuthenticated.value = authRepository.isAuthenticated()
        _username.value = authRepository.getUsername()
        authTokenCache.token = authRepository.getPat().getOrNull()
        _selectedMethod.value = authRepository.getAuthMethod().getOrNull()
        if (!_isAuthenticated.value && !authRepository.isOfflineMode()) _uiState.value = AuthUiState.ChoosingMethod
    } }

    fun selectMethod(method: AuthMethod) { _selectedMethod.value = method; _uiState.value = AuthUiState.MethodSelected(method) }
    fun clearSelectedMethod() { _selectedMethod.value = null; _uiState.value = AuthUiState.ChoosingMethod }

    fun startDeviceCodeLogin() { selectMethod(AuthMethod.DEVICE_CODE); startDeviceFlow(AuthMethod.DEVICE_CODE) }

    fun startOAuthWebLogin() {
        selectMethod(AuthMethod.OAUTH_WEB)
        startDeviceFlow(AuthMethod.OAUTH_WEB)
    }

    private fun startDeviceFlow(method: AuthMethod) { viewModelScope.launch {
        deviceFlowManager.startDeviceFlow().collect { state ->
            _uiState.value = when (state) {
                is DeviceFlowState.Requesting -> AuthUiState.Loading
                is DeviceFlowState.PendingUserAction -> AuthUiState.DeviceCodePending(state.userCode, state.verificationUri)
                is DeviceFlowState.Polling -> AuthUiState.DeviceCodePolling(state.attempt, state.max)
                is DeviceFlowState.Authorized -> { authTokenCache.token = state.token; _isAuthenticated.value = true; _username.value = state.username; authRepository.setOfflineMode(false); authRepository.saveAuthMethod(method); AuthUiState.Success(state.username, method) }
                is DeviceFlowState.Error -> AuthUiState.Error(state.message)
            }
        }
    } }

    fun importGhCliToken() { selectMethod(AuthMethod.GH_CLI_IMPORT); viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        ghCliAuthImporter.importToken().onSuccess { token -> authenticateWithPat(token) }
            .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Falha ao importar token do gh CLI") }
    } }

    fun authenticateWithSshKey() { selectMethod(AuthMethod.SSH_KEY); viewModelScope.launch {
        _uiState.value = AuthUiState.Loading
        val sshKeys = sshKeyManager.listKeys().getOrElse {
            _uiState.value = AuthUiState.Error("Falha ao verificar chaves SSH: ${it.message}")
            return@launch
        }
        if (sshKeys.isEmpty()) {
            _uiState.value = AuthUiState.Error("Nenhuma chave SSH encontrada. Gere ou importe uma chave para autenticar via SSH.")
            return@launch
        }
        authRepository.setOfflineMode(true)
        authRepository.saveAuthMethod(AuthMethod.SSH_KEY)
        authTokenCache.token = null
        _isAuthenticated.value = true
        _username.value = sshKeys.first().comment.ifBlank { "ssh-user" }
        _uiState.value = AuthUiState.Success(_username.value ?: "ssh-user", AuthMethod.SSH_KEY)
    } }
    fun continueOffline() { selectMethod(AuthMethod.OFFLINE); viewModelScope.launch { authRepository.setOfflineMode(true); authRepository.saveAuthMethod(AuthMethod.OFFLINE); authTokenCache.token = null; _uiState.value = AuthUiState.Offline } }

    fun authenticateWithPat(token: String) { if (token.isBlank()) { _uiState.value = AuthUiState.Error("Token cannot be empty"); return }
        viewModelScope.launch {
            selectMethod(AuthMethod.PAT); _uiState.value = AuthUiState.Loading; authRepository.setOfflineMode(false)
            authRepository.savePat(token, "temp").onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Failed to validate token format"); return@launch }
            githubRepository.getAuthenticatedUserSync().onSuccess { user ->
                authRepository.savePat(token, user.login); authRepository.saveAuthMethod(AuthMethod.PAT); authTokenCache.token = token; _isAuthenticated.value = true; _username.value = user.login; _uiState.value = AuthUiState.Success(user.login, AuthMethod.PAT)
            }.onFailure { error -> authRepository.logout(); authTokenCache.token = null; _uiState.value = AuthUiState.Error(error.message ?: "Authentication failed") }
        }
    }

    fun logout() { viewModelScope.launch { authRepository.clearAuthState(); authTokenCache.token = null; _isAuthenticated.value = false; _username.value = null; _selectedMethod.value = null; _uiState.value = AuthUiState.ChoosingMethod } }
    fun resetState() { _uiState.value = AuthUiState.ChoosingMethod }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object ChoosingMethod : AuthUiState()
    data class MethodSelected(val method: AuthMethod) : AuthUiState()
    object Loading : AuthUiState()
    data class DeviceCodePending(val userCode: String, val verificationUri: String) : AuthUiState()
    data class DeviceCodePolling(val attempt: Int, val max: Int) : AuthUiState()
    data class Success(val username: String, val method: AuthMethod) : AuthUiState()
    object Offline : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
