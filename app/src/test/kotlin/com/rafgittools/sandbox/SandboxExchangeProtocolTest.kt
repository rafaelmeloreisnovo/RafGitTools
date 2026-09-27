package com.rafgittools.sandbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandboxExchangeProtocolTest {

    private fun fixture(
        purpose: String = "Synthetic sandbox fixture",
        containsSecret: Boolean = false,
        testPlanId: String = "VECTRA_ARMV7_FREESTANDING_KAT_V1"
    ) = SandboxExchangeEnvelope(
        exchangeId = "SBX-SYNTHETIC-0001",
        sourceRepository = "owner/repo",
        sourceCommit = "0123456789abcdef0123456789abcdef01234567",
        custodyTargetId = "private-custody-v1",
        targetRoute = SandboxRoute.VECTRA_SANDBOX,
        testPlanId = testPlanId,
        authorizationReceiptId = "AUTH-SYNTHETIC-0001",
        classification = SandboxClassification.P2_PRIVATE_TEST_FIXTURE,
        purpose = purpose,
        retentionRule = "TEST_30D",
        artifacts = listOf(
            SandboxArtifactDescriptor(
                artifactId = "fixture-0001",
                artifactClass = SandboxArtifactClass.TEST_FIXTURE,
                opaqueRef = "opaque:fixture/0001",
                sha256 = "0".repeat(64),
                sizeBytes = 128,
                mediaType = "application/octet-stream",
                containsSecretMaterial = containsSecret
            )
        ),
        expectedRunnerCommit = "89abcdef0123456789abcdef0123456789abcdef",
        previousReceiptHash = "GENESIS",
        rollbackRef = "owner/repo@0123456789abcdef0123456789abcdef01234567"
    )

    @Test
    fun validEnvelopePasses() {
        val result = SandboxExchangeProtocol.validate(fixture())
        assertTrue(result.errors.toString(), result.allowed)
        assertTrue(SandboxExchangePlanner.planAllowed(fixture()))
    }

    @Test
    fun secretMaterialFailsClosed() {
        val result = SandboxExchangeProtocol.validate(fixture(containsSecret = true))
        assertFalse(result.allowed)
        assertTrue(result.errors.contains("SECRET_MATERIAL_BLOCKED"))
    }

    @Test
    fun credentialMarkerFailsClosed() {
        val result = SandboxExchangeProtocol.validate(
            fixture(purpose = "authorization: bearer hidden")
        )
        assertFalse(result.allowed)
        assertTrue(result.errors.contains("CREDENTIAL_MARKER_BLOCKED"))
    }

    @Test
    fun arbitraryPlanIsNotExecutable() {
        val envelope = fixture(testPlanId = "rm_-rf")
        assertTrue(SandboxExchangeProtocol.validate(envelope).allowed)
        assertFalse(SandboxExchangePlanner.planAllowed(envelope))
    }

    @Test
    fun canonicalHashIsStableAgainstArtifactOrdering() {
        val a = fixture()
        val second = a.artifacts.first().copy(
            artifactId = "fixture-0002",
            opaqueRef = "opaque:fixture/0002",
            sha256 = "1".repeat(64)
        )
        val forward = a.copy(artifacts = listOf(a.artifacts.first(), second))
        val reverse = a.copy(artifacts = listOf(second, a.artifacts.first()))
        assertEquals(
            SandboxExchangeProtocol.envelopeSha256(forward),
            SandboxExchangeProtocol.envelopeSha256(reverse)
        )
    }

    @Test
    fun observationCredentialMarkerFailsClosed() {
        val result = SandboxExchangeProtocol.validateObservation(
            outcome = "PASS",
            observedRunnerCommit = "89abcdef0123456789abcdef0123456789abcdef",
            gaps = listOf("authorization: bearer hidden")
        )
        assertFalse(result.allowed)
        assertTrue(result.errors.contains("OBSERVATION_CREDENTIAL_MARKER_BLOCKED"))
    }

    @Test
    fun observationRejectsMalformedRunnerCommit() {
        val result = SandboxExchangeProtocol.validateObservation(
            outcome = "PASS",
            observedRunnerCommit = "not-a-commit",
            gaps = emptyList()
        )
        assertFalse(result.allowed)
        assertTrue(result.errors.contains("INVALID_OBSERVED_RUNNER_COMMIT"))
    }

    @Test
    fun receiptHashBindsOutcomeAndRunner() {
        val envelope = fixture()
        val one = SandboxExchangeProtocol.receiptHash(
            envelope,
            "PASS",
            envelope.expectedRunnerCommit
        )
        val two = SandboxExchangeProtocol.receiptHash(
            envelope,
            "FAIL",
            envelope.expectedRunnerCommit
        )
        assertNotEquals(one, two)
    }
}
