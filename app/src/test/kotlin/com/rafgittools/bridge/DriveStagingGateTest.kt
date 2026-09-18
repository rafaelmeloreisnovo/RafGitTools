package com.rafgittools.bridge

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveStagingGateTest {
    @Test
    fun verifiedCopyProducesFailClosedRecipientReceipt() {
        val dir = Files.createTempDirectory("rafgittools-drive-gate").toFile()
        try {
            val staged = File(dir, "sample.txt")
            staged.writeText("abc")
            val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

            val receipt = DriveStagingGate.verifyAndWriteReceipt(
                stagedFile = staged,
                sourceProviderAuthority = "example.documents",
                sourceDisplayName = "sample.txt",
                expectedBytes = 3,
                expectedSha256 = expected,
                createdAtEpochMs = 1L
            )

            assertTrue(receipt.isFile)
            val body = receipt.readText()
            assertTrue(body.contains("\"verifiedByReadback\": true"))
            assertTrue(body.contains("TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED"))
            assertTrue(body.contains("\"claimAllowed\": false"))
            assertFalse(body.contains("content://"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun hashMismatchFailsBeforeReceiptPromotion() {
        val dir = Files.createTempDirectory("rafgittools-drive-gate-mismatch").toFile()
        try {
            val staged = File(dir, "sample.txt")
            staged.writeText("abc")
            var failed = false
            try {
                DriveStagingGate.verifyAndWriteReceipt(
                    stagedFile = staged,
                    sourceProviderAuthority = null,
                    sourceDisplayName = "sample.txt",
                    expectedBytes = 3,
                    expectedSha256 = "0".repeat(64),
                    createdAtEpochMs = 1L
                )
            } catch (_: IOException) {
                failed = true
            }
            assertTrue(failed)
            assertFalse(File(dir, "sample.txt.stage-receipt.json").exists())
        } finally {
            dir.deleteRecursively()
        }
    }
}
