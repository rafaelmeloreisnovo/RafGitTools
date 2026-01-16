package com.rafgittools.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.github.GithubDataRepository
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
    private val githubDataRepository: GithubDataRepository,
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
    
    fun setQuery(query: String) {
        _query.value = query
    }
    
    fun setSearchType(type: SearchType) {
        _searchType.value = type
    }
    
    fun search() {
        viewModelScope.launch {
            if (query.value.isBlank()) {
                _uiState.value = UiState.Error("Search query cannot be empty")
                _results.value = emptyList()
                return@launch
            }
            _uiState.value = UiState.Loading
            try {
                _results.value = when (searchType.value) {
                    SearchType.REPOSITORIES -> {
                        val result = githubDataRepository.searchRepositories(query.value)
                        result.getOrThrow().map { repo ->
                            SearchResult.Repository(
                                name = repo.name,
                                owner = repo.owner.login,
                                description = repo.description
                            )
                        }
                    }
                    SearchType.CODE -> {
                        val response = githubApiService.searchCode(query.value)
                        response.items.map { codeItem ->
                            SearchResult.Code(
                                path = codeItem.path,
                                repo = codeItem.repository.fullName,
                                snippet = codeItem.textMatches?.firstOrNull()?.fragment
                                    ?: "Snippet unavailable"
                            )
                        }
                    }
                    SearchType.ISSUES -> {
                        val response = githubApiService.searchIssues(query.value)
                        response.items.map { issue ->
                            SearchResult.Issue(
                                title = issue.title,
                                number = issue.number,
                                repo = extractRepoFromUrl(issue.htmlUrl)
                            )
                        }
                    }
                    SearchType.USERS -> {
                        val result = githubDataRepository.searchUsers(query.value)
                        result.getOrThrow().map { user ->
                            SearchResult.User(
                                login = user.login,
                                name = user.name,
                                avatarUrl = user.avatarUrl
                            )
                        }
                    }
                }
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

    private fun extractRepoFromUrl(htmlUrl: String): String {
        val normalized = htmlUrl.removePrefix("https://github.com/")
        val parts = normalized.split("/")
        return if (parts.size >= 2) {
            "${parts[0]}/${parts[1]}"
        } else {
            "Unknown repository"
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
