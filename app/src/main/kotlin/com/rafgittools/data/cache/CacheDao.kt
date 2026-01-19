package com.rafgittools.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for cache entries
 * 
 * Provides async access to cached content using Flow
 */
@Dao
interface CacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheEntry(entry: CacheEntry)
    
    @Query("SELECT * FROM cache_entries WHERE `key` = :key")
    suspend fun getCacheEntry(key: String): CacheEntry?
    
    @Query("SELECT * FROM cache_entries WHERE `key` = :key")
    fun getCacheEntryFlow(key: String): Flow<CacheEntry?>
    
    @Query("DELETE FROM cache_entries WHERE `key` = :key")
    suspend fun deleteCacheEntry(key: String)
    
    @Query("DELETE FROM cache_entries WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredEntries(currentTime: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM cache_entries")
    suspend fun clearAllCache()
}

/**
 * DAO for repository name cache
 * 
 * Provides async access to cached repository names using Flow
 */
@Dao
interface RepositoryNameCacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repository: RepositoryNameCache)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepositories(repositories: List<RepositoryNameCache>)
    
    @Query("SELECT * FROM repository_name_cache ORDER BY updatedAt DESC")
    fun getAllRepositoriesFlow(): Flow<List<RepositoryNameCache>>
    
    @Query("SELECT * FROM repository_name_cache ORDER BY updatedAt DESC")
    suspend fun getAllRepositories(): List<RepositoryNameCache>
    
    @Query("SELECT * FROM repository_name_cache WHERE id = :id")
    suspend fun getRepositoryById(id: Long): RepositoryNameCache?

    @Query("SELECT * FROM repository_name_cache WHERE fullName = :fullName")
    suspend fun getRepositoryByFullName(fullName: String): RepositoryNameCache?
    
    @Query("SELECT * FROM repository_name_cache WHERE ownerLogin = :owner")
    fun getRepositoriesByOwnerFlow(owner: String): Flow<List<RepositoryNameCache>>
    
    @Query("SELECT * FROM repository_name_cache WHERE name LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%'")
    fun searchRepositoriesFlow(query: String): Flow<List<RepositoryNameCache>>

    @Query("SELECT * FROM repository_name_cache WHERE name LIKE '%' || :query || '%' OR fullName LIKE '%' || :query || '%'")
    suspend fun searchRepositories(query: String): List<RepositoryNameCache>
    
    @Query("DELETE FROM repository_name_cache WHERE id = :id")
    suspend fun deleteRepository(id: Long)
    
    @Query("DELETE FROM repository_name_cache")
    suspend fun clearAllRepositories()
}

/**
 * DAO for user cache
 * 
 * Provides async access to cached user data using Flow
 */
@Dao
interface UserCacheDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserCache)
    
    @Query("SELECT * FROM user_cache WHERE login = :login")
    suspend fun getUserByLogin(login: String): UserCache?
    
    @Query("SELECT * FROM user_cache WHERE login = :login")
    fun getUserByLoginFlow(login: String): Flow<UserCache?>
    
    @Query("SELECT * FROM user_cache WHERE id = :id")
    suspend fun getUserById(id: Long): UserCache?
    
    @Query("DELETE FROM user_cache WHERE login = :login")
    suspend fun deleteUser(login: String)
    
    @Query("DELETE FROM user_cache")
    suspend fun clearAllUsers()
}
