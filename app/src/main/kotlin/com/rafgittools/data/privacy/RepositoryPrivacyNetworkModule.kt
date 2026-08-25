package com.rafgittools.data.privacy

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryPrivacyNetworkModule {
    @Provides
    @Singleton
    fun provideRepositoryPrivacyApi(retrofit: Retrofit): RepositoryPrivacyApi =
        retrofit.create(RepositoryPrivacyApi::class.java)
}
