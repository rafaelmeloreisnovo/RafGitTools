package com.rafgittools.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineOperationDao {

    @Query("SELECT * FROM offline_operations ORDER BY id ASC")
    suspend fun loadAll(): List<OfflineOperationEntity>

    @Query("SELECT * FROM offline_operations ORDER BY id ASC")
    fun observeAll(): Flow<List<OfflineOperationEntity>>

    @Query("SELECT COUNT(*) FROM offline_operations")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<OfflineOperationEntity>)

    @Query("DELETE FROM offline_operations")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(operations: List<OfflineOperationEntity>) {
        deleteAll()
        if (operations.isNotEmpty()) insertAll(operations)
    }
}
