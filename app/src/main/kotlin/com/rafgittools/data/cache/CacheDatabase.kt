package com.rafgittools.data.cache

import androidx.room.Database
import androidx.room.RoomDatabase

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
    version = 1,
    exportSchema = true
)
abstract class CacheDatabase : RoomDatabase() {
    
    abstract fun cacheDao(): CacheDao
    abstract fun repositoryNameCacheDao(): RepositoryNameCacheDao
    abstract fun userCacheDao(): UserCacheDao
}
