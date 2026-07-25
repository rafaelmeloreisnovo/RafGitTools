package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageProfileDao {
    @Upsert
    suspend fun upsert(profile: StorageProfileEntity)

    @Query("SELECT * FROM storage_profiles ORDER BY displayName ASC")
    fun observeAll(): Flow<List<StorageProfileEntity>>

    @Query("SELECT * FROM storage_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getById(profileId: String): StorageProfileEntity?

    @Query("UPDATE storage_profiles SET isEnabled = :enabled, updatedAt = :updatedAt WHERE profileId = :profileId")
    suspend fun setEnabled(profileId: String, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM storage_profiles WHERE profileId = :profileId")
    suspend fun deleteLocalProfile(profileId: String)
}

@Dao
interface RepositoryRefDao {
    @Upsert
    suspend fun upsertAll(refs: List<RepositoryRefEntity>)

    @Query(
        """SELECT * FROM repository_refs
           WHERE profileId = :profileId AND repositoryFullName = :repositoryFullName
           ORDER BY isDefault DESC, refType ASC, refName ASC"""
    )
    fun observeForRepository(
        profileId: String,
        repositoryFullName: String
    ): Flow<List<RepositoryRefEntity>>

    @Query(
        """SELECT * FROM repository_refs
           WHERE profileId = :profileId AND repositoryFullName = :repositoryFullName AND refName = :refName
           LIMIT 1"""
    )
    suspend fun get(
        profileId: String,
        repositoryFullName: String,
        refName: String
    ): RepositoryRefEntity?

    @Query(
        """DELETE FROM repository_refs
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND lastIndexedAt < :indexedBefore"""
    )
    suspend fun deleteStale(
        profileId: String,
        repositoryFullName: String,
        indexedBefore: Long
    ): Int
}

@Dao
interface VirtualTreeDao {
    @Upsert
    suspend fun upsertAll(entries: List<VirtualTreeEntryEntity>)

    @Query(
        """SELECT * FROM virtual_tree_entries
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
    ): Flow<List<VirtualTreeEntryEntity>>

    @Query(
        """SELECT * FROM virtual_tree_entries
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
    ): VirtualTreeEntryEntity?

    @Query(
        """SELECT COUNT(*) FROM virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName"""
    )
    suspend fun countForRef(
        profileId: String,
        repositoryFullName: String,
        refName: String
    ): Int

    @Query(
        """SELECT path FROM virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND isFavorite = 1"""
    )
    suspend fun listFavoritePaths(
        profileId: String,
        repositoryFullName: String,
        refName: String
    ): List<String>

    @Query(
        """SELECT * FROM virtual_tree_entries
           WHERE profileId = :profileId
             AND (name LIKE '%' || :query || '%' OR path LIKE '%' || :query || '%')
           ORDER BY isFavorite DESC, name COLLATE NOCASE ASC
           LIMIT :limit"""
    )
    suspend fun search(profileId: String, query: String, limit: Int = 100): List<VirtualTreeEntryEntity>

    @Query(
        """UPDATE virtual_tree_entries
           SET isFavorite = :favorite, lastAccessedAt = :updatedAt
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND path = :path"""
    )
    suspend fun setFavorite(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        path: String,
        favorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """DELETE FROM virtual_tree_entries
           WHERE profileId = :profileId
             AND repositoryFullName = :repositoryFullName
             AND refName = :refName
             AND lastIndexedAt < :indexedBefore"""
    )
    suspend fun deleteStale(
        profileId: String,
        repositoryFullName: String,
        refName: String,
        indexedBefore: Long
    ): Int
}
