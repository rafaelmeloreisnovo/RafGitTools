package com.rafgittools.ui.screens.pullrequests

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for pull request detail screen
 */
@HiltViewModel
class PullRequestDetailViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PullRequestDetailUiState>(PullRequestDetailUiState.Loading)
    val uiState: StateFlow<PullRequestDetailUiState> = _uiState.asStateFlow()
    
    private val _pullRequest = MutableStateFlow<GithubPullRequest?>(null)
    val pullRequest: StateFlow<GithubPullRequest?> = _pullRequest.asStateFlow()
    
    private val _files = MutableStateFlow<List<GithubPullRequestFile>>(emptyList())
    val files: StateFlow<List<GithubPullRequestFile>> = _files.asStateFlow()
    
    private val _commits = MutableStateFlow<List<GithubCommit>>(emptyList())
    val commits: StateFlow<List<GithubCommit>> = _commits.asStateFlow()
    
    private val _reviews = MutableStateFlow<List<GithubReview>>(emptyList())
    val reviews: StateFlow<List<GithubReview>> = _reviews.asStateFlow()
    
    private val _selectedTab = MutableStateFlow(PullRequestTab.CONVERSATION)
    val selectedTab: StateFlow<PullRequestTab> = _selectedTab.asStateFlow()
    
    private var currentOwner: String = ""
    private var currentRepo: String = ""
    private var currentPrNumber: Int = 0
    
    fun loadPullRequest(owner: String, repo: String, prNumber: Int) {
        currentOwner = owner
        currentRepo = repo
        currentPrNumber = prNumber
        
        viewModelScope.launch {
            _uiState.value = PullRequestDetailUiState.Loading
            
            try {
                // Load PR details
                val prData = githubApiService.getPullRequest(owner, repo, prNumber)
                _pullRequest.value = prData
                
                // Load files
                try {
                    val filesData = githubApiService.getPullRequestFiles(owner, repo, prNumber)
                    _files.value = filesData
                } catch (e: Exception) {
                    // Files are optional, don't fail the whole load
                    _files.value = emptyList()
                }
                
                // Load commits
                try {
                    val commitsData = githubApiService.getPullRequestCommits(owner, repo, prNumber)
                    _commits.value = commitsData
                } catch (e: Exception) {
                    _commits.value = emptyList()
                }
                
                // Load reviews
                try {
                    val reviewsData = githubApiService.getPullRequestReviews(owner, repo, prNumber)
                    _reviews.value = reviewsData
                } catch (e: Exception) {
                    _reviews.value = emptyList()
                }
                
                _uiState.value = PullRequestDetailUiState.Success
            } catch (e: Exception) {
                _uiState.value = PullRequestDetailUiState.Error(
                    e.message ?: "Failed to load pull request"
                )
            }
        }
    }
    
    fun refresh() {
        if (currentOwner.isNotEmpty() && currentRepo.isNotEmpty() && currentPrNumber > 0) {
            loadPullRequest(currentOwner, currentRepo, currentPrNumber)
        }
    }
    
    fun selectTab(tab: PullRequestTab) {
        _selectedTab.value = tab
    }
}

/**
 * UI state for pull request detail screen
 */
sealed class PullRequestDetailUiState {
    object Loading : PullRequestDetailUiState()
    object Success : PullRequestDetailUiState()
    data class Error(val message: String) : PullRequestDetailUiState()
}
