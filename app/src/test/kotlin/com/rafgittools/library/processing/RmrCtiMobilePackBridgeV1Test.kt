package com.rafgittools.library.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RmrCtiMobilePackBridgeV1Test {
    private fun pack(
        claimAllowed: Boolean = false
    ) = RmrCtiMobileContentPackV1(
        schema = "rmrcti.content-pack/v1",
        packId = "PACK-001",
        sourceArchive = "/private/local/path/NOVOexport.zip",
        sourceMember = "docs/example.txt",
        sourceMemberCrc32 = "89abcdef",
        sourceMemberUncompressedBytes = 100000,
        category = "document_text",
        lifecycleRole = "SOURCE",
        terms = listOf("geometry", "evidence"),
        sampleText = "bounded sample only",
        sampleScope = "bounded_head_and_tail_not_full_member",
        epistemicState = "OBSERVED_SAMPLE",
        claimAllowed = claimAllowed
    )

    @Test
    fun bridgeCreatesQuickBoundedSampleWithoutFullHash() {
        val result = RmrCtiMobilePackBridgeV1.bridge(
            pack = pack(),
            createdAtEpochMs = 1L
        )

        assertEquals(LibraryRigorLevel.QUICK, result.job.rigor)
        assertEquals(
            LibraryContentScope.BOUNDED_SAMPLE,
            result.job.source.contentScope
        )
        assertNull(result.job.source.contentSha256)
        assertTrue(result.requiresFullMemberForDeeperRigor)
        assertEquals(64, result.sourceArchiveLocatorSha256.length)
        assertFalse(
            result.job.source.opaqueLocatorHash.contains("/private/local/path/")
        )
        assertEquals("bounded sample only", result.input.text)
    }

    @Test
    fun structuralRigorCannotTreatBoundedSampleHashAsFullSourceHash() {
        val bridged = RmrCtiMobilePackBridgeV1.bridge(pack(), 1L)
        val structural = bridged.job.copy(rigor = LibraryRigorLevel.STRUCTURAL)

        val result = LibraryLocalJobExecutor().execute(
            job = structural,
            input = bridged.input,
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8L * 1024L * 1024L,
                batteryPercent = 90,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.CHECKPOINTED, result.receipt.finalState)
        assertTrue(result.receipt.gaps.contains("FULL_SOURCE_SCOPE_REQUIRED"))
        assertNull(result.descriptors?.byteVector?.sha256)
    }

    @Test
    fun quickBoundedSampleCanProduceLightweightDescriptor() {
        val bridged = RmrCtiMobilePackBridgeV1.bridge(pack(), 1L)
        val result = LibraryLocalJobExecutor().execute(
            job = bridged.job,
            input = bridged.input,
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8L * 1024L * 1024L,
                batteryPercent = 90,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.SUCCEEDED, result.receipt.finalState)
        assertNull(result.descriptors?.byteVector?.sha256)
        assertTrue(
            result.descriptors?.evidenceNotes
                ?.contains("SOURCE_CONTENT_SCOPE=BOUNDED_SAMPLE") == true
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun claimAllowedPackIsRejected() {
        RmrCtiMobilePackBridgeV1.bridge(
            pack = pack(claimAllowed = true),
            createdAtEpochMs = 1L
        )
    }
}
