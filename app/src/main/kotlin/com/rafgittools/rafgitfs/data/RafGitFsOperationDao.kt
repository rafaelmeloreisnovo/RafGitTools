package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RafGitFsOperationDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJob(job: RafGitFsTransferJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: RafGitFsTransferJobEntity)

    @Query(
        """SELECT * FROM rafgitfs_transfer_jobs
           WHERE profileId = :profileId
           ORDER BY updatedAt DESC"""
    )
    fun observeJobs(profileId: String): Flow<List<RafGitFsTransferJobEntity>>

    @Query(
        """UPDATE rafgitfs_transfer_jobs
           SET status = :status,
               phase = :phase,
               transferredBytes = :transferredBytes,
               retryCount = :retryCount,
               errorCode = :errorCode,
               updatedAt = :updatedAt
           WHERE jobId = :jobId"""
    )
    suspend fun updateJobState(
        jobId: String,
        status: String,
        phase: String,
        transferredBytes: Long,
        retryCount: Int,
        errorCode: String?,
        updatedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStagedOperation(operation: RafGitFsStagedOperationEntity)

    @Query(
        """SELECT * FROM rafgitfs_staged_operations
           WHERE workspaceId = :workspaceId
           ORDER BY createdAt ASC"""
    )
    fun observeStagedOperations(workspaceId: String): Flow<List<RafGitFsStagedOperationEntity>>

    @Query(
        """UPDATE rafgitfs_staged_operations
           SET state = :state, updatedAt = :updatedAt
           WHERE operationId = :operationId"""
    )
    suspend fun updateStagedState(operationId: String, state: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertConflict(conflict: RafGitFsSyncConflictEntity)

    @Query(
        """SELECT * FROM rafgitfs_sync_conflicts
           WHERE profileId = :profileId AND state != 'RESOLVED'
           ORDER BY detectedAt DESC"""
    )
    fun observeOpenConflicts(profileId: String): Flow<List<RafGitFsSyncConflictEntity>>

    @Query(
        """UPDATE rafgitfs_sync_conflicts
           SET state = 'RESOLVED',
               resolvedAt = :resolvedAt,
               resolutionReceiptId = :receiptId
           WHERE conflictId = :conflictId
             AND state != 'RESOLVED'"""
    )
    suspend fun resolveConflict(conflictId: String, receiptId: String, resolvedAt: Long): Int

    /**
     * Receipts are immutable. ABORT rejects accidental overwrite of an existing receipt.
     * No update or delete method is intentionally exposed by this DAO.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun appendReceipt(receipt: RafGitFsOperationReceiptEntity)

    @Query(
        """SELECT * FROM rafgitfs_operation_receipts
           WHERE profileId = :profileId
           ORDER BY createdAt DESC"""
    )
    fun observeReceipts(profileId: String): Flow<List<RafGitFsOperationReceiptEntity>>

    @Query("SELECT * FROM rafgitfs_operation_receipts WHERE receiptId = :receiptId LIMIT 1")
    suspend fun getReceipt(receiptId: String): RafGitFsOperationReceiptEntity?
}
