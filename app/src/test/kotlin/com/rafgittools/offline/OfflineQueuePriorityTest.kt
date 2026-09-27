package com.rafgittools.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OfflineQueuePriorityTest {
    @Test
    fun dequeueBestSelectsHighestAndPreservesFifoForTies() {
        val queue = OfflineQueue<String>()
        queue.enqueue("first-low")
        queue.enqueue("first-high")
        queue.enqueue("second-high")

        val first = queue.dequeueBest {
            if (it.endsWith("high")) 10 else 1
        }
        val second = queue.dequeueBest {
            if (it.endsWith("high")) 10 else 1
        }

        assertEquals("first-high", first)
        assertEquals("second-high", second)
        assertEquals(listOf("first-low"), queue.snapshot())
    }

    @Test
    fun persistenceFailureRollsBackPriorityRemoval() {
        var fail = false
        val storage = object : OfflineQueueStorage<String> {
            private var saved = emptyList<String>()

            override fun load(): List<String> = saved

            override fun replace(items: List<String>) {
                if (fail) error("synthetic persistence failure")
                saved = items
            }
        }

        val queue = OfflineQueue(storage)
        queue.enqueue("low")
        queue.enqueue("high")
        fail = true

        assertThrows(IllegalStateException::class.java) {
            queue.dequeueBest { if (it == "high") 10 else 1 }
        }
        assertEquals(listOf("low", "high"), queue.snapshot())
    }
}
