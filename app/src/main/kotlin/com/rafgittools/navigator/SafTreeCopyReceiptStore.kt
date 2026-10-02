package com.rafgittools.navigator

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

data class SafTreeCopyReceiptEvent(
    val schema: String = "rafgittools.saf-tree-copy-event.v1",
    val operationId: String,
    val sequence: Long,
    val eventType: String,
    val sourceRootSha256: String,
    val destinationRootSha256: String,
    val targetRootName: String,
    val relativePath: String = "",
    val sourceRefSha256: String = "TOKEN_VAZIO",
    val destinationRefSha256: String = "TOKEN_VAZIO",
    val bytes: Long? = null,
    val sourceSha256: String = "TOKEN_VAZIO",
    val destinationSha256: String = "TOKEN_VAZIO",
    val resultCode: String = "TOKEN_VAZIO",
    val previousEventSha256: String = "GENESIS",
    val eventSha256: String = "",
    val claimAllowed: Boolean = false
)

data class SafTreeCopyReceiptVerification(
    val verified: Boolean,
    val eventCount: Long,
    val chainHeadSha256: String,
    val errorCode: String = "TOKEN_VAZIO"
)

/**
 * App-private append-only hash chain. Events contain path names and fingerprints,
 * never SAF capability URIs or source content.
 */
class SafTreeCopyReceiptStore(private val receiptFile: File) {
    private val gson = GsonBuilder().disableHtmlEscaping().create()
    private var nextSequence = 0L
    private var previousHash = "GENESIS"

    init {
        if (receiptFile.exists()) {
            val verification = verifyFile(receiptFile)
            if (!verification.verified) throw IOException(verification.errorCode)
            nextSequence = verification.eventCount
            previousHash = verification.chainHeadSha256
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun append(
        operationId: String,
        eventType: String,
        sourceRootSha256: String,
        destinationRootSha256: String,
        targetRootName: String,
        relativePath: String = "",
        sourceRefSha256: String = "TOKEN_VAZIO",
        destinationRefSha256: String = "TOKEN_VAZIO",
        bytes: Long? = null,
        sourceSha256: String = "TOKEN_VAZIO",
        destinationSha256: String = "TOKEN_VAZIO",
        resultCode: String = "TOKEN_VAZIO"
    ): SafTreeCopyReceiptEvent {
        require(eventType.isNotBlank()) { "COPY_RECEIPT_EVENT_TYPE_REQUIRED" }
        require(operationId.isNotBlank()) { "COPY_RECEIPT_OPERATION_ID_REQUIRED" }
        require(!relativePath.contains('\n') && !relativePath.contains('\r')) {
            "COPY_RECEIPT_PATH_HAS_LINE_BREAK"
        }
        if (bytes != null) require(bytes >= 0L) { "COPY_RECEIPT_NEGATIVE_BYTES" }

        val unhashed = SafTreeCopyReceiptEvent(
            operationId = operationId,
            sequence = nextSequence,
            eventType = eventType,
            sourceRootSha256 = sourceRootSha256,
            destinationRootSha256 = destinationRootSha256,
            targetRootName = targetRootName,
            relativePath = relativePath,
            sourceRefSha256 = sourceRefSha256,
            destinationRefSha256 = destinationRefSha256,
            bytes = bytes,
            sourceSha256 = sourceSha256,
            destinationSha256 = destinationSha256,
            resultCode = resultCode,
            previousEventSha256 = previousHash
        )
        val hash = sha256(gson.toJson(unhashed).toByteArray(Charsets.UTF_8))
        val event = unhashed.copy(eventSha256 = hash)

        val parent = receiptFile.parentFile ?: throw IOException("COPY_RECEIPT_PARENT_MISSING")
        if (!parent.exists() && !parent.mkdirs()) throw IOException("COPY_RECEIPT_DIR_CREATE_FAILED")
        FileOutputStream(receiptFile, true).use { output ->
            output.write((gson.toJson(event) + "\n").toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
        nextSequence += 1
        previousHash = hash
        return event
    }

    fun verify(): SafTreeCopyReceiptVerification = verifyFile(receiptFile)

    companion object {
        private val gson = GsonBuilder().disableHtmlEscaping().create()

        fun verifyFile(file: File): SafTreeCopyReceiptVerification {
            if (!file.isFile) {
                return SafTreeCopyReceiptVerification(false, 0L, "GENESIS", "COPY_RECEIPT_MISSING")
            }
            var expectedSequence = 0L
            var previous = "GENESIS"
            return try {
                file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.filter { it.isNotBlank() }.forEach { line ->
                        val event = gson.fromJson(line, SafTreeCopyReceiptEvent::class.java)
                            ?: throw IOException("COPY_RECEIPT_EVENT_INVALID")
                        if (event.schema != "rafgittools.saf-tree-copy-event.v1") {
                            throw IOException("COPY_RECEIPT_SCHEMA_INVALID")
                        }
                        if (event.sequence != expectedSequence) {
                            throw IOException("COPY_RECEIPT_SEQUENCE_INVALID")
                        }
                        if (event.previousEventSha256 != previous) {
                            throw IOException("COPY_RECEIPT_PREVIOUS_HASH_INVALID")
                        }
                        val expectedHash = sha256(
                            gson.toJson(event.copy(eventSha256 = "")).toByteArray(Charsets.UTF_8)
                        )
                        if (event.eventSha256 != expectedHash) {
                            throw IOException("COPY_RECEIPT_EVENT_HASH_INVALID")
                        }
                        previous = event.eventSha256
                        expectedSequence += 1
                    }
                }
                SafTreeCopyReceiptVerification(true, expectedSequence, previous)
            } catch (failure: Exception) {
                SafTreeCopyReceiptVerification(
                    false,
                    expectedSequence,
                    previous,
                    failure.message ?: "COPY_RECEIPT_VERIFY_FAILED"
                )
            }
        }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it) }
    }
}
