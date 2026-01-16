package com.rafgittools.ui.screens.createpr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.usecase.github.CreatePullRequestParams
import com.rafgittools.domain.usecase.github.CreatePullRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePullRequestViewModel @Inject constructor(
    private val createPullRequestUseCase: CreatePullRequestUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    
    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()
    
    private val _baseBranch = MutableStateFlow("main")
    val baseBranch: StateFlow<String> = _baseBranch.asStateFlow()
    
    private val _headBranch = MutableStateFlow("")
    val headBranch: StateFlow<String> = _headBranch.asStateFlow()
    
    fun setTitle(title: String) {
        _title.value = title
    }
    
    fun setBody(body: String) {
        _body.value = body
    }
    
    fun setBaseBranch(branch: String) {
        _baseBranch.value = branch
    }
    
    fun setHeadBranch(branch: String) {
        _headBranch.value = branch
    }
    
    fun createPullRequest(owner: String, repo: String) {
        if (title.value.isBlank()) {
            _uiState.value = UiState.Error("Pull request title cannot be empty")
            return
        }
        if (headBranch.value.isBlank()) {
            _uiState.value = UiState.Error("Head branch cannot be empty")
            return
        }
        if (baseBranch.value.isBlank()) {
            _uiState.value = UiState.Error("Base branch cannot be empty")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = createPullRequestUseCase(
                CreatePullRequestParams(
                    owner = owner,
                    repo = repo,
                    title = title.value,
                    body = body.value.ifBlank { null },
                    head = headBranch.value,
                    base = baseBranch.value
                )
            )
            _uiState.value = result.fold(
                onSuccess = { UiState.Success("Pull request created successfully") },
                onFailure = { UiState.Error(it.message ?: "Failed to create pull request") }
            )
        }
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}
