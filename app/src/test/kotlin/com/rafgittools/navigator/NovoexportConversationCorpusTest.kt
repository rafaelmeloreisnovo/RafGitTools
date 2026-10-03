package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovoexportConversationCorpusTest {
    private fun entry(name: String, id: String = name): NovoexportSafInventory.Entry =
        NovoexportSafInventory.Entry(
            uri = "content://provider/document/$id",
            documentId = id,
            name = name,
            mimeType = "application/json",
            sizeBytes = 1L
        )

    @Test
    fun exact000Through050IsComplete() {
        val entries = (0..50).map { index ->
            entry(NovoexportConversationCorpus.expectedFileName(index))
        }
        val coverage = NovoexportConversationCorpus.coverage(entries)

        assertEquals(51, coverage.expectedCount)
        assertEquals(51, coverage.presentCount)
        assertTrue(coverage.complete)
        assertTrue(coverage.missingIndices.isEmpty())
        assertTrue(coverage.duplicateIndices.isEmpty())
        assertTrue(coverage.gapRefs().isEmpty())
    }

    @Test
    fun missing018RemainsExplicitGap() {
        val entries = (0..50)
            .filterNot { it == 18 }
            .map { index -> entry(NovoexportConversationCorpus.expectedFileName(index)) }
        val coverage = NovoexportConversationCorpus.coverage(entries)

        assertFalse(coverage.complete)
        assertEquals(listOf(18), coverage.missingIndices)
        assertTrue(coverage.gapRefs().contains("CONVERSATIONS_MISSING_018"))
    }

    @Test
    fun duplicatesAndOutOfRangeFailClosed() {
        val entries = listOf(
            entry("conversations-000.json", "a"),
            entry("CONVERSATIONS-000.JSON", "b"),
            entry("conversations-051.json", "c"),
            entry("conversation-001.json", "d"),
            entry("codex-001.json", "e")
        )
        val coverage = NovoexportConversationCorpus.coverage(entries)

        assertFalse(coverage.complete)
        assertEquals(listOf(0), coverage.duplicateIndices)
        assertEquals(listOf("conversations-051.json"), coverage.outOfRangeNames)
        assertTrue(coverage.gapRefs().contains("CONVERSATIONS_DUPLICATE_000"))
        assertTrue(coverage.gapRefs().contains("CONVERSATIONS_OUT_OF_RANGE_PRESENT"))
    }
}
