package com.rafgittools.ui.screens.lfs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.gitlfs.LfsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LfsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<LfsUiState>(LfsUiState.Loading)
    val uiState: StateFlow<LfsUiState> = _uiState.asStateFlow()

    private val _trackedPatterns = MutableStateFlow<List<String>>(emptyList())
    val trackedPatterns: StateFlow<List<String>> = _trackedPatterns.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private var repoPath: String = ""

    fun load(path: String) {
        repoPath = path
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = LfsUiState.Loading
            if (!LfsManager.isAvailable()) {
                _uiState.value = LfsUiState.NotAvailable
                return@launch
            }
            LfsManager.listTracked(repoPath)
                .onSuccess { patterns ->
                    _trackedPatterns.value = patterns
                    _uiState.value = if (patterns.isEmpty()) LfsUiState.Empty else LfsUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = LfsUiState.Error(e.message ?: "Failed to list tracked patterns")
                }
        }
    }

    fun install() {
        viewModelScope.launch(Dispatchers.IO) {
            LfsManager.install(repoPath)
                .onSuccess { _snackbar.value = "git lfs install succeeded" }
                .onFailure { e -> _snackbar.value = e.message }
            refresh()
        }
    }

    fun track(pattern: String) {
        if (pattern.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            /* LfsManager.track wraps `git lfs track "<pattern>"` */
            runCatching { LfsManager.track(pattern = pattern, repoPath = repoPath) }
                .onSuccess { result ->
                    result
                        .onSuccess { _snackbar.value = "Now tracking: $pattern" }
                        .onFailure { e -> _snackbar.value = e.message }
                }
                .onFailure { e -> _snackbar.value = e.message }
            refresh()
        }
    }

    fun fetch(remote: String = "origin") {
        viewModelScope.launch(Dispatchers.IO) {
            LfsManager.fetch(repoPath, remote)
                .onSuccess { _snackbar.value = "LFS fetch complete" }
                .onFailure { e -> _snackbar.value = e.message }
        }
    }

    fun pull(remote: String = "origin") {
        viewModelScope.launch(Dispatchers.IO) {
            LfsManager.pull(repoPath, remote)
                .onSuccess { _snackbar.value = "LFS pull complete" }
                .onFailure { e -> _snackbar.value = e.message }
        }
    }

    fun loadEnv(onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            LfsManager.env(repoPath)
                .onSuccess { onResult(it) }
                .onFailure { e -> _snackbar.value = e.message }
        }
    }

    fun snackbarShown() {
        _snackbar.value = null
    }
}

sealed class LfsUiState {
    object Loading : LfsUiState()
    object NotAvailable : LfsUiState()
    object Empty : LfsUiState()
    object Success : LfsUiState()
    data class Error(val message: String) : LfsUiState()
}
