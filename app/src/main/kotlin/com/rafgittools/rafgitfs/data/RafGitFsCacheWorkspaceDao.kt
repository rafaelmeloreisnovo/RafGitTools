package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RafGitFsCacheWorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCache(entry: RafGitFsContentCacheEntity)

    @Query(
        """SELECT * FROM rafgitfs_content_cache
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND path = :path
           LIMIT 1"""
    )
    suspend fun getCacheEntry(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String
    ): RafGitFsContentCacheEntity?

    @Query(
        """SELECT * FROM rafgitfs_content_cache
           WHERE profileId = :profileId
           ORDER BY pinnedOffline DESC, lastAccessedAt DESC"""
    )
    fun observeCache(profileId: String): Flow<List<RafGitFsContentCacheEntity>>

    @Query(
        """UPDATE rafgitfs_content_cache
           SET lastAccessedAt = :accessedAt
           WHERE cacheKey = :cacheKey"""
    )
    suspend fun touch(cacheKey: String, accessedAt: Long)

    @Query(
        """UPDATE rafgitfs_content_cache
           SET pinnedOffline = :pinned, expiresAt = CASE WHEN :pinned THEN NULL ELSE expiresAt END
           WHERE cacheKey = :cacheKey"""
    )
    suspend fun setPinned(cacheKey: String, pinned: Boolean)

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM rafgitfs_content_cache WHERE profileId = :profileId")
    suspend fun totalCacheBytes(profileId: String): Long

    @Query("DELETE FROM rafgitfs_content_cache WHERE cacheKey = :cacheKey AND pinnedOffline = 0")
    suspend fun deleteUnpinned(cacheKey: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWorkspace(workspace: RafGitFsWorkspaceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspace(workspace: RafGitFsWorkspaceEntity)

    @Query(
        """SELECT * FROM rafgitfs_workspaces
           WHERE profileId = :profileId
           ORDER BY updatedAt DESC"""
    )
    fun observeWorkspaces(profileId: String): Flow<List<RafGitFsWorkspaceEntity>>

    @Query("SELECT * FROM rafgitfs_workspaces WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getWorkspace(workspaceId: String): RafGitFsWorkspaceEntity?
}
