package com.rafgittools.rafgitfs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RafGitFsProfileDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: RafGitFsStorageProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: RafGitFsStorageProfileEntity)

    @Query("SELECT * FROM rafgitfs_storage_profiles ORDER BY displayName COLLATE NOCASE ASC")
    fun observeProfiles(): Flow<List<RafGitFsStorageProfileEntity>>

    @Query("SELECT * FROM rafgitfs_storage_profiles WHERE profileId = :profileId LIMIT 1")
    suspend fun getProfile(profileId: String): RafGitFsStorageProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRefs(refs: List<RafGitFsRepositoryRefEntity>)

    @Query(
        """SELECT * FROM rafgitfs_repository_refs
           WHERE profileId = :profileId AND repositoryFullName = :repositoryFullName
           ORDER BY isDefault DESC, refType ASC, refName COLLATE NOCASE ASC"""
    )
    fun observeRefs(
        profileId: String,
        repositoryFullName: String
    ): Flow<List<RafGitFsRepositoryRefEntity>>

    @Query(
        """DELETE FROM rafgitfs_repository_refs
           WHERE profileId = :profileId AND repositoryFullName = :repositoryFullName"""
    )
    suspend fun deleteRefs(profileId: String, repositoryFullName: String)

    @Transaction
    suspend fun replaceRefs(
        profileId: String,
        repositoryFullName: String,
        refs: List<RafGitFsRepositoryRefEntity>
    ) {
        require(refs.all { it.profileId == profileId && it.repositoryFullName == repositoryFullName }) {
            "All refs must belong to the requested profile and repository"
        }
        deleteRefs(profileId, repositoryFullName)
        upsertRefs(refs)
    }
}
