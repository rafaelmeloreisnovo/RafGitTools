package com.rafgittools.library.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLocalProcessingTest {
    private fun job(
        rigor: LibraryRigorLevel = LibraryRigorLevel.STRUCTURAL,
        mediaType: String = "text/plain",
        maxBytes: Long = 1024 * 1024
    ) = LibraryLocalJob(
        jobId = "JOB-LIBRARY-0001",
        source = LibrarySourceRef(
            sourceId = "SRC-001",
            sourceKind = LibrarySourceKind.GENERATED_FIXTURE,
            opaqueLocatorHash = "0".repeat(64),
            displayName = "fixture.txt",
            mediaType = mediaType,
            sizeBytes = 32,
            contentSha256 = null,
            modifiedTime = null
        ),
        requestedStages = listOf(
            LibraryJobStage.DISCOVER,
            LibraryJobStage.INGEST,
            LibraryJobStage.EXTRACT,
            LibraryJobStage.VECTORIZE
        ),
        rigor = rigor,
        budget = LibraryProcessingBudget(
            maxBytes = maxBytes,
            maxWorkingMemoryBytes = 4 * 1024 * 1024,
            maxItems = 1,
            maxAttempts = 3,
            checkpointEveryBytes = 64 * 1024,
            minBatteryPercent = 10,
            thermalPolicy = LibraryThermalPolicy.NORMAL,
            networkPolicy = LibraryNetworkPolicy.OFFLINE_ONLY
        ),
        outputNamespace = "library/test",
        idempotencyKey = "IDEMPOTENCY-LIBRARY-0001",
        createdAtEpochMs = 1L
    )

    @Test
    fun structuralTextProducesDeterministicVectorsAndSha() {
        val executor = LibraryLocalJobExecutor()
        val input = LibraryJobInput(
            bytes = "cachorro de oculos cachorro".toByteArray(),
            text = "cachorro de oculos cachorro"
        )
        val env = LibraryJobEnvironment(
            availableWorkingMemoryBytes = 8 * 1024 * 1024,
            batteryPercent = 80,
            nowEpochMs = 2L
        )

        val a = executor.execute(job(), input, env)
        val b = executor.execute(job(), input, env)

        assertEquals(LibraryJobState.SUCCEEDED, a.receipt.finalState)
        assertEquals(a.receipt.descriptorSha256, b.receipt.descriptorSha256)
        assertNotNull(a.descriptors?.byteVector?.sha256)
        assertEquals(4, a.descriptors?.textVector?.tokenCount)
    }

    @Test
    fun quickRigorDoesNotRequireFullHash() {
        val result = LibraryLocalJobExecutor().execute(
            job = job(rigor = LibraryRigorLevel.QUICK),
            input = LibraryJobInput(bytes = byteArrayOf(1, 2, 3, 4)),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8 * 1024 * 1024,
                batteryPercent = 90,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.SUCCEEDED, result.receipt.finalState)
        assertNull(result.descriptors?.byteVector?.sha256)
    }

    @Test
    fun evidenceImageWithoutDecodedPixelsCheckpointsInsteadOfDowngrading() {
        val result = LibraryLocalJobExecutor().execute(
            job = job(
                rigor = LibraryRigorLevel.EVIDENCE,
                mediaType = "image/png"
            ),
            input = LibraryJobInput(bytes = byteArrayOf(1, 2, 3, 4)),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8 * 1024 * 1024,
                batteryPercent = 90,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.CHECKPOINTED, result.receipt.finalState)
        assertTrue(result.receipt.gaps.contains("REQUIRED_VISUAL_VECTOR_TOKEN_VAZIO"))
    }

    @Test
    fun insufficientBatteryBlocksWithoutRigorDowngrade() {
        val result = LibraryLocalJobExecutor().execute(
            job = job(rigor = LibraryRigorLevel.MULTIMODAL),
            input = LibraryJobInput(bytes = byteArrayOf(1, 2, 3)),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8 * 1024 * 1024,
                batteryPercent = 5,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.BLOCKED_RESOURCE, result.receipt.finalState)
        assertNull(result.descriptors)
    }

    @Test
    fun unimplementedMaterializeStageCheckpointsWithoutFalsePass() {
        val base = job(rigor = LibraryRigorLevel.STRUCTURAL)
        val requested = base.copy(
            requestedStages = base.requestedStages + LibraryJobStage.MATERIALIZE
        )

        val result = LibraryLocalJobExecutor().execute(
            job = requested,
            input = LibraryJobInput(
                bytes = "materialize later".toByteArray(),
                text = "materialize later"
            ),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8 * 1024 * 1024,
                batteryPercent = 90,
                nowEpochMs = 2L
            )
        )

        assertEquals(LibraryJobState.CHECKPOINTED, result.receipt.finalState)
        assertFalse(result.receipt.completedStages.contains(LibraryJobStage.MATERIALIZE))
        assertTrue(result.receipt.gaps.contains("MATERIALIZE_EXECUTOR_NOT_IMPLEMENTED"))
        assertNotNull(result.descriptors)
        assertNotNull(result.receipt.descriptorSha256)
    }

    @Test
    fun visualDescriptorIsDeterministicAndQ16Bounded() {
        val pixels = byteArrayOf(
            0, 16, 32, 48,
            64, 80, 96, 112,
            -128, -112, -96, -80,
            -64, -48, -32, -1
        )
        val a = LocalDescriptorEngine.visualVectorGray8(pixels, 4, 4)
        val b = LocalDescriptorEngine.visualVectorGray8(pixels, 4, 4)

        assertEquals(a, b)
        assertEquals(16, a.intensityHistogram16Q16.size)
        assertEquals(4, a.quadrantMeanQ16.size)
        assertTrue(a.horizontalGradientQ16 in 0..65536)
        assertTrue(a.verticalGradientQ16 in 0..65536)
        assertTrue(a.diagonalGradientQ16 in 0..65536)
    }

    @Test
    fun visualSimilarityIsNotIdentityProof() {
        val a = listOf(65536, 0, 0, 0)
        val b = listOf(65000, 536, 0, 0)
        val score = LocalDescriptorEngine.cosineLikeQ16(a, b)
        assertTrue(score > 60000)
        assertFalse(score == 0)
    }
}
