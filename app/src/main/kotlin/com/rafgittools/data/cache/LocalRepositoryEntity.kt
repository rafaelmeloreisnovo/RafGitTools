package com.rafgittools.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_repositories")
data class LocalRepositoryEntity(
    @PrimaryKey val path: String,
    val name: String,
    val remoteUrl: String?,
    val currentBranch: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: String = "SYNCED"
)
