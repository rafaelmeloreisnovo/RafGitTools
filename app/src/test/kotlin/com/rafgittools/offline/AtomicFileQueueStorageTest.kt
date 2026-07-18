package com.rafgittools.offline

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AtomicFileQueueStorageTest {

    @Test
    fun `queue survives reconstruction from atomic file`() {
        val directory = createTempDir(prefix = "raf-offline-")
        try {
            val file = File(directory, "queue.bin")
            val storage = AtomicFileQueueStorage(
                file = file,
                encode = { value: String -> value.encodeToByteArray() },
                decode = { bytes -> bytes.decodeToString() }
            )

            OfflineQueue(storage).apply {
                enqueue("first")
                enqueue("second")
            }

            val restored = OfflineQueue(storage)
            assertEquals(listOf("first", "second"), restored.snapshot())
            assertEquals("first", restored.dequeue())
            assertEquals(listOf("second"), OfflineQueue(storage).snapshot())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `failed persistence rolls back mutation`() {
        val failingStorage = object : OfflineQueueStorage<String> {
            override fun load(): List<String> = emptyList()
            override fun replace(items: List<String>) {
                throw IllegalStateException("disk full")
            }
        }
        val queue = OfflineQueue(failingStorage)

        assertFailsWith<IllegalStateException> { queue.enqueue("not-durable") }
        assertEquals(emptyList(), queue.snapshot())
    }
}
