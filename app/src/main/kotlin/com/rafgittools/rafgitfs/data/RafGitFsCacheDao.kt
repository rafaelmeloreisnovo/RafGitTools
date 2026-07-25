package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentCacheDao {
    @Upsert
    suspend fun upsert(entry: ContentCacheEntity)

    @Query("SELECT * FROM content_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): ContentCacheEntity?

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId=:profileId AND repositoryFullName=:repositoryFullName
             AND refName=:refName AND path=:path AND gitSha=:gitSha
           LIMIT 1"""
    )
    suspend fun getByIdentity(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        gitSha: String
    ): ContentCacheEntity?

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId=:profileId AND repositoryFullName=:repositoryFullName
             AND refName=:refName AND path=:path
           ORDER BY createdAt DESC"""
    )
    suspend fun listForPath(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String
    ): List<ContentCacheEntity>

    @Query("SELECT * FROM content_cache WHERE cacheKey = :cacheKey LIMIT 1")
    fun observeByKey(cacheKey: String): Flow<ContentCacheEntity?>

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId=:profileId
           ORDER BY pinned DESC, lastAccessedAt DESC"""
    )
    suspend fun listForProfile(profileId: String): List<ContentCacheEntity>

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId = :profileId AND pinned = 1
           ORDER BY repositoryFullName ASC, path ASC"""
    )
    fun observePinned(profileId: String): Flow<List<ContentCacheEntity>>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM content_cache WHERE profileId = :profileId")
    suspend fun totalBytes(profileId: String): Long

    @Query("SELECT COUNT(*) FROM content_cache WHERE profileId=:profileId AND pinned=1")
    suspend fun pinnedCount(profileId: String): Int

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId = :profileId AND pinned = 0
           ORDER BY lastAccessedAt ASC
           LIMIT :limit"""
    )
    suspend fun evictionCandidates(profileId: String, limit: Int): List<ContentCacheEntity>

    @Query("UPDATE content_cache SET lastAccessedAt = :accessedAt WHERE cacheKey = :cacheKey")
    suspend fun touch(cacheKey: String, accessedAt: Long = System.currentTimeMillis())

    @Query("UPDATE content_cache SET pinned = :pinned, cacheState = :cacheState, expiresAt=:expiresAt WHERE cacheKey = :cacheKey")
    suspend fun setPinned(cacheKey: String, pinned: Boolean, cacheState: String, expiresAt: Long?)

    @Query("UPDATE content_cache SET cacheState=:cacheState WHERE cacheKey=:cacheKey")
    suspend fun setState(cacheKey: String, cacheState: String)

    @Query("DELETE FROM content_cache WHERE cacheKey IN (:cacheKeys) AND pinned = 0")
    suspend fun deleteUnpinnedByKeys(cacheKeys: List<String>): Int

    @Query("DELETE FROM content_cache WHERE profileId = :profileId AND pinned = 0 AND expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredUnpinned(profileId: String, now: Long): Int

    @Query("DELETE FROM content_cache WHERE cacheKey = :cacheKey AND pinned = 0")
    suspend fun deleteUnpinned(cacheKey: String): Int
}
