package com.rafgittools.rafgitfs.cache

import com.rafgittools.rafgitfs.index.RafGitFsContentSnapshot

/**
 * Cache states exposed to Room and Compose. PARTIAL is internal/retryable and
 * must never be promoted to a complete offline copy.
 */
enum class RafGitFsCacheState {
    REMOTE_ONLY,
    METADATA_CACHED,
    PARTIAL,
    CONTENT_CACHED,
    PINNED_OFFLINE,
    STALE,
    CORRUPTED
}

data class RafGitFsCacheIdentity(
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val blobSha: String
) {
    init {
        require(profileId.isNotBlank())
        require(repositoryFullName.contains('/'))
        require(refName.isNotBlank())
        require(path.isNotBlank() && !path.startsWith('/'))
        require(blobSha.length in setOf(40, 64))
    }
}

data class RafGitFsCachePolicy(
    val maxCacheBytes: Long,
    val maxSingleFileBytes: Long = 50L * 1024L * 1024L,
    val expiresAfterMillis: Long = 7L * 24L * 60L * 60L * 1000L
) {
    init {
        require(maxCacheBytes >= 16L * 1024L * 1024L)
        require(maxSingleFileBytes in 1L..maxCacheBytes)
        require(expiresAfterMillis > 0L)
    }
}

data class RafGitFsCachedContent(
    val snapshot: RafGitFsContentSnapshot,
    val localPath: String,
    val checksumSha256: String,
    val state: RafGitFsCacheState,
    val pinned: Boolean,
    val observedAt: Long
)

sealed interface RafGitFsCacheResult<out T> {
    data class Success<T>(val value: T) : RafGitFsCacheResult<T>
    data class TokenVazio<T>(val reason: String, val partialValue: T? = null) : RafGitFsCacheResult<T>
    data class Failure(val code: String, val message: String, val retryable: Boolean) : RafGitFsCacheResult<Nothing>
}

data class RafGitFsCacheMaintenanceReport(
    val expiredRemoved: Int,
    val lruRemoved: Int,
    val bytesBefore: Long,
    val bytesAfter: Long,
    val pinnedPreserved: Int,
    val missingFilesReconciled: Int
)
