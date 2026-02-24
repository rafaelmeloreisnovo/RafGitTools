package com.rafgittools.core.logging

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private val Context.diffAuditDataStore: DataStore<Preferences> by preferencesDataStore(name = "diff_audit_logs")

@Singleton
class DiffAuditLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.diffAuditDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val DIFF_LOG_KEY = stringPreferencesKey("diff_log")
        private const val MAX_LOGS = 1000
    }

    private val gson = Gson()

    fun logDiff(entry: DiffAuditEntry) {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    val existingLog = preferences[DIFF_LOG_KEY] ?: "[]"
                    val entries = DiffAuditLogCodec.deserialize(existingLog, gson).toMutableList()
                    entries.add(entry)
                    val trimmed = if (entries.size > MAX_LOGS) {
                        entries.takeLast(MAX_LOGS)
                    } else {
                        entries
                    }
                    preferences[DIFF_LOG_KEY] = DiffAuditLogCodec.serialize(trimmed, gson)
                }
            } catch (_: Exception) {
                // Fail silently to avoid interrupting git operations.
            }
        }
    }

    suspend fun getEntries(limit: Int = 100): List<DiffAuditEntry> {
        return try {
            withContext(Dispatchers.IO) {
                dataStore.data.first()[DIFF_LOG_KEY]?.let { log ->
                    DiffAuditLogCodec.deserialize(log, gson).takeLast(limit)
                } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Legacy synchronous adapter.
     *
     * Contract: this method must never be called from the main thread.
     */
    fun getEntriesBlocking(limit: Int = 100): List<DiffAuditEntry> {
        check(android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            "getEntriesBlocking must not be called from the main thread"
        }

        return runBlocking(Dispatchers.IO) {
            getEntries(limit)
        }
    }

}

internal object DiffAuditLogCodec {
    fun serialize(entries: List<DiffAuditEntry>, gson: Gson): String {
        val dto = DiffAuditLogDto(entries = entries.map { DiffAuditEntryDto.fromDomain(it) })
        return gson.toJson(dto)
    }

    fun deserialize(json: String, gson: Gson): List<DiffAuditEntry> {
        if (json.isBlank() || json == "[]") return emptyList()

        return try {
            gson.fromJson(json, DiffAuditLogDto::class.java)
                ?.entries
                .orEmpty()
                .map { it.toDomain() }
        } catch (_: JsonSyntaxException) {
            deserializeLegacyArray(json, gson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun deserializeLegacyArray(json: String, gson: Gson): List<DiffAuditEntry> {
        return try {
            val legacyList = gson.fromJson(json, Array<DiffAuditEntryDto>::class.java)
            legacyList?.map { it.toDomain() }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }
}

internal data class DiffAuditLogDto(
    val entries: List<DiffAuditEntryDto>
)

internal data class DiffAuditEntryDto(
    val oldPath: String?,
    val newPath: String?,
    val changeType: String,
    val timestamp: Long,
    val diffSizeBytes: Long,
    val fileSizeBytes: Long,
    val md5: String
) {
    fun toDomain(): DiffAuditEntry {
        return DiffAuditEntry(
            oldPath = oldPath,
            newPath = newPath,
            changeType = changeType,
            timestamp = timestamp,
            diffSizeBytes = diffSizeBytes,
            fileSizeBytes = fileSizeBytes,
            md5 = md5
        )
    }

    companion object {
        fun fromDomain(entry: DiffAuditEntry): DiffAuditEntryDto {
            return DiffAuditEntryDto(
                oldPath = entry.oldPath,
                newPath = entry.newPath,
                changeType = entry.changeType,
                timestamp = entry.timestamp,
                diffSizeBytes = entry.diffSizeBytes,
                fileSizeBytes = entry.fileSizeBytes,
                md5 = entry.md5
            )
        }
    }
}

data class DiffAuditEntry(
    val oldPath: String?,
    val newPath: String?,
    val changeType: String,
    val timestamp: Long,
    val diffSizeBytes: Long,
    val fileSizeBytes: Long,
    val md5: String
)

fun md5Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("MD5").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
