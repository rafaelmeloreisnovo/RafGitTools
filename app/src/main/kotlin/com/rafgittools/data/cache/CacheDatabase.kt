package com.rafgittools.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rafgittools.offline.OfflineOperationDao
import com.rafgittools.offline.OfflineOperationEntity
import com.rafgittools.rafgitfs.data.ContentCacheDao
import com.rafgittools.rafgitfs.data.ContentCacheEntity
import com.rafgittools.rafgitfs.data.OperationReceiptDao
import com.rafgittools.rafgitfs.data.OperationReceiptEntity
import com.rafgittools.rafgitfs.data.RafGitFsRoomV6
import com.rafgittools.rafgitfs.data.RepositoryRefDao
import com.rafgittools.rafgitfs.data.RepositoryRefEntity
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StagedOperationEntity
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.StorageProfileEntity
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.SyncConflictEntity
import com.rafgittools.rafgitfs.data.TransferJobDao
import com.rafgittools.rafgitfs.data.TransferJobEntity
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import com.rafgittools.rafgitfs.data.VirtualTreeEntryEntity
import com.rafgittools.rafgitfs.data.WorkspaceDao
import com.rafgittools.rafgitfs.data.WorkspaceEntity

/**
 * Room database for caching and RafGitFS local metadata.
 *
 * GitHub remains the remote authority. Room is a reconstructible local index,
 * bounded content-cache registry, operation queue and append-only receipt store.
 */
@Database(
    entities = [
        CacheEntry::class,
        RepositoryNameCache::class,
        UserCache::class,
        OfflineOperationEntity::class,
        LocalRepositoryEntity::class,
        StorageProfileEntity::class,
        RepositoryRefEntity::class,
        VirtualTreeEntryEntity::class,
        ContentCacheEntity::class,
        WorkspaceEntity::class,
        TransferJobEntity::class,
        StagedOperationEntity::class,
        SyncConflictEntity::class,
        OperationReceiptEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class CacheDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE cache_entries ADD COLUMN lastAccessedAt INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "UPDATE cache_entries SET lastAccessedAt = createdAt WHERE lastAccessedAt = 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS offline_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        repoPath TEXT NOT NULL,
                        command TEXT NOT NULL,
                        args TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS local_repositories (
                        path TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        remoteUrl TEXT,
                        currentBranch TEXT,
                        lastUpdated INTEGER NOT NULL,
                        syncState TEXT NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN watchersCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN openIssuesCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN isFork INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN defaultBranch TEXT NOT NULL DEFAULT 'main'")
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN createdAtGh TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE repository_name_cache ADD COLUMN updatedAtGh TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(
            RafGitFsRoomV6.FROM_VERSION,
            RafGitFsRoomV6.TO_VERSION
        ) {
            override fun migrate(database: SupportSQLiteDatabase) {
                RafGitFsRoomV6.createStatements.forEach(database::execSQL)
            }
        }
    }

    abstract fun cacheDao(): CacheDao
    abstract fun repositoryNameCacheDao(): RepositoryNameCacheDao
    abstract fun userCacheDao(): UserCacheDao
    abstract fun offlineOperationDao(): OfflineOperationDao
    abstract fun localRepositoryDao(): LocalRepositoryDao

    abstract fun storageProfileDao(): StorageProfileDao
    abstract fun repositoryRefDao(): RepositoryRefDao
    abstract fun virtualTreeDao(): VirtualTreeDao
    abstract fun contentCacheDao(): ContentCacheDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun transferJobDao(): TransferJobDao
    abstract fun stagedOperationDao(): StagedOperationDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun operationReceiptDao(): OperationReceiptDao
}
