package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Upsert
    suspend fun upsert(workspace: WorkspaceEntity)

    @Query("SELECT * FROM workspaces WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun observeAll(profileId: String): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE workspaceId = :workspaceId LIMIT 1")
    suspend fun getById(workspaceId: String): WorkspaceEntity?

    @Query("DELETE FROM workspaces WHERE workspaceId = :workspaceId")
    suspend fun deleteLocalWorkspace(workspaceId: String): Int
}

@Dao
interface TransferJobDao {
    @Upsert
    suspend fun upsert(job: TransferJobEntity)

    @Query(
        """SELECT * FROM transfer_jobs
           WHERE profileId = :profileId
             AND syncState IN ('SCANNING','DIFF_READY','PLAN_READY','APPROVAL_REQUIRED','EXECUTING','PAUSED')
           ORDER BY updatedAt DESC"""
    )
    fun observeActive(profileId: String): Flow<List<TransferJobEntity>>

    @Query(
        """SELECT * FROM transfer_jobs
           WHERE profileId=:profileId
             AND operationType IN ('CACHE_DOWNLOAD','PIN_OFFLINE')
             AND syncState IN ('PAUSED','FAILED')
             AND retryCount < maxRetries
           ORDER BY createdAt ASC
           LIMIT :limit"""
    )
    suspend fun listResumableCacheJobs(profileId: String, limit: Int): List<TransferJobEntity>

    @Query("SELECT * FROM transfer_jobs WHERE jobId = :jobId LIMIT 1")
    suspend fun getById(jobId: String): TransferJobEntity?

    @Query(
        """UPDATE transfer_jobs
           SET bytesCompleted = :bytesCompleted, bytesTotal = :bytesTotal, updatedAt = :updatedAt
           WHERE jobId = :jobId"""
    )
    suspend fun updateProgress(
        jobId: String,
        bytesCompleted: Long,
        bytesTotal: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """UPDATE transfer_jobs
           SET phase = :phase, syncState = :syncState, lastErrorCode = :lastErrorCode,
               retryCount = :retryCount, updatedAt = :updatedAt
           WHERE jobId = :jobId"""
    )
    suspend fun updateState(
        jobId: String,
        phase: String,
        syncState: String,
        retryCount: Int,
        lastErrorCode: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface StagedOperationDao {
    @Upsert
    suspend fun upsert(operation: StagedOperationEntity)

    @Query("SELECT * FROM staged_operations WHERE workspaceId = :workspaceId ORDER BY createdAt ASC")
    suspend fun listForWorkspace(workspaceId: String): List<StagedOperationEntity>

    @Query("SELECT * FROM staged_operations WHERE jobId = :jobId ORDER BY createdAt ASC")
    suspend fun listForJob(jobId: String): List<StagedOperationEntity>

    @Query("DELETE FROM staged_operations WHERE operationId = :operationId")
    suspend fun delete(operationId: String): Int
}

@Dao
interface SyncConflictDao {
    @Upsert
    suspend fun upsert(conflict: SyncConflictEntity)

    @Query(
        """SELECT * FROM sync_conflicts
           WHERE workspaceId = :workspaceId AND resolvedAt IS NULL
           ORDER BY detectedAt ASC"""
    )
    fun observeUnresolved(workspaceId: String): Flow<List<SyncConflictEntity>>

    @Query(
        """UPDATE sync_conflicts
           SET resolution = :resolution, resolvedAt = :resolvedAt
           WHERE conflictId = :conflictId AND resolvedAt IS NULL"""
    )
    suspend fun resolve(conflictId: String, resolution: String, resolvedAt: Long = System.currentTimeMillis()): Int
}

/** Append-only API: insert and read are exposed; update/delete are intentionally absent. */
@Dao
interface OperationReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun append(receipt: OperationReceiptEntity)

    @Query("SELECT * FROM operation_receipts WHERE requestId = :requestId LIMIT 1")
    suspend fun getByRequestId(requestId: String): OperationReceiptEntity?

    @Query(
        """SELECT * FROM operation_receipts
           WHERE profileId = :profileId
           ORDER BY createdAt DESC
           LIMIT :limit"""
    )
    fun observeRecent(profileId: String, limit: Int = 100): Flow<List<OperationReceiptEntity>>
}
