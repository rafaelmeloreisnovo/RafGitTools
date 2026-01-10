package com.rafgittools.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rafgittools.core.localization.Language
import com.rafgittools.data.cache.AsyncCacheManager
import com.rafgittools.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for settings screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val cacheManager: AsyncCacheManager
) : ViewModel() {
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()
    
    private val _gitConfig = MutableStateFlow(GitConfig())
    val gitConfig: StateFlow<GitConfig> = _gitConfig.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load dark mode preference
            preferencesRepository.isDarkModeFlow.collect { isDark ->
                _isDarkMode.value = isDark
            }
        }
        
        viewModelScope.launch {
            // Load language preference
            preferencesRepository.languageFlow.collect { language ->
                _currentLanguage.value = language
            }
        }
        
        viewModelScope.launch {
            // Load git config
            val userName = preferencesRepository.getString("git_user_name", "")
            val userEmail = preferencesRepository.getString("git_user_email", "")
            val signCommits = preferencesRepository.getBoolean("git_sign_commits", false)
            _gitConfig.value = GitConfig(userName, userEmail, signCommits)
        }
    }
    
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
            _isDarkMode.value = enabled
        }
    }
    
    fun setLanguage(language: Language) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
            _currentLanguage.value = language
        }
    }
    
    fun setGitConfig(userName: String, userEmail: String) {
        viewModelScope.launch {
            preferencesRepository.setString("git_user_name", userName)
            preferencesRepository.setString("git_user_email", userEmail)
            _gitConfig.value = _gitConfig.value.copy(userName = userName, userEmail = userEmail)
        }
    }
    
    fun setSignCommits(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBoolean("git_sign_commits", enabled)
            _gitConfig.value = _gitConfig.value.copy(signCommits = enabled)
        }
    }
    
    fun clearCache() {
        viewModelScope.launch {
            cacheManager.clearAllCache()
        }
    }
    
    fun openPrivacyPolicy() {
        // This would typically open a URL in the browser
        // The URL opening would be handled by the Activity
    }
    
    fun openLicenses() {
        // This would open the licenses screen or URL
    }
}

/**
 * Git configuration data
 */
data class GitConfig(
    val userName: String = "",
    val userEmail: String = "",
    val signCommits: Boolean = false
)
