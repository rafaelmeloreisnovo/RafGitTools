package com.rafgittools.core.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages application localization and language changes
 */
@Singleton
class LocalizationManager @Inject constructor() {
    private val localeMutex = Mutex()
    
    /**
     * Updates the application context with the specified language
     * 
     * Since the app's minimum SDK is 24 (Android 7.0), we can always use
     * the modern createConfigurationContext method.
     */
    fun setLocale(context: Context, language: Language): Context {
        val locale = language.locale
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return context.createConfigurationContext(configuration)
    }

    /**
     * Apply locale safely during app startup to avoid race conditions.
     */
    suspend fun applyLocale(context: Context, language: Language): Context {
        return localeMutex.withLock {
            setLocale(context, language)
        }
    }
    
    /**
     * Gets the current system locale
     * 
     * Since the app's minimum SDK is 24 (Android 7.0), we can use the
     * modern locales API.
     */
    fun getCurrentLocale(context: Context): Locale {
        return context.resources.configuration.locales[0]
    }
    
    /**
     * Gets the current language based on system locale
     */
    fun getCurrentLanguage(context: Context): Language {
        val locale = getCurrentLocale(context)
        return Language.fromLocale(locale)
    }
}
