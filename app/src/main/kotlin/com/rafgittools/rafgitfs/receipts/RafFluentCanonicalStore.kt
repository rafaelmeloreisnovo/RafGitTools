package com.rafgittools.rafgitfs.receipts

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Append-first custody for canonical RAFAELIA Fluent Forward Message-mode bytes.
 *
 * This store deliberately has no Room/SQLite dependency. The payload bytes are
 * preserved exactly as produced by the compiler/runtime authority. A four-byte
 * big-endian length prefix is local framing only and is not part of the event.
 *
 * Canonical payload invariant: first byte == 0x93 (MessagePack fixarray(3)),
 * corresponding to Forward Message mode: [tag, time, record].
 */
class RafFluentCanonicalStore(
    private val file: File,
    private val maxEventBytes: Int = DEFAULT_MAX_EVENT_BYTES
) {
    data class RecordRef(val offset: Long, val payloadBytes: Int)

    class CorruptStoreException(message: String) : IllegalStateException(message)

    init {
        require(maxEventBytes in 1..MAX_ALLOWED_EVENT_BYTES) {
            "maxEventBytes must be in 1..$MAX_ALLOWED_EVENT_BYTES"
        }
    }

    @Synchronized
    fun append(canonicalEvent: ByteArray): RecordRef {
        validateEvent(canonicalEvent)
        file.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "cannot create receipt directory: $parent" }
        }

        // Never append behind an incomplete/corrupt tail. A damaged canonical
        // stream must be surfaced and repaired explicitly rather than hidden by
        // later valid records.
        val offset = validateExistingStream()

        FileOutputStream(file, true).use { out ->
            writeU32Be(out, canonicalEvent.size)
            out.write(canonicalEvent)
            out.flush()
            out.fd.sync()
        }
        return RecordRef(offset = offset, payloadBytes = canonicalEvent.size)
    }

    @Synchronized
    fun replay(consumer: (ByteArray) -> Unit): Int {
        if (!file.exists()) return 0
        var count = 0
        BufferedInputStream(FileInputStream(file)).use { input ->
            var offset = 0L
            while (true) {
                val length = readLengthOrEof(input, offset) ?: break
                if (length <= 0 || length > maxEventBytes) {
                    throw CorruptStoreException("invalid event length=$length at offset=$offset")
                }
                val payload = ByteArray(length)
                readFully(input, payload, offset + FRAME_BYTES)
                validateEvent(payload)
                consumer(payload)
                count++
                offset += FRAME_BYTES + length.toLong()
            }
        }
        return count
    }

    @Synchronized
    fun validateExistingStream(): Long {
        if (!file.exists()) return 0L
        var offset = 0L
        BufferedInputStream(FileInputStream(file)).use { input ->
            val scratch = ByteArray(SKIP_BUFFER_BYTES)
            while (true) {
                val length = readLengthOrEof(input, offset) ?: return offset
                if (length <= 0 || length > maxEventBytes) {
                    throw CorruptStoreException("invalid event length=$length at offset=$offset")
                }
                var remaining = length
                var first = true
                while (remaining > 0) {
                    val take = minOf(remaining, scratch.size)
                    val n = input.read(scratch, 0, take)
                    if (n < 0) {
                        throw CorruptStoreException("truncated event at offset=$offset expected=$length")
                    }
                    if (first) {
                        if (n == 0) continue
                        if ((scratch[0].toInt() and 0xff) != FORWARD_MESSAGE_ARRAY3) {
                            throw CorruptStoreException("non-Forward Message payload at offset=$offset")
                        }
                        first = false
                    }
                    remaining -= n
                }
                offset += FRAME_BYTES + length.toLong()
            }
        }
    }

    private fun validateEvent(event: ByteArray) {
        require(event.isNotEmpty()) { "canonical event must not be empty" }
        require(event.size <= maxEventBytes) { "canonical event exceeds maxEventBytes=$maxEventBytes" }
        require((event[0].toInt() and 0xff) == FORWARD_MESSAGE_ARRAY3) {
            "expected Fluent Forward Message mode fixarray(3) prefix 0x93"
        }
    }

    private fun readLengthOrEof(input: BufferedInputStream, offset: Long): Int? {
        val b0 = input.read()
        if (b0 < 0) return null
        val b1 = input.read()
        val b2 = input.read()
        val b3 = input.read()
        if (b1 < 0 || b2 < 0 || b3 < 0) {
            throw CorruptStoreException("truncated length prefix at offset=$offset")
        }
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun readFully(input: BufferedInputStream, target: ByteArray, payloadOffset: Long) {
        var pos = 0
        while (pos < target.size) {
            val n = input.read(target, pos, target.size - pos)
            if (n < 0) throw EOFException("truncated canonical event at offset=$payloadOffset")
            pos += n
        }
    }

    private fun writeU32Be(out: FileOutputStream, value: Int) {
        out.write((value ushr 24) and 0xff)
        out.write((value ushr 16) and 0xff)
        out.write((value ushr 8) and 0xff)
        out.write(value and 0xff)
    }

    companion object {
        const val DEFAULT_MAX_EVENT_BYTES: Int = 1 shl 20
        const val MAX_ALLOWED_EVENT_BYTES: Int = 16 shl 20
        private const val FRAME_BYTES: Long = 4L
        private const val FORWARD_MESSAGE_ARRAY3: Int = 0x93
        private const val SKIP_BUFFER_BYTES: Int = 8192
    }
}
