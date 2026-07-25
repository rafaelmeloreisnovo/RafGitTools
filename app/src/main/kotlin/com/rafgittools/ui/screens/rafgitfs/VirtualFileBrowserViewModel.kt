package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.cache.RepositoryNameCacheDao
import com.rafgittools.rafgitfs.data.RepositoryRefDao
import com.rafgittools.rafgitfs.data.RepositoryRefEntity
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import com.rafgittools.rafgitfs.data.VirtualTreeEntryEntity
import com.rafgittools.rafgitfs.index.RafGitFsGithubIndexer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VirtualFileBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositoryNameCacheDao: RepositoryNameCacheDao,
    private val repositoryRefDao: RepositoryRefDao,
    private val virtualTreeDao: VirtualTreeDao,
    private val indexer: RafGitFsGithubIndexer
) : ViewModel() {

    val profileId: String = decode(savedStateHandle.get<String>("profileId"))
    val repositoryFullName: String = decode(savedStateHandle.get<String>("repositoryFullName"))

    private val _currentRef = MutableStateFlow(decode(savedStateHandle.get<String>("refName")))
    val currentRef: StateFlow<String> = _currentRef.asStateFlow()

    private val _currentPath = MutableStateFlow(
        RafGitFsUiPaths.fromRoute(decode(savedStateHandle.get<String>("path")))
    )
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _status = MutableStateFlow(RafGitFsUiStatus())
    val status: StateFlow<RafGitFsUiStatus> = _status.asStateFlow()

    val refs: StateFlow<List<RepositoryRefEntity>> = repositoryRefDao
        .observeForRepository(profileId, repositoryFullName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val children: StateFlow<List<VirtualTreeEntryEntity>> = combine(
        _currentRef,
        _currentPath
    ) { ref, path -> ref to path }
        .flatMapLatest { (ref, path) ->
            indexer.observeChildren(profileId, repositoryFullName, ref, path)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibleEntries: StateFlow<List<VirtualTreeEntryEntity>> = combine(
        children,
        _query
    ) { entries, search ->
        val normalized = search.trim()
        if (normalized.isEmpty()) entries
        else entries.filter {
            it.name.contains(normalized, ignoreCase = true) ||
                it.path.contains(normalized, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (_currentRef.value.isBlank() || _currentRef.value == "__default__") {
                _currentRef.value = repositoryNameCacheDao
                    .getRepositoryByFullName(repositoryFullName)
                    ?.defaultBranch
                    ?: "main"
            }
            refresh()
        }
    }

    fun setQuery(value: String) {
        _query.value = value.take(160)
    }

    fun navigateTo(path: String) {
        _currentPath.value = RafGitFsUiPaths.normalize(path)
        _query.value = ""
    }

    fun navigateUp(): Boolean {
        val current = _currentPath.value
        if (current.isEmpty()) return false
        _currentPath.value = RafGitFsUiPaths.parent(current)
        return true
    }

    fun selectRef(refName: String) {
        if (refName == _currentRef.value || refName.isBlank()) return
        _currentRef.value = refName
        _currentPath.value = ""
        _query.value = ""
        refreshTree()
    }

    fun refresh() {
        if (profileId.isBlank() || repositoryFullName.isBlank()) {
            _status.value = RafGitFsUiStatus(
                RafGitFsUiEvidence.TOKEN_VAZIO,
                "TOKEN_VAZIO",
                "PROFILE_OR_REPOSITORY_MISSING"
            )
            return
        }
        viewModelScope.launch {
            _status.value = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, "Refreshing refs")
            val refsResult = indexer.refreshRefs(profileId, repositoryFullName)
            _status.value = refsResult.toUiStatus()
            refreshTreeInternal()
        }
    }

    fun refreshTree() {
        viewModelScope.launch { refreshTreeInternal() }
    }

    private suspend fun refreshTreeInternal() {
        val ref = _currentRef.value
        if (ref.isBlank()) {
            _status.value = RafGitFsUiStatus(
                RafGitFsUiEvidence.TOKEN_VAZIO,
                "TOKEN_VAZIO",
                "REF_NOT_SELECTED"
            )
            return
        }
        _status.value = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, "Indexing $ref")
        _status.value = indexer.refreshTree(profileId, repositoryFullName, ref).toUiStatus()
    }

    fun toggleFavorite(entry: VirtualTreeEntryEntity) {
        viewModelScope.launch {
            virtualTreeDao.setFavorite(
                profileId = profileId,
                repositoryFullName = repositoryFullName,
                refName = _currentRef.value,
                path = entry.path,
                favorite = !entry.isFavorite
            )
        }
    }

    private fun decode(value: String?): String = value
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()
}
