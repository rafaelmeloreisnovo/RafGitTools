package com.rafgittools.data.cache

import com.rafgittools.domain.model.github.GithubRepository
import com.rafgittools.domain.model.github.GithubUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Asynchronous cache manager for GitHub data
 * 
 * Provides Flow-based async access to cached data with automatic
 * background updates and expiration handling.
 */
@Singleton
class AsyncCacheManager @Inject constructor(
    private val cacheDao: CacheDao,
    private val repositoryNameCacheDao: RepositoryNameCacheDao,
    private val userCacheDao: UserCacheDao
) {
    // Coroutine scope for background cache operations
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        // Cache expiration times in milliseconds
        const val REPOSITORY_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
        const val USER_CACHE_DURATION = 10 * 60 * 1000L // 10 minutes
        const val CONTENT_CACHE_DURATION = 15 * 60 * 1000L // 15 minutes
    }
    
    // ==================== Repository Cache ====================
    
    /**
     * Cache repositories asynchronously
     * Does not block - runs in background
     */
    fun cacheRepositoriesAsync(repositories: List<GithubRepository>) {
        cacheScope.launch {
            val cacheEntries = repositories.map { repo ->
                RepositoryNameCache(
                    id = repo.id,
                    name = repo.name,
                    fullName = repo.fullName,
                    ownerLogin = repo.owner.login,
                    description = repo.description,
                    language = repo.language,
                    stargazersCount = repo.stargazersCount,
                    forksCount = repo.forksCount,
                    isPrivate = repo.isPrivate,
                    updatedAt = System.currentTimeMillis()
                )
            }
            repositoryNameCacheDao.insertRepositories(cacheEntries)
        }
    }
    
    /**
     * Get cached repositories as Flow
     * Emits updates whenever cache changes
     */
    fun getCachedRepositoriesFlow(): Flow<List<RepositoryNameCache>> {
        return repositoryNameCacheDao.getAllRepositoriesFlow()
    }
    
    /**
     * Get cached repositories synchronously
     */
    suspend fun getCachedRepositories(): List<RepositoryNameCache> {
        return repositoryNameCacheDao.getAllRepositories()
    }

    /**
     * Get cached repository by full name
     */
    suspend fun getCachedRepositoryByFullName(fullName: String): RepositoryNameCache? {
        return repositoryNameCacheDao.getRepositoryByFullName(fullName)
    }
    
    /**
     * Search cached repositories by name
     */
    fun searchCachedRepositoriesFlow(query: String): Flow<List<RepositoryNameCache>> {
        return repositoryNameCacheDao.searchRepositoriesFlow(query)
    }

    /**
     * Search cached repositories synchronously
     */
    suspend fun searchCachedRepositories(query: String): List<RepositoryNameCache> {
        return repositoryNameCacheDao.searchRepositories(query)
    }
    
    // ==================== User Cache ====================
    
    /**
     * Cache user asynchronously
     */
    fun cacheUserAsync(user: GithubUser) {
        cacheScope.launch {
            val cacheEntry = UserCache(
                id = user.id,
                login = user.login,
                avatarUrl = user.avatarUrl,
                name = user.name,
                bio = user.bio,
                location = user.location,
                company = user.company,
                updatedAt = System.currentTimeMillis()
            )
            userCacheDao.insertUser(cacheEntry)
        }
    }
    
    /**
     * Get cached user as Flow
     */
    fun getCachedUserFlow(login: String): Flow<UserCache?> {
        return userCacheDao.getUserByLoginFlow(login)
    }
    
    /**
     * Get cached user synchronously
     */
    suspend fun getCachedUser(login: String): UserCache? {
        return userCacheDao.getUserByLogin(login)
    }
    
    // ==================== Generic Content Cache ====================
    
    /**
     * Cache content asynchronously with specified duration
     */
    fun cacheContentAsync(
        key: String,
        content: String,
        contentType: String,
        durationMs: Long = CONTENT_CACHE_DURATION,
        etag: String? = null
    ) {
        cacheScope.launch {
            val entry = CacheEntry(
                key = key,
                content = content,
                contentType = contentType,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + durationMs,
                etag = etag
            )
            cacheDao.insertCacheEntry(entry)
        }
    }
    
    /**
     * Get cached content as Flow
     */
    fun getCachedContentFlow(key: String): Flow<CacheEntry?> {
        return cacheDao.getCacheEntryFlow(key)
    }
    
    /**
     * Get cached content synchronously
     */
    suspend fun getCachedContent(key: String): CacheEntry? {
        val entry = cacheDao.getCacheEntry(key)
        // Return null if expired
        return if (entry != null && entry.expiresAt > System.currentTimeMillis()) {
            entry
        } else {
            // Clean up expired entry in background
            entry?.let { 
                cacheScope.launch { 
                    cacheDao.deleteCacheEntry(key) 
                } 
            }
            null
        }
    }
    
    /**
     * Check if content is cached and valid
     */
    suspend fun isCacheValid(key: String): Boolean {
        val entry = cacheDao.getCacheEntry(key)
        return entry != null && entry.expiresAt > System.currentTimeMillis()
    }
    
    // ==================== Cache Management ====================
    
    /**
     * Clear expired cache entries in background
     */
    fun cleanExpiredCacheAsync() {
        cacheScope.launch {
            cacheDao.deleteExpiredEntries()
        }
    }
    
    /**
     * Clear all cache data
     */
    suspend fun clearAllCache() {
        cacheDao.clearAllCache()
        repositoryNameCacheDao.clearAllRepositories()
        userCacheDao.clearAllUsers()
    }
    
    /**
     * Clear only repository cache
     */
    suspend fun clearRepositoryCache() {
        repositoryNameCacheDao.clearAllRepositories()
    }
    
    /**
     * Clear only user cache
     */
    suspend fun clearUserCache() {
        userCacheDao.clearAllUsers()
    }
}
