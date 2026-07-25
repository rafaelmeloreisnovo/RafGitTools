package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RafGitFsTreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<RafGitFsVirtualTreeEntryEntity>)

    @Query(
        """SELECT * FROM rafgitfs_virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND parentPath = :parentPath
           ORDER BY CASE entryType WHEN 'DIRECTORY' THEN 0 ELSE 1 END, name COLLATE NOCASE ASC"""
    )
    fun observeChildren(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        parentPath: String
    ): Flow<List<RafGitFsVirtualTreeEntryEntity>>

    @Query(
        """SELECT * FROM rafgitfs_virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND path = :path
           LIMIT 1"""
    )
    suspend fun getEntry(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String
    ): RafGitFsVirtualTreeEntryEntity?

    @Query(
        """SELECT * FROM rafgitfs_virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND (name LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%')
           ORDER BY path COLLATE NOCASE ASC
           LIMIT :limit"""
    )
    suspend fun search(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        query: String,
        limit: Int
    ): List<RafGitFsVirtualTreeEntryEntity>

    @Query(
        """DELETE FROM rafgitfs_virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName"""
    )
    suspend fun deleteTree(profileId: String, repositoryFullName: String, refName: String)

    @Transaction
    suspend fun replaceTree(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        entries: List<RafGitFsVirtualTreeEntryEntity>
    ) {
        require(entries.all {
            it.profileId == profileId &&
                it.repositoryFullName == repositoryFullName &&
                it.refName == refName
        }) { "All entries must belong to the requested tree" }
        deleteTree(profileId, repositoryFullName, refName)
        upsertEntries(entries)
    }
}
