package com.rafgittools.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.domain.model.github.GithubEvent
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private val _repositories = MutableStateFlow<List<GithubRepoModel>>(emptyList())
    val repositories: StateFlow<List<GithubRepoModel>> = _repositories.asStateFlow()

    private val _starred = MutableStateFlow<List<GithubRepoModel>>(emptyList())
    val starred: StateFlow<List<GithubRepoModel>> = _starred.asStateFlow()

    private val _events = MutableStateFlow<List<GithubEvent>>(emptyList())
    val events: StateFlow<List<GithubEvent>> = _events.asStateFlow()

    private val _tabLoading = MutableStateFlow(false)
    val tabLoading: StateFlow<Boolean> = _tabLoading.asStateFlow()

    private val loadedTabs = mutableSetOf<Int>()

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
                        loadedTabs.clear()
                        loadTab(0, user.login)
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

    fun loadTab(tab: Int, username: String) {
        if (loadedTabs.contains(tab)) return
        viewModelScope.launch {
            _tabLoading.value = true
            when (tab) {
                0 -> githubDataRepository.getUserRepositoriesByUsername(username).fold(
                    onSuccess = { _repositories.value = it },
                    onFailure = {}
                )
                1 -> githubDataRepository.getUserStarred(username).fold(
                    onSuccess = { _starred.value = it },
                    onFailure = {}
                )
                2 -> githubDataRepository.getUserEvents(username).fold(
                    onSuccess = { _events.value = it },
                    onFailure = {}
                )
            }
            loadedTabs.add(tab)
            _tabLoading.value = false
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
