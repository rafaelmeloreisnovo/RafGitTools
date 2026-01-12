package com.rafgittools.ui.screens.releases

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReleasesViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _releases = MutableStateFlow<List<Release>>(emptyList())
    val releases: StateFlow<List<Release>> = _releases.asStateFlow()
    
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
    
    data class Release(
        val id: Long,
        val tagName: String,
        val name: String,
        val body: String?,
        val draft: Boolean,
        val prerelease: Boolean
    )
}
