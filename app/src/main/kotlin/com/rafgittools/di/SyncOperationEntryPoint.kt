package com.rafgittools.di

import com.rafgittools.data.git.JGitService
import com.rafgittools.data.github.GithubApiService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncOperationEntryPoint {
    fun jGitService(): JGitService
    fun githubApiService(): GithubApiService
}
