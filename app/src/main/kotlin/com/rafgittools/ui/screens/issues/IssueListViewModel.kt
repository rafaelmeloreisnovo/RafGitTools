package com.rafgittools.ui.screens.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.GithubIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for issue list screen
 */
@HiltViewModel
class IssueListViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<IssueListUiState>(IssueListUiState.Loading)
    val uiState: StateFlow<IssueListUiState> = _uiState.asStateFlow()
    
    private val _issues = MutableStateFlow<List<GithubIssue>>(emptyList())
    val issues: StateFlow<List<GithubIssue>> = _issues.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(IssueFilter.OPEN)
    val selectedFilter: StateFlow<IssueFilter> = _selectedFilter.asStateFlow()
    
    private var currentOwner: String = ""
    private var currentRepo: String = ""
    
    fun loadIssues(owner: String, repo: String) {
        currentOwner = owner
        currentRepo = repo
        fetchIssues()
    }
    
    fun setFilter(filter: IssueFilter) {
        _selectedFilter.value = filter
        fetchIssues()
    }
    
    fun refresh() {
        fetchIssues()
    }
    
    private fun fetchIssues() {
        viewModelScope.launch {
            _uiState.value = IssueListUiState.Loading
            
            try {
                val issueList = githubApiService.getIssues(
                    owner = currentOwner,
                    repo = currentRepo,
                    state = _selectedFilter.value.apiValue
                )
                
                _issues.value = issueList
                _uiState.value = if (issueList.isEmpty()) {
                    IssueListUiState.Empty
                } else {
                    IssueListUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = IssueListUiState.Error(
                    e.message ?: "Failed to load issues"
                )
            }
        }
    }
}

/**
 * UI state for issue list screen
 */
sealed class IssueListUiState {
    object Loading : IssueListUiState()
    object Empty : IssueListUiState()
    object Success : IssueListUiState()
    data class Error(val message: String) : IssueListUiState()
}
