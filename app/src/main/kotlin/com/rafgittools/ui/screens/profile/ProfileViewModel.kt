package com.rafgittools.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the user profile screen
 */
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()
    
    private val _name = MutableStateFlow<String?>(null)
    val name: StateFlow<String?> = _name.asStateFlow()
    
    private val _bio = MutableStateFlow<String?>(null)
    val bio: StateFlow<String?> = _bio.asStateFlow()
    
    private val _publicRepos = MutableStateFlow(0)
    val publicRepos: StateFlow<Int> = _publicRepos.asStateFlow()
    
    private val _followers = MutableStateFlow(0)
    val followers: StateFlow<Int> = _followers.asStateFlow()
    
    private val _following = MutableStateFlow(0)
    val following: StateFlow<Int> = _following.asStateFlow()
    
    fun loadProfile(username: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                _username.value = username
                // TODO: Implement actual profile loading from GitHub API
                // For now, set placeholder values
                _name.value = null
                _bio.value = null
                _publicRepos.value = 0
                _followers.value = 0
                _following.value = 0
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
    
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
