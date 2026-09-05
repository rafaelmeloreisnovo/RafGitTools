package com.rafgittools.data.github

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local append-only custody log for repository governance observations and mutations.
 *
 * V2 records form a SHA-256 hash chain. The format is CIS-style audit evidence, not a
 * claim of CIS certification. Legacy V1 lines remain readable and are never rewritten.
 * A receipt proves that an observation/attempt was recorded; provider acceptance and
 * authoritative re-probe remain separate evidence.
 */
@Singleton
class RepositoryGovernanceReceiptStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val receiptFile = File(context.filesDir, "repository-governance-receipts.jsonl")
    private val lock = Any()
    private val gson = Gson()

    data class ReceiptEnvelope(
        val schema: String = SCHEMA,
        val sequence: Long,
        val receipt_id: String,
        val timestamp_epoch_ms: Long,
        val repository: String,
        val operation: String,
        val outcome: String,
        val before_snapshot: String? = null,
        val after_snapshot: String? = null,
        val details: String,
        val gaps: List<String> = emptyList(),
        val previous_hash: String,
        val record_hash: String
    )

    data class GovernanceReceiptChainStatus(
        val valid: Boolean,
        val chainedRecords: Long,
        val legacyRecords: Long,
        val headHash: String?,
        val error: String? = null
    )

    fun append(
        repository: String,
        operation: String,
        outcome: String,
        details: String
    ): String = appendDetailed(
        repository = repository,
        operation = operation,
        outcome = outcome,
        details = details
    )

    fun appendDetailed(
        repository: String,
        operation: String,
        outcome: String,
        details: String,
        beforeSnapshot: String? = null,
        afterSnapshot: String? = null,
        gaps: List<String> = emptyList()
    ): String = synchronized(lock) {
        receiptFile.parentFile?.mkdirs()
        val tail = tailEnvelope()
        val sequence = (tail?.sequence ?: 0L) + 1L
        val previousHash = tail?.record_hash ?: GENESIS_HASH
        val now = System.currentTimeMillis()
        val receiptId = "RG2-$now-$sequence-${repository.hashCode().toUInt().toString(16)}"
        val normalizedGaps = gaps.map { it.take(MAX_FIELD_CHARS) }
        val normalizedDetails = details.take(MAX_DETAILS_CHARS)
        val normalizedBefore = beforeSnapshot?.take(MAX_SNAPSHOT_CHARS)
        val normalizedAfter = afterSnapshot?.take(MAX_SNAPSHOT_CHARS)
        val payload = canonicalPayload(
            sequence = sequence,
            receiptId = receiptId,
            timestamp = now,
            repository = repository,
            operation = operation,
            outcome = outcome,
            beforeSnapshot = normalizedBefore,
            afterSnapshot = normalizedAfter,
            details = normalizedDetails,
            gaps = normalizedGaps,
            previousHash = previousHash
        )
        val recordHash = sha256(payload)
        val envelope = ReceiptEnvelope(
            sequence = sequence,
            receipt_id = receiptId,
            timestamp_epoch_ms = now,
            repository = repository,
            operation = operation,
            outcome = outcome,
            before_snapshot = normalizedBefore,
            after_snapshot = normalizedAfter,
            details = normalizedDetails,
            gaps = normalizedGaps,
            previous_hash = previousHash,
            record_hash = recordHash
        )
        receiptFile.appendText(gson.toJson(envelope) + "\n", Charsets.UTF_8)
        receiptId
    }

    fun verifyChain(): GovernanceReceiptChainStatus = synchronized(lock) {
        if (!receiptFile.isFile) {
            return@synchronized GovernanceReceiptChainStatus(
                valid = true,
                chainedRecords = 0,
                legacyRecords = 0,
                headHash = null
            )
        }

        var expectedPrevious = GENESIS_HASH
        var chained = 0L
        var legacy = 0L
        var head: String? = null
        var lineNumber = 0L

        receiptFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                lineNumber += 1
                if (line.isBlank()) continue
                val envelope = parseV2Envelope(line)
                if (envelope == null) {
                    if (chained > 0) {
                        return@synchronized GovernanceReceiptChainStatus(
                            valid = false,
                            chainedRecords = chained,
                            legacyRecords = legacy,
                            headHash = head,
                            error = "non-v2 record after chained log at line $lineNumber"
                        )
                    }
                    legacy += 1
                    continue
                }

                if (envelope.previous_hash != expectedPrevious) {
                    return@synchronized GovernanceReceiptChainStatus(
                        valid = false,
                        chainedRecords = chained,
                        legacyRecords = legacy,
                        headHash = head,
                        error = "previous_hash mismatch at line $lineNumber"
                    )
                }
                val expectedHash = sha256(
                    canonicalPayload(
                        sequence = envelope.sequence,
                        receiptId = envelope.receipt_id,
                        timestamp = envelope.timestamp_epoch_ms,
                        repository = envelope.repository,
                        operation = envelope.operation,
                        outcome = envelope.outcome,
                        beforeSnapshot = envelope.before_snapshot,
                        afterSnapshot = envelope.after_snapshot,
                        details = envelope.details,
                        gaps = envelope.gaps,
                        previousHash = envelope.previous_hash
                    )
                )
                if (expectedHash != envelope.record_hash) {
                    return@synchronized GovernanceReceiptChainStatus(
                        valid = false,
                        chainedRecords = chained,
                        legacyRecords = legacy,
                        headHash = head,
                        error = "record_hash mismatch at line $lineNumber"
                    )
                }
                expectedPrevious = envelope.record_hash
                head = envelope.record_hash
                chained += 1
            }
        }
        GovernanceReceiptChainStatus(
            valid = true,
            chainedRecords = chained,
            legacyRecords = legacy,
            headHash = head
        )
    }

    fun path(): String = receiptFile.absolutePath

    private fun tailEnvelope(): ReceiptEnvelope? {
        if (!receiptFile.isFile) return null
        var tail: ReceiptEnvelope? = null
        receiptFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.filter { it.isNotBlank() }.forEach { line ->
                parseV2Envelope(line)?.let { tail = it }
            }
        }
        return tail
    }

    private fun parseV2Envelope(line: String): ReceiptEnvelope? = runCatching {
        val root = JsonParser.parseString(line)
        if (!root.isJsonObject) return@runCatching null
        val obj = root.asJsonObject
        if (obj.get("schema")?.takeIf { it.isJsonPrimitive }?.asString != SCHEMA) {
            return@runCatching null
        }
        REQUIRED_V2_FIELDS.forEach { field ->
            if (!obj.has(field) || obj.get(field).isJsonNull) return@runCatching null
        }
        gson.fromJson(obj, ReceiptEnvelope::class.java)
    }.getOrNull()

    private fun canonicalPayload(
        sequence: Long,
        receiptId: String,
        timestamp: Long,
        repository: String,
        operation: String,
        outcome: String,
        beforeSnapshot: String?,
        afterSnapshot: String?,
        details: String,
        gaps: List<String>,
        previousHash: String
    ): String = buildString {
        append(SCHEMA).append('\n')
        append(sequence).append('\n')
        append(receiptId).append('\n')
        append(timestamp).append('\n')
        append(repository).append('\n')
        append(operation).append('\n')
        append(outcome).append('\n')
        append(beforeSnapshot.orEmpty()).append('\n')
        append(afterSnapshot.orEmpty()).append('\n')
        append(details).append('\n')
        gaps.forEach { append(it).append('\u001f') }
        append('\n').append(previousHash)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    companion object {
        private const val SCHEMA = "rafgittools.repository-governance-audit.v2"
        private const val GENESIS_HASH = "GENESIS"
        private const val MAX_DETAILS_CHARS = 8192
        private const val MAX_SNAPSHOT_CHARS = 32768
        private const val MAX_FIELD_CHARS = 1024
        private val REQUIRED_V2_FIELDS = setOf(
            "sequence",
            "receipt_id",
            "timestamp_epoch_ms",
            "repository",
            "operation",
            "outcome",
            "details",
            "previous_hash",
            "record_hash"
        )
    }
}
