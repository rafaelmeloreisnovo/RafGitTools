package com.rafgittools.ui.screens.worktree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.worktree.WorktreeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorktreeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<WorktreeUiState>(WorktreeUiState.Loading)
    val uiState: StateFlow<WorktreeUiState> = _uiState.asStateFlow()

    private val _worktrees = MutableStateFlow<List<WorktreeManager.WorktreeInfo>>(emptyList())
    val worktrees: StateFlow<List<WorktreeManager.WorktreeInfo>> = _worktrees.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private var repoPath: String = ""

    fun load(path: String) {
        repoPath = path
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = WorktreeUiState.Loading
            WorktreeManager.list(repoPath)
                .onSuccess { list ->
                    _worktrees.value = list
                    _uiState.value = if (list.isEmpty()) WorktreeUiState.Empty else WorktreeUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = WorktreeUiState.Error(e.message ?: "Failed to list worktrees")
                }
        }
    }

    fun addWorktree(path: String, branch: String, createBranch: Boolean) {
        if (path.isBlank() || branch.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            WorktreeManager.add(path, branch, repoPath, createBranch)
                .onSuccess {
                    _snackbar.value = "Worktree added at $path"
                    refresh()
                }
                .onFailure { e -> _snackbar.value = "Add failed: ${e.message}" }
        }
    }

    fun removeWorktree(path: String, force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            WorktreeManager.remove(path, repoPath, force)
                .onSuccess {
                    _snackbar.value = "Worktree removed"
                    _worktrees.value = _worktrees.value.filter { it.path != path }
                    if (_worktrees.value.isEmpty()) _uiState.value = WorktreeUiState.Empty
                }
                .onFailure { e -> _snackbar.value = "Remove failed: ${e.message}" }
        }
    }

    fun pruneWorktrees() {
        viewModelScope.launch(Dispatchers.IO) {
            WorktreeManager.prune(repoPath)
                .onSuccess { _snackbar.value = "Stale worktrees pruned"; refresh() }
                .onFailure { e -> _snackbar.value = "Prune failed: ${e.message}" }
        }
    }

    fun lockWorktree(path: String, reason: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            WorktreeManager.lock(path, repoPath, reason)
                .onSuccess { _snackbar.value = "Worktree locked"; refresh() }
                .onFailure { e -> _snackbar.value = "Lock failed: ${e.message}" }
        }
    }

    fun unlockWorktree(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            WorktreeManager.unlock(path, repoPath)
                .onSuccess { _snackbar.value = "Worktree unlocked"; refresh() }
                .onFailure { e -> _snackbar.value = "Unlock failed: ${e.message}" }
        }
    }

    fun snackbarShown() { _snackbar.value = null }
}

sealed class WorktreeUiState {
    object Loading : WorktreeUiState()
    object Empty : WorktreeUiState()
    object Success : WorktreeUiState()
    data class Error(val message: String) : WorktreeUiState()
}
