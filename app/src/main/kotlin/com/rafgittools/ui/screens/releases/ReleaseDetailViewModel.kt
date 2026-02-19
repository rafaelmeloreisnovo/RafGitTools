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
class ReleaseDetailViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _release = MutableStateFlow<GithubRelease?>(null)
    val release: StateFlow<GithubRelease?> = _release.asStateFlow()

    private var currentOwner = ""
    private var currentRepo = ""
    private var currentReleaseId = 0L

    fun loadRelease(owner: String, repo: String, releaseId: Long) {
        currentOwner = owner
        currentRepo = repo
        currentReleaseId = releaseId
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = githubApiService.getRelease(owner, repo, releaseId)
                _release.value = result
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load release")
            }
        }
    }

    fun refresh() {
        if (currentOwner.isBlank()) return
        loadRelease(currentOwner, currentRepo, currentReleaseId)
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
