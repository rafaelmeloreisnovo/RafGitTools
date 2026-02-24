package com.rafgittools.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache entry entity for Room database
 * 
 * Stores cached content with expiration time for async caching
 */
@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey
    val key: String,
    val content: String,
    val contentType: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val etag: String? = null
)

/**
 * Repository name cache entity
 * 
 * Caches repository names for quick access
 */
@Entity(tableName = "repository_name_cache")
data class RepositoryNameCache(
    @PrimaryKey
    val id: Long,
    val name: String,
    val fullName: String,
    val ownerLogin: String,
    val description: String?,
    val language: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val isPrivate: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User cache entity
 * 
 * Caches user information for quick access
 */
@Entity(tableName = "user_cache")
data class UserCache(
    @PrimaryKey
    val id: Long,
    val login: String,
    val avatarUrl: String,
    val name: String?,
    val bio: String?,
    val location: String?,
    val company: String?,
    val updatedAt: Long = System.currentTimeMillis()
)
