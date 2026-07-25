package com.rafgittools.rafgitfs.index

import android.util.Base64
import com.rafgittools.rafgitfs.remote.RafGitFsBlobDto

object RafGitFsContentDecoder {
    const val DEFAULT_MAX_IN_MEMORY_BYTES: Long = 5L * 1024L * 1024L

    data class Decoded(
        val bytes: ByteArray,
        val textUtf8: String?
    )

    fun decode(
        blob: RafGitFsBlobDto,
        maxBytes: Long = DEFAULT_MAX_IN_MEMORY_BYTES
    ): Decoded {
        require(maxBytes in 1..50L * 1024L * 1024L) { "maxBytes outside bounded range" }
        require(blob.size <= maxBytes) { "blob exceeds in-memory limit" }
        require(blob.encoding == "base64") { "unsupported blob encoding" }
        val content = requireNotNull(blob.content) { "blob content missing" }
        val bytes = Base64.decode(content, Base64.DEFAULT)
        val text = if (looksLikeText(bytes)) bytes.toString(Charsets.UTF_8) else null
        return Decoded(bytes = bytes, textUtf8 = text)
    }

    private fun looksLikeText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        val sample = bytes.take(4096)
        if (sample.any { it == 0.toByte() }) return false
        val controls = sample.count { value ->
            val unsigned = value.toInt() and 0xFF
            unsigned < 0x09 || unsigned in 0x0E..0x1F
        }
        return controls * 20 <= sample.size
    }
}
