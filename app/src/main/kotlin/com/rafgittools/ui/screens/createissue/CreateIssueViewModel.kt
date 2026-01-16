package com.rafgittools.ui.screens.createissue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.usecase.github.CreateIssueParams
import com.rafgittools.domain.usecase.github.CreateIssueUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateIssueViewModel @Inject constructor(
    private val createIssueUseCase: CreateIssueUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    
    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()
    
    fun setTitle(title: String) {
        _title.value = title
    }
    
    fun setBody(body: String) {
        _body.value = body
    }
    
    fun createIssue(owner: String, repo: String) {
        if (title.value.isBlank()) {
            _uiState.value = UiState.Error("Issue title cannot be empty")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = createIssueUseCase(
                CreateIssueParams(
                    owner = owner,
                    repo = repo,
                    title = title.value,
                    body = body.value.ifBlank { null }
                )
            )
            _uiState.value = result.fold(
                onSuccess = { UiState.Success("Issue created successfully") },
                onFailure = { UiState.Error(it.message ?: "Failed to create issue") }
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
