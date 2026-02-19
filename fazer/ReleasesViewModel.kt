package com.rafgittools.ui.screens.releases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.GithubRelease
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReleasesViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _releases = MutableStateFlow<List<GithubRelease>>(emptyList())
    val releases: StateFlow<List<GithubRelease>> = _releases.asStateFlow()

    private var currentOwner = ""
    private var currentRepo = ""
    private var currentPage = 1
    private val perPage = 20

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    fun loadReleases(owner: String, repo: String) {
        currentOwner = owner
        currentRepo = repo
        currentPage = 1
        _releases.value = emptyList()
        fetchPage(1, reset = true)
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value) return
        fetchPage(currentPage + 1, reset = false)
    }

    fun refresh() {
        if (currentOwner.isBlank()) return
        loadReleases(currentOwner, currentRepo)
    }

    private fun fetchPage(page: Int, reset: Boolean) {
        viewModelScope.launch {
            if (reset) _uiState.value = UiState.Loading else _isLoadingMore.value = true
            try {
                val result = githubApiService.getReleases(
                    owner = currentOwner,
                    repo = currentRepo,
                    page = page,
                    perPage = perPage
                )
                val updated = if (reset) result else _releases.value + result
                _releases.value = updated
                _hasMore.value = result.size == perPage
                currentPage = page
                _uiState.value = if (updated.isEmpty()) UiState.Empty else UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load releases")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
