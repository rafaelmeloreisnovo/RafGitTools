package com.rafgittools.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafgittools.core.localization.Language
import com.rafgittools.ui.theme.CustomTheme
import com.rafgittools.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user preferences using DataStore
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    private val syncPreferences = context.getSharedPreferences("settings_sync", Context.MODE_PRIVATE)
    private val languageMutex = Mutex()
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private const val SYNC_LANGUAGE_KEY = "language_sync"
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val CUSTOM_THEME_KEY = stringPreferencesKey("custom_theme")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        private val HAPTIC_FEEDBACK_KEY = booleanPreferencesKey("haptic_feedback")
    }
    
    /**
     * Flow of the currently selected language
     */
    val languageFlow: Flow<Language> = dataStore.data
        .map { preferences ->
            val languageCode = preferences[LANGUAGE_KEY] ?: Language.ENGLISH.code
            Language.fromCode(languageCode)
        }
    
    /**
     * Flow of dark mode preference
     */
    val isDarkModeFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    
    /**
     * Flow of theme mode preference
     */
    val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val themeModeStr = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(themeModeStr)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }
    
    /**
     * Flow of custom theme preference
     */
    val customThemeFlow: Flow<CustomTheme> = dataStore.data
        .map { preferences ->
            val themeStr = preferences[CUSTOM_THEME_KEY] ?: CustomTheme.GITHUB.name
            CustomTheme.fromName(themeStr) ?: CustomTheme.GITHUB
        }
    
    /**
     * Flow of dynamic color preference
     */
    val dynamicColorFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: true
        }
    
    /**
     * Flow of haptic feedback preference
     */
    val hapticFeedbackFlow: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[HAPTIC_FEEDBACK_KEY] ?: true
        }
    
    /**
     * Set the preferred language
     */
    suspend fun setLanguage(language: Language) {
        languageMutex.withLock {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = language.code
            }
        }
        setLanguageSync(language)
    }

    /**
     * Get the currently selected language
     */
    suspend fun getLanguage(): Language {
        return languageMutex.withLock {
            languageFlow.first()
        }
    }

    /**
     * Get the language synchronously for early startup (fallbacks to DataStore once if needed).
     */
    fun getLanguageSync(): Language {
        val cached = syncPreferences.getString(SYNC_LANGUAGE_KEY, null)
        if (cached != null) {
            return Language.fromCode(cached)
        }
        val fallback = runBlocking(Dispatchers.IO) { languageFlow.first() }
        setLanguageSync(fallback)
        return fallback
    }

    private fun setLanguageSync(language: Language) {
        syncPreferences.edit().putString(SYNC_LANGUAGE_KEY, language.code).apply()
    }
    
    /**
     * Set dark mode preference
     */
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    /**
     * Get dark mode preference
     */
    suspend fun isDarkMode(): Boolean {
        return isDarkModeFlow.first()
    }
    
    /**
     * Set theme mode preference
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }
    
    /**
     * Get theme mode preference
     */
    suspend fun getThemeMode(): ThemeMode {
        return themeModeFlow.first()
    }
    
    /**
     * Set custom theme preference
     */
    suspend fun setCustomTheme(theme: CustomTheme) {
        dataStore.edit { preferences ->
            preferences[CUSTOM_THEME_KEY] = theme.name
        }
    }
    
    /**
     * Get custom theme preference
     */
    suspend fun getCustomTheme(): CustomTheme {
        return customThemeFlow.first()
    }
    
    /**
     * Set dynamic color preference
     */
    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
    
    /**
     * Get dynamic color preference
     */
    suspend fun isDynamicColor(): Boolean {
        return dynamicColorFlow.first()
    }
    
    /**
     * Set haptic feedback preference
     */
    suspend fun setHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_KEY] = enabled
        }
    }
    
    /**
     * Get haptic feedback preference
     */
    suspend fun isHapticFeedbackEnabled(): Boolean {
        return hapticFeedbackFlow.first()
    }
    
    /**
     * Set a string preference
     */
    suspend fun setString(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }
    
    /**
     * Get a string preference
     */
    suspend fun getString(key: String, defaultValue: String = ""): String {
        val prefKey = stringPreferencesKey(key)
        return dataStore.data.first()[prefKey] ?: defaultValue
    }
    
    /**
     * Set a boolean preference
     */
    suspend fun setBoolean(key: String, value: Boolean) {
        val prefKey = booleanPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[prefKey] = value
        }
    }
    
    /**
     * Get a boolean preference
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val prefKey = booleanPreferencesKey(key)
        return dataStore.data.first()[prefKey] ?: defaultValue
    }
}
