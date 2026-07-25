package com.rafgittools.rafgitfs.write

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RafGitFsWriteNetworkModule {
    @Provides
    @Singleton
    fun provideRafGitFsGithubWriteApiService(
        retrofit: Retrofit
    ): RafGitFsGithubWriteApiService = retrofit.create(RafGitFsGithubWriteApiService::class.java)
}
