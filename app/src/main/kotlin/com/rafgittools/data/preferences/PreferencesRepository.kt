package com.rafgittools.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rafgittools.core.localization.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
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
     * Set the preferred language
     */
    suspend fun setLanguage(language: Language) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }
    
    /**
     * Get the currently selected language
     */
    suspend fun getLanguage(): Language {
        return languageFlow.first()
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
