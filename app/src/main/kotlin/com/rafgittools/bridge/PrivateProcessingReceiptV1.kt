package com.rafgittools.bridge

import com.google.gson.GsonBuilder
import java.security.MessageDigest

data class PrivateProcessingActivityReceiptV1(
    val schema: String = "rafgittools.private-processing-activity/v1",
    val operationId: String,
    val sourceProviderAuthority: String,
    val sourceSha256: String,
    val sourceScope: String = "FULL_SOURCE",
    val bytesRead: Long,
    val intakeId: String,
    val contentKind: String,
    val structuralVector: JsonStructuralVector?,
    val processorSource: String = "RafGitTools/CorpusIntakeGate",
    val processorArtifactSha256: String = "TOKEN_VAZIO_DEVICE_APK_HASH_NOT_BOUND",
    val androidSdk: Int,
    val androidAbis: List<String>,
    val destinationRepositorySha256: String,
    val outputMode: String = "PRIVATE_ACTIVITY_RECEIPT_ONLY",
    val rawPayloadUploaded: Boolean = false,
    val publicationState: String = "PENDING_PRIVATE_GITHUB_WRITE",
    val gaps: List<String>,
    val receiptSha256: String,
    val claimAllowed: Boolean = false,
    val createdAtEpochMs: Long
)

object PrivateProcessingReceiptGateV1 {
    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val sha256Pattern = Regex("^[0-9a-f]{64}$")

    fun build(
        sourceProviderAuthority: String,
        sourceSha256: String,
        bytesRead: Long,
        intake: CorpusIntakeResult,
        destinationRepositoryFullName: String,
        androidSdk: Int,
        androidAbis: List<String>,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ): PrivateProcessingActivityReceiptV1 {
        require(sha256Pattern.matches(sourceSha256.lowercase())) { "source SHA-256 invalid" }
        require(bytesRead >= 0L) { "bytesRead must be non-negative" }
        require(sourceProviderAuthority.isNotBlank()) { "provider authority missing" }
        require(destinationRepositoryFullName.contains('/')) { "destination repository identity invalid" }

        val destinationHash = sha256(destinationRepositoryFullName)
        val operationSeed = listOf(
            sourceSha256.lowercase(),
            intake.intakeId,
            destinationHash,
            createdAtEpochMs.toString()
        ).joinToString("|")
        val operationId = "PP-" + sha256(operationSeed).take(24)

        val gaps = buildList {
            add("DEVICE_APK_SHA256_NOT_BOUND")
            add("SECRET_AUTOMATION_LANE_NOT_REQUIRED_FOR_EXPLICIT_DEVICE_WRITE")
            if (intake.structuralVector == null) add("STRUCTURAL_VECTOR_NOT_APPLICABLE_OR_NOT_RUN")
        }.distinct().sorted()

        val canonical = buildString {
            append("private-processing-activity-v1|")
            append(operationId).append('|')
            append(sourceProviderAuthority).append('|')
            append(sourceSha256.lowercase()).append('|')
            append(bytesRead).append('|')
            append(intake.intakeId).append('|')
            append(intake.contentKind).append('|')
            append(intake.structuralVector?.toString().orEmpty()).append('|')
            append(androidSdk).append('|')
            append(androidAbis.sorted()).append('|')
            append(destinationHash).append('|')
            append(gaps).append('|')
            append("raw_payload_uploaded=false|claim_allowed=false|")
            append(createdAtEpochMs)
        }

        return PrivateProcessingActivityReceiptV1(
            operationId = operationId,
            sourceProviderAuthority = sourceProviderAuthority,
            sourceSha256 = sourceSha256.lowercase(),
            bytesRead = bytesRead,
            intakeId = intake.intakeId,
            contentKind = intake.contentKind,
            structuralVector = intake.structuralVector,
            androidSdk = androidSdk,
            androidAbis = androidAbis.distinct().sorted(),
            destinationRepositorySha256 = destinationHash,
            gaps = gaps,
            receiptSha256 = sha256(canonical),
            createdAtEpochMs = createdAtEpochMs
        ).also(::validate)
    }

    fun validate(receipt: PrivateProcessingActivityReceiptV1) {
        require(receipt.schema == "rafgittools.private-processing-activity/v1")
        require(receipt.operationId.startsWith("PP-"))
        require(sha256Pattern.matches(receipt.sourceSha256))
        require(sha256Pattern.matches(receipt.destinationRepositorySha256))
        require(sha256Pattern.matches(receipt.receiptSha256))
        require(receipt.bytesRead >= 0L)
        require(receipt.sourceScope == "FULL_SOURCE")
        require(receipt.outputMode == "PRIVATE_ACTIVITY_RECEIPT_ONLY")
        require(!receipt.rawPayloadUploaded) { "raw corpus publication is forbidden" }
        require(!receipt.claimAllowed) { "processing receipt cannot promote a claim" }
    }

    fun toJson(receipt: PrivateProcessingActivityReceiptV1): String {
        validate(receipt)
        return gson.toJson(receipt) + "\n"
    }

    fun destinationPath(receipt: PrivateProcessingActivityReceiptV1): String {
        validate(receipt)
        return "memory_bridge/private_processing/receipts/" +
            receipt.operationId + ".activity.v1.json"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
