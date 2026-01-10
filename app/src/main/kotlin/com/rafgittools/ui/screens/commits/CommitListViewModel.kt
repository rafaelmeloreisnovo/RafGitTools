package com.rafgittools.ui.screens.commits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.repository.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for commit list screen
 */
@HiltViewModel
class CommitListViewModel @Inject constructor(
    private val gitRepository: GitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CommitListUiState>(CommitListUiState.Loading)
    val uiState: StateFlow<CommitListUiState> = _uiState.asStateFlow()
    
    private val _commits = MutableStateFlow<List<GitCommit>>(emptyList())
    val commits: StateFlow<List<GitCommit>> = _commits.asStateFlow()
    
    private val _currentBranch = MutableStateFlow<String?>(null)
    val currentBranch: StateFlow<String?> = _currentBranch.asStateFlow()
    
    private var currentRepoPath: String = ""
    private var currentLimit: Int = 50
    
    fun loadCommits(repoPath: String) {
        currentRepoPath = repoPath
        currentLimit = 50
        viewModelScope.launch {
            _uiState.value = CommitListUiState.Loading
            
            // Get current branch
            gitRepository.getStatus(repoPath)
                .onSuccess { status ->
                    _currentBranch.value = status.branch
                }
            
            // Get commits
            gitRepository.getCommits(repoPath, null, currentLimit)
                .onSuccess { commitList ->
                    _commits.value = commitList
                    _uiState.value = if (commitList.isEmpty()) {
                        CommitListUiState.Empty
                    } else {
                        CommitListUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = CommitListUiState.Error(
                        error.message ?: "Failed to load commits"
                    )
                }
        }
    }
    
    fun loadMore() {
        currentLimit += 50
        viewModelScope.launch {
            gitRepository.getCommits(currentRepoPath, null, currentLimit)
                .onSuccess { commitList ->
                    _commits.value = commitList
                }
        }
    }
    
    fun refresh() {
        if (currentRepoPath.isNotEmpty()) {
            loadCommits(currentRepoPath)
        }
    }
}

/**
 * UI state for commit list screen
 */
sealed class CommitListUiState {
    object Loading : CommitListUiState()
    object Empty : CommitListUiState()
    object Success : CommitListUiState()
    data class Error(val message: String) : CommitListUiState()
}
