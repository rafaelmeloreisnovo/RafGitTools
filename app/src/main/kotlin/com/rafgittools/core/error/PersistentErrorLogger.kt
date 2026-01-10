package com.rafgittools.core.error

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.errorDataStore: DataStore<Preferences> by preferencesDataStore(name = "error_logs")

/**
 * Persistent Error Logger
 * 
 * Stores error logs for analysis and prevention.
 * Implements secure error storage with automatic cleanup.
 */
@Singleton
class PersistentErrorLogger @Inject constructor(
    private val context: Context
) : ErrorLogger {
    
    private val dataStore = context.errorDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private val ERROR_LOG_KEY = stringPreferencesKey("error_log")
        private const val MAX_ERRORS = 1000
    }
    
    /**
     * Log error to persistent storage
     */
    override fun log(error: ErrorDetails) {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    val existingLog = preferences[ERROR_LOG_KEY] ?: "[]"
                    val errors = parseErrors(existingLog).toMutableList()
                    errors.add(error)
                    
                    // Keep only last MAX_ERRORS
                    val trimmedErrors = if (errors.size > MAX_ERRORS) {
                        errors.takeLast(MAX_ERRORS)
                    } else {
                        errors
                    }
                    
                    preferences[ERROR_LOG_KEY] = serializeErrors(trimmedErrors)
                }
            } catch (e: Exception) {
                // Fail silently - don't crash on error logging
                android.util.Log.e("ErrorLogger", "Failed to log error", e)
            }
        }
    }
    
    /**
     * Get recent errors
     */
    override fun getErrors(limit: Int): List<ErrorDetails> {
        return try {
            kotlinx.coroutines.runBlocking {
                dataStore.data.first()[ERROR_LOG_KEY]?.let { log ->
                    parseErrors(log).takeLast(limit)
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear all error logs
     */
    override fun clearErrors() {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences.remove(ERROR_LOG_KEY)
                }
            } catch (e: Exception) {
                android.util.Log.e("ErrorLogger", "Failed to clear errors", e)
            }
        }
    }
    
    private fun serializeErrors(errors: List<ErrorDetails>): String {
        val errorStrings = errors.map { error ->
            """{"type":"${error.type}","message":"${escapeJson(error.message)}","context":"${escapeJson(error.context)}","timestamp":${error.timestamp}}"""
        }
        return "[${errorStrings.joinToString(",")}]"
    }
    
    private fun parseErrors(json: String): List<ErrorDetails> {
        if (json == "[]" || json.isBlank()) return emptyList()
        
        return try {
            json.trim('[', ']')
                .split("},")
                .mapNotNull { errorJson ->
                    parseError(errorJson.trim('{', '}') + "}")
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun parseError(json: String): ErrorDetails? {
        return try {
            val parts = json.trim('{', '}').split("\",\"")
            val type = ErrorType.valueOf(parts[0].substringAfter("\":\"").trim('"'))
            val message = parts[1].substringAfter("\":\"").trim('"')
            val context = parts[2].substringAfter("\":\"").trim('"')
            val timestamp = parts[3].substringAfter("\":").trim('}').toLong()
            
            ErrorDetails(
                type = type,
                message = unescapeJson(message),
                context = unescapeJson(context),
                timestamp = timestamp
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun escapeJson(str: String): String {
        return str.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    private fun unescapeJson(str: String): String {
        return str.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}
