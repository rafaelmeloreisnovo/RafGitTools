package com.rafgittools.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rafgittools.offline.OfflineOperationDao
import com.rafgittools.offline.OfflineOperationEntity

/**
 * Room database for caching
 *
 * Contains tables for caching repository names, user data, generic content,
 * durable offline operations, and local repository sync state.
 */
@Database(
    entities = [
        CacheEntry::class,
        RepositoryNameCache::class,
        UserCache::class,
        OfflineOperationEntity::class,
        LocalRepositoryEntity::class
    ],
    version = 5,
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
    }

    abstract fun cacheDao(): CacheDao
    abstract fun repositoryNameCacheDao(): RepositoryNameCacheDao
    abstract fun userCacheDao(): UserCacheDao
    abstract fun offlineOperationDao(): OfflineOperationDao
    abstract fun localRepositoryDao(): LocalRepositoryDao
}
