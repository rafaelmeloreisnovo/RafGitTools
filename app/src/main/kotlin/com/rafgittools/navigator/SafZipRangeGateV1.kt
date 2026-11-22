package com.rafgittools.navigator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Android SAF entry into the ZIP directory reader.
 *
 * SAF providers MAY stage an entire document when openFileDescriptor is called.
 * For that reason, refuse big/unknown files BEFORE opening any descriptor.
 * Large Google Drive ZIP/ZIPRAF requires separately authorized HTTP Range adapter.
 */
object SafZipRangeGateV1 {
    const val MAX_SMALL_SAF_SOURCE_BYTES = 16L * 1024L * 1024L

    @Throws(IOException::class)
    fun inspectSelected(
        resolver: ContentResolver,
        uri: Uri
    ): ZipCentralDirectoryRangeV1.Index {
        if (uri.scheme != "content") throw IOException("SAF_CONTENT_URI_REQUIRED")
        val projection = arrayOf(OpenableColumns.SIZE)
        val reportedBytes = try {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                val col = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (col < 0 || !cursor.moveToFirst() || cursor.isNull(col)) null
                else cursor.getLong(col)
            }
        } catch (e: SecurityException) {
            throw IOException("SAF_SOURCE_PERMISSION_DENIED", e)
        }
        if (reportedBytes == null || reportedBytes < 0L) {
            throw IOException("SAF_SOURCE_SIZE_TOKEN_VAZIO")
        }
        if (reportedBytes > MAX_SMALL_SAF_SOURCE_BYTES) {
            throw IOException("SAF_LARGE_ARCHIVE_REQUIRES_REMOTE_RANGE")
        }
        val handle = try {
            resolver.openFileDescriptor(uri, "r")
                ?: throw IOException("SAF_DESCRIPTOR_UNAVAILABLE")
        } catch (e: SecurityException) {
            throw IOException("SAF_SOURCE_PERMISSION_DENIED", e)
        }
        handle.use { descriptor ->
            if (descriptor.statSize < 0L || descriptor.statSize != reportedBytes) {
                throw IOException("SAF_POSITIONED_LENGTH_UNKNOWN_OR_CHANGED")
            }
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                if (channel.size() != reportedBytes) {
                    throw IOException("SAF_POSITIONED_LENGTH_MISMATCH")
                }
                val source = object : ZipCentralDirectoryRangeV1.Reader {
                    override val length: Long = reportedBytes

                    override fun readAt(offset: Long, byteCount: Int): ByteArray {
                        if (byteCount < 0 || offset < 0 || offset > length ||
                            byteCount.toLong() > length - offset) {
                            throw IOException("SAF_POSITION_OUT_OF_BOUNDS")
                        }
                        val buffer = ByteBuffer.allocate(byteCount)
                        var current = offset
                        while (buffer.hasRemaining()) {
                            val read = channel.read(buffer, current)
                            if (read <= 0) throw IOException("SAF_POSITIONED_READ_UNAVAILABLE")
                            current += read
                        }
                        return buffer.array()
                    }
                }
                return ZipCentralDirectoryRangeV1.inspect(source)
            }
        }
    }
}
