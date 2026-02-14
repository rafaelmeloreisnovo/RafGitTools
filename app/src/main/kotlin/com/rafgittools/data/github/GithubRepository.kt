package com.rafgittools.data.github

import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.cache.AsyncCacheManager
import com.rafgittools.data.cache.RepositoryNameCache
import com.rafgittools.domain.error.toAppError
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.domain.model.github.GithubSearchIssue
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel
import com.rafgittools.domain.model.github.GithubUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for GitHub operations with async caching
 * 
 * Provides access to GitHub API with automatic caching of responses.
 * Uses Flow for async data delivery and cache updates.
 */
@Singleton
class GithubDataRepository @Inject constructor(
    private val githubApiService: GithubApiService,
    private val authRepository: AuthRepository,
    private val cacheManager: AsyncCacheManager
) {
    
    /**
     * Get current authenticated user
     * 
     * First returns cached data (if available), then fetches fresh data from API
     * and updates cache asynchronously.
     */
    fun getAuthenticatedUser(): Flow<Result<GithubUser>> = flow {
        // First, try to get from cache
        val username = authRepository.getUsername()
        if (username != null) {
            val cachedUser = cacheManager.getCachedUser(username)
            if (cachedUser != null) {
                emit(Result.success(cachedUser.toGithubUser()))
            }
        }
        
        // Then fetch from API
        try {
            val user = githubApiService.getAuthenticatedUser()
            // Cache asynchronously
            cacheManager.cacheUserAsync(user)
            emit(Result.success(user))
        } catch (e: Exception) {
            // If we already emitted cached data, don't emit error
            if (username == null) {
                emit(Result.failure(e))
            }
        }
    }
    
    /**
     * Get authenticated user synchronously
     */
    suspend fun getAuthenticatedUserSync(): Result<GithubUser> {
        return try {
            val user = githubApiService.getAuthenticatedUser()
            cacheManager.cacheUserAsync(user)
            Result.success(user)
        } catch (e: Exception) {
            val cachedUser = authRepository.getUsername()?.let { cacheManager.getCachedUser(it) }
            if (cachedUser != null) {
                Result.success(cachedUser.toGithubUser())
            } else {
                Result.failure(e.toAppError())
            }
        }
    }
    
    /**
     * Get user repositories with async caching
     * 
     * Returns cached repositories immediately if available, then fetches
     * from API and updates cache in background.
     */
    fun getUserRepositories(
        page: Int = 1,
        perPage: Int = 30
    ): Flow<Result<List<GithubRepoModel>>> = flow {
        // First emit cached data if available
        val cachedRepos = cacheManager.getCachedRepositories()
        if (cachedRepos.isNotEmpty() && page == 1) {
            emit(Result.success(cachedRepos.map { it.toGithubRepository() }))
        }
        
        // Then fetch from API
        try {
            val repositories = githubApiService.getUserRepositories(
                page = page,
                perPage = perPage
            )
            // Cache asynchronously (only first page)
            if (page == 1) {
                cacheManager.cacheRepositoriesAsync(repositories)
            }
            emit(Result.success(repositories))
        } catch (e: Exception) {
            // If we didn't emit cached data, emit error
            if (cachedRepos.isEmpty() || page != 1) {
                emit(Result.failure(e))
            }
        }
    }
    
    /**
     * Get user repositories synchronously
     */
    suspend fun getUserRepositoriesSync(
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GithubRepoModel>> {
        val cachedRepos = if (page == 1) cacheManager.getCachedRepositories() else emptyList()
        return try {
            val repositories = githubApiService.getUserRepositories(
                page = page,
                perPage = perPage
            )
            if (page == 1) {
                cacheManager.cacheRepositoriesAsync(repositories)
            }
            Result.success(repositories)
        } catch (e: Exception) {
            if (cachedRepos.isNotEmpty()) {
                Result.success(cachedRepos.map { it.toGithubRepository() })
            } else {
                Result.failure(e.toAppError())
            }
        }
    }
    
    /**
     * Get cached repositories as Flow
     * Useful for observing cache changes
     */
    fun getCachedRepositoriesFlow(): Flow<List<RepositoryNameCache>> {
        return cacheManager.getCachedRepositoriesFlow()
    }
    
    /**
     * Search cached repositories
     */
    fun searchCachedRepositories(query: String): Flow<List<RepositoryNameCache>> {
        return cacheManager.searchCachedRepositoriesFlow(query)
    }
    
    /**
     * Get a specific repository
     */
    suspend fun getRepository(owner: String, repo: String): Result<GithubRepoModel> {
        val cachedRepo = cacheManager.getCachedRepositoryByFullName("$owner/$repo")
        return try {
            val repository = githubApiService.getRepository(owner, repo)
            Result.success(repository)
        } catch (e: Exception) {
            if (cachedRepo != null) {
                Result.success(cachedRepo.toGithubRepository())
            } else {
                Result.failure(e.toAppError())
            }
        }
    }
    
    /**
     * Search repositories
     */
    suspend fun searchRepositories(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GithubRepoModel>> {
        return try {
            val response = githubApiService.searchRepositories(query, page, perPage)
            Result.success(response.items)
        } catch (e: Exception) {
            val cached = if (page == 1) cacheManager.searchCachedRepositories(query) else emptyList()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toGithubRepository() })
            } else {
                Result.failure(e.toAppError())
            }
        }
    }

    /**
     * Search issues
     */
    suspend fun searchIssues(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GithubSearchIssue>> {
        return try {
            val response = githubApiService.searchIssues(query, page, perPage)
            Result.success(response.items)
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    /**
     * Search users
     */
    suspend fun searchUsers(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GithubUser>> {
        return try {
            val response = githubApiService.searchUsers(query, page, perPage)
            Result.success(response.items)
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    /**
     * Search code
     */
    suspend fun searchCode(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): Result<List<GithubCodeSearchItem>> {
        return try {
            val response = githubApiService.searchCode(query, page, perPage)
            Result.success(response.items)
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }
    
    /**
     * Get user by username
     */
    suspend fun getUser(username: String): Result<GithubUser> {
        return try {
            // First check cache
            val cachedUser = cacheManager.getCachedUser(username)
            if (cachedUser != null) {
                return Result.success(cachedUser.toGithubUser())
            }
            
            val user = githubApiService.getUser(username)
            cacheManager.cacheUserAsync(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearCache() {
        cacheManager.clearAllCache()
    }
    
    /**
     * Clean expired cache entries
     */
    fun cleanExpiredCache() {
        cacheManager.cleanExpiredCacheAsync()
    }
}

// Extension functions for converting cache entities to domain models

private fun com.rafgittools.data.cache.RepositoryNameCache.toGithubRepository(): GithubRepoModel {
    return GithubRepoModel(
        id = id,
        name = name,
        fullName = fullName,
        owner = GithubUser(
            id = 0,
            login = ownerLogin,
            avatarUrl = "",
            htmlUrl = "https://github.com/$ownerLogin",
            type = "User"
        ),
        description = description,
        htmlUrl = "https://github.com/$fullName",
        cloneUrl = "https://github.com/$fullName.git",
        sshUrl = "git@github.com:$fullName.git",
        language = language,
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        watchersCount = 0,
        openIssuesCount = 0,
        isPrivate = isPrivate,
        isFork = false,
        defaultBranch = "main",
        createdAt = "",
        updatedAt = "",
        pushedAt = null
    )
}

private fun com.rafgittools.data.cache.UserCache.toGithubUser(): GithubUser {
    return GithubUser(
        id = id,
        login = login,
        avatarUrl = avatarUrl,
        htmlUrl = "https://github.com/$login",
        type = "User",
        name = name,
        bio = bio,
        location = location,
        company = company
    )
}
