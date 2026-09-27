package com.rafgittools.library.processing

import com.rafgittools.offline.OfflineQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RigorBiasedLibraryQueueV1Test {
    private fun known(value: Int) = RigorChannelQ16.known(value)

    private fun bias(path: RmrCtiForestPath, floor: LibraryRigorLevel) =
        RigorBiasInputV1(
            identityCompleteness = known(65536),
            provenanceCompleteness = known(65536),
            structureCompleteness = known(65536),
            noveltyGapPressure = known(0),
            relationSupport = known(65536),
            multimodalCompleteness = known(65536),
            contradictionPressure = known(0),
            reproducibilityCompleteness = known(65536),
            freshnessDriftPressure = known(0),
            forestPath = path,
            requestedFloor = floor
        )

    private fun job(id: String, rigor: LibraryRigorLevel) = LibraryLocalJob(
        jobId = id,
        source = LibrarySourceRef(
            sourceId = "SRC-" + id,
            sourceKind = LibrarySourceKind.GENERATED_FIXTURE,
            opaqueLocatorHash = "0".repeat(64),
            displayName = id,
            mediaType = "text/plain",
            sizeBytes = 10,
            contentSha256 = null,
            modifiedTime = null
        ),
        requestedStages = listOf(LibraryJobStage.EXTRACT),
        rigor = rigor,
        budget = LibraryProcessingBudget(
            maxBytes = 1024,
            maxWorkingMemoryBytes = 1024 * 1024,
            maxItems = 1,
            maxAttempts = 1,
            checkpointEveryBytes = 256,
            minBatteryPercent = 0,
            thermalPolicy = LibraryThermalPolicy.NORMAL,
            networkPolicy = LibraryNetworkPolicy.OFFLINE_ONLY
        ),
        outputNamespace = "library/queue",
        idempotencyKey = "IDEMPOTENCY-" + id,
        createdAtEpochMs = 1
    )

    @Test
    fun urgentJobRunsBeforeEarlierProcessualJob() {
        val raw = OfflineQueue<RigorBiasedQueuedJobV1>()
        val queue = RigorBiasedLibraryQueueV1(raw)

        queue.enqueue(
            RigorBiasedQueuedJobV1(
                job("JOB-PROCESSUAL-001", LibraryRigorLevel.QUICK),
                bias(RmrCtiForestPath.PROCESSUAL, LibraryRigorLevel.QUICK)
            )
        )
        queue.enqueue(
            RigorBiasedQueuedJobV1(
                job("JOB-URGENT-0001", LibraryRigorLevel.QUICK),
                bias(RmrCtiForestPath.URGENT, LibraryRigorLevel.QUICK)
            )
        )

        val selected = queue.dequeueBest()!!
        assertEquals("JOB-URGENT-0001", selected.original.job.jobId)
        assertFalse(selected.decision.evidencePromotionAllowed)
    }

    @Test
    fun biasNeverLowersOriginalRigor() {
        val raw = OfflineQueue<RigorBiasedQueuedJobV1>()
        val queue = RigorBiasedLibraryQueueV1(raw)

        queue.enqueue(
            RigorBiasedQueuedJobV1(
                job("JOB-EVIDENCE-001", LibraryRigorLevel.EVIDENCE),
                bias(RmrCtiForestPath.PROCESSUAL, LibraryRigorLevel.EVIDENCE)
            )
        )

        val selected = queue.dequeueBest()!!
        assertEquals(LibraryRigorLevel.EVIDENCE, selected.effectiveJob.rigor)
    }

    @Test
    fun provenanceEnvelopeIsDeterministicAndKeepsTokenVazioSource() {
        val x = bias(
            RmrCtiForestPath.MENOSPREZADO,
            LibraryRigorLevel.QUICK
        ).copy(
            identityCompleteness = RigorChannelQ16.tokenVazio()
        )

        val sourceMap = mapOf(
            "IDENTITY" to listOf("SOURCE_IDENTITY_SCAN"),
            "NOVELTY_GAP" to listOf("RMR_NEUROMETRICS")
        )

        val a = RigorBiasDecisionEnvelopeFactoryV1.evaluate(
            input = x,
            signalProvenance = sourceMap,
            forestProvenanceIds = listOf("RMR_FOREST"),
            sourceRegistryIds = listOf("RAFGITTOOLS_RIGOR_BIAS_CROSSREPO_V1")
        )
        val b = RigorBiasDecisionEnvelopeFactoryV1.evaluate(
            input = x,
            signalProvenance = sourceMap,
            forestProvenanceIds = listOf("RMR_FOREST"),
            sourceRegistryIds = listOf("RAFGITTOOLS_RIGOR_BIAS_CROSSREPO_V1")
        )

        assertEquals(a, b)
        assertEquals(64, a.inputSha256.length)
        assertEquals(
            RigorChannelState.TOKEN_VAZIO,
            x.identityCompleteness.state
        )
        assertFalse(a.claimAllowed)
    }
}
