package com.rafgittools.core.privacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.rafgittools.core.security.EncryptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

/**
 * Encrypted storage for privacy settings and audit logs
 * 
 * Uses Android DataStore with encryption for secure storage of privacy-related data.
 */
@Singleton
class EncryptedPrivacyStorage @Inject constructor(
    private val context: Context,
    private val encryptionManager: EncryptionManager
) {
    private val dataStore = context.privacyDataStore
    
    companion object {
        private val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
        private val USAGE_STATS_ENABLED = booleanPreferencesKey("usage_stats_enabled")
        private val PERSONALIZATION_ENABLED = booleanPreferencesKey("personalization_enabled")
        private val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        private val AUTO_DELETE_INACTIVE = booleanPreferencesKey("auto_delete_inactive")
        private val ENCRYPT_LOCAL_DATA = booleanPreferencesKey("encrypt_local_data")
        private val BIOMETRIC_AUTH_ENABLED = booleanPreferencesKey("biometric_auth_enabled")
        private val SCREEN_CAPTURE_BLOCKED = booleanPreferencesKey("screen_capture_blocked")
        private val CLIPBOARD_SECURITY_ENABLED = booleanPreferencesKey("clipboard_security_enabled")
        private val PRIVACY_EVENTS_LOG = stringPreferencesKey("privacy_events_log")
    }
    
    /**
     * Flow of current privacy settings
     */
    val privacySettingsFlow: Flow<PrivacySettings> = dataStore.data
        .map { preferences ->
            PrivacySettings(
                analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: false,
                crashReportingEnabled = preferences[CRASH_REPORTING_ENABLED] ?: false,
                usageStatsEnabled = preferences[USAGE_STATS_ENABLED] ?: false,
                personalizationEnabled = preferences[PERSONALIZATION_ENABLED] ?: false,
                dataRetentionDays = preferences[DATA_RETENTION_DAYS] ?: 90,
                autoDeleteInactiveData = preferences[AUTO_DELETE_INACTIVE] ?: true,
                encryptLocalData = preferences[ENCRYPT_LOCAL_DATA] ?: true,
                biometricAuthEnabled = preferences[BIOMETRIC_AUTH_ENABLED] ?: false,
                screenCaptureBlocked = preferences[SCREEN_CAPTURE_BLOCKED] ?: false,
                clipboardSecurityEnabled = preferences[CLIPBOARD_SECURITY_ENABLED] ?: true
            )
        }
    
    /**
     * Save privacy settings
     */
    suspend fun savePrivacySettings(settings: PrivacySettings) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_ENABLED] = settings.analyticsEnabled
            preferences[CRASH_REPORTING_ENABLED] = settings.crashReportingEnabled
            preferences[USAGE_STATS_ENABLED] = settings.usageStatsEnabled
            preferences[PERSONALIZATION_ENABLED] = settings.personalizationEnabled
            preferences[DATA_RETENTION_DAYS] = settings.dataRetentionDays
            preferences[AUTO_DELETE_INACTIVE] = settings.autoDeleteInactiveData
            preferences[ENCRYPT_LOCAL_DATA] = settings.encryptLocalData
            preferences[BIOMETRIC_AUTH_ENABLED] = settings.biometricAuthEnabled
            preferences[SCREEN_CAPTURE_BLOCKED] = settings.screenCaptureBlocked
            preferences[CLIPBOARD_SECURITY_ENABLED] = settings.clipboardSecurityEnabled
        }
    }
    
    /**
     * Get current privacy settings
     */
    suspend fun getPrivacySettings(): PrivacySettings {
        return privacySettingsFlow.first()
    }
    
    /**
     * Log a privacy event to encrypted audit trail
     * 
     * @param event The privacy event to log
     */
    suspend fun logPrivacyEvent(event: PrivacyEvent) {
        dataStore.edit { preferences ->
            val existingLog = preferences[PRIVACY_EVENTS_LOG] ?: "[]"
            val events = parsePrivacyEvents(existingLog).toMutableList()
            events.add(event)
            
            // Keep only last 1000 events to prevent unlimited growth
            val trimmedEvents = if (events.size > 1000) {
                events.takeLast(1000)
            } else {
                events
            }
            
            // Encrypt the log before storing
            val encryptedLog = encryptionManager.encrypt(serializePrivacyEvents(trimmedEvents))
            preferences[PRIVACY_EVENTS_LOG] = encryptedLog
        }
    }
    
    /**
     * Get privacy event audit trail
     * 
     * @return List of all logged privacy events
     */
    suspend fun getPrivacyEvents(): List<PrivacyEvent> {
        val encryptedLog = dataStore.data.first()[PRIVACY_EVENTS_LOG] ?: return emptyList()
        val decryptedLog = encryptionManager.decrypt(encryptedLog)
        return parsePrivacyEvents(decryptedLog)
    }
    
    /**
     * Clear all privacy data (for account deletion)
     */
    suspend fun clearAllPrivacyData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    // Helper methods for serialization
    
    private fun serializePrivacyEvents(events: List<PrivacyEvent>): String {
        // Simple JSON-like serialization
        val eventStrings = events.map { event ->
            """{"id":"${event.id}","type":"${event.type}","timestamp":"${event.timestamp.time}","details":"${event.details ?: ""}"}"""
        }
        return "[${eventStrings.joinToString(",")}]"
    }
    
    private fun parsePrivacyEvents(json: String): List<PrivacyEvent> {
        if (json == "[]" || json.isBlank()) return emptyList()
        
        // Simple JSON parsing (in production, use a proper JSON library)
        return try {
            json.trim('[', ']')
                .split("},")
                .mapNotNull { eventJson ->
                    parsePrivacyEvent(eventJson.trim('{', '}') + "}")
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parsePrivacyEvent(json: String): PrivacyEvent? {
        return try {
            val parts = json.trim('{', '}').split("\",\"")
            val id = parts[0].substringAfter("\":\"").trim('"')
            val type = PrivacyEventType.valueOf(parts[1].substringAfter("\":\"").trim('"'))
            val timestamp = Date(parts[2].substringAfter("\":\"").trim('"').toLong())
            val details = parts.getOrNull(3)?.substringAfter("\":\"")?.trim('"', '}')
            
            PrivacyEvent(
                id = id,
                type = type,
                timestamp = timestamp,
                details = details,
                ipAddress = null,
                userId = null
            )
        } catch (e: Exception) {
            null
        }
    }
}
