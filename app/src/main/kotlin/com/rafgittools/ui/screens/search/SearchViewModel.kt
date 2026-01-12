package com.rafgittools.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the search screen
 */
@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _searchType = MutableStateFlow(SearchType.REPOSITORIES)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()
    
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()
    
    fun setQuery(query: String) {
        _query.value = query
    }
    
    fun setSearchType(type: SearchType) {
        _searchType.value = type
    }
    
    fun search() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // TODO: Implement actual search logic with GitHub API or local Git search
                // For now, simulate with empty results
                _results.value = emptyList()
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }
    
    fun clearResults() {
        _results.value = emptyList()
        _uiState.value = UiState.Idle
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
    
    enum class SearchType {
        REPOSITORIES,
        CODE,
        ISSUES,
        USERS
    }
    
    sealed class SearchResult {
        data class Repository(val name: String, val owner: String, val description: String?) : SearchResult()
        data class Code(val path: String, val repo: String, val snippet: String) : SearchResult()
        data class Issue(val title: String, val number: Int, val repo: String) : SearchResult()
        data class User(val login: String, val name: String?, val avatarUrl: String?) : SearchResult()
    }
}
