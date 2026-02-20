package com.rafgittools.ui.screens.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.storage.RepoStorage
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AddRepositoryViewModel @Inject constructor(
    private val gitRepository: GitRepository,
    private val authRepository: AuthRepository,
    private val repoStorage: RepoStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRepositoryUiState())
    val uiState: StateFlow<AddRepositoryUiState> = _uiState.asStateFlow()

    private var repoNameEditedManually = false

    fun onRemoteUrlChanged(value: String) {
        _uiState.update { current ->
            val derivedName = deriveRepoName(value)
            val nextRepoName = if (!repoNameEditedManually) {
                derivedName
            } else {
                current.repoName
            }

            current.copy(
                remoteUrl = value,
                repoName = nextRepoName,
                errorMessage = null,
                isSuccess = false
            )
        }
    }

    fun onRepoNameChanged(value: String) {
        repoNameEditedManually = value.isNotBlank()
        _uiState.update {
            it.copy(repoName = value, errorMessage = null, isSuccess = false)
        }
    }

    fun cloneRepository() {
        val current = _uiState.value
        val remoteUrl = current.remoteUrl.trim()

        if (remoteUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Remote URL is required") }
            return
        }

        viewModelScope.launch {
            val derivedRepoName = deriveRepoName(remoteUrl)
            val repoName = current.repoName.trim().ifBlank { derivedRepoName }

            if (repoName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Repository name is required") }
                return@launch
            }

            val localPath = File(repoStorage.baseDir, repoName).absolutePath
            val credentials = authRepository.getPat().getOrNull()?.let { Credentials.Token(it) }

            _uiState.update {
                it.copy(
                    repoName = repoName,
                    isLoading = true,
                    errorMessage = null,
                    isSuccess = false
                )
            }

            gitRepository.cloneRepository(remoteUrl, localPath, credentials)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = null, isSuccess = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to clone repository",
                            isSuccess = false
                        )
                    }
                }
        }
    }

    private fun deriveRepoName(url: String): String {
        val trimmed = url.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""

        val candidate = trimmed.substringAfterLast('/').substringAfterLast(':')
        return candidate.removeSuffix(".git")
    }
}

data class AddRepositoryUiState(
    val remoteUrl: String = "",
    val repoName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
