package com.rafgittools.navigator

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlin.math.min

/**
 * ZIP/ZIP64 directory metadata only. No decompression, execution, extraction or source mutation.
 * Reader must perform exact positioned reads, never fetch an entire remote archive as fallback.
 */
object ZipCentralDirectoryRangeV1 {
    interface Reader {
        val length: Long
        @Throws(IOException::class)
        fun readAt(offset: Long, byteCount: Int): ByteArray
    }

    data class Entry(
        val path: String,
        val compressedBytes: Long,
        val originalBytes: Long,
        val localHeaderOffset: Long,
        val method: Int,
        val encrypted: Boolean,
        val unsafePath: Boolean
    )

    data class Index(
        val schema: String = "rafgittools.zip-directory-range/v1",
        val state: String,
        val archiveBytes: Long,
        val centralDirectoryOffset: Long,
        val centralDirectoryBytes: Long,
        val declaredEntries: Long,
        val observedEntries: Int,
        val entries: List<Entry>,
        val sourceBytesRead: Long,
        val sourceContentDownloaded: Boolean = false,
        val payloadDecompressed: Boolean = false,
        val claimAllowed: Boolean = false,
        val gaps: List<String>
    )

    private const val EOCD = 0x06054b50L
    private const val ZIP64_EOCD = 0x06064b50L
    private const val ZIP64_LOCATOR = 0x07064b50L
    private const val CENTRAL_HEADER = 0x02014b50L
    private const val MAX_TAIL = 65557
    private const val MAX_CENTRAL = 2 * 1024 * 1024
    private const val MAX_ENTRIES = 10000

    private fun u16(b: ByteArray, p: Int): Int {
        if (p < 0 || p > b.size - 2) throw IOException("ZIP_HEADER_TRUNCATED")
        return (b[p].toInt() and 255) or ((b[p + 1].toInt() and 255) shl 8)
    }

    private fun u32(b: ByteArray, p: Int): Long =
        u16(b, p).toLong() or (u16(b, p + 2).toLong() shl 16)

    private fun u64(b: ByteArray, p: Int): Long {
        val lo = u32(b, p)
        val hi = u32(b, p + 4)
        if (hi > 0x7fffffffL) throw IOException("ZIP64_OFFSET_UNREPRESENTABLE")
        return (hi shl 32) or lo
    }

    private fun safePath(path: String): Boolean =
        path.isNotBlank() && path.length <= 4096 && !path.startsWith('/') &&
            !path.contains('\\') && !path.contains('\u0000') &&
            !path.contains(':') && path.none { it.code < 32 } &&
            path.removeSuffix("/").split('/').none { it.isEmpty() || it == "." || it == ".." }

    private fun decodeName(raw: ByteArray, utf8: Boolean): String {
        if (!utf8 && raw.any { (it.toInt() and 128) != 0 }) {
            throw IOException("ZIP_NON_UTF8_NAME_ENCODING_UNKNOWN")
        }
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(ByteBuffer.wrap(raw)).toString()
        } catch (_: Exception) {
            throw IOException("ZIP_INVALID_UTF8_FILENAME")
        }
    }

    @Throws(IOException::class)
    fun inspect(source: Reader, maxCentralBytes: Int = 1_048_576, maxEntries: Int = 4096): Index {
        if (maxCentralBytes !in 46..MAX_CENTRAL || maxEntries !in 1..MAX_ENTRIES) {
            throw IOException("ZIP_INVALID_BUDGET")
        }
        val len = source.length
        if (len < 22) throw IOException("ZIP_TOO_SHORT")
        var consumed = 0L
        fun take(offset: Long, count: Int): ByteArray {
            if (count < 0 || offset < 0 || offset > len || count.toLong() > len - offset) {
                throw IOException("ZIP_RANGE_OUTSIDE_SOURCE")
            }
            if (consumed + count.toLong() > MAX_TAIL + 76L + maxCentralBytes.toLong()) {
                throw IOException("ZIP_READ_BUDGET_EXCEEDED")
            }
            val result = source.readAt(offset, count)
            if (result.size != count) throw IOException("ZIP_SHORT_POSITIONED_READ")
            consumed += count
            return result
        }
        val tailBytes = min(len, MAX_TAIL.toLong()).toInt()
        val tailBase = len - tailBytes
        val tail = take(tailBase, tailBytes)
        var eocd = -1
        for (p in tail.size - 22 downTo 0) {
            if (u32(tail, p) == EOCD && p + 22 + u16(tail, p + 20) == tail.size) {
                eocd = p
                break
            }
        }
        if (eocd < 0) throw IOException("ZIP_END_RECORD_NOT_FOUND")
        val eocdAbsolute = tailBase + eocd
        if (u16(tail, eocd + 4) != 0 || u16(tail, eocd + 6) != 0) {
            throw IOException("ZIP_MULTIDISK_NOT_SUPPORTED")
        }
        val thisDiskEntries = u16(tail, eocd + 8)
        var total = u16(tail, eocd + 10).toLong()
        var directorySize = u32(tail, eocd + 12)
        var directoryOffset = u32(tail, eocd + 16)
        val needs64 = total == 0xffffL || thisDiskEntries == 0xffff ||
            directorySize == 0xffffffffL || directoryOffset == 0xffffffffL
        val directoryEnd: Long
        if (needs64) {
            if (eocdAbsolute < 20) throw IOException("ZIP64_LOCATOR_MISSING")
            val locator = take(eocdAbsolute - 20, 20)
            if (u32(locator, 0) != ZIP64_LOCATOR || u32(locator, 4) != 0L ||
                u32(locator, 16) != 1L) throw IOException("ZIP64_LOCATOR_INVALID")
            val zip64Offset = u64(locator, 8)
            if (zip64Offset > eocdAbsolute - 20 - 56) throw IOException("ZIP64_BAD_RECORD_OFFSET")
            val record = take(zip64Offset, 56)
            if (u32(record, 0) != ZIP64_EOCD || u64(record, 4) < 44L ||
                u32(record, 16) != 0L || u32(record, 20) != 0L) {
                throw IOException("ZIP64_RECORD_INVALID")
            }
            if (u64(record, 24) != u64(record, 32)) {
                throw IOException("ZIP64_MULTIDISK_NOT_SUPPORTED")
            }
            total = u64(record, 32)
            directorySize = u64(record, 40)
            directoryOffset = u64(record, 48)
            directoryEnd = zip64Offset
        } else {
            if (thisDiskEntries.toLong() != total) throw IOException("ZIP_MULTIDISK_ENTRIES")
            directoryEnd = eocdAbsolute
        }
        if (directoryOffset > directoryEnd || directorySize > directoryEnd - directoryOffset) {
            throw IOException("ZIP_CENTRAL_DIRECTORY_OUTSIDE_SOURCE")
        }
        if (directorySize == 0L && total != 0L) throw IOException("ZIP_DIRECTORY_MISSING")
        val gaps = mutableListOf<String>()
        if (directorySize > maxCentralBytes) gaps += "CENTRAL_DIRECTORY_BYTE_BUDGET"
        val windowSize = min(directorySize, maxCentralBytes.toLong()).toInt()
        val window = take(directoryOffset, windowSize)
        val entries = ArrayList<Entry>(min(maxEntries, 1024))
        var pos = 0
        while (pos < window.size && entries.size < maxEntries && entries.size.toLong() < total) {
            if (window.size - pos < 46) {
                gaps += "CENTRAL_ENTRY_TRUNCATED_AT_BUDGET"
                break
            }
            if (u32(window, pos) != CENTRAL_HEADER) throw IOException("ZIP_CENTRAL_HEADER_INVALID")
            val flags = u16(window, pos + 8)
            val method = u16(window, pos + 10)
            var compressed = u32(window, pos + 20)
            var original = u32(window, pos + 24)
            val nameLen = u16(window, pos + 28)
            val extraLen = u16(window, pos + 30)
            val commentLen = u16(window, pos + 32)
            var localOffset = u32(window, pos + 42)
            val fullHeaderLength = 46L + nameLen + extraLen + commentLen
            if (fullHeaderLength > window.size - pos) {
                gaps += "CENTRAL_ENTRY_EXCEEDS_READ_WINDOW"
                break
            }
            if (nameLen > 4096) throw IOException("ZIP_FILENAME_LIMIT")
            val filename = decodeName(
                window.copyOfRange(pos + 46, pos + 46 + nameLen), flags and 0x0800 != 0
            )
            var extra = pos + 46 + nameLen
            val extraEnd = extra + extraLen
            if (compressed == 0xffffffffL || original == 0xffffffffL ||
                localOffset == 0xffffffffL) {
                var found64 = false
                while (extra < extraEnd) {
                    if (extraEnd - extra < 4) throw IOException("ZIP_EXTRA_TRUNCATED")
                    val id = u16(window, extra)
                    val dataLen = u16(window, extra + 2)
                    extra += 4
                    if (dataLen > extraEnd - extra) throw IOException("ZIP_EXTRA_LENGTH_INVALID")
                    if (id == 0x0001) {
                        found64 = true
                        val end = extra + dataLen
                        fun next64(): Long {
                            if (extra > end - 8) throw IOException("ZIP64_EXTRA_MISSING_FIELD")
                            val n = u64(window, extra)
                            extra += 8
                            return n
                        }
                        if (original == 0xffffffffL) original = next64()
                        if (compressed == 0xffffffffL) compressed = next64()
                        if (localOffset == 0xffffffffL) localOffset = next64()
                        break
                    }
                    extra += dataLen
                }
                if (!found64) throw IOException("ZIP64_EXTRA_REQUIRED")
            }
            if (localOffset >= directoryOffset || original < 0 || compressed < 0) {
                throw IOException("ZIP_MEMBER_RANGE_INVALID")
            }
            entries += Entry(filename, compressed, original, localOffset, method,
                flags and 1 != 0, !safePath(filename))
            pos += fullHeaderLength.toInt()
        }
        if (entries.size >= maxEntries && entries.size.toLong() < total) {
            gaps += "ENTRY_COUNT_BUDGET"
        }
        if (directorySize <= maxCentralBytes && entries.size.toLong() != total &&
            "ENTRY_COUNT_BUDGET" !in gaps) {
            throw IOException("ZIP_DECLARED_ENTRY_COUNT_MISMATCH")
        }
        if (entries.any { it.unsafePath }) gaps += "UNSAFE_MEMBER_PATHS_PRESENT"
        if (entries.any { it.encrypted }) gaps += "ENCRYPTED_MEMBERS_PRESENT"
        if (directorySize <= maxCentralBytes && pos.toLong() != directorySize &&
            "ENTRY_COUNT_BUDGET" !in gaps) {
            throw IOException("ZIP_CENTRAL_DIRECTORY_SIZE_MISMATCH")
        }
        return Index(
            state = if (gaps.isEmpty()) "DIRECTORY_INDEXED_METADATA_ONLY"
                else "DIRECTORY_PARTIAL_OR_RESTRICTED",
            archiveBytes = len,
            centralDirectoryOffset = directoryOffset,
            centralDirectoryBytes = directorySize,
            declaredEntries = total,
            observedEntries = entries.size,
            entries = entries,
            sourceBytesRead = consumed,
            gaps = gaps.distinct()
        )
    }
}
