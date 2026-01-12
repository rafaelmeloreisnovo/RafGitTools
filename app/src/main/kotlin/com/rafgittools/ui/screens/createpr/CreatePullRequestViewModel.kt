package com.rafgittools.ui.screens.createpr

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreatePullRequestViewModel @Inject constructor() : ViewModel() {
    
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
    
    fun createPullRequest() {
        // TODO: Implement PR creation
        _uiState.value = UiState.Success
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
