package com.rafgittools.data.github

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local append-only custody log for repository governance mutations.
 *
 * Receipts are evidence of an attempted provider operation, not proof that a requested
 * setting became effective. The provider result is recorded separately in [outcome].
 */
@Singleton
class RepositoryGovernanceReceiptStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val receiptFile = File(context.filesDir, "repository-governance-receipts.jsonl")
    private val lock = Any()

    fun append(
        repository: String,
        operation: String,
        outcome: String,
        details: String
    ): String {
        val now = System.currentTimeMillis()
        val receiptId = "RG-$now-${repository.hashCode().toUInt().toString(16)}"
        val line = buildString {
            append('{')
            append("\"receipt_id\":\"").append(json(receiptId)).append("\",")
            append("\"timestamp_epoch_ms\":").append(now).append(',')
            append("\"repository\":\"").append(json(repository)).append("\",")
            append("\"operation\":\"").append(json(operation)).append("\",")
            append("\"outcome\":\"").append(json(outcome)).append("\",")
            append("\"details\":\"").append(json(details)).append("\"")
            append('}')
            append('\n')
        }
        synchronized(lock) {
            receiptFile.parentFile?.mkdirs()
            receiptFile.appendText(line, Charsets.UTF_8)
        }
        return receiptId
    }

    fun path(): String = receiptFile.absolutePath

    private fun json(value: String): String = buildString(value.length + 16) {
        value.forEach { c ->
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }
}
