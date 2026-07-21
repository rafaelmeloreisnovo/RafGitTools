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
 * and durable offline operations.
 */
@Database(
    entities = [
        CacheEntry::class,
        RepositoryNameCache::class,
        UserCache::class,
        OfflineOperationEntity::class
    ],
    version = 3,
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
    }

    abstract fun cacheDao(): CacheDao
    abstract fun repositoryNameCacheDao(): RepositoryNameCacheDao
    abstract fun userCacheDao(): UserCacheDao
    abstract fun offlineOperationDao(): OfflineOperationDao
}
