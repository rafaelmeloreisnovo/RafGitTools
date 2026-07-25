package com.rafgittools.rafgitfs.policy

import com.rafgittools.rafgitfs.model.*

/**
 * Pure, side-effect-free policy evaluation for RafGitFS.
 *
 * This class does not execute GitHub operations. It only decides whether a
 * request may advance to a future adapter. Prompt 1 therefore remains safe and
 * non-destructive.
 */
object RafGitFsPolicyEngine {

    private val readOnlyOperations = setOf(
        RafGitFsOperationType.LIST_REPOSITORIES,
        RafGitFsOperationType.LIST_REFS,
        RafGitFsOperationType.LIST_TREE,
        RafGitFsOperationType.READ_CONTENT,
        RafGitFsOperationType.CACHE_CONTENT,
        RafGitFsOperationType.PIN_OFFLINE
    )

    private val irreversibleOperations = setOf(RafGitFsOperationType.DELETE)

    fun evaluate(
        profile: RafGitFsStorageProfile,
        request: RafGitFsOperationRequest
    ): RafGitFsPolicyDecision {
        val reasons = mutableListOf<String>()
        val nextSteps = mutableListOf<String>()

        if (!profile.enabled) reasons += "PROFILE_DISABLED"
        if (request.profileId != profile.id) reasons += "PROFILE_ID_MISMATCH"
        if (profile.provider != RafGitFsProvider.GITHUB) reasons += "UNSUPPORTED_PROVIDER"
        if (profile.claimAllowed) reasons += "CLAIM_ALLOWED_MUST_REMAIN_FALSE_IN_V1"
        if (!profile.receiptRequired) reasons += "RECEIPT_REQUIRED"

        val isReadOnly = request.operation in readOnlyOperations
        val isWrite = !isReadOnly

        if (profile.accessMode == RafGitFsAccessMode.READ_ONLY && isWrite) {
            reasons += "READ_ONLY_PROFILE_BLOCKS_WRITE"
        }

        if (profile.writePolicy == RafGitFsWritePolicy.BLOCKED && isWrite) {
            reasons += "WRITE_POLICY_BLOCKED"
        }

        if (profile.writePolicy == RafGitFsWritePolicy.DIRECT_COMMIT && isWrite) {
            reasons += "DIRECT_COMMIT_NOT_AVAILABLE_IN_V1"
        }

        if (request.operation in irreversibleOperations) {
            reasons += "IRREVERSIBLE_OPERATION_NOT_AVAILABLE_IN_V1"
        }

        if (isWrite && !request.dryRunCompleted) {
            reasons += "DRY_RUN_REQUIRED"
            nextSteps += "Generate a deterministic operation plan and dry-run receipt."
        }

        if (isWrite && !request.humanApproved) {
            reasons += "HUMAN_APPROVAL_REQUIRED"
            nextSteps += "Collect explicit bounded human approval."
        }

        val ref = request.target?.ref
        if (isWrite && ref != null && isProtected(ref, profile.protectedBranchPatterns)) {
            reasons += "PROTECTED_BRANCH_WRITE_BLOCKED"
            nextSteps += "Create a temporary branch and open a pull request."
        }

        val allowed = reasons.isEmpty() && isReadOnly
        val risk = when {
            request.operation in irreversibleOperations -> RafGitFsRiskLevel.CRITICAL
            isWrite -> RafGitFsRiskLevel.HIGH
            request.operation == RafGitFsOperationType.PIN_OFFLINE -> RafGitFsRiskLevel.MEDIUM
            else -> RafGitFsRiskLevel.LOW
        }

        if (!allowed && nextSteps.isEmpty()) {
            nextSteps += "Preserve the request as TOKEN_VAZIO or revise it within the V1 read-only scope."
        }

        return RafGitFsPolicyDecision(
            allowed = allowed,
            riskLevel = risk,
            reasons = reasons,
            requiredNextSteps = nextSteps
        )
    }

    internal fun isProtected(ref: String, patterns: Set<String>): Boolean = patterns.any { pattern ->
        if (pattern.endsWith("/*")) {
            ref.startsWith(pattern.removeSuffix("*"))
        } else {
            ref == pattern
        }
    }
}
