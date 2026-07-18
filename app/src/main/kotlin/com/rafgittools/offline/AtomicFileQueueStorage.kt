package com.rafgittools.offline

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Atomic file-backed implementation of [OfflineQueueStorage].
 *
 * Format v1 is length-prefixed and bounded. The caller owns item encoding so
 * this class does not depend on reflection, Java serialization or a specific
 * JSON library.
 */
class AtomicFileQueueStorage<T>(
    private val file: File,
    private val encode: (T) -> ByteArray,
    private val decode: (ByteArray) -> T,
    private val maxItems: Int = 10_000,
    private val maxItemBytes: Int = 1 shl 20
) : OfflineQueueStorage<T> {

    init {
        require(maxItems > 0) { "maxItems must be positive" }
        require(maxItemBytes > 0) { "maxItemBytes must be positive" }
    }

    override fun load(): List<T> {
        if (!file.exists()) return emptyList()

        try {
            DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
                require(input.readInt() == MAGIC) { "Invalid offline queue magic" }
                require(input.readInt() == VERSION) { "Unsupported offline queue version" }
                val count = input.readInt()
                require(count in 0..maxItems) { "Offline queue item count exceeds limit" }

                return List(count) {
                    val length = input.readInt()
                    require(length in 0..maxItemBytes) { "Offline queue item exceeds size limit" }
                    val payload = ByteArray(length)
                    input.readFully(payload)
                    decode(payload)
                }.also {
                    require(input.read() == -1) { "Trailing bytes in offline queue file" }
                }
            }
        } catch (error: EOFException) {
            throw IllegalStateException("Offline queue file is truncated", error)
        }
    }

    override fun replace(items: List<T>) {
        require(items.size <= maxItems) { "Offline queue item count exceeds limit" }
        val parent = file.absoluteFile.parentFile
            ?: throw IllegalStateException("Offline queue path has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("Unable to create offline queue directory")
        }

        val encoded = items.map { item ->
            encode(item).also { payload ->
                require(payload.size <= maxItemBytes) { "Offline queue item exceeds size limit" }
            }
        }

        val temp = File(parent, ".${file.name}.tmp")
        try {
            FileOutputStream(temp).use { stream ->
                DataOutputStream(stream).use { output ->
                    output.writeInt(MAGIC)
                    output.writeInt(VERSION)
                    output.writeInt(encoded.size)
                    encoded.forEach { payload ->
                        output.writeInt(payload.size)
                        output.write(payload)
                    }
                    output.flush()
                    stream.fd.sync()
                }
            }

            if (!temp.renameTo(file)) {
                throw IllegalStateException("Unable to atomically replace offline queue file")
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    companion object {
        private const val MAGIC = 0x52465131 // RFQ1
        private const val VERSION = 1
    }
}
