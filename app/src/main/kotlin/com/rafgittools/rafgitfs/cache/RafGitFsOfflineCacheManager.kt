package com.rafgittools.rafgitfs.cache

import com.rafgittools.rafgitfs.data.ContentCacheDao
import com.rafgittools.rafgitfs.data.ContentCacheEntity
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import com.rafgittools.rafgitfs.index.RafGitFsContentSnapshot
import com.rafgittools.rafgitfs.index.RafGitFsGithubIndexer
import com.rafgittools.rafgitfs.remote.RafGitFsRemoteResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsOfflineCacheManager @Inject constructor(
    private val profileDao: StorageProfileDao,
    private val treeDao: VirtualTreeDao,
    private val cacheDao: ContentCacheDao,
    private val indexer: RafGitFsGithubIndexer,
    private val fileStore: RafGitFsAtomicFileStore,
    private val maintenance: RafGitFsCacheMaintenance
) {
    suspend fun read(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        allowNetwork: Boolean = true,
        pinAfterDownload: Boolean = false
    ): RafGitFsCacheResult<RafGitFsCachedContent> {
        val normalizedPath = path.trim('/')
        val profile = profileDao.getById(profileId)
            ?: return RafGitFsCacheResult.TokenVazio("PROFILE_NOT_FOUND")
        if (!profile.isEnabled) return RafGitFsCacheResult.TokenVazio("PROFILE_DISABLED")

        val treeEntry = treeDao.getEntry(profileId, repositoryFullName, refName, normalizedPath)
            ?: return RafGitFsCacheResult.TokenVazio("TREE_ENTRY_NOT_INDEXED")
        val blobSha = treeEntry.gitSha
            ?: return RafGitFsCacheResult.TokenVazio("ENTRY_SHA_MISSING")
        if (treeEntry.entryType !in setOf("FILE", "SYMLINK")) {
            return RafGitFsCacheResult.TokenVazio("ENTRY_NOT_CACHEABLE:${treeEntry.entryType}")
        }

        val identity = try {
            RafGitFsCacheIdentity(profileId, repositoryFullName, refName, normalizedPath, blobSha)
        } catch (error: IllegalArgumentException) {
            return RafGitFsCacheResult.Failure("INVALID_IDENTITY", error.message.orEmpty(), false)
        }
        val policy = RafGitFsCachePolicy(
            maxCacheBytes = profile.maxCacheBytes,
            maxSingleFileBytes = minOf(profile.maxCacheBytes, MAX_SINGLE_FILE_BYTES)
        )

        markOlderGenerationsStale(identity)
        cacheDao.getByIdentity(profileId, repositoryFullName, refName, normalizedPath, blobSha)?.let { cached ->
            val local = readVerified(cached, policy.maxSingleFileBytes)
            if (local is RafGitFsCacheResult.Success) {
                return if (pinAfterDownload && !local.value.pinned) {
                    setPinned(cached.cacheKey, true)
                } else local
            }
            if (!allowNetwork) return local
        }

        if (!allowNetwork) {
            treeDao.setCacheState(
                profileId, repositoryFullName, refName, normalizedPath,
                RafGitFsCacheState.REMOTE_ONLY.name, null
            )
            return RafGitFsCacheResult.TokenVazio("OFFLINE_CACHE_MISS")
        }
        val expectedSize = treeEntry.sizeBytes
        if (expectedSize != null && expectedSize > policy.maxSingleFileBytes) {
            return RafGitFsCacheResult.TokenVazio("FILE_EXCEEDS_CACHE_POLICY:$expectedSize")
        }

        return when (val remote = indexer.readContent(
            profileId, repositoryFullName, refName, normalizedPath, policy.maxSingleFileBytes
        )) {
            is RafGitFsRemoteResult.Observed -> persistObserved(identity, remote.value, policy, pinAfterDownload)
            is RafGitFsRemoteResult.TokenVazio -> RafGitFsCacheResult.TokenVazio(
                reason = remote.reason,
                partialValue = null
            )
            is RafGitFsRemoteResult.RateLimited -> RafGitFsCacheResult.TokenVazio(
                "RATE_LIMITED:${remote.retryAfterSeconds ?: remote.resetAtEpochSeconds ?: "UNKNOWN"}"
            )
            is RafGitFsRemoteResult.Failure -> RafGitFsCacheResult.Failure(
                "REMOTE_FAILURE_${remote.statusCode ?: 0}", remote.message, remote.retryable
            )
            is RafGitFsRemoteResult.NotModified -> RafGitFsCacheResult.TokenVazio("REMOTE_NOT_MODIFIED_WITHOUT_LOCAL_CONTENT")
        }
    }

    suspend fun setPinned(cacheKey: String, pinned: Boolean): RafGitFsCacheResult<RafGitFsCachedContent> {
        val entry = cacheDao.getByKey(cacheKey)
            ?: return RafGitFsCacheResult.TokenVazio("CACHE_ENTRY_NOT_FOUND")
        val profile = profileDao.getById(entry.profileId)
            ?: return RafGitFsCacheResult.TokenVazio("PROFILE_NOT_FOUND")
        val verified = readVerified(entry, minOf(profile.maxCacheBytes, MAX_SINGLE_FILE_BYTES))
        if (verified !is RafGitFsCacheResult.Success) return verified
        val now = System.currentTimeMillis()
        val state = if (pinned) RafGitFsCacheState.PINNED_OFFLINE else RafGitFsCacheState.CONTENT_CACHED
        cacheDao.setPinned(
            cacheKey = cacheKey,
            pinned = pinned,
            cacheState = state.name,
            expiresAt = if (pinned) null else now + DEFAULT_EXPIRY_MILLIS
        )
        treeDao.setCacheState(
            entry.profileId, entry.repositoryFullName, entry.refName, entry.path,
            state.name, entry.localPath, now
        )
        return RafGitFsCacheResult.Success(
            verified.value.copy(state = state, pinned = pinned, observedAt = now)
        )
    }

    suspend fun remove(cacheKey: String): RafGitFsCacheResult<Unit> {
        val entry = cacheDao.getByKey(cacheKey)
            ?: return RafGitFsCacheResult.Success(Unit)
        if (entry.pinned) return RafGitFsCacheResult.TokenVazio("PINNED_ENTRY_REQUIRES_UNPIN")
        if (!fileStore.delete(cacheKey)) {
            return RafGitFsCacheResult.Failure("CACHE_FILE_DELETE_FAILED", cacheKey, true)
        }
        cacheDao.deleteUnpinned(cacheKey)
        treeDao.setCacheState(
            entry.profileId, entry.repositoryFullName, entry.refName, entry.path,
            RafGitFsCacheState.REMOTE_ONLY.name, null
        )
        return RafGitFsCacheResult.Success(Unit)
    }

    suspend fun reconcile(profileId: String): RafGitFsCacheMaintenanceReport = maintenance.reconcile(profileId)

    private suspend fun persistObserved(
        identity: RafGitFsCacheIdentity,
        snapshot: RafGitFsContentSnapshot,
        policy: RafGitFsCachePolicy,
        pinned: Boolean
    ): RafGitFsCacheResult<RafGitFsCachedContent> {
        if (snapshot.blobSha.lowercase() != identity.blobSha.lowercase()) {
            return corruption(identity, "REMOTE_BLOB_SHA_CHANGED")
        }
        if (snapshot.bytes.size.toLong() != snapshot.sizeBytes) {
            return corruption(identity, "REMOTE_SIZE_MISMATCH")
        }
        if (!RafGitFsChecksums.verifyGitBlob(snapshot.bytes, identity.blobSha)) {
            return corruption(identity, "GIT_BLOB_HASH_MISMATCH")
        }

        val cacheKey = RafGitFsCacheKeys.key(identity)
        if (!maintenance.ensureCapacity(identity.profileId, snapshot.sizeBytes, cacheKey)) {
            return RafGitFsCacheResult.TokenVazio("CACHE_BUDGET_EXHAUSTED")
        }

        val now = System.currentTimeMillis()
        val partialPath = fileStore.partial(cacheKey).absolutePath
        cacheDao.upsert(
            ContentCacheEntity(
                cacheKey = cacheKey,
                profileId = identity.profileId,
                repositoryFullName = identity.repositoryFullName,
                refName = identity.refName,
                path = identity.path,
                gitSha = identity.blobSha,
                localPath = partialPath,
                sizeBytes = snapshot.sizeBytes,
                cacheState = RafGitFsCacheState.PARTIAL.name,
                pinned = false,
                checksumAlgorithm = "SHA-256",
                checksumHex = "",
                createdAt = now,
                lastAccessedAt = now,
                expiresAt = now + policy.expiresAfterMillis
            )
        )
        treeDao.setCacheState(
            identity.profileId, identity.repositoryFullName, identity.refName, identity.path,
            RafGitFsCacheState.PARTIAL.name, partialPath, now
        )

        val finalFile = try {
            fileStore.writeAtomic(cacheKey, snapshot.bytes)
        } catch (error: IOException) {
            return RafGitFsCacheResult.Failure("ATOMIC_CACHE_WRITE_FAILED", error.message.orEmpty(), true)
        }
        val checksum = RafGitFsChecksums.sha256(finalFile)
        val state = if (pinned) RafGitFsCacheState.PINNED_OFFLINE else RafGitFsCacheState.CONTENT_CACHED
        val finalEntry = ContentCacheEntity(
            cacheKey = cacheKey,
            profileId = identity.profileId,
            repositoryFullName = identity.repositoryFullName,
            refName = identity.refName,
            path = identity.path,
            gitSha = identity.blobSha,
            localPath = finalFile.absolutePath,
            sizeBytes = snapshot.sizeBytes,
            cacheState = state.name,
            pinned = pinned,
            checksumAlgorithm = "SHA-256",
            checksumHex = checksum,
            createdAt = now,
            lastAccessedAt = now,
            expiresAt = if (pinned) null else now + policy.expiresAfterMillis
        )
        cacheDao.upsert(finalEntry)
        treeDao.setCacheState(
            identity.profileId, identity.repositoryFullName, identity.refName, identity.path,
            state.name, finalFile.absolutePath, now
        )
        return RafGitFsCacheResult.Success(
            RafGitFsCachedContent(snapshot, finalFile.absolutePath, checksum, state, pinned, now)
        )
    }

    private suspend fun readVerified(
        entry: ContentCacheEntity,
        maxBytes: Long
    ): RafGitFsCacheResult<RafGitFsCachedContent> {
        val bytes = fileStore.read(entry.cacheKey, maxBytes)
            ?: return corruption(entry.toIdentity(), "CACHE_FILE_MISSING_OR_OVERSIZED")
        if (bytes.size.toLong() != entry.sizeBytes) return corruption(entry.toIdentity(), "CACHE_SIZE_MISMATCH")
        val sha256 = RafGitFsChecksums.sha256(bytes)
        if (entry.checksumHex.isBlank() || !RafGitFsChecksums.constantTimeEquals(sha256, entry.checksumHex)) {
            return corruption(entry.toIdentity(), "CACHE_SHA256_MISMATCH")
        }
        if (!RafGitFsChecksums.verifyGitBlob(bytes, entry.gitSha)) {
            return corruption(entry.toIdentity(), "CACHE_GIT_BLOB_MISMATCH")
        }
        val now = System.currentTimeMillis()
        cacheDao.touch(entry.cacheKey, now)
        val state = runCatching { RafGitFsCacheState.valueOf(entry.cacheState) }
            .getOrDefault(if (entry.pinned) RafGitFsCacheState.PINNED_OFFLINE else RafGitFsCacheState.CONTENT_CACHED)
        val snapshot = RafGitFsContentSnapshot(
            entry.repositoryFullName,
            entry.refName,
            entry.path,
            entry.gitSha,
            entry.sizeBytes,
            bytes,
            decodeUtf8(bytes),
            now
        )
        return RafGitFsCacheResult.Success(
            RafGitFsCachedContent(snapshot, entry.localPath, sha256, state, entry.pinned, now)
        )
    }

    private suspend fun markOlderGenerationsStale(identity: RafGitFsCacheIdentity) {
        cacheDao.listForPath(identity.profileId, identity.repositoryFullName, identity.refName, identity.path)
            .filter { it.gitSha != identity.blobSha && it.cacheState != RafGitFsCacheState.CORRUPTED.name }
            .forEach { cacheDao.setState(it.cacheKey, RafGitFsCacheState.STALE.name) }
    }

    private suspend fun corruption(
        identity: RafGitFsCacheIdentity,
        reason: String
    ): RafGitFsCacheResult.TokenVazio<RafGitFsCachedContent> {
        val cacheKey = RafGitFsCacheKeys.key(identity)
        cacheDao.setState(cacheKey, RafGitFsCacheState.CORRUPTED.name)
        treeDao.setCacheState(
            identity.profileId, identity.repositoryFullName, identity.refName, identity.path,
            RafGitFsCacheState.CORRUPTED.name, fileStore.resolve(cacheKey).absolutePath
        )
        return RafGitFsCacheResult.TokenVazio(reason)
    }

    private fun ContentCacheEntity.toIdentity() = RafGitFsCacheIdentity(
        profileId, repositoryFullName, refName, path, gitSha
    )

    private fun decodeUtf8(bytes: ByteArray): String? {
        if (bytes.any { it == 0.toByte() }) return null
        val value = bytes.toString(Charsets.UTF_8)
        if (value.isEmpty()) return ""
        val replacements = value.count { it == '\uFFFD' }
        return value.takeIf { replacements * 100 <= value.length }
    }

    companion object {
        const val MAX_SINGLE_FILE_BYTES = 50L * 1024L * 1024L
        const val DEFAULT_EXPIRY_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
