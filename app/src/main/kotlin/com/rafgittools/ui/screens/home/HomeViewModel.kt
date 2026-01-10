package com.rafgittools.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.cache.RepositoryNameCache
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.domain.model.github.GithubUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel

/**
 * ViewModel for home screen
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubDataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _user = MutableStateFlow<GithubUser?>(null)
    val user: StateFlow<GithubUser?> = _user.asStateFlow()
    
    private val _repositories = MutableStateFlow<List<GithubRepoModel>>(emptyList())
    val repositories: StateFlow<List<GithubRepoModel>> = _repositories.asStateFlow()
    
    init {
        checkAuthAndLoadData()
    }
    
    private fun checkAuthAndLoadData() {
        viewModelScope.launch {
            _isAuthenticated.value = authRepository.isAuthenticated()
            if (_isAuthenticated.value) {
                loadUserData()
            } else {
                _uiState.value = HomeUiState.NotAuthenticated
            }
        }
    }
    
    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            // Load user info
            githubRepository.getAuthenticatedUser().first()
                .onSuccess { user ->
                    _user.value = user
                }
                .onFailure { error ->
                    // User info failed, but we can still show repos
                }
            
            // Load repositories
            githubRepository.getUserRepositories().first()
                .onSuccess { repos ->
                    _repositories.value = repos
                    _uiState.value = if (repos.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success
                    }
                }
                .onFailure { error ->
                    _uiState.value = HomeUiState.Error(
                        error.message ?: "Failed to load repositories"
                    )
                }
        }
    }
    
    fun refresh() {
        loadUserData()
    }
    
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isAuthenticated.value = false
            _user.value = null
            _repositories.value = emptyList()
            _uiState.value = HomeUiState.NotAuthenticated
        }
    }
}

/**
 * UI state for home screen
 */
sealed class HomeUiState {
    object Loading : HomeUiState()
    object NotAuthenticated : HomeUiState()
    object Empty : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
