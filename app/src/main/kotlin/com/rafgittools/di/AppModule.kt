package com.rafgittools.di

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.google.gson.FieldNamingPolicy
import com.rafgittools.BuildConfig
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        // FIX L4: log body only in debug builds — PATs/tokens must never appear in prod logs
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        // FIX N2: removed CertificatePinner with placeholder hash (sha256/AAAA…)
        //   A placeholder pin triggers SSLPeerUnverifiedException on every call.
        //   Re-add with the real sha256 public-key pin when releasing to production.
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // FIX N4: FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES maps snake_case JSON
        //   automatically. Fields that don't follow snake_case still need @SerializedName.
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGithubApiService(retrofit: Retrofit): GithubApiService =
        retrofit.create(GithubApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideCacheDatabase(@ApplicationContext context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, "rafgittools_cache.db").build()

    @Provides @Singleton
    fun provideCacheDao(db: CacheDatabase): CacheDao = db.cacheDao()

    @Provides @Singleton
    fun provideRepositoryNameCacheDao(db: CacheDatabase): RepositoryNameCacheDao = db.repositoryNameCacheDao()

    @Provides @Singleton
    fun provideUserCacheDao(db: CacheDatabase): UserCacheDao = db.userCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindGitRepository(impl: GitRepositoryImpl): GitRepository
}
