package com.rafgittools.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.domain.model.github.GithubUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubDataRepository,
    private val authTokenCache: AuthTokenCache,
    private val jGitService: JGitService,
    private val localRepositoryDao: LocalRepositoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _user = MutableStateFlow<GithubUser?>(null)
    val user: StateFlow<GithubUser?> = _user.asStateFlow()

    private val _remoteRepositories = MutableStateFlow<List<GithubRepoModel>>(emptyList())
    val remoteRepositories: StateFlow<List<GithubRepoModel>> = _remoteRepositories.asStateFlow()

    private val _localRepositories = MutableStateFlow<List<LocalRepoSummary>>(emptyList())
    val localRepositories: StateFlow<List<LocalRepoSummary>> = _localRepositories.asStateFlow()

    private val _activeTab = MutableStateFlow(HomeTab.REMOTE)
    val activeTab: StateFlow<HomeTab> = _activeTab.asStateFlow()

    init {
        checkAuthAndLoadData()
    }

    private fun checkAuthAndLoadData() {
        viewModelScope.launch {
            val offlineMode = authRepository.isOfflineMode()
            _isAuthenticated.value = authRepository.isAuthenticated() || offlineMode
            if (offlineMode) {
                _user.value = null
                _remoteRepositories.value = emptyList()
                loadLocalRepositories()
                return@launch
            }
            if (_isAuthenticated.value) {
                loadUserData()
            } else {
                _uiState.value = HomeUiState.NotAuthenticated
            }
            loadLocalRepositories()
        }
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            githubRepository.getAuthenticatedUserSync() // FIX L7
                .onSuccess { user -> _user.value = user }

            githubRepository.getUserRepositoriesSync() // FIX L9
                .onSuccess { repos ->
                    _remoteRepositories.value = repos
                    _uiState.value = if (repos.isEmpty() && _localRepositories.value.isEmpty()) {
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

    private fun loadLocalRepositories() {
        viewModelScope.launch {
            val localRepos = mutableListOf<LocalRepoSummary>()
            val knownEntities = localRepositoryDao.loadAll()
            val knownFiles = knownEntities
                .map { File(it.path) }
                .filter { it.exists() && it.isDirectory }
            knownFiles.forEach { dir ->
                buildLocalRepoSummary(dir)?.let { localRepos.add(it) }
            }
            _localRepositories.value = localRepos
            if (_uiState.value == HomeUiState.Loading && !_isAuthenticated.value) {
                _uiState.value = if (localRepos.isEmpty()) HomeUiState.Empty else HomeUiState.Success
            }
        }
    }

    private suspend fun buildLocalRepoSummary(dir: File): LocalRepoSummary? {
        return try {
            val repoPath = dir.absolutePath
            var currentBranch = "unknown"
            var lastCommitMsg = ""
            var lastCommitAuthor = ""

            jGitService.getBranches(repoPath).onSuccess { branches ->
                currentBranch = branches.firstOrNull { it.isCurrent }?.name ?: "unknown"
            }
            jGitService.getCommits(repoPath, branch = null, limit = 1).onSuccess { commits ->
                commits.firstOrNull()?.let { commit ->
                    lastCommitMsg = commit.message.lines().firstOrNull() ?: ""
                    lastCommitAuthor = commit.author.name
                }
            }
            LocalRepoSummary(
                name = dir.name,
                path = repoPath,
                currentBranch = currentBranch,
                lastCommitMessage = lastCommitMsg,
                lastCommitAuthor = lastCommitAuthor,
                lastModified = dir.lastModified()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun setActiveTab(tab: HomeTab) {
        _activeTab.value = tab
    }

    fun refresh() {
        loadUserData()
        loadLocalRepositories()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            authTokenCache.token = null
            _isAuthenticated.value = false
            _user.value = null
            _remoteRepositories.value = emptyList()
            _uiState.value = HomeUiState.NotAuthenticated
        }
    }

    enum class HomeTab { REMOTE, LOCAL }
}

data class LocalRepoSummary(
    val name: String,
    val path: String,
    val currentBranch: String,
    val lastCommitMessage: String,
    val lastCommitAuthor: String,
    val lastModified: Long
)

sealed class HomeUiState {
    object Loading : HomeUiState()
    object NotAuthenticated : HomeUiState()
    object Empty : HomeUiState()
    object Success : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
