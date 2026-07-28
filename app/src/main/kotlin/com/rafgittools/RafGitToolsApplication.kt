package com.rafgittools

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.rafgittools.core.error.ErrorHandler
import com.rafgittools.core.error.GlobalExceptionHandler
import com.rafgittools.core.error.PersistentErrorLogger
import com.rafgittools.core.localization.LocalizationManager
import com.rafgittools.data.auth.AuthRepository
import com.rafgittools.data.auth.AuthTokenCache
import com.rafgittools.data.preferences.PreferencesRepository
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rafgittools.offline.RepositorySyncWorker
import com.rafgittools.offline.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * RafGitTools Application class
 * 
 * This is the main application class that initializes Hilt dependency injection,
 * manages application-level components, and handles global language configuration.
 */
@HiltAndroidApp
class RafGitToolsApplication : Application() {
    
    @Inject
    lateinit var localizationManager: LocalizationManager
    
    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var authTokenCache: AuthTokenCache

    @Inject
    lateinit var errorLogger: PersistentErrorLogger
    
    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + ErrorHandler.createCoroutineExceptionHandler("app-scope")
    )
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize application-level components
        // This is where we would set up crash reporting, analytics, etc.
        ErrorHandler.initialize(errorLogger)
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())
        
        // Apply saved language preference
        // Note: This runs asynchronously. The MainActivity may start before this completes,
        // but the language will be properly applied when the user opens the app again.
        // For immediate application, the MainActivity also checks and applies the language
        // when it's created. A future enhancement could make this synchronous or add
        // proper synchronization to ensure language is always applied before UI is shown.
        applicationScope.launch {
            preferencesRepository.hydrateLanguageSyncCache()
            val savedLanguage = preferencesRepository.getLanguageSync()
            localizationManager.applyLocale(this@RafGitToolsApplication, savedLanguage)
            authTokenCache.token = authRepository.getPat().getOrNull()
        }

        schedulePeriodicSync()
        scheduleRepositorySync()
    }

    private fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleRepositorySync() {
        val request = PeriodicWorkRequestBuilder<RepositorySyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RepositorySyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Re-apply language settings when configuration changes
        applicationScope.launch {
            preferencesRepository.hydrateLanguageSyncCache()
            val savedLanguage = preferencesRepository.getLanguageSync()
            localizationManager.applyLocale(this@RafGitToolsApplication, savedLanguage)
            authTokenCache.token = authRepository.getPat().getOrNull()
        }
    }
}
