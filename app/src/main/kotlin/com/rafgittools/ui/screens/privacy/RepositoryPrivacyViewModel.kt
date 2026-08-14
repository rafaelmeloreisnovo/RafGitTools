package com.rafgittools.ui.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.privacy.RepositoryPrivacyBulkResult
import com.rafgittools.data.privacy.RepositoryPrivacyCandidate
import com.rafgittools.data.privacy.RepositoryPrivacyManager
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

    fun toggleRepository(id: Long) {
        val state = _uiState.value
        val candidate = state.candidates.firstOrNull { it.id == id } ?: return
        if (!candidate.eligible || state.executing) return
        val selected = state.selectedIds.toMutableSet()
        if (!selected.add(id)) selected.remove(id)
        _uiState.value = state.copy(selectedIds = selected)
    }

    fun toggleOwner(owner: String) {
        val state = _uiState.value
        if (state.executing) return
        val ids = state.candidates.filter { it.ownerLogin == owner && it.eligible }.map { it.id }.toSet()
        if (ids.isEmpty()) return
        val selected = state.selectedIds.toMutableSet()
        if (ids.all { it in selected }) selected.removeAll(ids) else selected.addAll(ids)
        _uiState.value = state.copy(selectedIds = selected)
    }

    fun selectAllEligible() {
        val state = _uiState.value
        if (state.executing) return
        _uiState.value = state.copy(
            selectedIds = state.candidates.filter { it.eligible }.mapTo(linkedSetOf()) { it.id }
        )
    }

    fun clearSelection() {
        if (!_uiState.value.executing) _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun executeMakePrivate(confirmation: String) {
        val state = _uiState.value
        val selected = state.candidates.filter { it.id in state.selectedIds && it.eligible }
        if (selected.isEmpty() || state.executing) return
        val expected = confirmationPhrase(selected.size)
        if (confirmation.trim() != expected) {
            _uiState.value = state.copy(error = "Confirmation phrase does not match: $expected")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(executing = true, error = null)
            val result = manager.makePrivate(selected)
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
        fun confirmationPhrase(count: Int): String = "PRIVATIZAR $count"
    }
}

data class RepositoryPrivacyUiState(
    val loading: Boolean = true,
    val executing: Boolean = false,
    val requiresAuthentication: Boolean = false,
    val candidates: List<RepositoryPrivacyCandidate> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null,
    val lastResult: RepositoryPrivacyBulkResult? = null
)
