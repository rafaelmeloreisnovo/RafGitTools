package com.rafgittools.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
        private val THEME_KEY = stringPreferencesKey("theme")
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
}
