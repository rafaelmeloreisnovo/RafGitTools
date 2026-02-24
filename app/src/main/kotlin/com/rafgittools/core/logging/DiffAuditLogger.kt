package com.rafgittools.core.logging

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun logDiff(entry: DiffAuditEntry) {
        scope.launch {
            try {
                dataStore.edit { preferences ->
                    val existingLog = preferences[DIFF_LOG_KEY] ?: "[]"
                    val entries = parseEntries(existingLog).toMutableList()
                    entries.add(entry)
                    val trimmed = if (entries.size > MAX_LOGS) {
                        entries.takeLast(MAX_LOGS)
                    } else {
                        entries
                    }
                    preferences[DIFF_LOG_KEY] = serializeEntries(trimmed)
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
                    parseEntries(log).takeLast(limit)
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

    private fun serializeEntries(entries: List<DiffAuditEntry>): String {
        val items = entries.map { entry ->
            """{"oldPath":"${escapeJson(entry.oldPath.orEmpty())}","newPath":"${escapeJson(entry.newPath.orEmpty())}","changeType":"${entry.changeType}","timestamp":${entry.timestamp},"diffSizeBytes":${entry.diffSizeBytes},"fileSizeBytes":${entry.fileSizeBytes},"md5":"${escapeJson(entry.md5)}"}"""
        }
        return "[${items.joinToString(",")}]"
    }

    private fun parseEntries(json: String): List<DiffAuditEntry> {
        if (json == "[]" || json.isBlank()) return emptyList()
        return try {
            json.trim('[', ']')
                .split("},")
                .mapNotNull { item ->
                    parseEntry(item.trim('{', '}') + "}")
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseEntry(json: String): DiffAuditEntry? {
        return try {
            val parts = json.trim('{', '}').split("\",\"")
            val oldPath = parts[0].substringAfter("\":\"").trim('"')
            val newPath = parts[1].substringAfter("\":\"").trim('"')
            val changeType = parts[2].substringAfter("\":\"").trim('"')
            val timestamp = parts[3].substringAfter("\":").trim('"').toLong()
            val diffSizeBytes = parts[4].substringAfter("\":").trim('"').toLong()
            val fileSizeBytes = parts[5].substringAfter("\":").trim('"').toLong()
            val md5 = parts[6].substringAfter("\":\"").trim('"', '}')

            DiffAuditEntry(
                oldPath = unescapeJson(oldPath),
                newPath = unescapeJson(newPath),
                changeType = changeType,
                timestamp = timestamp,
                diffSizeBytes = diffSizeBytes,
                fileSizeBytes = fileSizeBytes,
                md5 = unescapeJson(md5)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(value: String): String {
        return value.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
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
