package com.rafgittools.ui.screens.repository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.repository.GitRepository as IGitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for repository list screen
 */
@HiltViewModel
class RepositoryListViewModel @Inject constructor(
    private val gitRepository: IGitRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RepositoryListUiState>(RepositoryListUiState.Loading)
    val uiState: StateFlow<RepositoryListUiState> = _uiState.asStateFlow()
    
    init {
        loadRepositories()
    }
    
    fun loadRepositories() {
        viewModelScope.launch {
            _uiState.value = RepositoryListUiState.Loading
            
            gitRepository.getLocalRepositories()
                .onSuccess { repos ->
                    _uiState.value = if (repos.isEmpty()) {
                        RepositoryListUiState.Empty
                    } else {
                        RepositoryListUiState.Success(repos)
                    }
                }
                .onFailure { error ->
                    _uiState.value = RepositoryListUiState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }
}

/**
 * UI state for repository list
 */
sealed class RepositoryListUiState {
    object Loading : RepositoryListUiState()
    object Empty : RepositoryListUiState()
    data class Success(val repositories: List<GitRepository>) : RepositoryListUiState()
    data class Error(val message: String) : RepositoryListUiState()
}
