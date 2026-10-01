package com.rafgittools.execution

import com.rafgittools.library.processing.LibraryContentScope
import com.rafgittools.library.processing.LibraryJobStage
import com.rafgittools.library.processing.LibraryNetworkPolicy
import com.rafgittools.library.processing.LibraryRigorLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionGateContractV1Test {
    @Test
    fun `job is bounded read only offline and claim closed`() {
        val job = ExecutionGateContractV1.buildJob(
            displayName = "sample.txt",
            mediaType = "text/plain",
            sizeBytes = 128,
            contentSha256 = "a".repeat(64),
            opaqueLocatorSha256 = "b".repeat(64),
            nowEpochMs = 1234L
        )

        assertFalse(job.claimAllowed)
        assertTrue(job.source.readOnly)
        assertEquals(LibraryContentScope.FULL_SOURCE, job.source.contentScope)
        assertEquals(LibraryRigorLevel.STRUCTURAL, job.rigor)
        assertEquals(LibraryNetworkPolicy.OFFLINE_ONLY, job.budget.networkPolicy)
        assertEquals(
            listOf(
                LibraryJobStage.DISCOVER,
                LibraryJobStage.INGEST,
                LibraryJobStage.EXTRACT,
                LibraryJobStage.VECTORIZE
            ),
            job.requestedStages
        )
        assertEquals(ExecutionGateContractV1.MAX_SOURCE_BYTES, job.budget.maxBytes)
        assertEquals("a".repeat(64), job.source.contentSha256)
    }

    @Test
    fun `idempotency is deterministic and source bound`() {
        fun build(content: String, locator: String) = ExecutionGateContractV1.buildJob(
            displayName = "x.bin",
            mediaType = "application/octet-stream",
            sizeBytes = 1,
            contentSha256 = content,
            opaqueLocatorSha256 = locator,
            nowEpochMs = 1L
        )

        val first = build("1".repeat(64), "2".repeat(64))
        val replay = build("1".repeat(64), "2".repeat(64))
        val changed = build("3".repeat(64), "2".repeat(64))

        assertEquals(first.idempotencyKey, replay.idempotencyKey)
        assertEquals(first.jobId, replay.jobId)
        assertNotEquals(first.idempotencyKey, changed.idempotencyKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized source is rejected before job creation`() {
        ExecutionGateContractV1.buildJob(
            displayName = "large.bin",
            mediaType = null,
            sizeBytes = ExecutionGateContractV1.MAX_SOURCE_BYTES + 1,
            contentSha256 = "a".repeat(64),
            opaqueLocatorSha256 = "b".repeat(64),
            nowEpochMs = 1L
        )
    }
}
