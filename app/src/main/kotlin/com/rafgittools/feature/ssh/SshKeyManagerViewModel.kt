package com.rafgittools.feature.ssh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.security.SshKeyInfo
import com.rafgittools.core.security.SshKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SSH Key Manager UI
 *
 * Manages:
 * - List of stored SSH keys
 * - Generate new SSH keys (Ed25519, RSA, ECDSA)
 * - Import existing keys
 * - Delete keys
 * - Export public keys
 * - Passphrase validation
 */
@HiltViewModel
class SshKeyManagerViewModel @Inject constructor(
    private val sshKeyManager: SshKeyManager
) : ViewModel() {

    data class SshKeyManagerState(
        val keys: List<SshKeyInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val selectedKeyName: String? = null
    )

    sealed class SshKeyManagerEffect {
        data class ShowToast(val message: String) : SshKeyManagerEffect()
        data class NavigateToKeyDetail(val keyName: String) : SshKeyManagerEffect()
        object KeyGeneratedSuccess : SshKeyManagerEffect()
        object KeyDeletedSuccess : SshKeyManagerEffect()
    }

    private val _state = MutableStateFlow(SshKeyManagerState())
    val state: StateFlow<SshKeyManagerState> = _state.asStateFlow()

    private val _effects = MutableStateFlow<SshKeyManagerEffect?>(null)
    val effects: StateFlow<SshKeyManagerEffect?> = _effects.asStateFlow()

    init {
        loadKeys()
    }

    fun loadKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = sshKeyManager.listKeys()

            result.onSuccess { keys ->
                _state.value = _state.value.copy(
                    keys = keys,
                    isLoading = false
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load keys: ${exception.message}"
                )
            }
        }
    }

    fun generateKey(
        keyType: Int = SshKeyManager.KEY_TYPE_ED25519,
        keyName: String = "id_rsa",
        passphrase: String? = null,
        comment: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = sshKeyManager.generateKeyPair(
                keyType = keyType,
                keyName = keyName,
                passphrase = passphrase,
                comment = comment
            )

            result.onSuccess { keyInfo ->
                loadKeys()
                _effects.value = SshKeyManagerEffect.KeyGeneratedSuccess
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "SSH key generated: ${keyInfo.name}"
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to generate key: ${exception.message}"
                )
            }
        }
    }

    fun importKey(
        keyName: String,
        privateKeyContent: String,
        passphrase: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = sshKeyManager.importKey(
                keyName = keyName,
                privateKeyContent = privateKeyContent,
                passphrase = passphrase
            )

            result.onSuccess { keyInfo ->
                loadKeys()
                _effects.value = SshKeyManagerEffect.ShowToast("Key imported: ${keyInfo.name}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "SSH key imported: ${keyInfo.name}"
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to import key: ${exception.message}"
                )
            }
        }
    }

    fun deleteKey(keyName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = sshKeyManager.deleteKey(keyName)

            result.onSuccess {
                loadKeys()
                _effects.value = SshKeyManagerEffect.KeyDeletedSuccess
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "SSH key deleted: $keyName"
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to delete key: ${exception.message}"
                )
            }
        }
    }

    fun exportPublicKey(keyName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sshKeyManager.exportPublicKey(keyName)

            result.onSuccess { publicKey ->
                _effects.value = SshKeyManagerEffect.ShowToast("Public key copied to clipboard")
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    error = "Failed to export public key: ${exception.message}"
                )
            }
        }
    }

    fun validatePassphrase(keyName: String, passphrase: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = sshKeyManager.validatePassphrase(keyName, passphrase)

            result.onSuccess { isValid ->
                if (isValid) {
                    _effects.value = SshKeyManagerEffect.ShowToast("Passphrase is correct")
                } else {
                    _state.value = _state.value.copy(
                        error = "Passphrase is incorrect"
                    )
                }
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    error = "Failed to validate passphrase: ${exception.message}"
                )
            }
        }
    }

    fun selectKey(keyName: String) {
        _state.value = _state.value.copy(selectedKeyName = keyName)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }

    fun clearEffect() {
        _effects.value = null
    }
}
