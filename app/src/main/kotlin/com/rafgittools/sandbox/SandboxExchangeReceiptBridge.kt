package com.rafgittools.sandbox

import com.rafgittools.data.github.RepositoryGovernanceReceiptStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges sandbox observations into the existing RafGit Tools append-only
 * SHA-256 governance receipt chain.
 *
 * Metadata only: no raw fixtures, credentials, tokens or private keys.
 */
@Singleton
class SandboxExchangeReceiptBridge @Inject constructor(
    private val receiptStore: RepositoryGovernanceReceiptStore
) {
    fun record(
        envelope: SandboxExchangeEnvelope,
        outcome: String,
        observedRunnerCommit: String?,
        gaps: List<String> = emptyList()
    ): Result<SandboxExchangeReceiptResult> = runCatching {
        val validation = SandboxExchangeProtocol.validate(envelope)
        check(validation.allowed) {
            "Sandbox envelope rejected: " + validation.errors.joinToString(",")
        }
        val observation = SandboxExchangeProtocol.validateObservation(
            outcome = outcome,
            observedRunnerCommit = observedRunnerCommit,
            gaps = gaps
        )
        check(observation.allowed) {
            "Sandbox observation rejected: " + observation.errors.joinToString(",")
        }

        val chain = receiptStore.verifyChain()
        check(chain.valid) {
            "Governance receipt chain invalid: " + (chain.error ?: "unknown")
        }
        val expectedPrevious = chain.headHash ?: "GENESIS"
        check(envelope.previousReceiptHash == expectedPrevious) {
            "Sandbox previousReceiptHash does not match current governance ledger head"
        }

        val envelopeHash = SandboxExchangeProtocol.envelopeSha256(envelope)
        val receiptHash = SandboxExchangeProtocol.receiptHash(
            envelope = envelope,
            outcome = outcome,
            observedRunnerCommit = observedRunnerCommit
        )

        val receiptId = receiptStore.appendDetailed(
            repository = envelope.sourceRepository,
            operation = "SANDBOX_EXCHANGE:" + envelope.targetRoute.name,
            outcome = outcome,
            details = buildString {
                append("exchange_id=").append(envelope.exchangeId)
                append("; envelope_sha256=").append(envelopeHash)
                append("; sandbox_receipt_hash=").append(receiptHash)
                append("; custody_target_id=").append(envelope.custodyTargetId)
                append("; test_plan_id=").append(envelope.testPlanId)
                append("; evidence_state=").append(envelope.evidenceState.name)
                append("; claim_allowed=false")
            },
            beforeSnapshot = envelope.sourceCommit,
            afterSnapshot = observedRunnerCommit,
            gaps = gaps
        )

        SandboxExchangeReceiptResult(
            governanceReceiptId = receiptId,
            envelopeSha256 = envelopeHash,
            sandboxReceiptHash = receiptHash
        )
    }
}

data class SandboxExchangeReceiptResult(
    val governanceReceiptId: String,
    val envelopeSha256: String,
    val sandboxReceiptHash: String
)
