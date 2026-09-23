package com.rafgittools.ui.screens.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.FileContent
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.model.GitFile
import com.rafgittools.domain.model.GitTag
import com.rafgittools.workspace.ContextAddState
import com.rafgittools.workspace.ContextBroker
import com.rafgittools.workspace.ResourceRef
import com.rafgittools.workspace.ResourceVisibility
import com.rafgittools.workspace.WorkspaceSessionStore
import com.rafgittools.workspace.WorkspaceTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for file browser screen.
 *
 * Closes RG6: branch/tag ref selector — browse files at any ref without
 * checking out, using JGitService.listFiles(ref) and getFileContent(ref).
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val jGitService: JGitService,
    private val workspaceSessionStore: WorkspaceSessionStore,
    private val contextBroker: ContextBroker
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileBrowserUiState>(FileBrowserUiState.Loading)
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private val _files = MutableStateFlow<List<GitFile>>(emptyList())
    val files: StateFlow<List<GitFile>> = _files.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val breadcrumbs: StateFlow<List<Pair<String, String>>> = _breadcrumbs.asStateFlow()

    private val _selectedFile = MutableStateFlow<GitFile?>(null)
    val selectedFile: StateFlow<GitFile?> = _selectedFile.asStateFlow()

    private val _fileContent = MutableStateFlow<FileContent?>(null)
    val fileContent: StateFlow<FileContent?> = _fileContent.asStateFlow()

    // Branch/tag ref selector (closes RG6 — P33-20/21)
    private val _currentRef = MutableStateFlow("HEAD")
    val currentRef: StateFlow<String> = _currentRef.asStateFlow()

    private val _availableBranches = MutableStateFlow<List<GitBranch>>(emptyList())
    val availableBranches: StateFlow<List<GitBranch>> = _availableBranches.asStateFlow()

    private val _availableTags = MutableStateFlow<List<GitTag>>(emptyList())
    val availableTags: StateFlow<List<GitTag>> = _availableTags.asStateFlow()

    val workspaceSession = workspaceSessionStore.state
    val contextState = contextBroker.state

    private val _contextStatus = MutableStateFlow<String?>(null)
    val contextStatus: StateFlow<String?> = _contextStatus.asStateFlow()

    private var repoPath: String = ""

    fun loadRepository(path: String) {
        repoPath = path
        viewModelScope.launch {
            jGitService.getBranches(repoPath)
                .onSuccess { branches ->
                    _availableBranches.value = branches
                    val current = branches.firstOrNull { it.isCurrent }
                    if (current != null) _currentRef.value = current.shortName
                }
            jGitService.listTags(repoPath)
                .onSuccess { tags -> _availableTags.value = tags }
        }
        navigateTo("")
    }

    fun switchRef(ref: String) {
        _currentRef.value = ref
        _selectedFile.value = null
        _fileContent.value = null
        navigateTo("")
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            _uiState.value = FileBrowserUiState.Loading
            _currentPath.value = path
            _selectedFile.value = null
            _fileContent.value = null
            updateBreadcrumbs(path)

            jGitService.listFiles(repoPath, path, ref = _currentRef.value)
                .onSuccess { fileList ->
                    _files.value = fileList
                    _uiState.value = FileBrowserUiState.FileList
                }
                .onFailure { error ->
                    _uiState.value = FileBrowserUiState.Error(
                        error.message ?: "Failed to load files"
                    )
                }
        }
    }

    fun navigateUp() {
        val parentPath = _currentPath.value
            .trimEnd('/')
            .substringBeforeLast("/", "")
        navigateTo(parentPath)
    }

    fun openFile(file: GitFile) {
        if (file.isDirectory) {
            navigateTo(file.path)
            return
        }

        viewModelScope.launch {
            _uiState.value = FileBrowserUiState.Loading
            _selectedFile.value = file

            jGitService.getFileContent(repoPath, file.path, ref = _currentRef.value)
                .onSuccess { content ->
                    workspaceSessionStore.openTab(
                        resource = ResourceRef(
                            provider = "LOCAL_GIT",
                            repositoryOrCorpus = repoPath,
                            refOrGeneration = _currentRef.value,
                            pathOrLocator = file.path,
                            objectId = file.sha,
                            visibility = ResourceVisibility.LOCAL_ONLY
                        ),
                        title = file.name,
                        baseObjectId = file.sha
                    )
                    _fileContent.value = content
                    _uiState.value = FileBrowserUiState.FileView
                }
                .onFailure { error ->
                    _uiState.value = FileBrowserUiState.Error(
                        error.message ?: "Failed to load file content"
                    )
                }
        }
    }

    fun activateWorkspaceTab(tabId: String) {
        val tab = workspaceSessionStore.activateTab(tabId) ?: return
        loadWorkspaceTab(tab)
    }

    fun closeWorkspaceTab(tabId: String) {
        val wasActive = workspaceSessionStore.state.value.activeTabId == tabId
        val next = workspaceSessionStore.closeTab(tabId)
        if (!wasActive) return
        if (next == null) {
            closeFile()
        } else {
            loadWorkspaceTab(next)
        }
    }

    fun navigateWorkspaceBack() {
        workspaceSessionStore.navigateBack()?.let(::loadWorkspaceTab)
    }

    fun navigateWorkspaceForward() {
        workspaceSessionStore.navigateForward()?.let(::loadWorkspaceTab)
    }

    private fun loadWorkspaceTab(tab: WorkspaceTab) {
        val resource = tab.resource
        if (resource.provider != "LOCAL_GIT") {
            _uiState.value = FileBrowserUiState.Error(
                "Workspace provider not wired in FileBrowser: ${resource.provider}"
            )
            return
        }

        val targetRepoPath = resource.repositoryOrCorpus
        val targetRef = resource.refOrGeneration
        val targetPath = resource.pathOrLocator
        val targetParent = targetPath.substringBeforeLast("/", "")

        repoPath = targetRepoPath
        _currentRef.value = targetRef
        _currentPath.value = targetParent
        updateBreadcrumbs(targetParent)

        viewModelScope.launch {
            _uiState.value = FileBrowserUiState.Loading
            jGitService.getFileContent(targetRepoPath, targetPath, ref = targetRef)
                .onSuccess { content ->
                    _selectedFile.value = GitFile(
                        name = content.name,
                        path = content.path,
                        isDirectory = false,
                        size = content.size,
                        mode = "",
                        sha = resource.objectId
                    )
                    _fileContent.value = content
                    _uiState.value = FileBrowserUiState.FileView
                }
                .onFailure { error ->
                    _uiState.value = FileBrowserUiState.Error(
                        error.message ?: "Failed to reload workspace tab"
                    )
                }
        }
    }

    fun addCurrentFileToContext() {
        val tab = workspaceSessionStore.state.value.activeTab
            ?: return setContextStatus("TOKEN_VAZIO: no active workspace resource")
        val content = _fileContent.value
            ?: return setContextStatus("TOKEN_VAZIO: no loaded file content")

        val outcome = contextBroker.addText(
            resource = tab.resource,
            text = content.content,
            isBinary = content.isBinary
        )
        val message = when (outcome.state) {
            ContextAddState.ADDED -> "Context added"
            ContextAddState.ALREADY_PRESENT -> "Already in context"
            ContextAddState.REJECTED_EMPTY -> "Context rejected: empty file"
            ContextAddState.REJECTED_BINARY -> "Context rejected: binary file"
            ContextAddState.REJECTED_SEGMENT_LIMIT ->
                "Context rejected: file exceeds ${ContextBroker.MAX_SEGMENT_CHARS} characters"
            ContextAddState.REJECTED_TOTAL_LIMIT ->
                "Context rejected: bundle exceeds ${ContextBroker.MAX_TOTAL_CHARS} characters"
            ContextAddState.REJECTED_COUNT_LIMIT ->
                "Context rejected: maximum ${ContextBroker.MAX_SEGMENTS} segments"
        }
        setContextStatus(message)
    }

    fun clearContextStatus() {
        _contextStatus.value = null
    }

    private fun setContextStatus(message: String) {
        _contextStatus.value = message
    }

    fun closeFile() {
        _selectedFile.value = null
        _fileContent.value = null
        if (repoPath.isNotEmpty()) {
            navigateTo(_currentPath.value)
        } else {
            _uiState.value = FileBrowserUiState.FileList
        }
    }

    fun refresh() {
        if (_selectedFile.value != null) {
            _selectedFile.value?.let { openFile(it) }
        } else {
            navigateTo(_currentPath.value)
        }
    }

    private fun updateBreadcrumbs(path: String) {
        if (path.isEmpty()) {
            _breadcrumbs.value = emptyList()
            return
        }

        val parts = path.split("/")
        val crumbs = mutableListOf<Pair<String, String>>()
        var currentCrumbPath = ""

        for (part in parts) {
            if (part.isNotEmpty()) {
                currentCrumbPath = if (currentCrumbPath.isEmpty()) part else "$currentCrumbPath/$part"
                crumbs.add(part to currentCrumbPath)
            }
        }

        _breadcrumbs.value = crumbs
    }
}

sealed class FileBrowserUiState {
    object Loading : FileBrowserUiState()
    object FileList : FileBrowserUiState()
    object FileView : FileBrowserUiState()
    data class Error(val message: String) : FileBrowserUiState()
}
