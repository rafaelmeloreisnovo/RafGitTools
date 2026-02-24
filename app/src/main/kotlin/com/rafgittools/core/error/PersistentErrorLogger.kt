package com.rafgittools.core.error

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
    @ApplicationContext private val context: Context
) : ErrorLogger {
    
    private val dataStore = context.errorDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private val ERROR_LOG_KEY = stringPreferencesKey("error_log")
        private const val MAX_ERRORS = 1000
    }

    private val gson = Gson()
    
    /**
     * Log error to persistent storage
     */
    override fun log(error: ErrorDetails) {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    val existingLog = preferences[ERROR_LOG_KEY] ?: "[]"
                    val errors = PersistentErrorLogCodec.deserialize(existingLog, gson).toMutableList()
                    errors.add(error)
                    
                    // Keep only last MAX_ERRORS
                    val trimmedErrors = if (errors.size > MAX_ERRORS) {
                        errors.takeLast(MAX_ERRORS)
                    } else {
                        errors
                    }
                    
                    preferences[ERROR_LOG_KEY] = PersistentErrorLogCodec.serialize(trimmedErrors, gson)
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
    override suspend fun getErrors(limit: Int): List<ErrorDetails> {
        return try {
            withContext(Dispatchers.IO) {
                dataStore.data.first()[ERROR_LOG_KEY]?.let { log ->
                    PersistentErrorLogCodec.deserialize(log, gson).takeLast(limit)
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Legacy synchronous adapter.
     *
     * Contract: this method must never be called from the main thread.
     */
    fun getErrorsBlocking(limit: Int): List<ErrorDetails> {
        check(android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            "getErrorsBlocking must not be called from the main thread"
        }

        return runBlocking(Dispatchers.IO) {
            getErrors(limit)
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
    
}

internal object PersistentErrorLogCodec {
    fun serialize(errors: List<ErrorDetails>, gson: Gson): String {
        val dto = PersistentErrorLogDto(entries = errors.map { ErrorDetailsDto.fromDomain(it) })
        return gson.toJson(dto)
    }

    fun deserialize(json: String, gson: Gson): List<ErrorDetails> {
        if (json.isBlank() || json == "[]") return emptyList()

        return try {
            gson.fromJson(json, PersistentErrorLogDto::class.java)
                ?.entries
                .orEmpty()
                .mapNotNull { it.toDomainOrNull() }
        } catch (_: JsonSyntaxException) {
            deserializeLegacyArray(json, gson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun deserializeLegacyArray(json: String, gson: Gson): List<ErrorDetails> {
        return try {
            val legacy = gson.fromJson(json, Array<ErrorDetailsDto>::class.java)
            legacy?.mapNotNull { it.toDomainOrNull() }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

internal data class PersistentErrorLogDto(
    val entries: List<ErrorDetailsDto>
)

internal data class ErrorDetailsDto(
    val type: String,
    val message: String,
    val context: String,
    val timestamp: Long
) {
    fun toDomainOrNull(): ErrorDetails? {
        return try {
            ErrorDetails(
                type = ErrorType.valueOf(type),
                message = message,
                context = context,
                timestamp = timestamp
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        fun fromDomain(error: ErrorDetails): ErrorDetailsDto {
            return ErrorDetailsDto(
                type = error.type.name,
                message = error.message,
                context = error.context,
                timestamp = error.timestamp
            )
        }
    }
}
