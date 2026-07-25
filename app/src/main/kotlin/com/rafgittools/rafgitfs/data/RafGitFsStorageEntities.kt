package com.rafgittools.rafgitfs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rafgitfs_virtual_tree_entries",
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
        Index(value = ["profileId"], name = "idx_rafgitfs_tree_profile"),
        Index(value = ["repositoryFullName", "refName", "parentPath"], name = "idx_rafgitfs_tree_parent"),
        Index(value = ["gitSha"], name = "idx_rafgitfs_tree_sha"),
        Index(value = ["lastIndexedAt"], name = "idx_rafgitfs_tree_indexed")
    ],
    primaryKeys = ["profileId", "repositoryFullName", "refName", "path"]
)
data class RafGitFsVirtualTreeEntryEntity(
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
    val downloadUrl: String?,
    val cacheState: String,
    val localPath: String?,
    val etag: String?,
    val lastIndexedAt: Long,
    val lastAccessedAt: Long
)

@Entity(
    tableName = "rafgitfs_content_cache",
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
        Index(value = ["profileId"], name = "idx_rafgitfs_cache_profile"),
        Index(
            value = ["profileId", "repositoryFullName", "refName", "path"],
            unique = true,
            name = "idx_rafgitfs_cache_identity"
        ),
        Index(value = ["lastAccessedAt"], name = "idx_rafgitfs_cache_lru"),
        Index(value = ["expiresAt"], name = "idx_rafgitfs_cache_expiry")
    ],
    primaryKeys = ["cacheKey"]
)
data class RafGitFsContentCacheEntity(
    val cacheKey: String,
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val path: String,
    val gitSha: String,
    val localPath: String,
    val sizeBytes: Long,
    val cacheState: String,
    val pinnedOffline: Boolean,
    val checksumAlgorithm: String,
    val checksumValue: String,
    val downloadedAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long?
)

@Entity(
    tableName = "rafgitfs_workspaces",
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
        Index(value = ["profileId"], name = "idx_rafgitfs_workspaces_profile"),
        Index(value = ["repositoryFullName", "state"], name = "idx_rafgitfs_workspaces_repo_state"),
        Index(value = ["localRoot"], unique = true, name = "idx_rafgitfs_workspaces_local_root")
    ],
    primaryKeys = ["workspaceId"]
)
data class RafGitFsWorkspaceEntity(
    val workspaceId: String,
    val profileId: String,
    val repositoryFullName: String,
    val baseRef: String,
    val branchName: String?,
    val baseCommitSha: String?,
    val localRoot: String,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long
)
