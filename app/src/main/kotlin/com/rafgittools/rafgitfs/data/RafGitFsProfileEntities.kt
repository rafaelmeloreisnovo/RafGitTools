package com.rafgittools.rafgitfs.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "rafgitfs_storage_profiles",
    indices = [
        Index(value = ["provider", "owner"], name = "idx_rafgitfs_profiles_provider_owner"),
        Index(value = ["updatedAt"], name = "idx_rafgitfs_profiles_updated")
    ],
    primaryKeys = ["profileId"]
)
data class RafGitFsStorageProfileEntity(
    val profileId: String,
    val displayName: String,
    val provider: String,
    val scope: String,
    val owner: String,
    val selectedRepositoriesJson: String,
    val defaultRef: String,
    val accessMode: String,
    val cacheMode: String,
    val writePolicy: String,
    val receiptRequired: Boolean,
    val protectedBranchWrite: Boolean,
    val deleteAllowed: Boolean,
    val maxCacheBytes: Long,
    val largeFileThresholdBytes: Long,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "rafgitfs_repository_refs",
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
        Index(value = ["profileId"], name = "idx_rafgitfs_refs_profile"),
        Index(value = ["repositoryFullName", "refType"], name = "idx_rafgitfs_refs_repo_type"),
        Index(value = ["commitSha"], name = "idx_rafgitfs_refs_commit_sha")
    ],
    primaryKeys = ["profileId", "repositoryFullName", "refName"]
)
data class RafGitFsRepositoryRefEntity(
    val profileId: String,
    val repositoryFullName: String,
    val refName: String,
    val refType: String,
    val commitSha: String?,
    val isDefault: Boolean,
    val lastIndexedAt: Long
)
