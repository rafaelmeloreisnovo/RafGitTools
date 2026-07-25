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
           WHERE profileId = :profileId AND pinned = 1
           ORDER BY repositoryFullName ASC, path ASC"""
    )
    fun observePinned(profileId: String): Flow<List<ContentCacheEntity>>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM content_cache WHERE profileId = :profileId")
    suspend fun totalBytes(profileId: String): Long

    @Query(
        """SELECT * FROM content_cache
           WHERE profileId = :profileId AND pinned = 0
           ORDER BY lastAccessedAt ASC
           LIMIT :limit"""
    )
    suspend fun evictionCandidates(profileId: String, limit: Int): List<ContentCacheEntity>

    @Query("UPDATE content_cache SET lastAccessedAt = :accessedAt WHERE cacheKey = :cacheKey")
    suspend fun touch(cacheKey: String, accessedAt: Long = System.currentTimeMillis())

    @Query("UPDATE content_cache SET pinned = :pinned, cacheState = :cacheState WHERE cacheKey = :cacheKey")
    suspend fun setPinned(cacheKey: String, pinned: Boolean, cacheState: String)

    @Query("DELETE FROM content_cache WHERE cacheKey IN (:cacheKeys) AND pinned = 0")
    suspend fun deleteUnpinnedByKeys(cacheKeys: List<String>): Int

    @Query("DELETE FROM content_cache WHERE profileId = :profileId AND pinned = 0 AND expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredUnpinned(profileId: String, now: Long): Int

    @Query("DELETE FROM content_cache WHERE cacheKey = :cacheKey AND pinned = 0")
    suspend fun deleteUnpinned(cacheKey: String): Int
}
