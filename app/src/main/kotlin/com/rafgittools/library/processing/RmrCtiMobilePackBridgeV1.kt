package com.rafgittools.library.processing

import java.security.MessageDigest

data class RmrCtiMobileContentPackV1(
    val schema: String,
    val packId: String,
    val sourceArchive: String,
    val sourceMember: String,
    val sourceMemberCrc32: String,
    val sourceMemberUncompressedBytes: Long,
    val category: String,
    val lifecycleRole: String,
    val terms: List<String>,
    val sampleText: String,
    val sampleScope: String,
    val epistemicState: String,
    val claimAllowed: Boolean
)

data class RmrCtiMobilePackBridgeResultV1(
    val job: LibraryLocalJob,
    val input: LibraryJobInput,
    val sourceArchiveLocatorSha256: String,
    val requiresFullMemberForDeeperRigor: Boolean,
    val producerProvenanceIds: List<String>,
    val claimAllowed: Boolean = false
)

/**
 * Bridge for rmrcti.content-pack/v1.
 *
 * The pack contains a bounded textual sample, not the complete ZIP member.
 * V1 therefore emits a QUICK job with BOUNDED_SAMPLE scope and never assigns
 * contentSha256 to the source member.
 */
object RmrCtiMobilePackBridgeV1 {
    const val PRODUCER_BLOB =
        "f8e6687ef1994f64db8bcb5ce765f8cd21c842f1"
    const val PRODUCER_PATH = "rmrCti/rmrcti_dataset_mobile.py"

    private const val EXPECTED_SCHEMA = "rmrcti.content-pack/v1"
    private const val EXPECTED_SCOPE = "bounded_head_and_tail_not_full_member"
    private const val EXPECTED_STATE = "OBSERVED_SAMPLE"

    fun bridge(
        pack: RmrCtiMobileContentPackV1,
        createdAtEpochMs: Long,
        maxWorkingMemoryBytes: Long = 4L * 1024L * 1024L
    ): RmrCtiMobilePackBridgeResultV1 {
        validate(pack)

        val archiveHash = sha256(pack.sourceArchive)
        val memberIdentity = sha256(
            archiveHash + "|" +
                pack.sourceMember + "|" +
                pack.sourceMemberCrc32 + "|" +
                pack.sourceMemberUncompressedBytes
        )
        val sampleBytes = pack.sampleText.toByteArray(Charsets.UTF_8)
        val maxBytes = sampleBytes.size.toLong().coerceAtLeast(1L)

        val source = LibrarySourceRef(
            sourceId = "RMRPACK-" + memberIdentity.take(24),
            sourceKind = LibrarySourceKind.ZIP_ENTRY,
            opaqueLocatorHash = memberIdentity,
            displayName = pack.sourceMember,
            mediaType = "text/plain",
            sizeBytes = pack.sourceMemberUncompressedBytes,
            contentSha256 = null,
            modifiedTime = null,
            contentScope = LibraryContentScope.BOUNDED_SAMPLE,
            readOnly = true
        )

        val job = LibraryLocalJob(
            jobId = "JOB-RMRPACK-" + memberIdentity.take(20),
            source = source,
            requestedStages = listOf(
                LibraryJobStage.DISCOVER,
                LibraryJobStage.INGEST,
                LibraryJobStage.EXTRACT,
                LibraryJobStage.VECTORIZE
            ),
            rigor = LibraryRigorLevel.QUICK,
            budget = LibraryProcessingBudget(
                maxBytes = maxBytes,
                maxWorkingMemoryBytes = maxWorkingMemoryBytes.coerceAtLeast(1024L * 1024L),
                maxItems = 1,
                maxAttempts = 3,
                checkpointEveryBytes = maxBytes,
                minBatteryPercent = 10,
                thermalPolicy = LibraryThermalPolicy.NORMAL,
                networkPolicy = LibraryNetworkPolicy.OFFLINE_ONLY
            ),
            outputNamespace = "library/rmrcti/mobile-pack/" + memberIdentity.take(16),
            idempotencyKey = "RMRPACK-" + memberIdentity,
            createdAtEpochMs = createdAtEpochMs,
            claimAllowed = false
        )

        return RmrCtiMobilePackBridgeResultV1(
            job = job,
            input = LibraryJobInput(
                bytes = sampleBytes,
                text = pack.sampleText
            ),
            sourceArchiveLocatorSha256 = archiveHash,
            requiresFullMemberForDeeperRigor = true,
            producerProvenanceIds = listOf(
                "github:rafaelmeloreisnovo/llamaRafaelia:" +
                    PRODUCER_PATH + "@" + PRODUCER_BLOB,
                "rmrcti-pack:" + pack.packId,
                "rmrcti-member-crc32:" + pack.sourceMemberCrc32
            ),
            claimAllowed = false
        )
    }

    private fun validate(pack: RmrCtiMobileContentPackV1) {
        require(pack.schema == EXPECTED_SCHEMA) { "unexpected RMRCTI pack schema" }
        require(!pack.claimAllowed) { "claim-allowed RMRCTI pack rejected" }
        require(pack.sampleScope == EXPECTED_SCOPE) { "unexpected sample scope" }
        require(pack.epistemicState == EXPECTED_STATE) {
            "unexpected epistemic state"
        }
        require(pack.packId.isNotBlank()) { "pack_id required" }
        require(pack.sourceArchive.isNotBlank()) { "source archive required" }
        require(pack.sourceMember.isNotBlank()) { "source member required" }
        require(pack.sourceMemberUncompressedBytes >= 0L) {
            "source member size invalid"
        }
        require(pack.sourceMemberCrc32.isNotBlank()) { "source member crc32 required" }
        require(pack.lifecycleRole !in setOf("VENDOR_CACHE", "SENSITIVE_REVIEW")) {
            "disallowed lifecycle role"
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
