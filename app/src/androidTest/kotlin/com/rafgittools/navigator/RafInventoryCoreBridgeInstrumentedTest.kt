package com.rafgittools.navigator

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RafInventoryCoreBridgeInstrumentedTest {
    @Test
    fun fivePassMetadataTokenizationPreservesUnknownVersusZero() {
        val empty = RafInventoryCoreBridge.analyze(
            path = "001_PONTEIRO/empty.json",
            mime = "application/json",
            isDirectory = false,
            reportedBytes = 0L,
            parentKnown = true
        )
        val unknown = RafInventoryCoreBridge.analyze(
            path = "001_PONTEIRO/unknown.json",
            mime = "application/json",
            isDirectory = false,
            reportedBytes = null,
            parentKnown = true
        )

        assertEquals(listOf("001", "PONTEIRO", "empty", "json"), empty.tokens)
        assertEquals(2, empty.pathComponents)
        assertEquals(RafInventoryCoreBridge.ALL_PASSES, empty.passMask)
        assertTrue(empty.sizeKnown)
        assertFalse(unknown.sizeKnown)
        assertTrue(empty.parentKnown)
        assertEquals(empty.kindFlags, unknown.kindFlags)
    }

    @Test
    fun pathTraversalIsRejectedByNativeCore() {
        try {
            RafInventoryCoreBridge.analyze(
                path = "../private/conversations.json",
                mime = "application/json",
                isDirectory = false,
                reportedBytes = null,
                parentKnown = true
            )
            throw AssertionError("unsafe path accepted")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().startsWith("TOKEN_VAZIO_NATIVE_REJECTED"))
        }
    }
}
