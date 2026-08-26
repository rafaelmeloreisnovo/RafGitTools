package com.rafgittools.rafgitfs.receipts

import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RafFluentCanonicalStoreTest {
    private fun tempStoreFile(): File {
        val file = File.createTempFile("raf-fluent-canonical-", ".bin")
        assertTrue(file.delete())
        file.deleteOnExit()
        return file
    }

    private fun message(tag: Char, time: Int): ByteArray = byteArrayOf(
        0x93.toByte(),       // [tag, time, record]
        0xA1.toByte(), tag.code.toByte(),
        time.toByte(),
        0x80.toByte()        // empty record map for framing/custody test
    )

    @Test
    fun `append preserves exact canonical bytes and replay order`() {
        val file = tempStoreFile()
        val store = RafFluentCanonicalStore(file)
        val first = message('a', 1)
        val second = message('b', 2)

        val r1 = store.append(first)
        val r2 = store.append(second)

        assertEquals(0L, r1.offset)
        assertTrue(r2.offset > r1.offset)

        val replayed = mutableListOf<ByteArray>()
        assertEquals(2, store.replay { replayed += it })
        assertArrayEquals(first, replayed[0])
        assertArrayEquals(second, replayed[1])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non forward message payload is rejected before custody`() {
        RafFluentCanonicalStore(tempStoreFile()).append(byteArrayOf(0x91.toByte(), 0x00))
    }

    @Test(expected = RafFluentCanonicalStore.CorruptStoreException::class)
    fun `append refuses to hide an incomplete prior tail`() {
        val file = tempStoreFile()
        val store = RafFluentCanonicalStore(file)
        store.append(message('a', 1))

        FileOutputStream(file, true).use { out ->
            // Declares five bytes but writes only two: explicit damaged tail.
            out.write(byteArrayOf(0x00, 0x00, 0x00, 0x05, 0x93.toByte(), 0xA0.toByte()))
            out.flush()
            out.fd.sync()
        }

        store.append(message('b', 2))
    }
}
