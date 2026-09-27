package com.rafgittools.library.processing

import com.rafgittools.library.LibraryCatalogGate
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RmrCtiFullMemberStreamV1Test {
    private val payload = (
        "alpha beta gamma\n" +
            "alpha delta 123\n" +
            "rmrcti bounded stream\n"
        ).toByteArray(Charsets.UTF_8)

    private fun crc32(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return java.lang.Long.toHexString(crc.value)
            .padStart(8, '0')
            .takeLast(8)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun pack() = RmrCtiMobileContentPackV1(
        schema = "rmrcti.content-pack/v1",
        packId = "member-001",
        sourceArchive = "/private/NOVOexport.zip",
        sourceMember = "rmrCti/sample.txt",
        sourceMemberCrc32 = crc32(payload),
        sourceMemberUncompressedBytes = payload.size.toLong(),
        category = "document_text",
        lifecycleRole = "AUTHORIAL_WORK",
        terms = listOf("alpha", "rmrcti"),
        sampleText = "alpha...stream",
        sampleScope = "bounded_head_and_tail_not_full_member",
        epistemicState = "OBSERVED_SAMPLE",
        claimAllowed = false
    )

    private fun source() = RepeatableLibraryStreamSourceV1 {
        ByteArrayInputStream(payload)
    }

    private fun biasEnvelope(): RigorBiasDecisionEnvelopeV1 {
        val known = RigorChannelQ16.known(0)
        val input = RigorBiasInputV1(
            identityCompleteness = known,
            provenanceCompleteness = known,
            structureCompleteness = known,
            noveltyGapPressure = RigorChannelQ16.known(65536),
            relationSupport = known,
            multimodalCompleteness = known,
            contradictionPressure = RigorChannelQ16.known(65536),
            reproducibilityCompleteness = known,
            freshnessDriftPressure = RigorChannelQ16.known(32768),
            forestPath = RmrCtiForestPath.URGENT,
            curatedTop = true,
            requestedFloor = LibraryRigorLevel.QUICK
        )
        val provenance = mapOf(
            "IDENTITY" to listOf("test:identity"),
            "PROVENANCE" to listOf("test:provenance"),
            "STRUCTURE" to listOf("test:structure"),
            "NOVELTY_GAP" to listOf("test:novelty"),
            "RELATION" to listOf("test:relation"),
            "MULTIMODAL" to listOf("test:multimodal"),
            "CONTRADICTION" to listOf("test:contradiction"),
            "REPRODUCIBILITY" to listOf("test:reproducibility"),
            "FRESHNESS" to listOf("test:freshness")
        )
        return RigorBiasDecisionEnvelopeFactoryV1.evaluate(
            input = input,
            signalProvenance = provenance,
            forestProvenanceIds = listOf("test:forest"),
            sourceRegistryIds = listOf("test:registry")
        )
    }

    @Test
    fun streamingByteVectorMatchesInMemoryVector() {
        val analysis = LibraryStreamingDescriptorEngineV1.analyze(
            source = source(),
            expectedBytes = payload.size.toLong(),
            expectedCrc32 = crc32(payload),
            maxOutputBytes = payload.size.toLong(),
            chunkBytes = 4096,
            includeTextVector = true
        )

        assertEquals(
            LocalDescriptorEngine.byteVector(payload, includeSha256 = true),
            analysis.byteVector
        )
        assertEquals(
            LocalDescriptorEngine.textVector(payload.toString(Charsets.UTF_8)),
            analysis.textVector
        )
        assertEquals(sha256(payload), analysis.sha256)
        assertEquals(crc32(payload), analysis.crc32)
        assertTrue(analysis.gaps.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun crcMismatchFailsClosed() {
        LibraryStreamingDescriptorEngineV1.analyze(
            source = source(),
            expectedBytes = payload.size.toLong(),
            expectedCrc32 = "00000000",
            maxOutputBytes = payload.size.toLong(),
            chunkBytes = 4096,
            includeTextVector = false
        )
    }

    @Test
    fun fullMemberPromotesScopeAndBuildsVerifiedCatalog() {
        val p = pack()
        val bridged = RmrCtiMobilePackBridgeV1.bridge(
            pack = p,
            createdAtEpochMs = 1L
        )
        val plan = RmrCtiFullMemberStreamV1.plan(
            pack = p,
            bridged = bridged,
            targetRigor = LibraryRigorLevel.STRUCTURAL,
            promotionTrigger = RmrCtiFullMemberPromotionTriggerV1.RIGOR_BIAS,
            biasEnvelope = biasEnvelope()
        )

        val result = RmrCtiFullMemberStreamV1.execute(
            pack = p,
            bridged = bridged,
            plan = plan,
            streamSource = source(),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8L * 1024L * 1024L,
                batteryPercent = 80,
                nowEpochMs = 2L
            ),
            biasEnvelope = biasEnvelope()
        )

        assertEquals(LibraryJobState.SUCCEEDED, result.execution.receipt.finalState)
        assertEquals(sha256(payload), result.execution.descriptors?.byteVector?.sha256)
        assertEquals(sha256(payload), result.receipt.contentSha256)
        assertEquals(
            RmrCtiFullMemberPromotionTriggerV1.RIGOR_BIAS,
            result.receipt.promotionTrigger
        )
        assertNotNull(result.receipt.biasInputSha256)
        assertNotNull(result.receipt.biasEnvelopeSha256)
        assertNotNull(result.catalog)
        val gate = LibraryCatalogGate.validate(requireNotNull(result.catalog))
        assertTrue(gate.errors.toString(), gate.allowed)
        assertFalse(result.receipt.claimAllowed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun tamperedBiasPlanCannotExecuteWithoutOriginalEnvelope() {
        val p = pack()
        val bridged = RmrCtiMobilePackBridgeV1.bridge(p, 1L)
        val plan = RmrCtiFullMemberStreamV1.plan(
            pack = p,
            bridged = bridged,
            targetRigor = LibraryRigorLevel.STRUCTURAL,
            promotionTrigger = RmrCtiFullMemberPromotionTriggerV1.RIGOR_BIAS,
            biasEnvelope = biasEnvelope()
        ).copy(biasEnvelopeSha256 = "0".repeat(64))

        RmrCtiFullMemberStreamV1.execute(
            pack = p,
            bridged = bridged,
            plan = plan,
            streamSource = source(),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = 8L * 1024L * 1024L,
                batteryPercent = 80,
                nowEpochMs = 2L
            ),
            biasEnvelope = biasEnvelope()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun automaticEvidencePromotionIsRejected() {
        val p = pack()
        val bridged = RmrCtiMobilePackBridgeV1.bridge(p, 1L)
        RmrCtiFullMemberStreamV1.plan(
            pack = p,
            bridged = bridged,
            targetRigor = LibraryRigorLevel.EVIDENCE,
            promotionTrigger = RmrCtiFullMemberPromotionTriggerV1.EXPLICIT_HUMAN_REQUEST,
            explicitRequestId = "human-test-request"
        )
    }

    @Test
    fun pythonRouteUsesArgvWithoutShell() {
        val src = RmrCtiPythonStreamSourceV1(
            pythonExecutable = "python3",
            producerScriptPath = "/private/rmrcti_dataset_mobile.py",
            sourceArchivePath = "/private/NOVOexport.zip",
            member = "rmrCti/sample.txt",
            maxOutputBytes = 3L * 1024L * 1024L
        )
        val argv = src.command()
        assertEquals("python3", argv.first())
        assertEquals("stream", argv[2])
        assertTrue(argv.contains("--member"))
        assertTrue(argv.contains("--max-output-mib"))
        assertFalse(argv.any { it.contains("sh -c") })
    }
}
