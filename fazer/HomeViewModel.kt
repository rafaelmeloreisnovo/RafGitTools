package com.rafgittools.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.model.github.GithubUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubDataRepository,
    private val jGitService: JGitService
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _user = MutableStateFlow<GithubUser?>(null)
    val user: StateFlow<GithubUser?> = _user.asStateFlow()

    // GitHub remote repositories
    private val _remoteRepositories = MutableStateFlow<List<GithubRepoModel>>(emptyList())
    val remoteRepositories: StateFlow<List<GithubRepoModel>> = _remoteRepositories.asStateFlow()

    // Local cloned repositories (missing in original)
    private val _localRepositories = MutableStateFlow<List<LocalRepoSummary>>(emptyList())
    val localRepositories: StateFlow<List<LocalRepoSummary>> = _localRepositories.asStateFlow()

    private val _activeTab = MutableStateFlow(HomeTab.REMOTE)
    val activeTab: StateFlow<HomeTab> = _activeTab.asStateFlow()

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
            // Always load local repos regardless of auth state
            loadLocalRepositories()
        }
    }

    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val userDeferred = async {
                githubRepository.getAuthenticatedUser().first()
                    .onSuccess { _user.value = it }
            }
            val reposDeferred = async {
                githubRepository.getUserRepositories().first()
                    .onSuccess { repos ->
                        _remoteRepositories.value = repos
                    }
                    .onFailure { return@async it }
                null
            }

            userDeferred.await()
            val repoError = reposDeferred.await()

            if (repoError != null) {
                _uiState.value = HomeUiState.Error(repoError.message ?: "Failed to load repositories")
            } else if (_remoteRepositories.value.isEmpty() && _localRepositories.value.isEmpty()) {
                _uiState.value = HomeUiState.Empty
            } else {
                _uiState.value = HomeUiState.Success
            }
        }
    }

    /**
     * Scan device storage for local Git repositories.
     * Checks standard cloning locations used by the app.
     */
    private fun loadLocalRepositories() {
        viewModelScope.launch {
            val localRepos = mutableListOf<LocalRepoSummary>()
            val searchRoots = listOf(
                File("/sdcard/RafGitTools"),      // Default clone dir
                File("/sdcard/git"),               // Common user dir
                File("/storage/emulated/0/RafGitTools")
            )
            searchRoots.forEach { root ->
                if (root.exists() && root.isDirectory) {
                    root.listFiles()?.forEach { dir ->
                        if (dir.isDirectory && File(dir, ".git").exists()) {
                            val summary = buildLocalRepoSummary(dir)
                            if (summary != null) localRepos.add(summary)
                        }
                    }
                }
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
            jGitService.getCommitHistory(repoPath, maxCount = 1).onSuccess { commits ->
                commits.firstOrNull()?.let { commit ->
                    lastCommitMsg = commit.message.lines().firstOrNull() ?: ""
                    lastCommitAuthor = commit.authorName
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
