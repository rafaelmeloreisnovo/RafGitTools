package com.rafgittools.di

import android.content.Context
import androidx.room.Room
import com.rafgittools.data.auth.AuthInterceptor
import com.rafgittools.data.cache.CacheDao
import com.rafgittools.data.cache.CacheDatabase
import com.rafgittools.data.cache.RepositoryNameCacheDao
import com.rafgittools.data.cache.UserCacheDao
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.repository.GitRepositoryImpl
import com.rafgittools.domain.repository.GitRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.CertificatePinner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Configure certificate pinning for api.github.com
        // NOTE: Replace the placeholder pin value below with the actual, up-to-date
        // SHA-256 public key hash for api.github.com before releasing to production.
        val certificatePinner = CertificatePinner.Builder()
            .add(
                "api.github.com",
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            )
            .build()
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Add auth interceptor for PAT authentication
            .addInterceptor(loggingInterceptor)
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideGithubApiService(retrofit: Retrofit): GithubApiService {
        return retrofit.create(GithubApiService::class.java)
    }
}

/**
 * Cache module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    
    @Provides
    @Singleton
    fun provideCacheDatabase(@ApplicationContext context: Context): CacheDatabase {
        return Room.databaseBuilder(
            context,
            CacheDatabase::class.java,
            "rafgittools_cache.db"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideCacheDao(database: CacheDatabase): CacheDao {
        return database.cacheDao()
    }
    
    @Provides
    @Singleton
    fun provideRepositoryNameCacheDao(database: CacheDatabase): RepositoryNameCacheDao {
        return database.repositoryNameCacheDao()
    }
    
    @Provides
    @Singleton
    fun provideUserCacheDao(database: CacheDatabase): UserCacheDao {
        return database.userCacheDao()
    }
}

/**
 * Repository module for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindGitRepository(
        gitRepositoryImpl: GitRepositoryImpl
    ): GitRepository
}
