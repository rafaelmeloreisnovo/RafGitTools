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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── FIX L3 ──────────────────────────────────────────────────────────────────
// Antes: dois viewModelScope.launch { flow.collect{} }
//   → cada .collect{} bloqueia a coroutine para sempre (flows infinitos)
//   → a 2ª coroutine nunca termina a coleta do 1º flow (cada launch fica stuck)
// Agora: flow.onEach { }.launchIn(viewModelScope)
//   → ambos os flows correm em paralelo, sem bloquear, cancelados com o VM
// ─────────────────────────────────────────────────────────────────────────────

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
        observePreferences()
        loadGitConfig()
    }

    private fun observePreferences() {
        // FIX L3: onEach+launchIn → não bloqueia, cancela automaticamente com o VM
        preferencesRepository.isDarkModeFlow
            .onEach { _isDarkMode.value = it }
            .launchIn(viewModelScope)

        preferencesRepository.languageFlow
            .onEach { _currentLanguage.value = it }
            .launchIn(viewModelScope)
    }

    private fun loadGitConfig() {
        viewModelScope.launch {
            val userName    = preferencesRepository.getString("git_user_name", "")
            val userEmail   = preferencesRepository.getString("git_user_email", "")
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
        viewModelScope.launch { cacheManager.clearAllCache() }
    }

    fun openPrivacyPolicy() { /* handled by Activity/Navigation callback */ }
    fun openLicenses()      { /* handled by Activity/Navigation callback */ }
}

data class GitConfig(
    val userName: String = "",
    val userEmail: String = "",
    val signCommits: Boolean = false
)
