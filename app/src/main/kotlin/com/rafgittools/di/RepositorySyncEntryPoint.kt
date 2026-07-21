package com.rafgittools.di

import com.rafgittools.data.cache.LocalRepositoryDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RepositorySyncEntryPoint {
    fun localRepositoryDao(): LocalRepositoryDao
}
