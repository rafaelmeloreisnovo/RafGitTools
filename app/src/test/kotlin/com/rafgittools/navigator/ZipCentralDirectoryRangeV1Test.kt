package com.rafgittools.navigator

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class ZipCentralDirectoryRangeV1Test {
    private fun fromZip(vararg names: String): ZipCentralDirectoryRangeV1.Reader {
        val sink = ByteArrayOutputStream()
        ZipOutputStream(sink).use { zip ->
            for (name in names) {
                zip.putNextEntry(ZipEntry(name))
                zip.write("text".toByteArray())
                zip.closeEntry()
            }
        }
        val bytes = sink.toByteArray()
        return object : ZipCentralDirectoryRangeV1.Reader {
            override val length = bytes.size.toLong()
            override fun readAt(offset: Long, byteCount: Int): ByteArray =
                bytes.copyOfRange(offset.toInt(), offset.toInt() + byteCount)
        }
    }

    private fun put(bytes: ByteArray, at: Int, number: Long, count: Int) {
        for (i in 0 until count) bytes[at + i] = (number ushr (8 * i)).toByte()
    }

    private fun zip64Above4GiB(): ZipCentralDirectoryRangeV1.Reader {
        val basename = "huge.txt".toByteArray()
        val directoryOffset = 4_294_967_296L + 4096L
        val directory = ByteArray(46 + basename.size)
        put(directory, 0, 0x02014b50, 4)
        put(directory, 20, 4, 4)
        put(directory, 24, 4, 4)
        put(directory, 28, basename.size.toLong(), 2)
        basename.copyInto(directory, 46)
        val recordOffset = directoryOffset + directory.size
        val record = ByteArray(56)
        put(record, 0, 0x06064b50, 4)
        put(record, 4, 44, 8)
        put(record, 12, 45, 2)
        put(record, 14, 45, 2)
        put(record, 24, 1, 8)
        put(record, 32, 1, 8)
        put(record, 40, directory.size.toLong(), 8)
        put(record, 48, directoryOffset, 8)
        val locator = ByteArray(20)
        put(locator, 0, 0x07064b50, 4)
        put(locator, 8, recordOffset, 8)
        put(locator, 16, 1, 4)
        val end = ByteArray(22)
        put(end, 0, 0x06054b50, 4)
        put(end, 8, 65535, 2)
        put(end, 10, 65535, 2)
        put(end, 12, 0xffffffffL, 4)
        put(end, 16, 0xffffffffL, 4)
        val segments = listOf(
            directoryOffset to directory,
            recordOffset to record,
            recordOffset + 56 to locator,
            recordOffset + 76 to end
        )
        return object : ZipCentralDirectoryRangeV1.Reader {
            override val length = recordOffset + 56 + 20 + 22
            override fun readAt(offset: Long, byteCount: Int): ByteArray {
                val out = ByteArray(byteCount)
                for ((start, source) in segments) {
                    val left = maxOf(start, offset)
                    val right = minOf(start + source.size, offset + byteCount)
                    if (right > left) {
                        source.copyInto(out, (left - offset).toInt(),
                            (left - start).toInt(), (right - start).toInt())
                    }
                }
                return out
            }
        }
    }

    private fun rejected(fragment: String, call: () -> Unit) {
        try {
            call()
            fail("Expected IOException: $fragment")
        } catch (e: IOException) {
            assertTrue(e.message.orEmpty(), e.message.orEmpty().contains(fragment))
        }
    }

    @Test fun readsClassicZipDirectoryOnly() {
        val source = fromZip("dir/a.txt", "dir/b.txt")
        val r = ZipCentralDirectoryRangeV1.inspect(source)
        assertEquals("DIRECTORY_INDEXED_METADATA_ONLY", r.state)
        assertEquals(listOf("dir/a.txt", "dir/b.txt"), r.entries.map { it.path })
        assertFalse(r.payloadDecompressed)
        assertFalse(r.claimAllowed)
    }

    @Test fun supportsVirtualZip64DirectoryAbove4GiBWithoutAllocation() {
        val r = ZipCentralDirectoryRangeV1.inspect(zip64Above4GiB())
        assertTrue(r.archiveBytes > 4_294_967_296L)
        assertTrue(r.centralDirectoryOffset > 4_294_967_296L)
        assertEquals(1, r.observedEntries)
        assertEquals("huge.txt", r.entries.single().path)
        assertTrue(r.sourceBytesRead < 100_000L)
        assertFalse(r.sourceContentDownloaded)
    }

    @Test fun obeysEntryLimitWithoutClaimingCorruption() {
        val r = ZipCentralDirectoryRangeV1.inspect(fromZip("a", "b", "c"), maxEntries = 1)
        assertEquals("DIRECTORY_PARTIAL_OR_RESTRICTED", r.state)
        assertEquals(1, r.observedEntries)
        assertTrue(r.gaps.contains("ENTRY_COUNT_BUDGET"))
    }

    @Test fun detectsUnsafePathWithoutExtracting() {
        val r = ZipCentralDirectoryRangeV1.inspect(fromZip("../escape.txt"))
        assertTrue(r.entries.single().unsafePath)
        assertTrue(r.gaps.contains("UNSAFE_MEMBER_PATHS_PRESENT"))
    }

    @Test fun rejectsInteriorDoubleSlashInMemberPath() {
        val r = ZipCentralDirectoryRangeV1.inspect(fromZip("a//b.txt"))
        assertTrue(r.entries.single().unsafePath)
        assertTrue(r.gaps.contains("UNSAFE_MEMBER_PATHS_PRESENT"))
    }

    @Test fun failsOnNoCentralDirectory() {
        val empty = object : ZipCentralDirectoryRangeV1.Reader {
            override val length = 512L
            override fun readAt(offset: Long, byteCount: Int) = ByteArray(byteCount)
        }
        rejected("ZIP_END_RECORD_NOT_FOUND") {
            ZipCentralDirectoryRangeV1.inspect(empty)
        }
    }

    @Test fun rejectsRemoteReaderThatReturnsShortRange() {
        val reader = object : ZipCentralDirectoryRangeV1.Reader {
            override val length = 1024L
            override fun readAt(offset: Long, byteCount: Int) = ByteArray(byteCount - 1)
        }
        rejected("ZIP_SHORT_POSITIONED_READ") { ZipCentralDirectoryRangeV1.inspect(reader) }
    }

    @Test fun protectsInvalidBudgets() {
        rejected("ZIP_INVALID_BUDGET") {
            ZipCentralDirectoryRangeV1.inspect(fromZip("a"), maxCentralBytes = 20)
        }
    }
}
