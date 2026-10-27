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
                        selectedIds = emptySet(),
                        manualSelection = false
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
            manualSelection = false,
            error = null
        )
    }

    /** Wizard scope: one authorized owner or every authorized owner. */
    fun selectOwnerScope(owner: String?) {
        val state = _uiState.value
        if (state.executing) return
        val available = state.candidates.mapTo(linkedSetOf()) { it.ownerLogin }
        if (owner != null && owner !in available) return
        _uiState.value = state.copy(
            selectedOwners = owner?.let { setOf(it) },
            selectedIds = emptySet(),
            manualSelection = false,
            error = null
        )
    }

    fun selectAllOwners() {
        val state = _uiState.value
        if (!state.executing) _uiState.value =
            state.copy(selectedOwners = null, selectedIds = emptySet(), manualSelection = false, error = null)
    }

    fun clearAllOwners() {
        val state = _uiState.value
        if (!state.executing) _uiState.value =
            state.copy(selectedOwners = emptySet(), selectedIds = emptySet(), manualSelection = false, error = null)
    }

    /** Source and destination are mutually exclusive: same-state PATCH is impossible. */
    fun selectSource(source: PrivacyVisibilityTarget) {
        selectTarget(if (source == PrivacyVisibilityTarget.PUBLIC)
            PrivacyVisibilityTarget.PRIVATE else PrivacyVisibilityTarget.PUBLIC)
    }

    fun selectTarget(target: PrivacyVisibilityTarget) {
        val state = _uiState.value
        if (state.executing) return
        _uiState.value = state.copy(targetVisibility = target, selectedIds = emptySet(), manualSelection = false, error = null)
    }

    fun toggleRepository(id: Long) {
        val state = _uiState.value
        val candidate = state.candidates.firstOrNull { it.id == id } ?: return
        if (!state.isSelectable(candidate) || state.executing) return
        val selected = state.selectedCandidates().mapTo(linkedSetOf()) { it.id }
        if (!selected.add(id)) selected.remove(id)
        _uiState.value = state.copy(selectedIds = selected, manualSelection = true)
    }

    fun toggleOwner(owner: String) {
        val state = _uiState.value
        if (state.executing) return
        val ids = state.candidates.filter {
            it.ownerLogin == owner && state.isSelectable(it)
        }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        val selected = state.selectedCandidates().mapTo(linkedSetOf()) { it.id }
        if (ids.all { it in selected }) selected.removeAll(ids) else selected.addAll(ids)
        _uiState.value = state.copy(selectedIds = selected, manualSelection = true)
    }

    fun selectAllEligible() {
        val state = _uiState.value
        if (state.executing) return
        _uiState.value = state.copy(
            selectedIds = state.candidates.filter {
                state.isSelectable(it)
            }.mapTo(linkedSetOf()) { it.id },
            manualSelection = true
        )
    }

    fun clearSelection() {
        if (!_uiState.value.executing) _uiState.value = _uiState.value.copy(
            selectedIds = emptySet(), manualSelection = true
        )
    }

    fun useAutomaticSelection() {
        if (!_uiState.value.executing) _uiState.value = _uiState.value.copy(
            selectedIds = emptySet(), manualSelection = false, error = null
        )
    }

    fun executeChangeVisibility(
        confirmation: String,
        expectedRepositoryIds: Set<Long>? = null,
        expectedTarget: PrivacyVisibilityTarget? = null
    ) {
        val state = _uiState.value
        val selected = state.selectedCandidates()
        if (state.loading || state.requiresAuthentication || selected.isEmpty() || state.executing) return
        if ((expectedRepositoryIds != null && selected.mapTo(linkedSetOf()) { it.id } != expectedRepositoryIds) ||
            (expectedTarget != null && state.targetVisibility != expectedTarget)
        ) {
            _uiState.value = state.copy(error = "A seleção mudou. Revise o plano antes de confirmar.")
            return
        }
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
                manualSelection = false,
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
    val manualSelection: Boolean = false,
    val error: String? = null,
    val lastResult: RepositoryPrivacyBulkResult? = null
) {
    val sourceVisibility: PrivacyVisibilityTarget
        get() = if (targetVisibility == PrivacyVisibilityTarget.PRIVATE)
            PrivacyVisibilityTarget.PUBLIC else PrivacyVisibilityTarget.PRIVATE

    fun ownerIncluded(owner: String): Boolean = selectedOwners?.contains(owner) ?: true

    fun isSelectable(candidate: RepositoryPrivacyCandidate): Boolean =
        ownerIncluded(candidate.ownerLogin) && candidate.eligibleFor(targetVisibility)

    /** Automatic plan is preview-only until the user confirms the final dialog. */
    fun selectedCandidates(): List<RepositoryPrivacyCandidate> =
        candidates.filter { isSelectable(it) && (!manualSelection || it.id in selectedIds) }
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
