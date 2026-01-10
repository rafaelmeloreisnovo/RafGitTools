package com.rafgittools.ui.screens.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.model.GitStatus
import com.rafgittools.domain.repository.Credentials
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for repository detail screen
 */
@HiltViewModel
class RepositoryDetailViewModel @Inject constructor(
    private val gitRepository: IGitRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RepositoryDetailUiState>(RepositoryDetailUiState.Loading)
    val uiState: StateFlow<RepositoryDetailUiState> = _uiState.asStateFlow()
    
    private val _status = MutableStateFlow<GitStatus?>(null)
    val status: StateFlow<GitStatus?> = _status.asStateFlow()
    
    private val _branches = MutableStateFlow<List<GitBranch>>(emptyList())
    val branches: StateFlow<List<GitBranch>> = _branches.asStateFlow()
    
    private val _recentCommits = MutableStateFlow<List<GitCommit>>(emptyList())
    val recentCommits: StateFlow<List<GitCommit>> = _recentCommits.asStateFlow()
    
    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()
    
    private var currentRepoPath: String = ""
    
    fun loadRepository(repoPath: String) {
        currentRepoPath = repoPath
        viewModelScope.launch {
            _uiState.value = RepositoryDetailUiState.Loading
            
            gitRepository.getRepository(repoPath)
                .onSuccess { repo ->
                    _uiState.value = RepositoryDetailUiState.Success(repo)
                    loadStatus()
                    loadBranches()
                    loadRecentCommits()
                }
                .onFailure { error ->
                    _uiState.value = RepositoryDetailUiState.Error(
                        error.message ?: "Failed to load repository"
                    )
                }
        }
    }
    
    fun refresh() {
        if (currentRepoPath.isNotEmpty()) {
            loadStatus()
            loadBranches()
            loadRecentCommits()
        }
    }
    
    private fun loadStatus() {
        viewModelScope.launch {
            gitRepository.getStatus(currentRepoPath)
                .onSuccess { _status.value = it }
                .onFailure { /* Keep old status or show error */ }
        }
    }
    
    private fun loadBranches() {
        viewModelScope.launch {
            gitRepository.getBranches(currentRepoPath)
                .onSuccess { _branches.value = it }
                .onFailure { /* Keep old branches or show error */ }
        }
    }
    
    private fun loadRecentCommits() {
        viewModelScope.launch {
            gitRepository.getCommits(currentRepoPath, null, 10)
                .onSuccess { _recentCommits.value = it }
                .onFailure { /* Keep old commits or show error */ }
        }
    }
    
    fun stageFile(file: String) {
        viewModelScope.launch {
            gitRepository.stageFiles(currentRepoPath, listOf(file))
                .onSuccess {
                    _operationStatus.value = "Staged: $file"
                    loadStatus()
                }
                .onFailure {
                    _operationStatus.value = "Failed to stage: ${it.message}"
                }
        }
    }
    
    fun unstageFile(file: String) {
        viewModelScope.launch {
            gitRepository.unstageFiles(currentRepoPath, listOf(file))
                .onSuccess {
                    _operationStatus.value = "Unstaged: $file"
                    loadStatus()
                }
                .onFailure {
                    _operationStatus.value = "Failed to unstage: ${it.message}"
                }
        }
    }
    
    fun stageAllChanges() {
        viewModelScope.launch {
            val status = _status.value ?: return@launch
            val allFiles = status.modified + status.untracked + status.added
            if (allFiles.isEmpty()) return@launch
            
            gitRepository.stageFiles(currentRepoPath, allFiles)
                .onSuccess {
                    _operationStatus.value = "Staged all changes"
                    loadStatus()
                }
                .onFailure {
                    _operationStatus.value = "Failed to stage changes: ${it.message}"
                }
        }
    }
    
    fun unstageAllChanges() {
        viewModelScope.launch {
            val status = _status.value ?: return@launch
            val allFiles = status.added + status.changed
            if (allFiles.isEmpty()) return@launch
            
            gitRepository.unstageFiles(currentRepoPath, allFiles)
                .onSuccess {
                    _operationStatus.value = "Unstaged all changes"
                    loadStatus()
                }
                .onFailure {
                    _operationStatus.value = "Failed to unstage changes: ${it.message}"
                }
        }
    }
    
    fun commit(message: String) {
        viewModelScope.launch {
            gitRepository.commit(currentRepoPath, message)
                .onSuccess {
                    _operationStatus.value = "Committed: ${it.sha.take(7)}"
                    loadStatus()
                    loadRecentCommits()
                }
                .onFailure {
                    _operationStatus.value = "Failed to commit: ${it.message}"
                }
        }
    }
    
    fun push() {
        viewModelScope.launch {
            val credentials = getCredentials()
            gitRepository.push(currentRepoPath, "origin", null, credentials)
                .onSuccess {
                    _operationStatus.value = "Pushed successfully"
                }
                .onFailure {
                    _operationStatus.value = "Failed to push: ${it.message}"
                }
        }
    }
    
    fun pull() {
        viewModelScope.launch {
            val credentials = getCredentials()
            gitRepository.pull(currentRepoPath, "origin", null, credentials)
                .onSuccess {
                    _operationStatus.value = "Pulled successfully"
                    loadStatus()
                    loadRecentCommits()
                }
                .onFailure {
                    _operationStatus.value = "Failed to pull: ${it.message}"
                }
        }
    }
    
    fun fetch() {
        viewModelScope.launch {
            val credentials = getCredentials()
            gitRepository.fetch(currentRepoPath, "origin", credentials)
                .onSuccess {
                    _operationStatus.value = "Fetched successfully"
                    loadBranches()
                }
                .onFailure {
                    _operationStatus.value = "Failed to fetch: ${it.message}"
                }
        }
    }
    
    fun createBranch(branchName: String) {
        viewModelScope.launch {
            gitRepository.createBranch(currentRepoPath, branchName)
                .onSuccess {
                    _operationStatus.value = "Created branch: $branchName"
                    loadBranches()
                }
                .onFailure {
                    _operationStatus.value = "Failed to create branch: ${it.message}"
                }
        }
    }
    
    fun checkout(branchName: String) {
        viewModelScope.launch {
            gitRepository.checkoutBranch(currentRepoPath, branchName)
                .onSuccess {
                    _operationStatus.value = "Checked out: $branchName"
                    loadStatus()
                    loadBranches()
                    loadRecentCommits()
                }
                .onFailure {
                    _operationStatus.value = "Failed to checkout: ${it.message}"
                }
        }
    }
    
    fun clearOperationStatus() {
        _operationStatus.value = null
    }
    
    private suspend fun getCredentials(): Credentials? {
        return authRepository.getPat().getOrNull()?.let { Credentials.Token(it) }
    }
}

/**
 * UI state for repository detail screen
 */
sealed class RepositoryDetailUiState {
    object Loading : RepositoryDetailUiState()
    data class Success(val repository: GitRepository) : RepositoryDetailUiState()
    data class Error(val message: String) : RepositoryDetailUiState()
}
