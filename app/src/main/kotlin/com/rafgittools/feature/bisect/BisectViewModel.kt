package com.rafgittools.feature.bisect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.vcs.BisectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BisectCommit(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val date: String,
    val status: BisectStatus = BisectStatus.UNKNOWN
)

enum class BisectStatus {
    UNKNOWN, GOOD, BAD, SKIPPED
}

@HiltViewModel
class BisectViewModel @Inject constructor(
    private val bisectManager: BisectManager
) : ViewModel() {

    data class BisectState(
        val isInSession: Boolean = false,
        val candidates: List<BisectCommit> = emptyList(),
        val currentCommit: BisectCommit? = null,
        val goodCommits: List<BisectCommit> = emptyList(),
        val badCommits: List<BisectCommit> = emptyList(),
        val skippedCommits: List<BisectCommit> = emptyList(),
        val estimatedRemaining: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val bisectResult: String? = null
    )

    sealed class BisectEffect {
        data class ShowToast(val message: String) : BisectEffect()
        object BisectStarted : BisectEffect()
        object BisectEnded : BisectEffect()
        object CommitMarked : BisectEffect()
    }

    private val _state = MutableStateFlow(BisectState())
    val state: StateFlow<BisectState> = _state.asStateFlow()

    private val _effects = MutableStateFlow<BisectEffect?>(null)
    val effects: StateFlow<BisectEffect?> = _effects.asStateFlow()

    fun startBisect(goodCommitHash: String, badCommitHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = bisectManager.startBisect(goodCommitHash, badCommitHash)

            result.onSuccess { commits ->
                _state.value = _state.value.copy(
                    isInSession = true,
                    isLoading = false,
                    candidates = commits,
                    currentCommit = commits.firstOrNull(),
                    estimatedRemaining = calculateRemaining(commits.size)
                )
                _effects.value = BisectEffect.BisectStarted
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to start bisect: ${exception.message}"
                )
            }
        }
    }

    fun markCommitGood(commitHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = bisectManager.markCommitGood(commitHash)

            result.onSuccess { nextCommit ->
                if (nextCommit != null) {
                    val goodCommits = _state.value.goodCommits + (
                        _state.value.currentCommit?.copy(status = BisectStatus.GOOD)
                            ?: return@launch
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentCommit = nextCommit,
                        goodCommits = goodCommits,
                        estimatedRemaining = calculateRemaining(_state.value.candidates.size)
                    )
                } else {
                    endBisect()
                }
                _effects.value = BisectEffect.CommitMarked
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to mark commit: ${exception.message}"
                )
            }
        }
    }

    fun markCommitBad(commitHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = bisectManager.markCommitBad(commitHash)

            result.onSuccess { nextCommit ->
                if (nextCommit != null) {
                    val badCommits = _state.value.badCommits + (
                        _state.value.currentCommit?.copy(status = BisectStatus.BAD)
                            ?: return@launch
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentCommit = nextCommit,
                        badCommits = badCommits,
                        estimatedRemaining = calculateRemaining(_state.value.candidates.size)
                    )
                } else {
                    endBisect()
                }
                _effects.value = BisectEffect.CommitMarked
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to mark commit: ${exception.message}"
                )
            }
        }
    }

    fun skipCommit(commitHash: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = bisectManager.skipCommit(commitHash)

            result.onSuccess { nextCommit ->
                if (nextCommit != null) {
                    val skippedCommits = _state.value.skippedCommits + (
                        _state.value.currentCommit?.copy(status = BisectStatus.SKIPPED)
                            ?: return@launch
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        currentCommit = nextCommit,
                        skippedCommits = skippedCommits,
                        estimatedRemaining = calculateRemaining(_state.value.candidates.size)
                    )
                } else {
                    endBisect()
                }
                _effects.value = BisectEffect.CommitMarked
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to skip commit: ${exception.message}"
                )
            }
        }
    }

    fun endBisect() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val result = bisectManager.endBisect()

            result.onSuccess { resultHash ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    isInSession = false,
                    bisectResult = resultHash,
                    successMessage = "Bisect complete. First bad commit: $resultHash"
                )
                _effects.value = BisectEffect.BisectEnded
            }.onFailure { exception ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to end bisect: ${exception.message}"
                )
            }
        }
    }

    fun resetBisect() {
        viewModelScope.launch(Dispatchers.IO) {
            bisectManager.resetBisect()
            _state.value = BisectState()
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearEffect() {
        _effects.value = null
    }

    private fun calculateRemaining(total: Int): Int {
        return if (total > 0) {
            kotlin.math.ceil(kotlin.math.log(total.toDouble(), 2.0)).toInt()
        } else {
            0
        }
    }
}
