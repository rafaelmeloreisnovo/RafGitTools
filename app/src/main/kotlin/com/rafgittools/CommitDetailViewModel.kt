package com.rafgittools.ui.screens.commits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitDiff
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommitDetailViewModel @Inject constructor(
    private val jGitService: JGitService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _commit = MutableStateFlow<GitCommit?>(null)
    val commit: StateFlow<GitCommit?> = _commit.asStateFlow()

    private val _diffs = MutableStateFlow<List<GitDiff>>(emptyList())
    val diffs: StateFlow<List<GitDiff>> = _diffs.asStateFlow()

    private val _changedFiles = MutableStateFlow<List<String>>(emptyList())
    val changedFiles: StateFlow<List<String>> = _changedFiles.asStateFlow()

    private var repoPath = ""
    private var commitSha = ""

    fun loadCommit(repoPath: String, sha: String) {
        this.repoPath = repoPath
        this.commitSha = sha
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Load commit history and find matching commit
                jGitService.getCommits(repoPath, branch = null, limit = 200)
                    .onSuccess { commits ->
                        val found = commits.firstOrNull { it.sha.startsWith(sha) || sha.startsWith(it.sha) }
                        if (found != null) {
                            _commit.value = found
                            // Load diffs for this commit
                            loadDiffsForCommit(repoPath, found.sha)
                        } else {
                            _uiState.value = UiState.Error("Commit $sha not found in history")
                        }
                    }
                    .onFailure { e ->
                        _uiState.value = UiState.Error(e.message ?: "Failed to load commit")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load commit")
            }
        }
    }

    private suspend fun loadDiffsForCommit(repoPath: String, sha: String) {
        // Use getDiffBetweenCommits to get the actual diff for this specific commit
        // "$sha^" resolves to the parent commit; for initial commits fall back to empty diff
        jGitService.getDiffBetweenCommits(repoPath, oldCommitSha = "$sha^", newCommitSha = sha)
            .onSuccess { diffs ->
                _diffs.value = diffs
                _changedFiles.value = diffs.map { it.newPath ?: it.oldPath ?: "" }.filter { it.isNotBlank() }
                _uiState.value = UiState.Success
            }
            .onFailure {
                // Diffs optional — commit info alone is enough
                _changedFiles.value = emptyList()
                _uiState.value = UiState.Success
            }
    }

    fun refresh() {
        if (repoPath.isNotBlank()) loadCommit(repoPath, commitSha)
    }

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
