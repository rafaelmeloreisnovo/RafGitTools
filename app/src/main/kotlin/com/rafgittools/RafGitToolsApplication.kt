package com.rafgittools

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.rafgittools.core.localization.Language
import com.rafgittools.core.localization.LocalizationManager
import com.rafgittools.data.preferences.PreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize application-level components
        // This is where we would set up crash reporting, analytics, etc.
        
        // Apply saved language preference
        // Note: This runs asynchronously. The MainActivity may start before this completes,
        // but the language will be properly applied when the user opens the app again.
        // For immediate application, the MainActivity also checks and applies the language
        // when it's created. A future enhancement could make this synchronous or add
        // proper synchronization to ensure language is always applied before UI is shown.
        applicationScope.launch {
            val savedLanguage = preferencesRepository.getLanguage()
            localizationManager.setLocale(this@RafGitToolsApplication, savedLanguage)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Re-apply language settings when configuration changes
        applicationScope.launch {
            val savedLanguage = preferencesRepository.getLanguage()
            localizationManager.setLocale(this@RafGitToolsApplication, savedLanguage)
        }
    }
}
