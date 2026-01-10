package com.rafgittools.ui.screens.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.repository.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for branch list screen
 */
@HiltViewModel
class BranchListViewModel @Inject constructor(
    private val gitRepository: GitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BranchListUiState>(BranchListUiState.Loading)
    val uiState: StateFlow<BranchListUiState> = _uiState.asStateFlow()
    
    private val _branches = MutableStateFlow<List<GitBranch>>(emptyList())
    val branches: StateFlow<List<GitBranch>> = _branches.asStateFlow()
    
    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()
    
    private var currentRepoPath: String = ""
    
    fun loadBranches(repoPath: String) {
        currentRepoPath = repoPath
        viewModelScope.launch {
            _uiState.value = BranchListUiState.Loading
            
            gitRepository.getBranches(repoPath)
                .onSuccess { branchList ->
                    _branches.value = branchList
                    _uiState.value = if (branchList.isEmpty()) {
                        BranchListUiState.Empty
                    } else {
                        BranchListUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = BranchListUiState.Error(
                        error.message ?: "Failed to load branches"
                    )
                }
        }
    }
    
    fun createBranch(branchName: String) {
        viewModelScope.launch {
            gitRepository.createBranch(currentRepoPath, branchName)
                .onSuccess {
                    _operationStatus.value = "Created branch: $branchName"
                    loadBranches(currentRepoPath)
                }
                .onFailure { error ->
                    _operationStatus.value = "Failed to create branch: ${error.message}"
                }
        }
    }
    
    fun checkout(branch: GitBranch) {
        viewModelScope.launch {
            gitRepository.checkoutBranch(currentRepoPath, branch.shortName)
                .onSuccess {
                    _operationStatus.value = "Checked out: ${branch.shortName}"
                    loadBranches(currentRepoPath)
                }
                .onFailure { error ->
                    _operationStatus.value = "Failed to checkout: ${error.message}"
                }
        }
    }
    
    fun deleteBranch(branch: GitBranch) {
        viewModelScope.launch {
            // Note: Branch deletion is not yet implemented in JGitService
            // This would need to be added to the GitRepository interface
            _operationStatus.value = "Branch deletion is not yet supported. This feature will be available in a future update."
            // Refresh to ensure UI is in sync
            loadBranches(currentRepoPath)
        }
    }
    
    fun merge(branch: GitBranch) {
        viewModelScope.launch {
            gitRepository.merge(currentRepoPath, branch.shortName)
                .onSuccess {
                    _operationStatus.value = "Merged ${branch.shortName} into current branch"
                    loadBranches(currentRepoPath)
                }
                .onFailure { error ->
                    _operationStatus.value = "Failed to merge: ${error.message}"
                }
        }
    }
    
    fun refresh() {
        if (currentRepoPath.isNotEmpty()) {
            loadBranches(currentRepoPath)
        }
    }
    
    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}

/**
 * UI state for branch list screen
 */
sealed class BranchListUiState {
    object Loading : BranchListUiState()
    object Empty : BranchListUiState()
    object Success : BranchListUiState()
    data class Error(val message: String) : BranchListUiState()
}
