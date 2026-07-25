package com.rafgittools.rafgitfs.cache

import com.rafgittools.rafgitfs.data.TransferJobDao
import com.rafgittools.rafgitfs.data.TransferJobEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RafGitFsOfflineQueue @Inject constructor(
    private val jobDao: TransferJobDao,
    private val cacheManager: RafGitFsOfflineCacheManager
) {
    suspend fun enqueue(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        pinOffline: Boolean
    ): String {
        require(profileId.isNotBlank())
        require(repositoryFullName.contains('/'))
        require(refName.isNotBlank())
        require(path.isNotBlank())
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        jobDao.upsert(
            TransferJobEntity(
                jobId = id,
                profileId = profileId,
                requestId = id,
                operationType = if (pinOffline) "PIN_OFFLINE" else "CACHE_DOWNLOAD",
                phase = "QUEUED_OFFLINE",
                syncState = "PAUSED",
                repositoryFullName = repositoryFullName,
                refName = refName,
                path = path.trim('/'),
                bytesTotal = null,
                bytesCompleted = 0L,
                retryCount = 0,
                maxRetries = 3,
                lastErrorCode = "WAITING_FOR_NETWORK",
                createdAt = now,
                updatedAt = now,
                claimAllowed = false
            )
        )
        return id
    }

    suspend fun resume(profileId: String, limit: Int = 8): RafGitFsOfflineQueueReport {
        val jobs = jobDao.listResumableCacheJobs(profileId, limit.coerceIn(1, 32))
        var completed = 0
        var paused = 0
        var failed = 0
        jobs.forEach { job ->
            val repository = job.repositoryFullName
            val ref = job.refName
            val path = job.path
            if (repository == null || ref == null || path == null) {
                jobDao.updateState(
                    job.jobId, "IDENTITY_INVALID", "FAILED", job.maxRetries,
                    "TOKEN_VAZIO_JOB_IDENTITY"
                )
                failed += 1
                return@forEach
            }
            jobDao.updateState(job.jobId, "DOWNLOADING", "EXECUTING", job.retryCount, null)
            when (val result = cacheManager.read(
                profileId = job.profileId,
                repositoryFullName = repository,
                refName = ref,
                path = path,
                allowNetwork = true,
                pinAfterDownload = job.operationType == "PIN_OFFLINE"
            )) {
                is RafGitFsCacheResult.Success -> {
                    jobDao.updateProgress(job.jobId, result.value.snapshot.sizeBytes, result.value.snapshot.sizeBytes)
                    jobDao.updateState(job.jobId, "CACHED", "COMPLETE", job.retryCount, null)
                    completed += 1
                }
                is RafGitFsCacheResult.TokenVazio -> {
                    val nextRetry = job.retryCount + 1
                    val terminal = nextRetry >= job.maxRetries
                    jobDao.updateState(
                        job.jobId,
                        if (terminal) "TOKEN_VAZIO_TERMINAL" else "WAITING_RETRY",
                        if (terminal) "FAILED" else "PAUSED",
                        nextRetry,
                        result.reason.take(160)
                    )
                    if (terminal) failed += 1 else paused += 1
                }
                is RafGitFsCacheResult.Failure -> {
                    val nextRetry = job.retryCount + 1
                    val terminal = !result.retryable || nextRetry >= job.maxRetries
                    jobDao.updateState(
                        job.jobId,
                        if (terminal) "FAILED" else "WAITING_RETRY",
                        if (terminal) "FAILED" else "PAUSED",
                        nextRetry,
                        result.code.take(160)
                    )
                    if (terminal) failed += 1 else paused += 1
                }
            }
        }
        return RafGitFsOfflineQueueReport(jobs.size, completed, paused, failed)
    }
}

data class RafGitFsOfflineQueueReport(
    val attempted: Int,
    val completed: Int,
    val paused: Int,
    val failed: Int
)
