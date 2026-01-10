package com.rafgittools.ui.screens.pullrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.GithubPullRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for pull request list screen
 */
@HiltViewModel
class PullRequestListViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PullRequestListUiState>(PullRequestListUiState.Loading)
    val uiState: StateFlow<PullRequestListUiState> = _uiState.asStateFlow()
    
    private val _pullRequests = MutableStateFlow<List<GithubPullRequest>>(emptyList())
    val pullRequests: StateFlow<List<GithubPullRequest>> = _pullRequests.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(PullRequestFilter.OPEN)
    val selectedFilter: StateFlow<PullRequestFilter> = _selectedFilter.asStateFlow()
    
    private var currentOwner: String = ""
    private var currentRepo: String = ""
    
    fun loadPullRequests(owner: String, repo: String) {
        currentOwner = owner
        currentRepo = repo
        fetchPullRequests()
    }
    
    fun setFilter(filter: PullRequestFilter) {
        _selectedFilter.value = filter
        fetchPullRequests()
    }
    
    fun refresh() {
        fetchPullRequests()
    }
    
    private fun fetchPullRequests() {
        viewModelScope.launch {
            _uiState.value = PullRequestListUiState.Loading
            
            try {
                val prList = githubApiService.getPullRequests(
                    owner = currentOwner,
                    repo = currentRepo,
                    state = _selectedFilter.value.apiValue
                )
                
                _pullRequests.value = prList
                _uiState.value = if (prList.isEmpty()) {
                    PullRequestListUiState.Empty
                } else {
                    PullRequestListUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = PullRequestListUiState.Error(
                    e.message ?: "Failed to load pull requests"
                )
            }
        }
    }
}

/**
 * UI state for pull request list screen
 */
sealed class PullRequestListUiState {
    object Loading : PullRequestListUiState()
    object Empty : PullRequestListUiState()
    object Success : PullRequestListUiState()
    data class Error(val message: String) : PullRequestListUiState()
}
