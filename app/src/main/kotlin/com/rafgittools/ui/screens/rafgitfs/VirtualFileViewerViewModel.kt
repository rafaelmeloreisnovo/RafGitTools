package com.rafgittools.ui.screens.rafgitfs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.rafgitfs.index.RafGitFsContentSnapshot
import com.rafgittools.rafgitfs.index.RafGitFsGithubIndexer
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VirtualFileViewerUiState(
    val status: RafGitFsUiStatus = RafGitFsUiStatus(),
    val snapshot: RafGitFsContentSnapshot? = null
)

@HiltViewModel
class VirtualFileViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val indexer: RafGitFsGithubIndexer
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

    fun load() {
        if (profileId.isBlank() || repositoryFullName.isBlank() || refName.isBlank() || path.isBlank()) {
            _uiState.value = VirtualFileViewerUiState(
                status = RafGitFsUiStatus(
                    RafGitFsUiEvidence.TOKEN_VAZIO,
                    "TOKEN_VAZIO",
                    "FILE_IDENTITY_INCOMPLETE"
                )
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = VirtualFileViewerUiState(
                status = RafGitFsUiStatus(RafGitFsUiEvidence.LOADING, "Reading blob")
            )
            when (val result = indexer.readContent(profileId, repositoryFullName, refName, path)) {
                is RafGitFsRemoteResult.Observed -> {
                    _uiState.value = VirtualFileViewerUiState(
                        status = result.toUiStatus(),
                        snapshot = result.value
                    )
                }
                is RafGitFsRemoteResult.TokenVazio -> {
                    _uiState.value = VirtualFileViewerUiState(
                        status = result.toUiStatus(),
                        snapshot = result.partialValue
                    )
                }
                else -> _uiState.value = VirtualFileViewerUiState(status = result.toUiStatus())
            }
        }
    }

    private fun decode(value: String?): String = value
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()
}
