package com.rafgittools.library.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RigorBiasEngineV1Test {
    private fun known(value: Int) = RigorChannelQ16.known(value)

    private fun complete(
        floor: LibraryRigorLevel = LibraryRigorLevel.QUICK,
        path: RmrCtiForestPath = RmrCtiForestPath.PROCESSUAL
    ) = RigorBiasInputV1(
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

    @Test
    fun sameInputsProduceSameDecision() {
        val input = complete()
        assertEquals(
            RigorBiasEngineV1.evaluate(input),
            RigorBiasEngineV1.evaluate(input)
        )
    }

    @Test
    fun urgentRaisesPriorityButDoesNotPromoteEvidence() {
        val processual = RigorBiasEngineV1.evaluate(
            complete(path = RmrCtiForestPath.PROCESSUAL)
        )
        val urgent = RigorBiasEngineV1.evaluate(
            complete(path = RmrCtiForestPath.URGENT)
        )

        assertTrue(urgent.priorityBiasQ16 > processual.priorityBiasQ16)
        assertEquals(processual.recommendedRigor, urgent.recommendedRigor)
        assertFalse(urgent.evidencePromotionAllowed)
        assertFalse(urgent.claimAllowed)
    }

    @Test
    fun contradictionRaisesRequiredRigor() {
        val low = RigorBiasEngineV1.evaluate(complete())
        val high = RigorBiasEngineV1.evaluate(
            complete().copy(contradictionPressure = known(65536))
        )
        assertTrue(rank(high.recommendedRigor) >= rank(low.recommendedRigor))
        assertTrue((high.rigorPressureQ16 ?: 0) > (low.rigorPressureQ16 ?: 0))
    }

    @Test
    fun explicitEvidenceFloorCanNeverBeDowngraded() {
        val decision = RigorBiasEngineV1.evaluate(
            complete(floor = LibraryRigorLevel.EVIDENCE)
        )
        assertEquals(LibraryRigorLevel.EVIDENCE, decision.recommendedRigor)
    }

    @Test
    fun criticalTokenVazioForcesEvidenceInspectionWithoutInventingZero() {
        val input = complete().copy(
            identityCompleteness = RigorChannelQ16.tokenVazio()
        )
        val decision = RigorBiasEngineV1.evaluate(input)
        assertEquals(LibraryRigorLevel.EVIDENCE, decision.recommendedRigor)
        assertTrue(decision.unknownHeads.contains("IDENTITY"))
        val identity = decision.contributions.first { it.head == "IDENTITY" }
        assertNull(identity.sourceValueQ16)
        assertNull(identity.pressureQ16)
    }

    @Test
    fun raiseOnlyNeverLowersExistingJobRigor() {
        val job = LibraryLocalJob(
            jobId = "JOB-RIGOR-0001",
            source = LibrarySourceRef(
                sourceId = "SRC-RIGOR-001",
                sourceKind = LibrarySourceKind.GENERATED_FIXTURE,
                opaqueLocatorHash = "0".repeat(64),
                displayName = "fixture",
                mediaType = "application/octet-stream",
                sizeBytes = 1,
                contentSha256 = null,
                modifiedTime = null
            ),
            requestedStages = listOf(LibraryJobStage.EXTRACT),
            rigor = LibraryRigorLevel.EVIDENCE,
            budget = LibraryProcessingBudget(
                maxBytes = 1024,
                maxWorkingMemoryBytes = 1024 * 1024,
                maxItems = 1,
                maxAttempts = 1,
                checkpointEveryBytes = 1024,
                minBatteryPercent = 0,
                thermalPolicy = LibraryThermalPolicy.NORMAL,
                networkPolicy = LibraryNetworkPolicy.OFFLINE_ONLY
            ),
            outputNamespace = "library/rigor",
            idempotencyKey = "IDEMPOTENCY-RIGOR-0001",
            createdAtEpochMs = 1
        )
        val decision = RigorBiasEngineV1.evaluate(complete())
        val adjusted = RigorBiasEngineV1.raiseOnly(job, decision)
        assertEquals(LibraryRigorLevel.EVIDENCE, adjusted.rigor)
    }

    @Test
    fun rmrCtiAdapterUsesMeasuredFieldsAsPressureNotTruth() {
        val base = complete()
        val snapshot = RmrCtiNeurometricSnapshotV1(
            entropyMilli = 4400,
            semanticGapMilli = 7000,
            logicDepthPerByteQ16 = 30000,
            hammingPerBitQ16 = 50000,
            technicalDensityQ16 = 20000,
            latentTokenDensityQ16 = 15000,
            crossEntropyMilli = 6500,
            noiseFloorMilli = 800,
            forestPath = RmrCtiForestPath.MENOSPREZADO,
            curatedTop = true
        )
        val adapted = RmrCtiRigorAdapterV1.applyMeasuredPressure(base, snapshot)
        val decision = RigorBiasEngineV1.evaluate(adapted)

        assertEquals(RmrCtiForestPath.MENOSPREZADO, adapted.forestPath)
        assertTrue(decision.priorityBiasQ16 > 32768)
        assertFalse(decision.evidencePromotionAllowed)
    }

    private fun rank(value: LibraryRigorLevel): Int = when (value) {
        LibraryRigorLevel.QUICK -> 0
        LibraryRigorLevel.STRUCTURAL -> 1
        LibraryRigorLevel.MULTIMODAL -> 2
        LibraryRigorLevel.EVIDENCE -> 3
    }
}
