package com.rafgittools.rafgitfs.cache

import com.rafgittools.rafgitfs.data.ContentCacheDao
import com.rafgittools.rafgitfs.data.ContentCacheEntity
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsCacheMaintenance @Inject constructor(
    private val profileDao: StorageProfileDao,
    private val cacheDao: ContentCacheDao,
    private val treeDao: VirtualTreeDao,
    private val fileStore: RafGitFsAtomicFileStore
) {
    suspend fun ensureCapacity(profileId: String, incomingBytes: Long): Boolean {
        val profile = profileDao.getById(profileId) ?: return false
        if (incomingBytes < 0L || incomingBytes > profile.maxCacheBytes) return false
        reconcile(profileId)
        var total = cacheDao.totalBytes(profileId)
        if (total + incomingBytes <= profile.maxCacheBytes) return true
        val candidates = cacheDao.evictionCandidates(profileId, limit = 512)
        for (entry in candidates) {
            removeUnpinned(entry)
            total = (total - entry.sizeBytes).coerceAtLeast(0L)
            if (total + incomingBytes <= profile.maxCacheBytes) return true
        }
        return total + incomingBytes <= profile.maxCacheBytes
    }

    suspend fun reconcile(profileId: String, now: Long = System.currentTimeMillis()): RafGitFsCacheMaintenanceReport {
        val profile = profileDao.getById(profileId)
            ?: return RafGitFsCacheMaintenanceReport(0, 0, 0, 0, 0, 0)
        val before = cacheDao.totalBytes(profileId)
        var expired = 0
        var lru = 0
        var missing = 0

        cacheDao.listForProfile(profileId).forEach { entry ->
            val fileExists = fileStore.resolve(entry.cacheKey).isFile
            if (!fileExists) {
                missing += 1
                if (entry.pinned) {
                    cacheDao.setState(entry.cacheKey, RafGitFsCacheState.CORRUPTED.name)
                    markTree(entry, RafGitFsCacheState.CORRUPTED, null)
                } else {
                    cacheDao.deleteUnpinned(entry.cacheKey)
                    markTree(entry, RafGitFsCacheState.REMOTE_ONLY, null)
                }
            } else if (!entry.pinned && entry.expiresAt != null && entry.expiresAt < now) {
                if (removeUnpinned(entry)) expired += 1
            }
        }

        var total = cacheDao.totalBytes(profileId)
        if (total > profile.maxCacheBytes) {
            for (entry in cacheDao.evictionCandidates(profileId, 512)) {
                if (removeUnpinned(entry)) {
                    lru += 1
                    total = (total - entry.sizeBytes).coerceAtLeast(0L)
                }
                if (total <= profile.maxCacheBytes) break
            }
        }

        return RafGitFsCacheMaintenanceReport(
            expiredRemoved = expired,
            lruRemoved = lru,
            bytesBefore = before,
            bytesAfter = cacheDao.totalBytes(profileId),
            pinnedPreserved = cacheDao.pinnedCount(profileId),
            missingFilesReconciled = missing
        )
    }

    private suspend fun removeUnpinned(entry: ContentCacheEntity): Boolean {
        if (entry.pinned) return false
        if (!fileStore.delete(entry.cacheKey)) return false
        val removed = cacheDao.deleteUnpinned(entry.cacheKey) > 0
        if (removed) markTree(entry, RafGitFsCacheState.REMOTE_ONLY, null)
        return removed
    }

    private suspend fun markTree(
        entry: ContentCacheEntity,
        state: RafGitFsCacheState,
        localPath: String?
    ) {
        treeDao.setCacheState(
            entry.profileId,
            entry.repositoryFullName,
            entry.refName,
            entry.path,
            state.name,
            localPath
        )
    }
}
