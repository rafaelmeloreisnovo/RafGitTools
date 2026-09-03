package com.rafgittools.feature.worktree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.vcs.WorktreeInfo
import com.rafgittools.core.vcs.WorktreeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorktreeViewModel @Inject constructor(
    private val worktreeManager: WorktreeManager
) : ViewModel() {

    data class WorktreeState(
        val worktrees: List<WorktreeInfo> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val selectedWorktreePath: String? = null,
        val currentBranch: String? = null
    )

    sealed class WorktreeEffect {
        data class ShowToast(val message: String) : WorktreeEffect()
        data class NavigateToWorktree(val path: String) : WorktreeEffect()
        object WorktreeCreatedSuccess : WorktreeEffect()
        object WorktreeDeletedSuccess : WorktreeEffect()
    }

    private val _state = MutableStateFlow(WorktreeState())
    val state: StateFlow<WorktreeState> = _state.asStateFlow()

    private val _effects = MutableStateFlow<WorktreeEffect?>(null)
    val effects: StateFlow<WorktreeEffect?> = _effects.asStateFlow()

    init {
        loadWorktrees()
    }

    fun loadWorktrees() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = worktreeManager.listWorktrees()

            result.onSuccess { worktrees ->
                _state.value = _state.value.copy(
                    worktrees = worktrees,
                    isLoading = false
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load worktrees: ${exception.message}"
                )
            }
        }
    }

    fun createWorktree(
        path: String,
        branchName: String,
        commitHash: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = worktreeManager.createWorktree(
                path = path,
                branchName = branchName,
                commitHash = commitHash
            )

            result.onSuccess { worktreeInfo ->
                loadWorktrees()
                _effects.value = WorktreeEffect.WorktreeCreatedSuccess
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Worktree created: ${worktreeInfo.path}"
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to create worktree: ${exception.message}"
                )
            }
        }
    }

    fun deleteWorktree(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = worktreeManager.deleteWorktree(path)

            result.onSuccess {
                loadWorktrees()
                _effects.value = WorktreeEffect.WorktreeDeletedSuccess
                _state.value = _state.value.copy(
                    isLoading = false,
                    successMessage = "Worktree deleted: $path"
                )
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to delete worktree: ${exception.message}"
                )
            }
        }
    }

    fun getBranchInfo(worktreePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = worktreeManager.getBranchInfo(worktreePath)

            result.onSuccess { branch ->
                _state.value = _state.value.copy(currentBranch = branch)
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    error = "Failed to get branch info: ${exception.message}"
                )
            }
        }
    }

    fun selectWorktree(path: String) {
        _state.value = _state.value.copy(selectedWorktreePath = path)
        getBranchInfo(path)
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
