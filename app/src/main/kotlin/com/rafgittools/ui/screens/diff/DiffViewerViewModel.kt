package com.rafgittools.ui.screens.diff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
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
    private val jGitService: JGitService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<DiffViewerUiState>(DiffViewerUiState.Loading)
    val uiState: StateFlow<DiffViewerUiState> = _uiState.asStateFlow()
    
    private val _diffs = MutableStateFlow<List<GitDiff>>(emptyList())
    val diffs: StateFlow<List<GitDiff>> = _diffs.asStateFlow()
    
    private val _viewMode = MutableStateFlow(DiffViewMode.UNIFIED)
    val viewMode: StateFlow<DiffViewMode> = _viewMode.asStateFlow()
    
    private val _showStagedOnly = MutableStateFlow(false)
    val showStagedOnly: StateFlow<Boolean> = _showStagedOnly.asStateFlow()
    
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
    
    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            DiffViewMode.UNIFIED -> DiffViewMode.SPLIT
            DiffViewMode.SPLIT -> DiffViewMode.UNIFIED
        }
    }
    
    fun toggleStagedFilter() {
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
