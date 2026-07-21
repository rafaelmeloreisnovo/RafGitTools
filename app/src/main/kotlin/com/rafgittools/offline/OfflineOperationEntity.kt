package com.rafgittools.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_operations")
data class OfflineOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoPath: String,
    val command: String,
    val args: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
