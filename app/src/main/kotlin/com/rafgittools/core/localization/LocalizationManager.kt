package com.rafgittools.core.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages application localization and language changes
 */
@Singleton
class LocalizationManager @Inject constructor() {
    
    /**
     * Updates the application context with the specified language
     */
    fun setLocale(context: Context, language: Language): Context {
        val locale = language.locale
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Gets the current system locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * Gets the current language based on system locale
     */
    fun getCurrentLanguage(context: Context): Language {
        val locale = getCurrentLocale(context)
        return Language.fromLocale(locale)
    }
}
