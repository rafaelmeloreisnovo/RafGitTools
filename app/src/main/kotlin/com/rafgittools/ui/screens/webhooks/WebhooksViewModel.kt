package com.rafgittools.ui.screens.webhooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.CreateWebhookRequest
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.github.GithubWebhook
import com.rafgittools.data.github.WebhookConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WebhooksViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<WebhooksUiState>(WebhooksUiState.Loading)
    val uiState: StateFlow<WebhooksUiState> = _uiState.asStateFlow()

    private val _webhooks = MutableStateFlow<List<GithubWebhook>>(emptyList())
    val webhooks: StateFlow<List<GithubWebhook>> = _webhooks.asStateFlow()

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private var owner: String = ""
    private var repo: String = ""

    fun load(owner: String, repo: String) {
        this.owner = owner
        this.repo = repo
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = WebhooksUiState.Loading
            runCatching { githubApiService.getWebhooks(owner, repo) }
                .onSuccess { list ->
                    _webhooks.value = list
                    _uiState.value = if (list.isEmpty()) WebhooksUiState.Empty else WebhooksUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = WebhooksUiState.Error(e.message ?: "Failed to load webhooks")
                }
        }
    }

    fun createWebhook(url: String, events: List<String>) {
        if (url.isBlank()) return
        viewModelScope.launch {
            val request = CreateWebhookRequest(
                events = events.ifEmpty { listOf("push") },
                config = WebhookConfig(url = url)
            )
            runCatching { githubApiService.createWebhook(owner, repo, request) }
                .onSuccess {
                    _snackbar.value = "Webhook created"
                    refresh()
                }
                .onFailure { e -> _snackbar.value = "Create failed: ${e.message}" }
        }
    }

    fun deleteWebhook(hookId: Long) {
        viewModelScope.launch {
            runCatching { githubApiService.deleteWebhook(owner, repo, hookId) }
                .onSuccess {
                    _snackbar.value = "Webhook deleted"
                    _webhooks.value = _webhooks.value.filter { it.id != hookId }
                    if (_webhooks.value.isEmpty()) _uiState.value = WebhooksUiState.Empty
                }
                .onFailure { e -> _snackbar.value = "Delete failed: ${e.message}" }
        }
    }

    fun pingWebhook(hookId: Long) {
        viewModelScope.launch {
            runCatching { githubApiService.pingWebhook(owner, repo, hookId) }
                .onSuccess { _snackbar.value = "Ping sent" }
                .onFailure { e -> _snackbar.value = "Ping failed: ${e.message}" }
        }
    }

    fun snackbarShown() { _snackbar.value = null }
}

sealed class WebhooksUiState {
    object Loading : WebhooksUiState()
    object Empty : WebhooksUiState()
    object Success : WebhooksUiState()
    data class Error(val message: String) : WebhooksUiState()
}
