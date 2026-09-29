package com.rafgittools.navigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class NovoexportQueueStoreTest {
    private fun inventory(): NovoexportSafInventory.Result =
        NovoexportSafInventory.Result(
            treeUri = "content://provider/tree/root",
            visitedDocuments = 2,
            visitedDirectories = 1,
            candidateFiles = listOf(
                NovoexportSafInventory.Entry(
                    uri = "content://provider/document/a",
                    documentId = "a",
                    name = "conversations.json",
                    mimeType = "application/json",
                    sizeBytes = 123L
                )
            ),
            knownCandidateBytes = 123L,
            unknownSizeCandidateFiles = 0
        )

    @Test fun mergePersistsAndPreservesRetryStateAcrossReload() {
        val root = Files.createTempDirectory("novoexport-queue").toFile()
        val first = NovoexportQueueStore.mergeInventory(root, inventory(), nowEpochMs = 10)
        assertEquals(1, first.added)
        assertEquals(NovoexportQueueStore.PENDING, first.snapshot.items.single().state)

        val itemId = first.snapshot.items.single().id
        NovoexportQueueStore.transition(first.queueFile, itemId, NovoexportQueueStore.PROCESSING, nowEpochMs = 20)
        NovoexportQueueStore.transition(
            first.queueFile,
            itemId,
            NovoexportQueueStore.FAILED_RETRYABLE,
            error = "provider permission lost",
            nowEpochMs = 30
        )

        val merged = NovoexportQueueStore.mergeInventory(root, inventory(), nowEpochMs = 40)
        val resumed = NovoexportQueueStore.load(merged.queueFile).items.single()
        assertEquals(0, merged.added)
        assertEquals(1, merged.preserved)
        assertEquals(NovoexportQueueStore.FAILED_RETRYABLE, resumed.state)
        assertEquals(1, resumed.attempts)
        assertEquals("provider permission lost", resumed.lastError)
    }

    @Test fun transitionGraphFailsClosed() {
        assertTrue(NovoexportQueueStore.allowedTransition(NovoexportQueueStore.PENDING, NovoexportQueueStore.PROCESSING))
        assertTrue(NovoexportQueueStore.allowedTransition(NovoexportQueueStore.BLOCKED, NovoexportQueueStore.PENDING))
        assertFalse(NovoexportQueueStore.allowedTransition(NovoexportQueueStore.COMPLETE, NovoexportQueueStore.PROCESSING))
        assertFalse(NovoexportQueueStore.allowedTransition("TOKEN_VAZIO", NovoexportQueueStore.COMPLETE))
    }
}
