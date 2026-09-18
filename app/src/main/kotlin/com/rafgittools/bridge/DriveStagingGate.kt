package com.rafgittools.bridge

import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

data class DriveStagingReceipt(
    val schemaVersion: String = "1.0.0",
    val sourceKind: String = "ANDROID_SAF_DOCUMENT",
    val sourceProviderAuthority: String,
    val sourceDisplayName: String,
    val stagedFileName: String,
    val stagedBytes: Long,
    val sha256: String,
    val verifiedByReadback: Boolean,
    val recipientProvider: String = "GITHUB",
    val recipientRepository: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val recipientRef: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val recipientPath: String = "TOKEN_VAZIO_EXPLICIT_TARGET_REQUIRED",
    val promotionState: String = "STAGED_VERIFIED",
    val promotionAdapter: String = "RafGitFS",
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

/**
 * Fail-closed copy gate for an Android Storage Access Framework import.
 *
 * The receipt intentionally does not persist the raw content URI. Provider authority and the
 * display name are enough to reconstruct the local staging event without leaking a capability URI.
 * GitHub destination fields remain explicit TOKEN_VAZIO until the user binds a repository/ref/path.
 */
object DriveStagingGate {
    private val gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).buffered(64 * 1024).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read == 0) continue
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Throws(IOException::class)
    fun verifyAndWriteReceipt(
        stagedFile: File,
        sourceProviderAuthority: String?,
        sourceDisplayName: String,
        expectedBytes: Long,
        expectedSha256: String,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): File {
        if (!stagedFile.isFile) throw IOException("Staged file is missing")
        if (stagedFile.length() != expectedBytes) {
            throw IOException("Staged byte count mismatch")
        }

        val readbackSha256 = sha256(stagedFile)
        if (!readbackSha256.equals(expectedSha256, ignoreCase = true)) {
            throw IOException("Staged SHA-256 readback mismatch")
        }

        val receipt = DriveStagingReceipt(
            sourceProviderAuthority = sourceProviderAuthority
                ?.takeIf { it.isNotBlank() }
                ?: "TOKEN_VAZIO_PROVIDER_AUTHORITY",
            sourceDisplayName = sourceDisplayName,
            stagedFileName = stagedFile.name,
            stagedBytes = expectedBytes,
            sha256 = readbackSha256,
            verifiedByReadback = true,
            createdAtEpochMs = createdAtEpochMs
        )

        val parent = stagedFile.parentFile ?: throw IOException("Staging parent is missing")
        val receiptFile = File(parent, stagedFile.name + ".stage-receipt.json")
        val partFile = File(parent, receiptFile.name + ".part")

        if (partFile.exists() && !partFile.delete()) {
            throw IOException("Could not clear prior receipt staging file")
        }
        partFile.writeText(gson.toJson(receipt) + "\n", Charsets.UTF_8)

        if (receiptFile.exists() && !receiptFile.delete()) {
            partFile.delete()
            throw IOException("Could not replace prior staging receipt")
        }
        if (!partFile.renameTo(receiptFile)) {
            partFile.delete()
            throw IOException("Could not atomically promote staging receipt")
        }

        return receiptFile
    }
}
