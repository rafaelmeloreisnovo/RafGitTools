package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.cache.RepositoryNameCache
import com.rafgittools.data.cache.RepositoryNameCacheDao
import com.rafgittools.rafgitfs.index.RafGitFsGithubIndexer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoryStorageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repositoryDao: RepositoryNameCacheDao,
    private val indexer: RafGitFsGithubIndexer
) : ViewModel() {

    val profileId: String = savedStateHandle.get<String>("profileId").orEmpty()

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    private val _status = MutableStateFlow(RafGitFsUiStatus())
    val status: StateFlow<RafGitFsUiStatus> = _status.asStateFlow()

    val repositories: StateFlow<List<RepositoryNameCache>> = combine(
        repositoryDao.getAllRepositoriesFlow(),
        query
    ) { repositories, currentQuery ->
        val normalized = currentQuery.trim()
        if (normalized.isEmpty()) repositories
        else repositories.filter {
            it.name.contains(normalized, ignoreCase = true) ||
                it.fullName.contains(normalized, ignoreCase = true) ||
                it.description.orEmpty().contains(normalized, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun setQuery(value: String) {
        query.value = value.take(120)
    }

    fun refresh() {
        if (profileId.isBlank()) {
            _status.value = RafGitFsUiStatus(
                RafGitFsUiEvidence.TOKEN_VAZIO,
                "TOKEN_VAZIO",
                "PROFILE_ID_MISSING"
            )
            return
        }
        viewModelScope.launch {
            _status.value = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, "Refreshing repositories")
            _status.value = indexer.refreshRepositories(profileId).toUiStatus()
        }
    }
}
