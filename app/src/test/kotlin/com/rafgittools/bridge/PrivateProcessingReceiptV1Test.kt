package com.rafgittools.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateProcessingReceiptV1Test {
    private fun intake(): CorpusIntakeResult = CorpusIntakeResult(
        manifestFile = java.io.File("/tmp/private-manifest.json"),
        publicProjectionFile = java.io.File("/tmp/public-risk.json"),
        intakeId = "INTAKE-" + "a".repeat(24),
        publicRiskHandle = "RISK-test",
        contentKind = "JSON",
        structuralVector = JsonStructuralVector(
            objects = 3,
            arrays = 1,
            names = 9,
            strings = 4,
            numbers = 2,
            booleans = 1,
            nulls = 0,
            maxDepth = 4
        )
    )

    @Test
    fun receipt_is_private_activity_only_and_never_claim_promoted() {
        val receipt = PrivateProcessingReceiptGateV1.build(
            sourceProviderAuthority = "com.google.android.apps.docs.storage",
            sourceSha256 = "1".repeat(64),
            bytesRead = 1024,
            intake = intake(),
            destinationRepositoryFullName = "owner/private-memory",
            androidSdk = 29,
            androidAbis = listOf("armeabi-v7a"),
            createdAtEpochMs = 1_790_000_000_000L
        )

        assertFalse(receipt.rawPayloadUploaded)
        assertFalse(receipt.claimAllowed)
        assertEquals("FULL_SOURCE", receipt.sourceScope)
        assertEquals("PRIVATE_ACTIVITY_RECEIPT_ONLY", receipt.outputMode)
        assertTrue(receipt.gaps.contains("DEVICE_APK_SHA256_NOT_BOUND"))
        assertTrue(receipt.receiptSha256.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun destination_path_is_namespace_constrained() {
        val receipt = PrivateProcessingReceiptGateV1.build(
            sourceProviderAuthority = "drive-provider",
            sourceSha256 = "2".repeat(64),
            bytesRead = 7,
            intake = intake(),
            destinationRepositoryFullName = "owner/private-memory",
            androidSdk = 29,
            androidAbis = listOf("armeabi-v7a"),
            createdAtEpochMs = 1L
        )
        assertTrue(
            PrivateProcessingReceiptGateV1.destinationPath(receipt)
                .startsWith("memory_bridge/private_processing/receipts/")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_source_hash_fails_closed() {
        PrivateProcessingReceiptGateV1.build(
            sourceProviderAuthority = "drive-provider",
            sourceSha256 = "not-a-sha",
            bytesRead = 1,
            intake = intake(),
            destinationRepositoryFullName = "owner/private-memory",
            androidSdk = 29,
            androidAbis = listOf("armeabi-v7a"),
            createdAtEpochMs = 1L
        )
    }
}
