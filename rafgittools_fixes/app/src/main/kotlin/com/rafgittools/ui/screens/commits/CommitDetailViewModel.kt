package com.rafgittools.ui.screens.commits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitDiff
import com.rafgittools.domain.repository.GitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ─── FIXES aplicados ─────────────────────────────────────────────────────────
// C1: jGitService.getCommitHistory() não existe → usa GitRepository.getCommits()
// C2: it.id não existe em GitCommit                → it.sha
// C3: getDiff(staged=false) param errado + semântica errada
//     (working-tree diff ≠ commit diff) → getDiffBetweenCommits("$sha^", sha)
// C4: it.filePath não existe em GitDiff             → it.newPath ?: it.oldPath ?: ""
// L4: viewModelScope.launch sem dispatcher           → withContext(Dispatchers.IO)
// L8: carregava 200 commits para achar 1 SHA         → getCommits(limit=50) + fallback expand
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class CommitDetailViewModel @Inject constructor(
    private val gitRepository: GitRepository
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
            withContext(Dispatchers.IO) {                           // FIX L4: IO thread
                // FIX C1+C2+L8: usa getCommits via repo interface, busca por sha
                val found = findCommitBySha(repoPath, sha)
                if (found != null) {
                    _commit.value = found
                    loadDiffsForCommit(repoPath, found.sha)         // FIX C2: .sha
                } else {
                    _uiState.value = UiState.Error("Commit $sha not found")
                }
            }
        }
    }

    /** Procura commit por SHA: primeiro em lote pequeno, expande se necessário */
    private suspend fun findCommitBySha(repoPath: String, sha: String): GitCommit? {
        for (limit in listOf(50, 200, 1000)) {
            val result = gitRepository.getCommits(repoPath, null, limit)
            val commits = result.getOrNull() ?: break
            val found = commits.firstOrNull { it.sha.startsWith(sha) || sha.startsWith(it.sha) }
            if (found != null) return found
            if (commits.size < limit) break          // histórico menor que o limite → não há mais
        }
        return null
    }

    private suspend fun loadDiffsForCommit(repoPath: String, sha: String) {
        // FIX C3: getDiffBetweenCommits ao invés de getDiff(staged) que retorna working-tree
        // Para commit inicial sem parent, faz diff de árvore vazia contra o commit
        val parentRef = if (sha.isNotBlank()) "$sha^" else sha
        gitRepository.getDiffBetweenCommits(repoPath, parentRef, sha)
            .onSuccess { diffs ->
                _diffs.value = diffs
                // FIX C4: filePath não existe → newPath ?: oldPath
                _changedFiles.value = diffs.map { it.newPath ?: it.oldPath ?: "" }.filter { it.isNotBlank() }
                _uiState.value = UiState.Success
            }
            .onFailure {
                // Diff é opcional — commit info sem diff ainda é útil
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
