package com.rafgittools.ui.screens.notifications

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
    
    data class Notification(
        val id: String,
        val title: String,
        val body: String,
        val repo: String,
        val unread: Boolean
    )
}
