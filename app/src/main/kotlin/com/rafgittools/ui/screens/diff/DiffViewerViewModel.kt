package com.rafgittools.ui.screens.diff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.InteractiveStagingService
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.DiffHunk
import com.rafgittools.domain.model.GitDiff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for diff viewer screen
 */
@HiltViewModel
class DiffViewerViewModel @Inject constructor(
    private val jGitService: JGitService,
    private val interactiveStagingService: InteractiveStagingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DiffViewerUiState>(DiffViewerUiState.Loading)
    val uiState: StateFlow<DiffViewerUiState> = _uiState.asStateFlow()
    
    private val _diffs = MutableStateFlow<List<GitDiff>>(emptyList())
    val diffs: StateFlow<List<GitDiff>> = _diffs.asStateFlow()
    
    private val _viewMode = MutableStateFlow(DiffViewMode.UNIFIED)
    val viewMode: StateFlow<DiffViewMode> = _viewMode.asStateFlow()
    
    private val _showStagedOnly = MutableStateFlow(false)
    val showStagedOnly: StateFlow<Boolean> = _showStagedOnly.asStateFlow()

    private val _isMutatingHunk = MutableStateFlow(false)
    val isMutatingHunk: StateFlow<Boolean> = _isMutatingHunk.asStateFlow()

    private val _hunkOperation = MutableStateFlow<HunkOperationState>(HunkOperationState.Idle)
    val hunkOperation: StateFlow<HunkOperationState> = _hunkOperation.asStateFlow()
    
    private var repoPath: String = ""
    
    fun loadDiff(path: String) {
        repoPath = path
        refresh()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DiffViewerUiState.Loading
            
            jGitService.getDiff(repoPath, _showStagedOnly.value)
                .onSuccess { diffList ->
                    _diffs.value = diffList
                    _uiState.value = if (diffList.isEmpty()) {
                        DiffViewerUiState.Empty
                    } else {
                        DiffViewerUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = DiffViewerUiState.Error(
                        error.message ?: "Failed to load diff"
                    )
                }
        }
    }

    /**
     * In unstaged mode this stages exactly one hunk. In staged mode it
     * unstages exactly one hunk. The service revalidates the selected hunk
     * against the current repository state before changing the index.
     */
    fun mutateHunk(diff: GitDiff, hunk: DiffHunk) {
        if (_isMutatingHunk.value || repoPath.isBlank()) return

        viewModelScope.launch {
            _isMutatingHunk.value = true
            val unstaging = _showStagedOnly.value
            try {
                val result = if (unstaging) {
                    interactiveStagingService.unstageHunk(repoPath, diff, hunk)
                } else {
                    interactiveStagingService.stageHunk(repoPath, diff, hunk)
                }

                result
                    .onSuccess {
                        val path = diff.newPath ?: diff.oldPath ?: "file"
                        _hunkOperation.value = HunkOperationState.Success(
                            if (unstaging) "Unstaged one hunk: $path"
                            else "Staged one hunk: $path"
                        )
                        refresh()
                    }
                    .onFailure { error ->
                        _hunkOperation.value = HunkOperationState.Error(
                            error.message ?: "Interactive hunk operation failed"
                        )
                    }
            } finally {
                _isMutatingHunk.value = false
            }
        }
    }

    fun clearHunkOperation() {
        _hunkOperation.value = HunkOperationState.Idle
    }
    
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            DiffViewMode.UNIFIED -> DiffViewMode.SPLIT
            DiffViewMode.SPLIT -> DiffViewMode.UNIFIED
        }
    }
    
    fun toggleStagedFilter() {
        if (_isMutatingHunk.value) return
        _showStagedOnly.value = !_showStagedOnly.value
        refresh()
    }
}

/**
 * UI state for diff viewer screen
 */
sealed class DiffViewerUiState {
    object Loading : DiffViewerUiState()
    object Empty : DiffViewerUiState()
    object Success : DiffViewerUiState()
    data class Error(val message: String) : DiffViewerUiState()
}

sealed class HunkOperationState {
    object Idle : HunkOperationState()
    data class Success(val message: String) : HunkOperationState()
    data class Error(val message: String) : HunkOperationState()
}
