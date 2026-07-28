package com.rafgittools.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalRepositoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: LocalRepositoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalRepositoryEntity)

    @Query("SELECT * FROM local_repositories ORDER BY name ASC")
    suspend fun loadAll(): List<LocalRepositoryEntity>

    @Query("SELECT * FROM local_repositories ORDER BY name ASC")
    fun observeAll(): Flow<List<LocalRepositoryEntity>>

    @Query("UPDATE local_repositories SET syncState = :syncState, lastUpdated = :updatedAt WHERE path = :path")
    suspend fun updateSyncState(path: String, syncState: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM local_repositories WHERE path = :path")
    suspend fun delete(path: String)
}
