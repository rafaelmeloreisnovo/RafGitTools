package com.rafgittools.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for caching
 * 
 * Contains tables for caching repository names, user data, and generic content
 */
@Database(
    entities = [
        CacheEntry::class,
        RepositoryNameCache::class,
        UserCache::class
    ],
    version = 2,
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
    }
    
    abstract fun cacheDao(): CacheDao
    abstract fun repositoryNameCacheDao(): RepositoryNameCacheDao
    abstract fun userCacheDao(): UserCacheDao
}
