package com.rafgittools.ui.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.privacy.RepositoryPrivacyBulkResult
import com.rafgittools.data.privacy.RepositoryPrivacyCandidate
import com.rafgittools.data.privacy.RepositoryPrivacyManager
import com.rafgittools.data.privacy.PrivacyVisibilityTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RepositoryPrivacyViewModel @Inject constructor(
    private val manager: RepositoryPrivacyManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(RepositoryPrivacyUiState())
    val uiState: StateFlow<RepositoryPrivacyUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            if (!authRepository.isAuthenticated() || authRepository.isOfflineMode()) {
                _uiState.value = RepositoryPrivacyUiState(
                    loading = false,
                    requiresAuthentication = true
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            manager.loadInventory()
                .onSuccess { repositories ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        requiresAuthentication = false,
                        candidates = repositories,
                        selectedIds = emptySet()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = error.message ?: "Unable to load GitHub repository inventory"
                    )
                }
        }
    }

    /**
     * Account/organization checklist filters the inventory only; it does not change
     * GitHub permissions. null = all available owners; empty = none.
     */
    fun toggleOwnerFilter(owner: String) {
        val state = _uiState.value
        if (state.executing) return
        val available = state.candidates.mapTo(linkedSetOf()) { it.ownerLogin }
        if (owner !in available) return
        _uiState.value = state.copy(
            selectedOwners = PrivacyOwnerSelection.toggle(state.selectedOwners, available, owner),
            selectedIds = emptySet(),
            error = null
        )
    }

    fun selectAllOwners() {
        val state = _uiState.value
        if (!state.executing) _uiState.value =
            state.copy(selectedOwners = null, selectedIds = emptySet(), error = null)
    }

    fun clearAllOwners() {
        val state = _uiState.value
        if (!state.executing) _uiState.value =
            state.copy(selectedOwners = emptySet(), selectedIds = emptySet(), error = null)
    }

    /** Source and destination are mutually exclusive: same-state PATCH is impossible. */
    fun selectSource(source: PrivacyVisibilityTarget) {
        selectTarget(if (source == PrivacyVisibilityTarget.PUBLIC)
            PrivacyVisibilityTarget.PRIVATE else PrivacyVisibilityTarget.PUBLIC)
    }

    fun selectTarget(target: PrivacyVisibilityTarget) {
        val state = _uiState.value
        if (state.executing) return
        _uiState.value = state.copy(targetVisibility = target, selectedIds = emptySet(), error = null)
    }

    fun toggleRepository(id: Long) {
        val state = _uiState.value
        val candidate = state.candidates.firstOrNull { it.id == id } ?: return
        if (!state.isSelectable(candidate) || state.executing) return
        val selected = state.selectedIds.toMutableSet()
        if (!selected.add(id)) selected.remove(id)
        _uiState.value = state.copy(selectedIds = selected)
    }

    fun toggleOwner(owner: String) {
        val state = _uiState.value
        if (state.executing) return
        val ids = state.candidates.filter {
            it.ownerLogin == owner && state.isSelectable(it)
        }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        val selected = state.selectedIds.toMutableSet()
        if (ids.all { it in selected }) selected.removeAll(ids) else selected.addAll(ids)
        _uiState.value = state.copy(selectedIds = selected)
    }

    fun selectAllEligible() {
        val state = _uiState.value
        if (state.executing) return
        _uiState.value = state.copy(
            selectedIds = state.candidates.filter {
                state.isSelectable(it)
            }.mapTo(linkedSetOf()) { it.id }
        )
    }

    fun clearSelection() {
        if (!_uiState.value.executing) _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun executeChangeVisibility(confirmation: String) {
        val state = _uiState.value
        val selected = state.selectedCandidates()
        if (selected.isEmpty() || state.executing) return
        val expected = confirmationPhrase(selected.size, state.targetVisibility)
        if (confirmation.trim() != expected) {
            _uiState.value = state.copy(error = "Confirmation phrase does not match: $expected")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(executing = true, error = null)
            val result = manager.changeVisibility(selected, state.targetVisibility)
            _uiState.value = _uiState.value.copy(
                executing = false,
                selectedIds = emptySet(),
                lastResult = result
            )
            reloadAfterMutation(result)
        }
    }

    private suspend fun reloadAfterMutation(result: RepositoryPrivacyBulkResult) {
        manager.loadInventory()
            .onSuccess { repositories ->
                _uiState.value = _uiState.value.copy(
                    candidates = repositories,
                    loading = false,
                    lastResult = result
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    lastResult = result,
                    error = "Mutation finished, inventory refresh failed: ${error.message}"
                )
            }
    }

    companion object {
        fun confirmationPhrase(count: Int, target: PrivacyVisibilityTarget = PrivacyVisibilityTarget.PRIVATE): String =
            "${target.confirmationVerb} $count"
    }
}

data class RepositoryPrivacyUiState(
    val loading: Boolean = true,
    val executing: Boolean = false,
    val requiresAuthentication: Boolean = false,
    val candidates: List<RepositoryPrivacyCandidate> = emptyList(),
    val selectedOwners: Set<String>? = null,
    val targetVisibility: PrivacyVisibilityTarget = PrivacyVisibilityTarget.PRIVATE,
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val lastResult: RepositoryPrivacyBulkResult? = null
) {
    val sourceVisibility: PrivacyVisibilityTarget
        get() = if (targetVisibility == PrivacyVisibilityTarget.PRIVATE)
            PrivacyVisibilityTarget.PUBLIC else PrivacyVisibilityTarget.PRIVATE

    fun ownerIncluded(owner: String): Boolean = selectedOwners?.contains(owner) ?: true

    fun isSelectable(candidate: RepositoryPrivacyCandidate): Boolean =
        ownerIncluded(candidate.ownerLogin) && candidate.eligibleFor(targetVisibility)

    fun selectedCandidates(): List<RepositoryPrivacyCandidate> =
        candidates.filter { it.id in selectedIds && isSelectable(it) }
}

/** Pure selection function: independently testable without Android or GitHub network. */
internal object PrivacyOwnerSelection {
    fun toggle(
        current: Set<String>?,
        available: Set<String>,
        owner: String
    ): Set<String> {
        val next = (current ?: available).intersect(available).toMutableSet()
        if (owner !in available) return next
        if (!next.add(owner)) next.remove(owner)
        return next
    }
}
