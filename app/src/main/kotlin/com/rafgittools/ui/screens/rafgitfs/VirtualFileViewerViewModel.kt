package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.rafgitfs.cache.RafGitFsCacheIdentity
import com.rafgittools.rafgitfs.cache.RafGitFsCacheKeys
import com.rafgittools.rafgitfs.cache.RafGitFsCacheResult
import com.rafgittools.rafgitfs.cache.RafGitFsCacheState
import com.rafgittools.rafgitfs.cache.RafGitFsCachedContent
import com.rafgittools.rafgitfs.cache.RafGitFsOfflineCacheManager
import com.rafgittools.rafgitfs.cache.RafGitFsOfflineQueue
import com.rafgittools.rafgitfs.index.RafGitFsContentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VirtualFileViewerUiState(
    val status: RafGitFsUiStatus = RafGitFsUiStatus(),
    val snapshot: RafGitFsContentSnapshot? = null,
    val cacheState: RafGitFsCacheState = RafGitFsCacheState.REMOTE_ONLY,
    val cacheKey: String? = null,
    val pinnedOffline: Boolean = false,
    val queueDetail: String? = null
)

@HiltViewModel
class VirtualFileViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cacheManager: RafGitFsOfflineCacheManager,
    private val offlineQueue: RafGitFsOfflineQueue
) : ViewModel() {

    val profileId: String = decode(savedStateHandle.get<String>("profileId"))
    val repositoryFullName: String = decode(savedStateHandle.get<String>("repositoryFullName"))
    val refName: String = decode(savedStateHandle.get<String>("refName"))
    val path: String = RafGitFsUiPaths.fromRoute(decode(savedStateHandle.get<String>("path")))

    private val _uiState = MutableStateFlow(VirtualFileViewerUiState())
    val uiState: StateFlow<VirtualFileViewerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() = read(allowNetwork = true, pin = false, title = "Reading or caching blob")

    fun loadOfflineOnly() = read(allowNetwork = false, pin = false, title = "Reading offline copy")

    fun pinOffline() = read(allowNetwork = true, pin = true, title = "Pinning offline copy")

    fun unpin() {
        val key = _uiState.value.cacheKey ?: return
        viewModelScope.launch {
            updateFromCacheResult(cacheManager.setPinned(key, false), "Offline pin removed")
        }
    }

    fun removeCachedCopy() {
        val key = _uiState.value.cacheKey ?: return
        viewModelScope.launch {
            when (val result = cacheManager.remove(key)) {
                is RafGitFsCacheResult.Success -> _uiState.value = VirtualFileViewerUiState(
                    status = RafGitFsUiStatus(RafGitFsUiEvidence.OBSERVED, "Local copy removed"),
                    cacheState = RafGitFsCacheState.REMOTE_ONLY
                )
                is RafGitFsCacheResult.TokenVazio -> setTokenVazio(result.reason)
                is RafGitFsCacheResult.Failure -> setFailure(result.code, result.message)
            }
        }
    }

    fun enqueueOfflinePin() {
        if (!identityComplete()) return
        viewModelScope.launch {
            val id = offlineQueue.enqueue(profileId, repositoryFullName, refName, path, pinOffline = true)
            _uiState.value = _uiState.value.copy(
                status = RafGitFsUiStatus(RafGitFsUiEvidence.TOKEN_VAZIO, "Queued for network", "Local queue only"),
                queueDetail = id.take(12)
            )
        }
    }

    fun resumeOfflineQueue() {
        viewModelScope.launch {
            val report = offlineQueue.resume(profileId)
            _uiState.value = _uiState.value.copy(
                status = RafGitFsUiStatus(
                    if (report.completed > 0) RafGitFsUiEvidence.OBSERVED else RafGitFsUiEvidence.TOKEN_VAZIO,
                    "Offline queue checked",
                    "attempted=${report.attempted}; completed=${report.completed}; paused=${report.paused}; failed=${report.failed}"
                )
            )
            if (report.completed > 0) loadOfflineOnly()
        }
    }

    private fun read(allowNetwork: Boolean, pin: Boolean, title: String) {
        if (!identityComplete()) {
            setTokenVazio("FILE_IDENTITY_INCOMPLETE")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, title)
            )
            updateFromCacheResult(
                cacheManager.read(profileId, repositoryFullName, refName, path, allowNetwork, pin),
                if (pin) "Pinned offline" else if (allowNetwork) "Content available" else "Offline copy verified"
            )
        }
    }

    private fun updateFromCacheResult(
        result: RafGitFsCacheResult<RafGitFsCachedContent>,
        successTitle: String
    ) {
        when (result) {
            is RafGitFsCacheResult.Success -> {
                val content = result.value
                _uiState.value = VirtualFileViewerUiState(
                    status = RafGitFsUiStatus(
                        RafGitFsUiEvidence.OBSERVED,
                        successTitle,
                        "${content.state.name}; SHA-256 ${content.checksumSha256.take(12)}"
                    ),
                    snapshot = content.snapshot,
                    cacheState = content.state,
                    cacheKey = keyFor(content.snapshot),
                    pinnedOffline = content.pinned,
                    queueDetail = _uiState.value.queueDetail
                )
            }
            is RafGitFsCacheResult.TokenVazio -> setTokenVazio(result.reason)
            is RafGitFsCacheResult.Failure -> setFailure(result.code, result.message)
        }
    }

    private fun keyFor(snapshot: RafGitFsContentSnapshot): String = RafGitFsCacheKeys.key(
        RafGitFsCacheIdentity(profileId, repositoryFullName, refName, path, snapshot.blobSha)
    )

    private fun setTokenVazio(reason: String) {
        _uiState.value = _uiState.value.copy(
            status = RafGitFsUiStatus(RafGitFsUiEvidence.TOKEN_VAZIO, "TOKEN_VAZIO", reason)
        )
    }

    private fun setFailure(code: String, message: String) {
        _uiState.value = _uiState.value.copy(
            status = RafGitFsUiStatus(RafGitFsUiEvidence.ERROR, code, message)
        )
    }

    private fun identityComplete(): Boolean =
        profileId.isNotBlank() && repositoryFullName.isNotBlank() && refName.isNotBlank() && path.isNotBlank()

    private fun decode(value: String?): String = value
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()
}
