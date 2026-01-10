package com.rafgittools.ui.screens.stash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.GitStash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for stash list screen
 */
@HiltViewModel
class StashListViewModel @Inject constructor(
    private val jGitService: JGitService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<StashListUiState>(StashListUiState.Loading)
    val uiState: StateFlow<StashListUiState> = _uiState.asStateFlow()
    
    private val _stashes = MutableStateFlow<List<GitStash>>(emptyList())
    val stashes: StateFlow<List<GitStash>> = _stashes.asStateFlow()
    
    private var repoPath: String = ""
    
    fun loadStashes(path: String) {
        repoPath = path
        refresh()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = StashListUiState.Loading
            
            jGitService.listStashes(repoPath)
                .onSuccess { stashList ->
                    _stashes.value = stashList
                    _uiState.value = if (stashList.isEmpty()) {
                        StashListUiState.Empty
                    } else {
                        StashListUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = StashListUiState.Error(
                        error.message ?: "Failed to load stashes"
                    )
                }
        }
    }
    
    fun createStash(message: String?, includeUntracked: Boolean) {
        viewModelScope.launch {
            jGitService.stash(repoPath, message, includeUntracked)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = StashListUiState.Error(
                        error.message ?: "Failed to create stash"
                    )
                }
        }
    }
    
    fun applyStash(index: Int) {
        viewModelScope.launch {
            val stashRef = "stash@{$index}"
            jGitService.stashApply(repoPath, stashRef)
                .onSuccess {
                    // Stash applied but not removed, just refresh
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = StashListUiState.Error(
                        error.message ?: "Failed to apply stash"
                    )
                }
        }
    }
    
    fun popStash(index: Int) {
        viewModelScope.launch {
            val stashRef = "stash@{$index}"
            jGitService.stashPop(repoPath, stashRef)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = StashListUiState.Error(
                        error.message ?: "Failed to pop stash"
                    )
                }
        }
    }
    
    fun dropStash(index: Int) {
        viewModelScope.launch {
            jGitService.stashDrop(repoPath, index)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = StashListUiState.Error(
                        error.message ?: "Failed to drop stash"
                    )
                }
        }
    }
}

/**
 * UI state for stash list screen
 */
sealed class StashListUiState {
    object Loading : StashListUiState()
    object Empty : StashListUiState()
    object Success : StashListUiState()
    data class Error(val message: String) : StashListUiState()
}
