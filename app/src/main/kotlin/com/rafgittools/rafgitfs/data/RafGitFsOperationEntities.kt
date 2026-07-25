package com.rafgittools.rafgitfs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rafgitfs_transfer_jobs",
    foreignKeys = [
        ForeignKey(
            entity = RafGitFsStorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_rafgitfs_jobs_profile"),
        Index(value = ["status", "updatedAt"], name = "idx_rafgitfs_jobs_status_updated"),
        Index(value = ["repositoryFullName", "refName"], name = "idx_rafgitfs_jobs_target")
    ],
    primaryKeys = ["jobId"]
)
data class RafGitFsTransferJobEntity(
    val jobId: String,
    val profileId: String,
    val operation: String,
    val phase: String,
    val status: String,
    val repositoryFullName: String?,
    val refName: String?,
    val targetPath: String?,
    val totalBytes: Long?,
    val transferredBytes: Long,
    val retryCount: Int,
    val maxRetries: Int,
    val errorCode: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "rafgitfs_staged_operations",
    foreignKeys = [
        ForeignKey(
            entity = RafGitFsStorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_rafgitfs_staged_profile"),
        Index(value = ["workspaceId"], name = "idx_rafgitfs_staged_workspace"),
        Index(value = ["jobId"], name = "idx_rafgitfs_staged_job"),
        Index(value = ["state", "createdAt"], name = "idx_rafgitfs_staged_state")
    ],
    primaryKeys = ["operationId"]
)
data class RafGitFsStagedOperationEntity(
    val operationId: String,
    val profileId: String,
    val workspaceId: String?,
    val jobId: String?,
    val operation: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val baseSha: String?,
    val contentSha: String?,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "rafgitfs_sync_conflicts",
    foreignKeys = [
        ForeignKey(
            entity = RafGitFsStorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_rafgitfs_conflicts_profile"),
        Index(value = ["state", "detectedAt"], name = "idx_rafgitfs_conflicts_state"),
        Index(
            value = ["profileId", "repositoryFullName", "refName", "path"],
            name = "idx_rafgitfs_conflicts_identity"
        )
    ],
    primaryKeys = ["conflictId"]
)
data class RafGitFsSyncConflictEntity(
    val conflictId: String,
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val conflictType: String,
    val state: String,
    val baseSha: String?,
    val localSha: String?,
    val remoteSha: String?,
    val detectedAt: Long,
    val resolvedAt: Long?,
    val resolutionReceiptId: String?
)

@Entity(
    tableName = "rafgitfs_operation_receipts",
    foreignKeys = [
        ForeignKey(
            entity = RafGitFsStorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_rafgitfs_receipts_profile"),
        Index(value = ["operation", "createdAt"], name = "idx_rafgitfs_receipts_operation"),
        Index(value = ["requestHash"], unique = true, name = "idx_rafgitfs_receipts_request_hash")
    ],
    primaryKeys = ["receiptId"]
)
data class RafGitFsOperationReceiptEntity(
    val receiptId: String,
    val profileId: String,
    val operation: String,
    val phase: String,
    val allowed: Boolean,
    val epistemicState: String,
    val claimAllowed: Boolean,
    val requestHash: String,
    val resultHash: String?,
    val reasonCode: String,
    val fOk: String,
    val fGap: String,
    val fNext: String,
    val createdAt: Long
)
