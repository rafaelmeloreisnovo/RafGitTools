package com.rafgittools.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.security.SshKeyManager
import com.rafgittools.data.auth.AuthMethod
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.auth.DeviceFlowState
import com.rafgittools.data.auth.GhCliAuthImporter
import com.rafgittools.data.auth.OAuthDeviceFlowManager
import com.rafgittools.data.github.GithubDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @Suppress("UNUSED_PARAMETER") private val githubRepository: GithubDataRepository,
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

    private var authJob: Job? = null

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            val authenticated = authRepository.isAuthenticated()
            val offline = authRepository.isOfflineMode()
            val method = authRepository.getAuthMethod().getOrNull()
            val token = authRepository.getPat().getOrNull()

            val tokenRequired = method in setOf(
                AuthMethod.PAT,
                AuthMethod.DEVICE_CODE,
                AuthMethod.OAUTH_WEB,
                AuthMethod.GH_CLI_IMPORT
            )

            if (authenticated && tokenRequired && token.isNullOrBlank()) {
                authRepository.clearAuthState()
                authTokenCache.token = null
                _isAuthenticated.value = false
                _username.value = null
                _selectedMethod.value = null
                _uiState.value = AuthUiState.ChoosingMethod
                return@launch
            }

            _isAuthenticated.value = authenticated
            _username.value = authRepository.getUsername()
            authTokenCache.token = token
            _selectedMethod.value = method

            if (!authenticated && !offline) {
                _selectedMethod.value = null
                _uiState.value = AuthUiState.ChoosingMethod
            }
        }
    }

    fun selectMethod(method: AuthMethod) {
        _selectedMethod.value = method
        _uiState.value = AuthUiState.MethodSelected(method)
    }

    fun clearSelectedMethod() {
        authJob?.cancel()
        authJob = null
        _selectedMethod.value = null
        _uiState.value = AuthUiState.ChoosingMethod
    }

    fun startDeviceCodeLogin() {
        selectMethod(AuthMethod.DEVICE_CODE)
        startDeviceFlow(AuthMethod.DEVICE_CODE)
    }

    fun startOAuthWebLogin() {
        selectMethod(AuthMethod.OAUTH_WEB)
        startDeviceFlow(AuthMethod.OAUTH_WEB)
    }

    private fun startDeviceFlow(method: AuthMethod) {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            deviceFlowManager.startDeviceFlow().collect { state ->
                _uiState.value = when (state) {
                    is DeviceFlowState.Requesting -> AuthUiState.Loading
                    is DeviceFlowState.PendingUserAction -> AuthUiState.DeviceCodePending(
                        state.userCode,
                        state.verificationUri
                    )

                    is DeviceFlowState.Polling -> AuthUiState.DeviceCodePolling(state.attempt, state.max)
                    is DeviceFlowState.Authorized -> {
                        authRepository.setOfflineMode(false)
                        val methodResult = authRepository.saveAuthMethod(method)
                        if (methodResult.isFailure) {
                            authRepository.clearAuthState()
                            authTokenCache.token = null
                            _isAuthenticated.value = false
                            _username.value = null
                            AuthUiState.Error(
                                "O login foi autorizado, mas não foi possível salvar o método de autenticação."
                            )
                        } else {
                            authTokenCache.token = state.token
                            _isAuthenticated.value = true
                            _username.value = state.username
                            AuthUiState.Success(state.username, method)
                        }
                    }

                    is DeviceFlowState.Error -> AuthUiState.Error(state.message)
                }
            }
        }
    }

    fun importGhCliToken() {
        selectMethod(AuthMethod.GH_CLI_IMPORT)
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            ghCliAuthImporter.importToken()
                .onSuccess { token -> authenticateWithToken(token, AuthMethod.GH_CLI_IMPORT) }
                .onFailure {
                    _uiState.value = AuthUiState.Error(
                        it.message ?: "Falha ao importar o token do gh CLI."
                    )
                }
        }
    }

    fun authenticateWithSshKey() {
        selectMethod(AuthMethod.SSH_KEY)
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val sshKeys = sshKeyManager.listKeys().getOrElse {
                _uiState.value = AuthUiState.Error("Falha ao verificar chaves SSH: ${it.message}")
                return@launch
            }
            if (sshKeys.isEmpty()) {
                _uiState.value = AuthUiState.Error(
                    "Nenhuma chave SSH encontrada. Gere ou importe uma chave para operações Git por SSH."
                )
                return@launch
            }
            authRepository.setOfflineMode(true)
            authRepository.saveAuthMethod(AuthMethod.SSH_KEY)
            authTokenCache.token = null
            _isAuthenticated.value = true
            _username.value = sshKeys.first().comment.ifBlank { "ssh-user" }
            _uiState.value = AuthUiState.Success(
                _username.value ?: "ssh-user",
                AuthMethod.SSH_KEY
            )
        }
    }

    fun continueOffline() {
        selectMethod(AuthMethod.OFFLINE)
        authJob?.cancel()
        authJob = viewModelScope.launch {
            authRepository.setOfflineMode(true)
            authRepository.saveAuthMethod(AuthMethod.OFFLINE)
            authTokenCache.token = null
            _isAuthenticated.value = false
            _username.value = null
            _uiState.value = AuthUiState.Offline
        }
    }

    fun authenticateWithPat(token: String) {
        authenticateWithToken(token, AuthMethod.PAT)
    }

    private fun authenticateWithToken(token: String, method: AuthMethod) {
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) {
            _uiState.value = AuthUiState.Error("O token do GitHub não pode ficar vazio.")
            return
        }

        authJob?.cancel()
        authJob = viewModelScope.launch {
            selectMethod(method)
            _uiState.value = AuthUiState.Loading
            authRepository.setOfflineMode(false)

            val username = deviceFlowManager.validateToken(normalizedToken).getOrElse { error ->
                authTokenCache.token = null
                _uiState.value = AuthUiState.Error(
                    error.message ?: "O GitHub recusou o token informado."
                )
                return@launch
            }

            authRepository.savePat(normalizedToken, username).onFailure { error ->
                authTokenCache.token = null
                _uiState.value = AuthUiState.Error(
                    "O token foi validado, mas não pôde ser protegido no aparelho: ${error.message}"
                )
                return@launch
            }

            authRepository.saveAuthMethod(method).onFailure { error ->
                authRepository.clearAuthState()
                authTokenCache.token = null
                _uiState.value = AuthUiState.Error(
                    "Falha ao concluir a sessão: ${error.message}"
                )
                return@launch
            }

            authTokenCache.token = normalizedToken
            _isAuthenticated.value = true
            _username.value = username
            _uiState.value = AuthUiState.Success(username, method)
        }
    }

    fun logout() {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            authRepository.clearAuthState()
            authTokenCache.token = null
            _isAuthenticated.value = false
            _username.value = null
            _selectedMethod.value = null
            _uiState.value = AuthUiState.ChoosingMethod
        }
    }

    fun resetState() {
        clearSelectedMethod()
    }

    override fun onCleared() {
        authJob?.cancel()
        super.onCleared()
    }
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
