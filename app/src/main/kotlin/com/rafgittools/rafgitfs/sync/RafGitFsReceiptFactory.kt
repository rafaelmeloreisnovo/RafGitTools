package com.rafgittools.rafgitfs.sync

import com.rafgittools.rafgitfs.data.OperationReceiptEntity
import java.util.UUID

object RafGitFsReceiptFactory {
    fun create(
        plan: RafGitFsSyncPlan,
        outcomes: List<RafGitFsExecutionOutcome>,
        finalPhase: RafGitFsSyncPhase,
        allowed: Boolean,
        result: String,
        createdAt: Long = System.currentTimeMillis()
    ): OperationReceiptEntity {
        val fOk = outcomes.filter { it.evidenceState == "OBSERVED" }
            .joinToString(prefix = "[", postfix = "]") { json(it.step.action.name + ":" + it.result) }
        val fGap = outcomes.filter { it.evidenceState != "OBSERVED" }
            .joinToString(prefix = "[", postfix = "]") { json(it.step.action.name + ":" + it.evidenceState) }
        val fNext = outcomes.filter { it.retryable || !it.step.executableNow }
            .joinToString(prefix = "[", postfix = "]") { json(it.step.reason) }
        val observedSha = outcomes.lastOrNull { it.observedSha != null }?.observedSha
        val evidence = when {
            outcomes.isEmpty() -> "TOKEN_VAZIO"
            outcomes.any { it.evidenceState == "CORRUPTED" || it.evidenceState == "ERROR" } -> "ERROR"
            outcomes.any { it.evidenceState != "OBSERVED" } -> "TOKEN_VAZIO"
            else -> "OBSERVED"
        }
        val requestHash = RafGitFsCanonical.sha256(
            listOf(plan.requestId, plan.profileId, plan.repositoryFullName, plan.refName, plan.planHash)
                .joinToString("\u0000")
        )
        val receiptPayload = listOf(
            requestHash,
            finalPhase.name,
            allowed.toString(),
            result,
            evidence,
            observedSha.orEmpty(),
            fOk,
            fGap,
            fNext,
            createdAt.toString()
        ).joinToString("\u0000")
        return OperationReceiptEntity(
            receiptId = UUID.randomUUID().toString(),
            requestId = plan.requestId,
            profileId = plan.profileId,
            operationType = plan.steps.joinToString(",") { it.action.name }.take(256),
            finalPhase = finalPhase.name,
            allowed = allowed,
            result = result,
            evidenceState = evidence,
            target = "${plan.repositoryFullName}@${plan.refName}",
            observedSha = observedSha,
            requestHash = requestHash,
            receiptHash = RafGitFsCanonical.sha256(receiptPayload),
            hashAlgorithm = "SHA-256",
            fOkJson = fOk,
            fGapJson = fGap,
            fNextJson = fNext,
            createdAt = createdAt,
            claimAllowed = false
        )
    }

    private fun json(value: String): String = buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                else -> append(char)
            }
        }
        append('"')
    }
}
