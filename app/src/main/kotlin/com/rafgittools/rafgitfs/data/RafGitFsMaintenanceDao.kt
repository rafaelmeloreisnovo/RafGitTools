package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface RafGitFsMaintenanceDao {

    @Query(
        """SELECT * FROM rafgitfs_content_cache
           WHERE profileId = :profileId
             AND pinnedOffline = 0
           ORDER BY lastAccessedAt ASC
           LIMIT :limit"""
    )
    suspend fun oldestEvictable(
        profileId: String,
        limit: Int
    ): List<RafGitFsContentCacheEntity>

    @Query(
        """DELETE FROM rafgitfs_content_cache
           WHERE pinnedOffline = 0
             AND expiresAt IS NOT NULL
             AND expiresAt < :now"""
    )
    suspend fun deleteExpiredUnpinned(now: Long): Int

    @Query(
        """DELETE FROM rafgitfs_virtual_tree_entries
           WHERE lastIndexedAt < :olderThan
             AND cacheState IN ('REMOTE_ONLY', 'METADATA_CACHED', 'STALE')"""
    )
    suspend fun deleteStaleTreeMetadata(olderThan: Long): Int

    @Query(
        """UPDATE rafgitfs_transfer_jobs
           SET status = 'PAUSED',
               errorCode = 'RECOVERED_AFTER_PROCESS_DEATH',
               updatedAt = :now
           WHERE status = 'RUNNING'
             AND updatedAt < :abandonedBefore"""
    )
    suspend fun pauseAbandonedJobs(abandonedBefore: Long, now: Long): Int

    /**
     * Operation receipts are intentionally absent from maintenance deletion.
     * They remain append-only audit records.
     */
}
