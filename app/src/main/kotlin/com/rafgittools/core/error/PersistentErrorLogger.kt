package com.rafgittools.core.error

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

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

    /**
     * Log error to persistent storage
     */
    override fun log(error: ErrorDetails) {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    val existingLog = preferences[ERROR_LOG_KEY] ?: "[]"
                    val errors = ErrorDetailsCodec.deserialize(existingLog).toMutableList()
                    errors.add(error)

                    // Keep only last MAX_ERRORS
                    val trimmedErrors = if (errors.size > MAX_ERRORS) {
                        errors.takeLast(MAX_ERRORS)
                    } else {
                        errors
                    }

                    preferences[ERROR_LOG_KEY] = ErrorDetailsCodec.serialize(trimmedErrors)
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
                    ErrorDetailsCodec.deserialize(log).takeLast(limit)
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

internal object ErrorDetailsCodec {
    private val gson = Gson()
    private val listType = object : TypeToken<List<StoredErrorDetails>>() {}.type

    fun serialize(errors: List<ErrorDetails>): String {
        val storedErrors = errors.map { it.toStoredError() }
        return gson.toJson(storedErrors, listType)
    }

    fun deserialize(json: String): List<ErrorDetails> {
        if (json == "[]" || json.isBlank()) return emptyList()

        val storedErrors = runCatching {
            gson.fromJson<List<StoredErrorDetails>>(json, listType)
        }.getOrNull() ?: return emptyList()

        return storedErrors.mapNotNull { it.toDomainErrorOrNull() }
    }
}

internal data class StoredErrorDetails(
    val type: String,
    val message: String,
    val context: String,
    val timestamp: Long,
    val stackTrace: String? = null
)

private fun ErrorDetails.toStoredError(): StoredErrorDetails {
    return StoredErrorDetails(
        type = type.name,
        message = message,
        context = context,
        timestamp = timestamp,
        stackTrace = stackTrace
    )
}

private fun StoredErrorDetails.toDomainErrorOrNull(): ErrorDetails? {
    val parsedType = runCatching { ErrorType.valueOf(type) }.getOrNull() ?: return null

    return ErrorDetails(
        type = parsedType,
        message = message,
        context = context,
        timestamp = timestamp,
        stackTrace = stackTrace
    )
}
