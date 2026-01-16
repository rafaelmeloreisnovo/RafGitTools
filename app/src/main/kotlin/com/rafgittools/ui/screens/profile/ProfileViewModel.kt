package com.rafgittools.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubDataRepository
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
class ProfileViewModel @Inject constructor(
    private val githubDataRepository: GithubDataRepository
) : ViewModel() {
    
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
                val result = if (username.isBlank()) {
                    githubDataRepository.getAuthenticatedUserSync()
                } else {
                    githubDataRepository.getUser(username)
                }
                result.fold(
                    onSuccess = { user ->
                        _username.value = user.login
                        _name.value = user.name
                        _bio.value = user.bio
                        _publicRepos.value = user.publicRepos
                        _followers.value = user.followers
                        _following.value = user.following
                        _uiState.value = UiState.Success
                    },
                    onFailure = { error ->
                        _uiState.value = UiState.Error(error.message ?: "Failed to load profile")
                    }
                )
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
