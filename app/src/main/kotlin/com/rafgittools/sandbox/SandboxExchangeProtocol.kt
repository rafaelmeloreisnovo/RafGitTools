package com.rafgittools.sandbox

import java.security.MessageDigest

/**
 * Metadata-only exchange protocol for private sandbox work.
 *
 * Raw payload bytes, credentials and private keys do not belong in this object.
 * The protocol carries opaque references + digests so custody and execution
 * remain separate.
 */
enum class SandboxRoute {
    VECTRA_SANDBOX,
    PCR_SANDBOX,
    RAFPOLIMATA_HANDOFF,
    PRIVATE_CUSTODY_ONLY
}

enum class SandboxArtifactClass {
    SOURCE_PATCH,
    TEST_FIXTURE,
    BUILD_ARTIFACT,
    SANITIZED_LOG,
    RECEIPT,
    MANIFEST,
    BENCHMARK_RESULT
}

enum class SandboxClassification {
    P0_PUBLIC_SAFE,
    P1_INTERNAL,
    P2_PRIVATE_TEST_FIXTURE,
    P3_RESTRICTED_METADATA
}

enum class SandboxEvidenceState {
    PLANNED,
    STAGED,
    EXECUTED,
    OBSERVED,
    TOKEN_VAZIO,
    REJECTED,
    QUARANTINED
}

data class SandboxArtifactDescriptor(
    val artifactId: String,
    val artifactClass: SandboxArtifactClass,
    val opaqueRef: String,
    val sha256: String,
    val sizeBytes: Long,
    val mediaType: String,
    val containsSecretMaterial: Boolean = false
)

data class SandboxExchangeEnvelope(
    val schema: String = SCHEMA,
    val exchangeId: String,
    val sourceRepository: String,
    val sourceCommit: String,
    val custodyTargetId: String,
    val targetRoute: SandboxRoute,
    val testPlanId: String,
    val authorizationReceiptId: String,
    val classification: SandboxClassification,
    val purpose: String,
    val retentionRule: String,
    val artifacts: List<SandboxArtifactDescriptor>,
    val expectedRunnerCommit: String?,
    val previousReceiptHash: String,
    val rollbackRef: String,
    val evidenceState: SandboxEvidenceState = SandboxEvidenceState.PLANNED,
    val claimAllowed: Boolean = false
) {
    companion object {
        const val SCHEMA = "rafgittools.sandbox-exchange.v1"
    }
}

data class SandboxValidationResult(
    val allowed: Boolean,
    val errors: List<String>
)

object SandboxExchangeProtocol {
    private val hex40 = Regex("^[0-9a-f]{40}$")
    private val hex64 = Regex("^[0-9a-f]{64}$")
    private val repoName = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
    private val opaqueRef = Regex("^opaque:[A-Za-z0-9._:/-]{4,240}$")
    private val safeId = Regex("^[A-Za-z0-9._:-]{4,160}$")

    private val credentialMarkers = listOf(
        "ghp_",
        "github_pat_",
        "-----begin private key-----",
        "-----begin openssh private key-----",
        "authorization: bearer ",
        "password=",
        "senha=",
        "seed phrase",
        "mnemonic="
    )

    fun validate(envelope: SandboxExchangeEnvelope): SandboxValidationResult {
        val errors = mutableListOf<String>()

        if (envelope.schema != SandboxExchangeEnvelope.SCHEMA) errors += "SCHEMA_MISMATCH"
        if (!envelope.exchangeId.startsWith("SBX-") || !safeId.matches(envelope.exchangeId)) {
            errors += "INVALID_EXCHANGE_ID"
        }
        if (!repoName.matches(envelope.sourceRepository)) errors += "INVALID_SOURCE_REPOSITORY"
        if (!hex40.matches(envelope.sourceCommit)) errors += "SOURCE_COMMIT_REQUIRED"
        if (envelope.expectedRunnerCommit != null &&
            !hex40.matches(envelope.expectedRunnerCommit)
        ) {
            errors += "INVALID_EXPECTED_RUNNER_COMMIT"
        }
        if (!safeId.matches(envelope.testPlanId)) errors += "INVALID_TEST_PLAN_ID"
        if (!safeId.matches(envelope.authorizationReceiptId)) {
            errors += "AUTHORIZATION_RECEIPT_REQUIRED"
        }
        if (envelope.custodyTargetId.isBlank()) errors += "CUSTODY_TARGET_REQUIRED"
        if (envelope.purpose.length !in 4..512) errors += "INVALID_PURPOSE"
        if (envelope.retentionRule.length !in 2..96) errors += "INVALID_RETENTION"
        if (envelope.artifacts.isEmpty()) errors += "ARTIFACT_REQUIRED"
        if (envelope.artifacts.size > 256) errors += "TOO_MANY_ARTIFACTS"
        if (envelope.rollbackRef.length !in 4..240) errors += "ROLLBACK_REF_REQUIRED"
        if (envelope.claimAllowed) errors += "CLAIM_PROMOTION_BLOCKED"

        if (envelope.previousReceiptHash != "GENESIS" &&
            !hex64.matches(envelope.previousReceiptHash)
        ) {
            errors += "INVALID_PREVIOUS_RECEIPT_HASH"
        }

        envelope.artifacts.forEach { artifact ->
            if (!safeId.matches(artifact.artifactId)) errors += "INVALID_ARTIFACT_ID"
            if (!opaqueRef.matches(artifact.opaqueRef)) errors += "OPAQUE_REF_REQUIRED"
            if (!hex64.matches(artifact.sha256)) errors += "ARTIFACT_SHA256_REQUIRED"
            if (artifact.sizeBytes < 0L || artifact.sizeBytes > MAX_ARTIFACT_BYTES) {
                errors += "ARTIFACT_SIZE_OUT_OF_RANGE"
            }
            if (artifact.mediaType.length !in 3..128) errors += "INVALID_MEDIA_TYPE"
            if (artifact.containsSecretMaterial) errors += "SECRET_MATERIAL_BLOCKED"
        }

        val stringsToScan = buildList {
            add(envelope.exchangeId)
            add(envelope.sourceRepository)
            add(envelope.custodyTargetId)
            add(envelope.testPlanId)
            add(envelope.authorizationReceiptId)
            add(envelope.purpose)
            add(envelope.retentionRule)
            add(envelope.rollbackRef)
            envelope.artifacts.forEach {
                add(it.artifactId)
                add(it.opaqueRef)
                add(it.mediaType)
            }
        }
        if (stringsToScan.any(::looksLikeCredential)) errors += "CREDENTIAL_MARKER_BLOCKED"

        return SandboxValidationResult(
            allowed = errors.isEmpty(),
            errors = errors.distinct()
        )
    }

    fun validateObservation(
        outcome: String,
        observedRunnerCommit: String?,
        gaps: List<String>
    ): SandboxValidationResult {
        val errors = mutableListOf<String>()
        if (outcome.isBlank() || outcome.length > 128) errors += "INVALID_OUTCOME"
        if (observedRunnerCommit != null && !hex40.matches(observedRunnerCommit)) {
            errors += "INVALID_OBSERVED_RUNNER_COMMIT"
        }
        if (gaps.size > 128) errors += "TOO_MANY_GAPS"
        if (gaps.any { it.length > 1024 }) errors += "GAP_TOO_LARGE"
        if (looksLikeCredential(outcome) || gaps.any(::looksLikeCredential)) {
            errors += "OBSERVATION_CREDENTIAL_MARKER_BLOCKED"
        }
        return SandboxValidationResult(
            allowed = errors.isEmpty(),
            errors = errors.distinct()
        )
    }

    fun envelopeSha256(envelope: SandboxExchangeEnvelope): String =
        sha256(canonicalPayload(envelope))

    fun receiptHash(
        envelope: SandboxExchangeEnvelope,
        outcome: String,
        observedRunnerCommit: String?
    ): String {
        val payload = buildString {
            field(envelope.previousReceiptHash)
            field(envelopeSha256(envelope))
            field(outcome)
            field(observedRunnerCommit.orEmpty())
        }
        return sha256(payload)
    }

    fun canonicalPayload(envelope: SandboxExchangeEnvelope): String = buildString {
        field(envelope.schema)
        field(envelope.exchangeId)
        field(envelope.sourceRepository)
        field(envelope.sourceCommit)
        field(envelope.custodyTargetId)
        field(envelope.targetRoute.name)
        field(envelope.testPlanId)
        field(envelope.authorizationReceiptId)
        field(envelope.classification.name)
        field(envelope.purpose)
        field(envelope.retentionRule)
        field(envelope.expectedRunnerCommit.orEmpty())
        field(envelope.previousReceiptHash)
        field(envelope.rollbackRef)
        field(envelope.evidenceState.name)
        field(envelope.claimAllowed.toString())

        envelope.artifacts.sortedBy { it.artifactId }.forEach { artifact ->
            field(artifact.artifactId)
            field(artifact.artifactClass.name)
            field(artifact.opaqueRef)
            field(artifact.sha256)
            field(artifact.sizeBytes.toString())
            field(artifact.mediaType)
            field(artifact.containsSecretMaterial.toString())
        }
    }

    private fun StringBuilder.field(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        append(bytes.size).append(':').append(value).append('|')
    }

    private fun looksLikeCredential(value: String): Boolean {
        val lower = value.lowercase()
        return credentialMarkers.any(lower::contains)
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private const val MAX_ARTIFACT_BYTES = 1_073_741_824L
}
