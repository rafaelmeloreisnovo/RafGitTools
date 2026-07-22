package com.rafgittools.ui.screens.bisect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.bisect.BisectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BisectViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<BisectUiState>(BisectUiState.NotStarted)
    val uiState: StateFlow<BisectUiState> = _uiState.asStateFlow()

    private val _log = MutableStateFlow("")
    val log: StateFlow<String> = _log.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private var repoPath: String = ""

    fun load(path: String) {
        repoPath = path
        BisectManager.resetStateForTesting()
        _uiState.value = BisectUiState.NotStarted
    }

    fun start(good: String, bad: String) {
        if (good.isBlank() || bad.isBlank()) {
            _snackbar.value = "Both good and bad commits are required"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = BisectUiState.Loading
            BisectManager.start(good, bad, repoPath)
                .onSuccess {
                    _uiState.value = BisectUiState.Running
                    loadLog()
                }
                .onFailure { e ->
                    _uiState.value = BisectUiState.NotStarted
                    _snackbar.value = "Start failed: ${e.message}"
                }
        }
    }

    fun markGood(commit: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            BisectManager.markGood(commit, repoPath)
                .onSuccess { loadLog() }
                .onFailure { e -> _snackbar.value = "Mark good failed: ${e.message}" }
        }
    }

    fun markBad(commit: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            BisectManager.markBad(commit, repoPath)
                .onSuccess { loadLog() }
                .onFailure { e -> _snackbar.value = "Mark bad failed: ${e.message}" }
        }
    }

    fun skip(commit: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            BisectManager.skip(commit, repoPath)
                .onSuccess { loadLog() }
                .onFailure { e -> _snackbar.value = "Skip failed: ${e.message}" }
        }
    }

    fun finish() {
        viewModelScope.launch(Dispatchers.IO) {
            BisectManager.finish(repoPath)
                .onSuccess {
                    _uiState.value = BisectUiState.NotStarted
                    _log.value = ""
                    _snackbar.value = "Bisect reset — back to original branch"
                }
                .onFailure { e -> _snackbar.value = "Reset failed: ${e.message}" }
        }
    }

    private fun loadLog() {
        BisectManager.log(repoPath)
            .onSuccess { _log.value = it }
            .onFailure { /* log not critical */ }
    }

    fun snackbarShown() { _snackbar.value = null }
}

sealed class BisectUiState {
    object NotStarted : BisectUiState()
    object Loading : BisectUiState()
    object Running : BisectUiState()
}
