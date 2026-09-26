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

    private val _githubProbeState =
        MutableStateFlow<GithubConnectivityState>(GithubConnectivityState.Idle)
    val githubProbeState: StateFlow<GithubConnectivityState> = _githubProbeState.asStateFlow()

    private val _remoteReceiptState =
        MutableStateFlow<RemoteReceiptState>(RemoteReceiptState.Idle)
    val remoteReceiptState: StateFlow<RemoteReceiptState> = _remoteReceiptState.asStateFlow()

    init {
        checkAuthAndLoadData()
    }

    /** GitHub is optional: Drive and Local remain usable without a GitHub session. */
    private fun checkAuthAndLoadData() {
        viewModelScope.launch {
            val offlineMode = authRepository.isOfflineMode()
            _isAuthenticated.value = authRepository.isAuthenticated() && !offlineMode

            if (_isAuthenticated.value) {
                val token = authRepository.getPat().getOrNull()
                if (token.isNullOrBlank()) {
                    authTokenCache.token = null
                    _isAuthenticated.value = false
                    _user.value = null
                    _remoteRepositories.value = emptyList()
                    _activeTab.value = HomeTab.DRIVE
                    _uiState.value = HomeUiState.Success
                } else {
                    authTokenCache.token = token
                    loadUserData()
                }
            } else {
                _user.value = null
                _remoteRepositories.value = emptyList()
                _activeTab.value = HomeTab.DRIVE
                _uiState.value = HomeUiState.Success
            }

            loadLocalRepositories()
        }
    }

    /** Load every GitHub page; never confuse the API's first 30 items with a full inventory. */
    fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            githubRepository.getAuthenticatedUserSync()
                .onSuccess { user -> _user.value = user }

            val allRepos = mutableListOf<GithubRepoModel>()
            val perPage = 100
            val maxPages = 50
            var page = 1
            var terminalError: Throwable? = null

            while (page <= maxPages) {
                val result = githubRepository.getUserRepositoriesSync(page = page, perPage = perPage)
                val batch = result.getOrNull()
                if (batch == null) {
                    terminalError = result.exceptionOrNull()
                    break
                }
                allRepos += batch
                if (batch.size < perPage) break
                page += 1
            }

            if (allRepos.isNotEmpty() || terminalError == null) {
                _remoteRepositories.value = allRepos.distinctBy { it.id }
                _uiState.value = HomeUiState.Success
            } else {
                _uiState.value = HomeUiState.Error(
                    terminalError?.message ?: "Failed to load repositories"
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
                _uiState.value = HomeUiState.Success
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
        } catch (_: Exception) {
            null
        }
    }

    fun setActiveTab(tab: HomeTab) {
        _activeTab.value = tab
    }

    /**
     * User-triggered live probe. This deliberately bypasses cache so PASS means
     * the GitHub API answered with the currently authenticated identity.
     */
    fun runGithubConnectivityTest() {
        if (!_isAuthenticated.value) {
            _githubProbeState.value = GithubConnectivityState.Failed("GitHub não autenticado")
            return
        }

        viewModelScope.launch {
            _githubProbeState.value = GithubConnectivityState.Running
            githubRepository.probeAuthenticatedUserLive()
                .onSuccess { liveUser ->
                    _githubProbeState.value = GithubConnectivityState.Passed(
                        login = liveUser.login,
                        loadedRepositories = _remoteRepositories.value.size
                    )
                }
                .onFailure { error ->
                    _githubProbeState.value = GithubConnectivityState.Failed(
                        error.message ?: "Falha no probe remoto do GitHub"
                    )
                }
        }
    }

    /**
     * Explicit remote-write probe. The receipt contains only bounded diagnostic
     * metadata; no token, path, corpus payload or local file content is sent.
     */
    fun createRemoteConnectivityReceipt() {
        if (!_isAuthenticated.value) {
            _remoteReceiptState.value = RemoteReceiptState.Failed("GitHub não autenticado")
            return
        }

        viewModelScope.launch {
            _remoteReceiptState.value = RemoteReceiptState.Running

            val liveUser = githubRepository.probeAuthenticatedUserLive().getOrElse { error ->
                _remoteReceiptState.value = RemoteReceiptState.Failed(
                    error.message ?: "Não foi possível validar a identidade remota"
                )
                return@launch
            }

            val epochMs = System.currentTimeMillis()
            val body = buildString {
                appendLine("RAFGITTOOLS_CONNECTIVITY_RECEIPT_V1")
                appendLine("source=RafGitTools Android")
                appendLine("github_identity=${liveUser.login}")
                appendLine("github_read=PASS")
                appendLine("github_write=requested_by_user")
                appendLine("loaded_repository_count=${_remoteRepositories.value.size}")
                appendLine("local_private_payload=NOT_UPLOADED")
                appendLine("drive_payload=NOT_UPLOADED")
                appendLine("epoch_ms=$epochMs")
                appendLine("claim_allowed=false")
            }

            githubRepository.createConnectivityReceiptIssue(
                owner = CONNECTIVITY_RECEIPT_OWNER,
                repo = CONNECTIVITY_RECEIPT_REPO,
                title = "RafGitTools connectivity receipt $epochMs",
                body = body
            ).onSuccess { issue ->
                _remoteReceiptState.value = RemoteReceiptState.Passed(
                    issueNumber = issue.number,
                    url = issue.htmlUrl
                )
            }.onFailure { error ->
                _remoteReceiptState.value = RemoteReceiptState.Failed(
                    error.message ?: "Falha ao criar receipt remoto"
                )
            }
        }
    }

    fun refresh() {
        if (_isAuthenticated.value) {
            loadUserData()
        } else {
            _uiState.value = HomeUiState.Success
        }
        loadLocalRepositories()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            authTokenCache.token = null
            _isAuthenticated.value = false
            _user.value = null
            _remoteRepositories.value = emptyList()
            _activeTab.value = HomeTab.DRIVE
            _uiState.value = HomeUiState.Success
            loadLocalRepositories()
        }
    }

    enum class HomeTab { REMOTE, DRIVE, LOCAL }

    companion object {
        private const val CONNECTIVITY_RECEIPT_OWNER = "rafaelmeloreisnovo"
        private const val CONNECTIVITY_RECEIPT_REPO = "RafGitTools"
    }
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


sealed class GithubConnectivityState {
    object Idle : GithubConnectivityState()
    object Running : GithubConnectivityState()
    data class Passed(val login: String, val loadedRepositories: Int) : GithubConnectivityState()
    data class Failed(val message: String) : GithubConnectivityState()
}

sealed class RemoteReceiptState {
    object Idle : RemoteReceiptState()
    object Running : RemoteReceiptState()
    data class Passed(val issueNumber: Int, val url: String) : RemoteReceiptState()
    data class Failed(val message: String) : RemoteReceiptState()
}
