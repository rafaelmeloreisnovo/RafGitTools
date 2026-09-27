package com.rafgittools.library.processing

import java.io.InputStream
import java.io.InputStreamReader
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.CRC32

fun interface RepeatableLibraryStreamSourceV1 {
    fun open(): InputStream
}

data class LibraryFullSourceStreamAnalysisV1(
    val byteVector: ByteVectorV1,
    val textVector: TextVectorV1?,
    val bytesRead: Long,
    val crc32: String,
    val sha256: String,
    val gaps: List<String>,
    val claimAllowed: Boolean = false
)

/**
 * Bounded-memory full-source descriptor engine.
 *
 * Pass 1: bytes -> size + CRC32 + SHA-256 + byte vector.
 * Pass 2 (text only): re-open source -> text vector, while SHA-256 is replayed
 * and compared with pass 1 to prove both passes saw the same byte source.
 */
object LibraryStreamingDescriptorEngineV1 {
    private const val Q16_ONE = 65536
    private const val UNIQUE_HASH_CAP = 8192
    private const val MAX_TOKEN_CHARS = 65536

    fun analyze(
        source: RepeatableLibraryStreamSourceV1,
        expectedBytes: Long,
        expectedCrc32: String,
        maxOutputBytes: Long,
        chunkBytes: Int = 64 * 1024,
        includeTextVector: Boolean
    ): LibraryFullSourceStreamAnalysisV1 {
        require(expectedBytes >= 0L) { "expectedBytes must be non-negative" }
        require(maxOutputBytes >= expectedBytes) { "maxOutputBytes below expected size" }
        require(maxOutputBytes <= Int.MAX_VALUE.toLong()) {
            "V1 byte vector supports at most Int.MAX_VALUE bytes"
        }
        require(chunkBytes in 4096..(1024 * 1024)) {
            "chunkBytes outside bounded range"
        }
        require(Regex("^[0-9a-fA-F]{8}$").matches(expectedCrc32)) {
            "expected CRC32 must be eight hex digits"
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val crc = CRC32()
        val bins = LongArray(16)
        var total = 0L
        var sum = 0L
        var nonZero = 0L
        var transitions = 0L
        var previous = -1
        val buffer = ByteArray(chunkBytes)

        source.open().use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                total += count.toLong()
                require(total <= maxOutputBytes) { "stream output budget exceeded" }

                digest.update(buffer, 0, count)
                crc.update(buffer, 0, count)

                for (i in 0 until count) {
                    val value = buffer[i].toInt() and 0xff
                    bins[value ushr 4] += 1L
                    sum += value.toLong()
                    if (value != 0) nonZero += 1L
                    if (previous >= 0 && value != previous) transitions += 1L
                    previous = value
                }
            }
        }

        require(total == expectedBytes) {
            "full member size mismatch: observed=" + total + " expected=" + expectedBytes
        }

        val crcHex = java.lang.Long.toHexString(crc.value)
            .padStart(8, '0')
            .takeLast(8)
            .lowercase()
        require(crcHex == expectedCrc32.lowercase()) {
            "full member CRC32 mismatch"
        }

        val shaHex = digest.digest().joinToString("") { "%02x".format(it) }
        val denom = total.coerceAtLeast(1L)
        val byteVector = ByteVectorV1(
            sizeBytes = total.toInt(),
            histogram16Q16 = bins.map { count ->
                ((count * Q16_ONE) / denom).toInt()
            },
            meanByteQ16 = if (total == 0L) 0 else
                ((sum * Q16_ONE) / (total * 255L)).toInt(),
            nonZeroRatioQ16 = ratioQ16(nonZero, total),
            transitionRatioQ16 = ratioQ16(
                transitions,
                (total - 1L).coerceAtLeast(1L)
            ),
            sha256 = shaHex
        )

        val textResult = if (includeTextVector) {
            analyzeTextReplay(
                source = source,
                expectedSha256 = shaHex,
                chunkChars = (chunkBytes / 2).coerceAtLeast(2048)
            )
        } else null

        return LibraryFullSourceStreamAnalysisV1(
            byteVector = byteVector,
            textVector = textResult?.first,
            bytesRead = total,
            crc32 = crcHex,
            sha256 = shaHex,
            gaps = textResult?.second.orEmpty(),
            claimAllowed = false
        )
    }

    private fun analyzeTextReplay(
        source: RepeatableLibraryStreamSourceV1,
        expectedSha256: String,
        chunkChars: Int
    ): Pair<TextVectorV1, List<String>> {
        val replayDigest = MessageDigest.getInstance("SHA-256")
        val buckets = LongArray(16)
        val seen = HashSet<Int>()
        var saturated = false
        var tokens = 0L
        var letters = 0L
        var digits = 0L
        var whitespace = 0L
        var totalChars = 0L
        val token = StringBuilder()

        fun flushToken() {
            if (token.isEmpty()) return
            val hash = fnv1a32(token.toString())
            buckets[(hash ushr 28) and 0x0f] += 1L
            if (hash !in seen) {
                if (seen.size < UNIQUE_HASH_CAP) seen += hash
                else saturated = true
            }
            tokens += 1L
            require(tokens <= Int.MAX_VALUE.toLong()) {
                "stream token count exceeds V1 integer range"
            }
            token.setLength(0)
        }

        source.open().use { raw ->
            DigestInputStream(raw, replayDigest).use { digestStream ->
                InputStreamReader(digestStream, Charsets.UTF_8).use { reader ->
                    val chars = CharArray(chunkChars)
                    while (true) {
                        val count = reader.read(chars)
                        if (count < 0) break
                        if (count == 0) continue

                        for (i in 0 until count) {
                            val ch = chars[i]
                            totalChars += 1L
                            when {
                                ch.isLetterOrDigit() -> {
                                    require(token.length < MAX_TOKEN_CHARS) {
                                        "stream text token exceeds bounded token size"
                                    }
                                    token.append(ch.lowercaseChar())
                                    if (ch.isLetter()) letters += 1L
                                    if (ch.isDigit()) digits += 1L
                                }
                                else -> {
                                    flushToken()
                                    if (ch.isWhitespace()) whitespace += 1L
                                }
                            }
                        }
                    }
                    flushToken()
                }
            }
        }

        val replaySha = replayDigest.digest()
            .joinToString("") { "%02x".format(it) }
        require(replaySha == expectedSha256) {
            "stream replay SHA-256 mismatch"
        }

        val tokenDenom = tokens.coerceAtLeast(1L)
        val charDenom = totalChars.coerceAtLeast(1L)
        val vector = TextVectorV1(
            tokenCount = tokens.toInt(),
            uniqueTokenEstimate = seen.size,
            tokenBuckets16Q16 = buckets.map { count ->
                ((count * Q16_ONE) / tokenDenom).toInt()
            },
            asciiLetterRatioQ16 = ratioQ16(letters, charDenom),
            digitRatioQ16 = ratioQ16(digits, charDenom),
            whitespaceRatioQ16 = ratioQ16(whitespace, charDenom)
        )
        val gaps = if (saturated) {
            listOf("TEXT_UNIQUE_ESTIMATE_SATURATED_AT_" + UNIQUE_HASH_CAP)
        } else emptyList()

        return vector to gaps
    }

    private fun ratioQ16(numerator: Long, denominator: Long): Int =
        ((numerator * Q16_ONE) / denominator.coerceAtLeast(1L))
            .coerceIn(0L, Q16_ONE.toLong())
            .toInt()

    private fun fnv1a32(value: String): Int {
        var hash = 0x811c9dc5.toInt()
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 0x01000193
        }
        return hash
    }
}
