package com.rafgittools.di

import android.content.Context
import androidx.room.Room
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.rafgittools.BuildConfig
import com.rafgittools.data.auth.AuthInterceptor
import com.rafgittools.data.cache.CacheDao
import com.rafgittools.data.cache.CacheDatabase
import com.rafgittools.data.cache.LocalRepositoryDao
import com.rafgittools.data.cache.RepositoryNameCacheDao
import com.rafgittools.data.cache.UserCacheDao
import com.rafgittools.data.github.GithubApiService
import com.rafgittools.data.repository.GitRepositoryImpl
import com.rafgittools.domain.repository.GitRepository
import com.rafgittools.offline.OfflineOperationDao
import com.rafgittools.offline.RoomOfflineQueueStorage
import com.rafgittools.rafgitfs.data.ContentCacheDao
import com.rafgittools.rafgitfs.data.OperationReceiptDao
import com.rafgittools.rafgitfs.data.RepositoryRefDao
import com.rafgittools.rafgitfs.data.StagedOperationDao
import com.rafgittools.rafgitfs.data.StorageProfileDao
import com.rafgittools.rafgitfs.data.SyncConflictDao
import com.rafgittools.rafgitfs.data.TransferJobDao
import com.rafgittools.rafgitfs.data.VirtualTreeDao
import com.rafgittools.rafgitfs.data.WorkspaceDao
import com.rafgittools.rafgitfs.remote.RafGitFsGithubApiService
import com.rafgittools.kernel.GovernanceGate
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

    @Provides
    @Singleton
    fun provideRafGitFsGithubApiService(retrofit: Retrofit): RafGitFsGithubApiService =
        retrofit.create(RafGitFsGithubApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideCacheDatabase(@ApplicationContext context: Context): CacheDatabase =
        Room.databaseBuilder(context, CacheDatabase::class.java, "rafgittools_cache.db")
            .addMigrations(
                CacheDatabase.MIGRATION_1_2,
                CacheDatabase.MIGRATION_2_3,
                CacheDatabase.MIGRATION_3_4,
                CacheDatabase.MIGRATION_4_5,
                CacheDatabase.MIGRATION_5_6
            )
            .build()

    @Provides @Singleton
    fun provideCacheDao(db: CacheDatabase): CacheDao = db.cacheDao()

    @Provides @Singleton
    fun provideRepositoryNameCacheDao(db: CacheDatabase): RepositoryNameCacheDao = db.repositoryNameCacheDao()

    @Provides @Singleton
    fun provideUserCacheDao(db: CacheDatabase): UserCacheDao = db.userCacheDao()

    @Provides @Singleton
    fun provideOfflineOperationDao(db: CacheDatabase): OfflineOperationDao = db.offlineOperationDao()

    @Provides @Singleton
    fun provideRoomOfflineQueueStorage(dao: OfflineOperationDao): RoomOfflineQueueStorage =
        RoomOfflineQueueStorage(dao)

    @Provides @Singleton
    fun provideLocalRepositoryDao(db: CacheDatabase): LocalRepositoryDao = db.localRepositoryDao()

    @Provides @Singleton
    fun provideStorageProfileDao(db: CacheDatabase): StorageProfileDao = db.storageProfileDao()

    @Provides @Singleton
    fun provideRepositoryRefDao(db: CacheDatabase): RepositoryRefDao = db.repositoryRefDao()

    @Provides @Singleton
    fun provideVirtualTreeDao(db: CacheDatabase): VirtualTreeDao = db.virtualTreeDao()

    @Provides @Singleton
    fun provideContentCacheDao(db: CacheDatabase): ContentCacheDao = db.contentCacheDao()

    @Provides @Singleton
    fun provideWorkspaceDao(db: CacheDatabase): WorkspaceDao = db.workspaceDao()

    @Provides @Singleton
    fun provideTransferJobDao(db: CacheDatabase): TransferJobDao = db.transferJobDao()

    @Provides @Singleton
    fun provideStagedOperationDao(db: CacheDatabase): StagedOperationDao = db.stagedOperationDao()

    @Provides @Singleton
    fun provideSyncConflictDao(db: CacheDatabase): SyncConflictDao = db.syncConflictDao()

    @Provides @Singleton
    fun provideOperationReceiptDao(db: CacheDatabase): OperationReceiptDao = db.operationReceiptDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindGitRepository(impl: GitRepositoryImpl): GitRepository
}

@Module
@InstallIn(SingletonComponent::class)
object KernelModule {
    @Provides
    @Singleton
    fun provideGovernanceGate(@ApplicationContext context: Context): GovernanceGate =
        GovernanceGate(context)
}
