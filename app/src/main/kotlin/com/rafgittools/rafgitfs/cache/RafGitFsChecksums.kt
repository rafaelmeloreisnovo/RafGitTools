package com.rafgittools.rafgitfs.cache

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object RafGitFsChecksums {
    fun sha256(bytes: ByteArray): String = digest("SHA-256", bytes)

    fun sha256(file: File): String = file.inputStream().use { digest("SHA-256", it) }

    /**
     * Verifies the canonical Git blob object hash: digest("blob <size>\u0000" + content).
     * GitHub repositories normally expose 40-char SHA-1 ids; 64-char SHA-256 ids are
     * accepted for forward compatibility.
     */
    fun verifyGitBlob(bytes: ByteArray, expectedHex: String): Boolean {
        val algorithm = when (expectedHex.length) {
            40 -> "SHA-1"
            64 -> "SHA-256"
            else -> return false
        }
        val digest = MessageDigest.getInstance(algorithm)
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        return constantTimeEquals(digest.digest().toHex(), expectedHex.lowercase())
    }

    fun constantTimeEquals(left: String, right: String): Boolean {
        val a = left.lowercase().toByteArray(Charsets.US_ASCII)
        val b = right.lowercase().toByteArray(Charsets.US_ASCII)
        return MessageDigest.isEqual(a, b)
    }

    private fun digest(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).toHex()

    private fun digest(algorithm: String, input: InputStream): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}
