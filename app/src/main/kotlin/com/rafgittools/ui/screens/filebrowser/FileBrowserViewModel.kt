package com.rafgittools.ui.screens.filebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.git.JGitService
import com.rafgittools.domain.model.FileContent
import com.rafgittools.domain.model.GitFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for file browser screen
 */
@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val jGitService: JGitService
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
    
    private var repoPath: String = ""
    
    fun loadRepository(path: String) {
        repoPath = path
        navigateTo("")
    }
    
    fun navigateTo(path: String) {
        viewModelScope.launch {
            _uiState.value = FileBrowserUiState.Loading
            _currentPath.value = path
            _selectedFile.value = null
            _fileContent.value = null
            updateBreadcrumbs(path)
            
            jGitService.listFiles(repoPath, path)
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
            
            jGitService.getFileContent(repoPath, file.path)
                .onSuccess { content ->
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
    
    fun closeFile() {
        _selectedFile.value = null
        _fileContent.value = null
        _uiState.value = FileBrowserUiState.FileList
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

/**
 * UI state for file browser screen
 */
sealed class FileBrowserUiState {
    object Loading : FileBrowserUiState()
    object FileList : FileBrowserUiState()
    object FileView : FileBrowserUiState()
    data class Error(val message: String) : FileBrowserUiState()
}
