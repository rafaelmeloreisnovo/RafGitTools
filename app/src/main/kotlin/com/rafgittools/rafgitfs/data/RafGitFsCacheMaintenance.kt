package com.rafgittools.rafgitfs.data

import androidx.room.withTransaction
import com.rafgittools.data.cache.CacheDatabase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bounded and deterministic cache cleanup.
 *
 * Pinned entries are never evicted. The database stores only metadata; callers
 * remain responsible for deleting the corresponding local file after a
 * successful database deletion.
 */
@Singleton
class RafGitFsCacheMaintenance @Inject constructor(
    private val database: CacheDatabase,
    private val contentCacheDao: ContentCacheDao
) {
    data class Result(
        val bytesBefore: Long,
        val bytesAfter: Long,
        val expiredRowsRemoved: Int,
        val lruRowsRemoved: Int,
        val cacheBudgetSatisfied: Boolean,
        val removedLocalPaths: List<String>
    )

    suspend fun prune(
        profileId: String,
        maxBytes: Long,
        now: Long = System.currentTimeMillis(),
        batchSize: Int = 64
    ): Result {
        require(profileId.isNotBlank()) { "profileId must not be blank" }
        require(maxBytes >= 0L) { "maxBytes must be non-negative" }
        require(batchSize in 1..512) { "batchSize must be between 1 and 512" }

        return database.withTransaction {
            val bytesBefore = contentCacheDao.totalBytes(profileId)
            val expiredRowsRemoved = contentCacheDao.deleteExpiredUnpinned(profileId, now)
            var currentBytes = contentCacheDao.totalBytes(profileId)
            var lruRowsRemoved = 0
            val removedPaths = mutableListOf<String>()

            while (currentBytes > maxBytes) {
                val candidates = contentCacheDao.evictionCandidates(profileId, batchSize)
                if (candidates.isEmpty()) break

                val selected = mutableListOf<ContentCacheEntity>()
                var projectedBytes = currentBytes
                for (candidate in candidates) {
                    if (projectedBytes <= maxBytes) break
                    selected += candidate
                    projectedBytes = (projectedBytes - candidate.sizeBytes).coerceAtLeast(0L)
                }

                if (selected.isEmpty()) break
                val deleted = contentCacheDao.deleteUnpinnedByKeys(selected.map { it.cacheKey })
                if (deleted == 0) break

                lruRowsRemoved += deleted
                removedPaths += selected.take(deleted).map { it.localPath }
                currentBytes = contentCacheDao.totalBytes(profileId)
            }

            Result(
                bytesBefore = bytesBefore,
                bytesAfter = currentBytes,
                expiredRowsRemoved = expiredRowsRemoved,
                lruRowsRemoved = lruRowsRemoved,
                cacheBudgetSatisfied = currentBytes <= maxBytes,
                removedLocalPaths = removedPaths.distinct()
            )
        }
    }
}
