package com.rafgittools.library.processing

import java.security.MessageDigest
import kotlin.math.min

object LocalDescriptorEngine {
    private const val Q16_ONE = 65536

    fun byteVector(bytes: ByteArray, includeSha256: Boolean): ByteVectorV1 {
        if (bytes.isEmpty()) {
            return ByteVectorV1(
                sizeBytes = 0,
                histogram16Q16 = List(16) { 0 },
                meanByteQ16 = 0,
                nonZeroRatioQ16 = 0,
                transitionRatioQ16 = 0,
                sha256 = if (includeSha256) sha256(bytes) else null
            )
        }

        val bins = IntArray(16)
        var sum = 0L
        var nonZero = 0L
        var transitions = 0L
        var previous = bytes[0].toInt() and 0xff

        bytes.forEachIndexed { index, raw ->
            val value = raw.toInt() and 0xff
            bins[value ushr 4] += 1
            sum += value.toLong()
            if (value != 0) nonZero += 1
            if (index > 0 && value != previous) transitions += 1
            previous = value
        }

        return ByteVectorV1(
            sizeBytes = bytes.size,
            histogram16Q16 = normalizeCountsQ16(bins, bytes.size),
            meanByteQ16 = ((sum * Q16_ONE) / (bytes.size.toLong() * 255L)).toInt(),
            nonZeroRatioQ16 = ratioQ16(nonZero, bytes.size.toLong()),
            transitionRatioQ16 = ratioQ16(
                transitions,
                (bytes.size - 1).coerceAtLeast(1).toLong()
            ),
            sha256 = if (includeSha256) sha256(bytes) else null
        )
    }

    fun textVector(text: String): TextVectorV1 {
        val buckets = IntArray(16)
        var tokens = 0
        var letters = 0L
        var digits = 0L
        var whitespace = 0L
        val seen = HashSet<Int>()

        val token = StringBuilder()
        fun flushToken() {
            if (token.isEmpty()) return
            val value = token.toString()
            val hash = fnv1a32(value)
            buckets[(hash ushr 28) and 0x0f] += 1
            seen += hash
            tokens += 1
            token.setLength(0)
        }

        text.forEach { ch ->
            when {
                ch.isLetterOrDigit() -> {
                    token.append(ch.lowercaseChar())
                    if (ch.isLetter()) letters += 1
                    if (ch.isDigit()) digits += 1
                }
                else -> {
                    flushToken()
                    if (ch.isWhitespace()) whitespace += 1
                }
            }
        }
        flushToken()

        val totalChars = text.length.coerceAtLeast(1).toLong()
        return TextVectorV1(
            tokenCount = tokens,
            uniqueTokenEstimate = seen.size,
            tokenBuckets16Q16 = normalizeCountsQ16(buckets, tokens.coerceAtLeast(1)),
            asciiLetterRatioQ16 = ratioQ16(letters, totalChars),
            digitRatioQ16 = ratioQ16(digits, totalChars),
            whitespaceRatioQ16 = ratioQ16(whitespace, totalChars)
        )
    }

    /**
     * The input is a decoded 8-bit grayscale tile, row-major.
     * Image decoding itself is deliberately outside this freestanding-ish core.
     */
    fun visualVectorGray8(
        pixels: ByteArray,
        width: Int,
        height: Int
    ): VisualVectorV1 {
        require(width > 0 && height > 0) { "positive dimensions required" }
        require(width.toLong() * height.toLong() == pixels.size.toLong()) {
            "pixel count does not match dimensions"
        }

        val bins = IntArray(16)
        val quadrantSum = LongArray(4)
        val quadrantCount = LongArray(4)
        var horizontal = 0L
        var vertical = 0L
        var diagonal = 0L
        var dark = 0L

        fun p(x: Int, y: Int): Int = pixels[y * width + x].toInt() and 0xff

        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = p(x, y)
                bins[value ushr 4] += 1
                if (value < 64) dark += 1

                val quadrant = (if (y * 2 >= height) 2 else 0) +
                    (if (x * 2 >= width) 1 else 0)
                quadrantSum[quadrant] += value.toLong()
                quadrantCount[quadrant] = quadrantCount[quadrant] + 1L

                if (x > 0) horizontal += kotlin.math.abs(value - p(x - 1, y)).toLong()
                if (y > 0) vertical += kotlin.math.abs(value - p(x, y - 1)).toLong()
                if (x > 0 && y > 0) {
                    diagonal += kotlin.math.abs(value - p(x - 1, y - 1)).toLong()
                }
            }
        }

        val horizontalEdges = height.toLong() * (width - 1).coerceAtLeast(1).toLong()
        val verticalEdges = width.toLong() * (height - 1).coerceAtLeast(1).toLong()
        val diagonalEdges = (width - 1).coerceAtLeast(1).toLong() *
            (height - 1).coerceAtLeast(1).toLong()

        return VisualVectorV1(
            width = width,
            height = height,
            intensityHistogram16Q16 = normalizeCountsQ16(bins, pixels.size),
            quadrantMeanQ16 = quadrantSum.indices.map { index ->
                val count = quadrantCount[index].coerceAtLeast(1L)
                ((quadrantSum[index] * Q16_ONE) / (count * 255L)).toInt()
            },
            horizontalGradientQ16 = normalizedGradientQ16(horizontal, horizontalEdges),
            verticalGradientQ16 = normalizedGradientQ16(vertical, verticalEdges),
            diagonalGradientQ16 = normalizedGradientQ16(diagonal, diagonalEdges),
            darkRatioQ16 = ratioQ16(dark, pixels.size.toLong())
        )
    }

    fun cosineLikeQ16(a: List<Int>, b: List<Int>): Int {
        require(a.size == b.size) { "vector dimensions differ" }
        if (a.isEmpty()) return 0
        var dot = 0L
        var normA = 0L
        var normB = 0L
        for (i in a.indices) {
            val av = a[i].toLong()
            val bv = b[i].toLong()
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        if (normA == 0L || normB == 0L) return 0

        val denominator = integerSqrt(normA) * integerSqrt(normB)
        if (denominator <= 0L) return 0
        return min(Q16_ONE.toLong(), (dot * Q16_ONE) / denominator).toInt()
    }

    private fun normalizedGradientQ16(sum: Long, count: Long): Int =
        ((sum * Q16_ONE) / (count.coerceAtLeast(1L) * 255L))
            .coerceIn(0L, Q16_ONE.toLong())
            .toInt()

    private fun normalizeCountsQ16(counts: IntArray, total: Int): List<Int> {
        val denom = total.coerceAtLeast(1).toLong()
        return counts.map { count ->
            ((count.toLong() * Q16_ONE) / denom).toInt()
        }
    }

    private fun ratioQ16(numerator: Long, denominator: Long): Int =
        ((numerator * Q16_ONE) / denominator.coerceAtLeast(1L))
            .coerceIn(0L, Q16_ONE.toLong())
            .toInt()

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun fnv1a32(value: String): Int {
        var hash = 0x811c9dc5.toInt()
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = hash xor (byte.toInt() and 0xff)
            hash *= 0x01000193
        }
        return hash
    }

    private fun integerSqrt(value: Long): Long {
        if (value <= 0L) return 0L
        var x = value
        var y = (x + 1L) ushr 1
        while (y < x) {
            x = y
            y = (x + value / x) ushr 1
        }
        return x
    }
}
