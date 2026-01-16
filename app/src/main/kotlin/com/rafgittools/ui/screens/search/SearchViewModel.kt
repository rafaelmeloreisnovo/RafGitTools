package com.rafgittools.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
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
class SearchViewModel @Inject constructor(
    private val githubApiService: GithubApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private val _searchType = MutableStateFlow(SearchType.REPOSITORIES)
    val searchType: StateFlow<SearchType> = _searchType.asStateFlow()
    
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()
    
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()
    
    private var currentPage = 1
    private var totalCount = 0
    private var hasMore = true
    private val perPage = 30
    
    fun setQuery(query: String) {
        _query.value = query
    }
    
    fun setSearchType(type: SearchType) {
        if (_searchType.value == type) return
        _searchType.value = type
        clearResults()
    }
    
    fun search() {
        val trimmedQuery = _query.value.trim()
        if (trimmedQuery.isEmpty()) {
            clearResults()
            return
        }
        
        resetSearchState()
        _results.value = emptyList()
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val searchResults = fetchSearchResults(trimmedQuery, page = 1)
                _results.value = searchResults
                currentPage = 1
                hasMore = _results.value.size < totalCount
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }
    
    fun loadNextPage() {
        val trimmedQuery = _query.value.trim()
        if (trimmedQuery.isEmpty() || !hasMore || _isLoadingMore.value) return
        
        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                val nextPage = currentPage + 1
                val nextResults = fetchSearchResults(trimmedQuery, page = nextPage)
                val combinedResults = _results.value + nextResults
                _results.value = combinedResults
                currentPage = nextPage
                hasMore = combinedResults.size < totalCount
            } catch (e: Exception) {
                if (_results.value.isEmpty()) {
                    _uiState.value = UiState.Error(e.message ?: "Failed to load more results")
                }
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
    
    fun clearResults() {
        resetSearchState()
        _results.value = emptyList()
        _uiState.value = UiState.Idle
    }

    private fun resetSearchState() {
        currentPage = 1
        totalCount = 0
        hasMore = true
        _isLoadingMore.value = false
    }
    
    private suspend fun fetchSearchResults(query: String, page: Int): List<SearchResult> {
        return when (_searchType.value) {
            SearchType.REPOSITORIES -> {
                val response = githubApiService.searchRepositories(
                    query = query,
                    page = page,
                    perPage = perPage
                )
                totalCount = response.total_count
                response.items.map { repo ->
                    SearchResult.Repository(
                        name = repo.name,
                        owner = repo.owner.login,
                        description = repo.description
                    )
                }
            }
            SearchType.CODE -> {
                val response = githubApiService.searchCode(
                    query = query,
                    page = page,
                    perPage = perPage
                )
                totalCount = response.total_count
                response.items.map { code ->
                    SearchResult.Code(
                        path = code.path,
                        repo = code.repository.full_name,
                        snippet = code.name
                    )
                }
            }
            SearchType.ISSUES -> {
                val response = githubApiService.searchIssues(
                    query = query,
                    page = page,
                    perPage = perPage
                )
                totalCount = response.total_count
                response.items.map { issue ->
                    SearchResult.Issue(
                        title = issue.title,
                        number = issue.number,
                        repo = repositoryNameFromUrl(issue.repository_url)
                    )
                }
            }
            SearchType.USERS -> {
                val response = githubApiService.searchUsers(
                    query = query,
                    page = page,
                    perPage = perPage
                )
                totalCount = response.total_count
                response.items.map { user ->
                    SearchResult.User(
                        login = user.login,
                        name = null,
                        avatarUrl = user.avatar_url
                    )
                }
            }
        }
    }
    
    private fun repositoryNameFromUrl(url: String): String {
        val parts = url.trimEnd('/').split("/")
        return if (parts.size >= 2) {
            parts.takeLast(2).joinToString("/")
        } else {
            url
        }
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
