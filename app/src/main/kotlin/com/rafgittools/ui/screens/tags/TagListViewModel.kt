package com.rafgittools.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.GitTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for tag list screen
 */
@HiltViewModel
class TagListViewModel @Inject constructor(
    private val jGitService: JGitService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TagListUiState>(TagListUiState.Loading)
    val uiState: StateFlow<TagListUiState> = _uiState.asStateFlow()
    
    private val _tags = MutableStateFlow<List<GitTag>>(emptyList())
    val tags: StateFlow<List<GitTag>> = _tags.asStateFlow()
    
    private var repoPath: String = ""
    
    fun loadTags(path: String) {
        repoPath = path
        refresh()
    }
    
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = TagListUiState.Loading
            
            jGitService.listTags(repoPath)
                .onSuccess { tagList ->
                    // Sort tags by name in reverse order (latest versions first)
                    _tags.value = tagList.sortedByDescending { it.name }
                    _uiState.value = if (tagList.isEmpty()) {
                        TagListUiState.Empty
                    } else {
                        TagListUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = TagListUiState.Error(
                        error.message ?: "Failed to load tags"
                    )
                }
        }
    }
    
    fun createLightweightTag(name: String) {
        viewModelScope.launch {
            jGitService.createLightweightTag(repoPath, name)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = TagListUiState.Error(
                        error.message ?: "Failed to create tag"
                    )
                }
        }
    }
    
    fun createAnnotatedTag(name: String, message: String) {
        viewModelScope.launch {
            jGitService.createAnnotatedTag(repoPath, name, message)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = TagListUiState.Error(
                        error.message ?: "Failed to create tag"
                    )
                }
        }
    }
    
    fun deleteTag(name: String) {
        viewModelScope.launch {
            jGitService.deleteTag(repoPath, name)
                .onSuccess {
                    refresh()
                }
                .onFailure { error ->
                    _uiState.value = TagListUiState.Error(
                        error.message ?: "Failed to delete tag"
                    )
                }
        }
    }
}

/**
 * UI state for tag list screen
 */
sealed class TagListUiState {
    object Loading : TagListUiState()
    object Empty : TagListUiState()
    object Success : TagListUiState()
    data class Error(val message: String) : TagListUiState()
}
