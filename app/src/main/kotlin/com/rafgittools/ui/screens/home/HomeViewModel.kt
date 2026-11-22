package com.rafgittools.ui.screens.home

import android.os.Build
import android.content.Context
import com.rafgittools.bridge.PrivateGitHubTreeIndexGate
import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubDataRepository
import com.rafgittools.data.github.GithubOrganizationInventory
import com.rafgittools.bridge.CorpusIntakeResult
import com.rafgittools.bridge.PrivateProcessingReceiptGateV1
import com.rafgittools.domain.model.github.GithubUser
import com.rafgittools.navigator.ConversationManifoldPublication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
    private val _remoteListingComplete = MutableStateFlow(false)
    val remoteListingComplete: StateFlow<Boolean> = _remoteListingComplete.asStateFlow()

    private val _observedOrganizations = MutableStateFlow<List<String>>(emptyList())
    val observedOrganizations: StateFlow<List<String>> = _observedOrganizations.asStateFlow()

    // Statuses are scoped to the current PAT session, not a membership grant.
    private val _organizationStatus = MutableStateFlow("TOKEN_VAZIO_NOT_REQUESTED")
    val organizationStatus: StateFlow<String> = _organizationStatus.asStateFlow()
    private val _organizationDiscoveryComplete = MutableStateFlow(false)
    val organizationDiscoveryComplete: StateFlow<Boolean> = _organizationDiscoveryComplete.asStateFlow()

    private var authGeneration = 0L
    private var orgGeneration = 0L
    private var authHydrationJob: Job? = null
    private var repoListJob: Job? = null
    private var orgListJob: Job? = null
    private var requestedOrganization: String? = null

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

    private val _privateProcessingPublishState =
        MutableStateFlow<PrivateProcessingPublishState>(PrivateProcessingPublishState.Idle)
    val privateProcessingPublishState: StateFlow<PrivateProcessingPublishState> =
        _privateProcessingPublishState.asStateFlow()

    init {
        checkAuthAndLoadData()
        observeLocalRepositoryRegistry()
    }

    /**
     * Called by the Home navigation result after login or offline selection.
     * Rehydrate persisted credentials and fetch current data without Activity.recreate().
     * An ordinary repository refresh is insufficient when this VM was first created offline.
     */
    fun onAuthReturned() {
        checkAuthAndLoadData()
    }

    /** An authenticated session may own personal AND organization repositories. */
    private fun checkAuthAndLoadData() {
        val generation = ++authGeneration
        authHydrationJob?.cancel()
        repoListJob?.cancel()
        orgListJob?.cancel()
        ++orgGeneration
        _observedOrganizations.value = emptyList()
        _organizationDiscoveryComplete.value = false
        _organizationStatus.value = "TOKEN_VAZIO_NOT_REQUESTED"

        authHydrationJob = viewModelScope.launch {
            val offlineMode = authRepository.isOfflineMode()
            val authenticated = authRepository.isAuthenticated() && !offlineMode
            if (!isActive || generation != authGeneration) return@launch
            _isAuthenticated.value = authenticated

            if (authenticated) {
                val token = authRepository.getPat().getOrNull()
                if (!isActive || generation != authGeneration) return@launch
                if (token.isNullOrBlank()) {
                    authTokenCache.token = null
                    _isAuthenticated.value = false
                    _user.value = null
                    _remoteRepositories.value = emptyList()
                    _remoteListingComplete.value = false
                    _activeTab.value = HomeTab.DRIVE
                    _uiState.value = HomeUiState.Success
                } else {
                    authTokenCache.token = token
                    _activeTab.value = HomeTab.REMOTE
                    loadUserData()
                }
            } else {
                // Do not preserve an earlier identity or organization inventory.
                authTokenCache.token = null
                _user.value = null
                _remoteRepositories.value = emptyList()
                _remoteListingComplete.value = false
                _activeTab.value = HomeTab.DRIVE
                _uiState.value = HomeUiState.Success
                requestedOrganization = null
            }
            loadLocalRepositories()
        }
    }

    suspend fun buildPrivateGithubIndex(
        context: Context,
        repository: GithubRepoModel
    ): Result<PrivateGitHubTreeIndexGate.Snapshot> =
        withContext(Dispatchers.IO) {
            githubRepository.indexPrivateConversationRepository(context.applicationContext, repository)
        }

    /**
     * Page through the live /user/repos affiliation inventory. A cached page
     * must NEVER be treated as proof that organization listing is complete.
     */
    fun loadUserData() {
        repoListJob?.cancel()
        val generation = authGeneration
        repoListJob = viewModelScope.launch {
            if (!_isAuthenticated.value || authTokenCache.token.isNullOrBlank()) return@launch
            _uiState.value = HomeUiState.Loading
            _remoteListingComplete.value = false

            // Remote-only identity probe: don't silently use a cached login.
            val identity = githubRepository.probeAuthenticatedUserLive()
            if (!isActive || generation != authGeneration) return@launch
            _user.value = identity.getOrNull()

            val allRepos = mutableListOf<GithubRepoModel>()
            val perPage = 100
            var page = 1
            var terminalError = false
            var exhaustedPages = false
            while (page <= 50) {
                if (!isActive || generation != authGeneration) return@launch
                val result = githubRepository.getUserRepositoriesLivePage(page, perPage)
                if (!isActive || generation != authGeneration) return@launch
                val batch = result.getOrNull()
                if (batch == null) {
                    terminalError = true
                    break
                }
                allRepos += batch
                if (batch.size < perPage) {
                    exhaustedPages = true
                    break
                }
                page++
            }

            if (!isActive || generation != authGeneration) return@launch
            _remoteListingComplete.value = !terminalError && exhaustedPages
            _remoteRepositories.value = allRepos.distinctBy { it.id }
            // Keep the read-only organization lookup available if the global
            // inventory endpoint is unavailable. No empty list becomes PASS.
            _uiState.value = HomeUiState.Success
            if (allRepos.isEmpty() && terminalError) {
                _organizationStatus.value = "USER_REPOSITORY_LIST_UNAVAILABLE"
            }

            // A missing membership list is not proof that there are no orgs.
            val membership = githubRepository.getAuthenticatedOrganizationsLivePage(1)
            if (!isActive || generation != authGeneration) return@launch
            _observedOrganizations.value = membership.getOrNull() ?: emptyList()
            _organizationDiscoveryComplete.value =
                membership.isSuccess && _observedOrganizations.value.size < 100

            requestedOrganization?.let { loadOrganizationRepositories(it) }
        }
    }

    /**
     * Explicitly load an org's authorized public/private repository metadata.
     * This does not change GitHub visibility, access or any repository content.
     */
    fun loadOrganizationRepositories(organization: String) {
        val org = organization.trim()
        if (!GithubOrganizationInventory.isValidLogin(org)) {
            _organizationStatus.value = "INVALID_ORGANIZATION_LOGIN"
            return
        }
        if (!_isAuthenticated.value || authTokenCache.token.isNullOrBlank()) {
            _organizationStatus.value = "TOKEN_VAZIO_GITHUB_AUTH_REQUIRED"
            return
        }
        requestedOrganization = org
        orgListJob?.cancel()
        val lookupGeneration = ++orgGeneration
        val authSnapshot = authGeneration
        orgListJob = viewModelScope.launch {
            _organizationStatus.value = "ORG_LOADING"
            val orgRepos = mutableListOf<GithubRepoModel>()
            var complete = false
            var error = false
            for (page in 1..50) {
                if (!isActive || lookupGeneration != orgGeneration ||
                    authSnapshot != authGeneration || !_isAuthenticated.value) return@launch
                val result = githubRepository.getOrganizationRepositoriesLivePage(org, page)
                if (!isActive || lookupGeneration != orgGeneration ||
                    authSnapshot != authGeneration || !_isAuthenticated.value) return@launch
                val batch = result.getOrNull()
                if (batch == null) {
                    error = true
                    break
                }
                orgRepos += batch
                if (batch.size < 100) {
                    complete = true
                    break
                }
            }
            if (!isActive || lookupGeneration != orgGeneration ||
                authSnapshot != authGeneration || !_isAuthenticated.value) return@launch
            // On error keep prior observations in the same auth session, but
            // mark them partial; never convert a failed fetch into an empty proof.
            val previous = _remoteRepositories.value
            val remaining = if (complete) previous.filterNot {
                GithubOrganizationInventory.matchesOwner(org, it.owner.login)
            } else previous
            _remoteRepositories.value = (remaining + orgRepos).distinctBy { it.id }
            _organizationStatus.value = when {
                error -> "ORG_PARTIAL_OR_PERMISSION_BLOCKED"
                complete -> "ORG_AUTHORIZED_RESULTS_COMPLETE"
                else -> "ORG_PAGE_LIMIT_PARTIAL"
            }
        }
    }

    private fun observeLocalRepositoryRegistry() {
        viewModelScope.launch {
            localRepositoryDao.observeAll().collectLatest { entities ->
                val localRepos = mutableListOf<LocalRepoSummary>()
                entities
                    .map { File(it.path) }
                    .filter { it.exists() && it.isDirectory }
                    .forEach { dir ->
                        buildLocalRepoSummary(dir)?.let { localRepos.add(it) }
                    }
                _localRepositories.value = localRepos
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

    fun publishPrivateProcessingReceipt(
        target: GithubRepoModel,
        sourceBytes: Long,
        sourceSha256: String,
        sourceProviderAuthority: String,
        intake: CorpusIntakeResult
    ) {
        if (!_isAuthenticated.value) {
            _privateProcessingPublishState.value =
                PrivateProcessingPublishState.Failed("GitHub não autenticado")
            return
        }
        if (!target.isPrivate) {
            _privateProcessingPublishState.value =
                PrivateProcessingPublishState.Failed("Destino precisa estar privado")
            return
        }

        viewModelScope.launch {
            _privateProcessingPublishState.value = PrivateProcessingPublishState.Running
            val receipt = runCatching {
                PrivateProcessingReceiptGateV1.build(
                    sourceProviderAuthority = sourceProviderAuthority,
                    sourceSha256 = sourceSha256,
                    bytesRead = sourceBytes,
                    intake = intake,
                    destinationRepositoryFullName = target.fullName,
                    androidSdk = Build.VERSION.SDK_INT,
                    androidAbis = Build.SUPPORTED_ABIS.toList()
                )
            }.getOrElse { error ->
                _privateProcessingPublishState.value =
                    PrivateProcessingPublishState.Failed(
                        error.message ?: "Falha ao montar receipt privado"
                    )
                return@launch
            }

            val path = PrivateProcessingReceiptGateV1.destinationPath(receipt)
            val body = PrivateProcessingReceiptGateV1.toJson(receipt)
            githubRepository.createPrivateProcessingFile(
                owner = target.owner.login,
                repo = target.name,
                path = path,
                utf8Content = body,
                message = "receipt(private-processing): " + receipt.operationId,
                branch = target.defaultBranch
            ).onSuccess { result ->
                _privateProcessingPublishState.value =
                    PrivateProcessingPublishState.Passed(
                        operationId = receipt.operationId,
                        commitSha = result.commit.sha,
                        path = path
                    )
            }.onFailure { error ->
                _privateProcessingPublishState.value =
                    PrivateProcessingPublishState.Failed(
                        error.message ?: "Falha ao publicar receipt privado"
                    )
            }
        }
    }

    suspend fun publishConversationManifold(
        target: GithubRepoModel,
        plan: ConversationManifoldPublication.Plan,
        confirmedPlanSha256: String,
        resolver: ContentResolver
    ): Result<ConversationManifoldPublication.Published> = withContext(Dispatchers.IO) {
        if (!_isAuthenticated.value) return@withContext Result.failure(
            IllegalStateException("GitHub não autenticado")
        )
        if (!target.isPrivate) return@withContext Result.failure(
            SecurityException("O destino Git precisa ser privado")
        )
        if (plan.githubOwner != target.owner.login || plan.githubRepository != target.name) {
            return@withContext Result.failure(
                IllegalArgumentException("O plano não corresponde ao repositório privado selecionado")
            )
        }
        runCatching {
            ConversationManifoldPublication.publish(
                resolver = resolver,
                plan = plan,
                confirmedPlanSha256 = confirmedPlanSha256,
                githubWrite = { owner, repo, path, content, message ->
                    githubRepository.createPrivateProcessingFile(
                        owner = owner,
                        repo = repo,
                        path = path,
                        utf8Content = content,
                        message = message,
                        branch = target.defaultBranch
                    ).map { it.commit.sha }
                },
                githubRead = { owner, repo, path ->
                    githubRepository.readPrivateProcessingFile(
                        owner = owner,
                        repo = repo,
                        path = path,
                        branch = target.defaultBranch
                    )
                }
            )
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
        ++authGeneration
        ++orgGeneration
        repoListJob?.cancel()
        orgListJob?.cancel()
        authHydrationJob?.cancel()
        _observedOrganizations.value = emptyList()
        _organizationDiscoveryComplete.value = false
        _organizationStatus.value = "TOKEN_VAZIO_NOT_REQUESTED"
        requestedOrganization = null
        viewModelScope.launch {
            authRepository.logout()
            authTokenCache.token = null
            _isAuthenticated.value = false
            _user.value = null
            _remoteRepositories.value = emptyList()
            _remoteListingComplete.value = false
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

sealed class PrivateProcessingPublishState {
    object Idle : PrivateProcessingPublishState()
    object Running : PrivateProcessingPublishState()
    data class Passed(
        val operationId: String,
        val commitSha: String,
        val path: String
    ) : PrivateProcessingPublishState()
    data class Failed(val message: String) : PrivateProcessingPublishState()
}
