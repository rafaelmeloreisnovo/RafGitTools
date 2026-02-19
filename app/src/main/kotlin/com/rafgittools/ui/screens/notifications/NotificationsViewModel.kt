package com.rafgittools.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.domain.model.github.GithubNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _notifications = MutableStateFlow<List<GithubNotification>>(emptyList())
    val notifications: StateFlow<List<GithubNotification>> = _notifications.asStateFlow()

    private val _showAll = MutableStateFlow(false)
    val showAll: StateFlow<Boolean> = _showAll.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = githubApiService.getNotifications(
                    all = _showAll.value,
                    participating = false,
                    page = 1,
                    perPage = 50
                )
                _notifications.value = result
                _unreadCount.value = result.count { it.unread }
                _uiState.value = if (result.isEmpty()) UiState.Empty else UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load notifications")
            }
        }
    }

    fun toggleShowAll() {
        _showAll.value = !_showAll.value
        loadNotifications()
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                githubApiService.markNotificationRead(notificationId)
                // Update local state optimistically
                _notifications.value = _notifications.value.map { n ->
                    if (n.id == notificationId) {
                        // GithubNotification is a data class — copy with unread=false
                        n.copy(unread = false)
                    } else n
                }
                _unreadCount.value = _notifications.value.count { it.unread }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to mark as read: ${e.message}")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                githubApiService.markAllNotificationsRead(
                    com.rafgittools.data.github.MarkNotificationsReadRequest(read = true)
                )
                _notifications.value = _notifications.value.map { it.copy(unread = false) }
                _unreadCount.value = 0
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to mark all as read: ${e.message}")
            }
        }
    }

    fun refresh() = loadNotifications()

    sealed class UiState {
        object Loading : UiState()
        object Empty : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
}
