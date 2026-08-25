package com.rafgittools.rafgitfs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent storage profile. Secrets and OAuth tokens are deliberately excluded.
 */
@Entity(
    tableName = "storage_profiles",
    indices = [
        Index(value = ["owner"], name = "idx_storage_profiles_owner"),
        Index(value = ["isEnabled"], name = "idx_storage_profiles_enabled")
    ]
)
data class StorageProfileEntity(
    @PrimaryKey val profileId: String,
    val displayName: String,
    val provider: String,
    val scope: String,
    val owner: String,
    val selectedRepositoriesJson: String,
    val defaultRef: String,
    val accessMode: String,
    val cachePolicy: String,
    val writePolicy: String,
    val maxCacheBytes: Long,
    val receiptRequired: Boolean,
    val protectedBranchWrite: Boolean,
    val deleteEnabled: Boolean,
    val claimAllowed: Boolean,
    val isEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "repository_refs",
    primaryKeys = ["profileId", "repositoryFullName", "refName"],
    foreignKeys = [
        ForeignKey(
            entity = StorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_repository_refs_profile"),
        Index(value = ["repositoryFullName"], name = "idx_repository_refs_repo"),
        Index(value = ["gitSha"], name = "idx_repository_refs_sha")
    ]
)
data class RepositoryRefEntity(
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val refType: String,
    val gitSha: String?,
    val isDefault: Boolean,
    val lastIndexedAt: Long
)

@Entity(
    tableName = "virtual_tree_entries",
    primaryKeys = ["profileId", "repositoryFullName", "refName", "path"],
    foreignKeys = [
        ForeignKey(
            entity = StorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_virtual_tree_profile"),
        Index(
            value = ["profileId", "repositoryFullName", "refName", "parentPath"],
            name = "idx_virtual_tree_children"
        ),
        Index(value = ["gitSha"], name = "idx_virtual_tree_sha"),
        Index(value = ["isFavorite"], name = "idx_virtual_tree_favorite")
    ]
)
data class VirtualTreeEntryEntity(
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val parentPath: String,
    val name: String,
    val entryType: String,
    val gitSha: String?,
    val sizeBytes: Long?,
    val mimeType: String?,
    val cacheState: String,
    val localPath: String?,
    val isFavorite: Boolean,
    val lastIndexedAt: Long,
    val lastAccessedAt: Long
)

@Entity(
    tableName = "content_cache",
    foreignKeys = [
        ForeignKey(
            entity = StorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_content_cache_profile"),
        Index(
            value = ["profileId", "repositoryFullName", "refName", "path", "gitSha"],
            name = "idx_content_cache_identity",
            unique = true
        ),
        Index(value = ["pinned", "lastAccessedAt"], name = "idx_content_cache_lru"),
        Index(value = ["expiresAt"], name = "idx_content_cache_expiry")
    ]
)
data class ContentCacheEntity(
    @PrimaryKey val cacheKey: String,
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val gitSha: String,
    val localPath: String,
    val sizeBytes: Long,
    val cacheState: String,
    val pinned: Boolean,
    val checksumAlgorithm: String,
    val checksumHex: String,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long?
)

@Entity(
    tableName = "workspaces",
    foreignKeys = [
        ForeignKey(
            entity = StorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_workspaces_profile"),
        Index(value = ["repositoryFullName"], name = "idx_workspaces_repo"),
        Index(value = ["state"], name = "idx_workspaces_state")
    ]
)
data class WorkspaceEntity(
    @PrimaryKey val workspaceId: String,
    val profileId: String,
    val repositoryFullName: String,
    val baseRef: String,
    val branchName: String?,
    val localRoot: String,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
    val claimAllowed: Boolean
)

@Entity(
    tableName = "transfer_jobs",
    foreignKeys = [
        ForeignKey(
            entity = StorageProfileEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"], name = "idx_transfer_jobs_profile"),
        Index(value = ["syncState"], name = "idx_transfer_jobs_state"),
        Index(value = ["updatedAt"], name = "idx_transfer_jobs_updated")
    ]
)
data class TransferJobEntity(
    @PrimaryKey val jobId: String,
    val profileId: String,
    val requestId: String,
    val operationType: String,
    val phase: String,
    val syncState: String,
    val repositoryFullName: String?,
    val refName: String?,
    val path: String?,
    val bytesTotal: Long?,
    val bytesCompleted: Long,
    val retryCount: Int,
    val maxRetries: Int,
    val lastErrorCode: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val claimAllowed: Boolean
)

@Entity(
    tableName = "staged_operations",
    indices = [
        Index(value = ["jobId"], name = "idx_staged_operations_job"),
        Index(value = ["workspaceId"], name = "idx_staged_operations_workspace"),
        Index(value = ["state"], name = "idx_staged_operations_state")
    ]
)
data class StagedOperationEntity(
    @PrimaryKey val operationId: String,
    val jobId: String?,
    val workspaceId: String?,
    val operationType: String,
    val repositoryFullName: String,
    val refName: String?,
    val path: String?,
    val baseSha: String?,
    val localSha: String?,
    val payloadHash: String?,
    val state: String,
    val createdAt: Long
)

@Entity(
    tableName = "sync_conflicts",
    indices = [
        Index(value = ["jobId"], name = "idx_sync_conflicts_job"),
        Index(value = ["workspaceId"], name = "idx_sync_conflicts_workspace"),
        Index(value = ["resolvedAt"], name = "idx_sync_conflicts_resolved")
    ]
)
data class SyncConflictEntity(
    @PrimaryKey val conflictId: String,
    val jobId: String?,
    val workspaceId: String?,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val conflictState: String,
    val localSha: String?,
    val remoteSha: String?,
    val resolution: String?,
    val detectedAt: Long,
    val resolvedAt: Long?
)

/**
 * Append-only receipt. No DAO update/delete method is exposed for this table.
 */
@Entity(
    tableName = "operation_receipts",
    indices = [
        Index(value = ["requestId"], name = "idx_operation_receipts_request", unique = true),
        Index(value = ["profileId"], name = "idx_operation_receipts_profile"),
        Index(value = ["createdAt"], name = "idx_operation_receipts_created")
    ]
)
data class OperationReceiptEntity(
    @PrimaryKey val receiptId: String,
    val requestId: String,
    val profileId: String,
    val operationType: String,
    val finalPhase: String,
    val allowed: Boolean,
    val result: String,
    val evidenceState: String,
    val target: String,
    val observedSha: String?,
    val requestHash: String,
    val receiptHash: String,
    val hashAlgorithm: String,
    val fOkJson: String,
    val fGapJson: String,
    val fNextJson: String,
    val createdAt: Long,
    val claimAllowed: Boolean
)
