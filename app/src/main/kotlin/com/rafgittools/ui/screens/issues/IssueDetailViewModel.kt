package com.rafgittools.ui.screens.issues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.github.CreateCommentRequest
import com.rafgittools.domain.model.github.GithubComment
import com.rafgittools.domain.model.github.GithubIssue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for issue detail screen
 */
@HiltViewModel
class IssueDetailViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<IssueDetailUiState>(IssueDetailUiState.Loading)
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()
    
    private val _issue = MutableStateFlow<GithubIssue?>(null)
    val issue: StateFlow<GithubIssue?> = _issue.asStateFlow()
    
    private val _comments = MutableStateFlow<List<GithubComment>>(emptyList())
    val comments: StateFlow<List<GithubComment>> = _comments.asStateFlow()
    
    private var currentOwner: String = ""
    private var currentRepo: String = ""
    private var currentIssueNumber: Int = 0
    
    fun loadIssue(owner: String, repo: String, issueNumber: Int) {
        currentOwner = owner
        currentRepo = repo
        currentIssueNumber = issueNumber
        
        viewModelScope.launch {
            _uiState.value = IssueDetailUiState.Loading
            
            try {
                // Load issue details
                val issueData = githubApiService.getIssue(owner, repo, issueNumber)
                _issue.value = issueData
                
                // Load comments
                val commentsData = githubApiService.getIssueComments(owner, repo, issueNumber)
                _comments.value = commentsData
                
                _uiState.value = IssueDetailUiState.Success
            } catch (e: Exception) {
                _uiState.value = IssueDetailUiState.Error(
                    e.message ?: "Failed to load issue"
                )
            }
        }
    }
    
    fun refresh() {
        if (currentOwner.isNotEmpty() && currentRepo.isNotEmpty() && currentIssueNumber > 0) {
            loadIssue(currentOwner, currentRepo, currentIssueNumber)
        }
    }
    
    fun addComment(body: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val newComment = githubApiService.createIssueComment(
                    currentOwner,
                    currentRepo,
                    currentIssueNumber,
                    CreateCommentRequest(body)
                )
                
                // Add the new comment to the list
                _comments.value = _comments.value + newComment
                onComplete()
            } catch (e: Exception) {
                // Handle error - could show a snackbar
                onComplete()
            }
        }
    }
}

/**
 * UI state for issue detail screen
 */
sealed class IssueDetailUiState {
    object Loading : IssueDetailUiState()
    object Success : IssueDetailUiState()
    data class Error(val message: String) : IssueDetailUiState()
}
