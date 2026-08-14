package com.rafgittools.rafgitfs.assurance

enum class RafGitFsSecurityDecision { ALLOW, BLOCK, TOKEN_VAZIO }

data class RafGitFsPublicationContext(
    val generatedBranch: String?,
    val baseBranch: String,
    val planHash: String?,
    val approvalExact: Boolean,
    val unresolvedConflicts: Int,
    val stagedFileCount: Int,
    val forcePush: Boolean,
    val draftPullRequest: Boolean,
    val claimAllowed: Boolean,
    val secretsPersistedInRoom: Boolean,
    val workspacePrivate: Boolean
)

data class RafGitFsSecurityAssessment(
    val decision: RafGitFsSecurityDecision,
    val controls: List<RafGitFsControlResult>
) {
    val blockingCodes: List<String>
        get() = controls.filter { it.decision == RafGitFsSecurityDecision.BLOCK }.map { it.code }
    val tokenVazioCodes: List<String>
        get() = controls.filter { it.decision == RafGitFsSecurityDecision.TOKEN_VAZIO }.map { it.code }
}

data class RafGitFsControlResult(
    val code: String,
    val decision: RafGitFsSecurityDecision,
    val detail: String
)

object RafGitFsSecurityPolicy {
    private val protectedBranches = setOf(
        "main", "master", "develop", "development", "production", "release"
    )

    fun assessPublication(context: RafGitFsPublicationContext): RafGitFsSecurityAssessment {
        val controls = listOf(
            observed(
                "SEC-CLAIM-001",
                !context.claimAllowed,
                "claimAllowed must remain false"
            ),
            observed(
                "SEC-SECRET-002",
                !context.secretsPersistedInRoom,
                "tokens and credentials must not be persisted in Room"
            ),
            observed(
                "SEC-STORE-003",
                context.workspacePrivate,
                "workspace must remain under app-private filesDir"
            ),
            known(
                "SEC-PLAN-004",
                context.planHash?.matches(Regex("[0-9a-f]{64}")) == true,
                context.planHash != null,
                "canonical SHA-256 plan hash required"
            ),
            observed(
                "SEC-APPROVAL-005",
                context.approvalExact,
                "approval must match the exact request and plan hash"
            ),
            observed(
                "SEC-CONFLICT-006",
                context.unresolvedConflicts == 0,
                "unresolved conflicts block remote publication"
            ),
            observed(
                "SEC-STAGE-007",
                context.stagedFileCount > 0,
                "at least one checksum-valid staged file is required"
            ),
            known(
                "SEC-BRANCH-008",
                context.generatedBranch?.startsWith("rafgitfs/") == true &&
                    context.generatedBranch.lowercase() !in protectedBranches,
                context.generatedBranch != null,
                "only generated rafgitfs/* branches are writable"
            ),
            observed(
                "SEC-BASE-009",
                context.baseBranch.isNotBlank(),
                "base branch identity must be explicit"
            ),
            observed(
                "SEC-FORCE-010",
                !context.forcePush,
                "force push is forbidden"
            ),
            observed(
                "SEC-PR-011",
                context.draftPullRequest,
                "pull request must be opened in draft state"
            )
        )
        val decision = when {
            controls.any { it.decision == RafGitFsSecurityDecision.BLOCK } -> RafGitFsSecurityDecision.BLOCK
            controls.any { it.decision == RafGitFsSecurityDecision.TOKEN_VAZIO } -> RafGitFsSecurityDecision.TOKEN_VAZIO
            else -> RafGitFsSecurityDecision.ALLOW
        }
        return RafGitFsSecurityAssessment(decision, controls)
    }

    fun protectedBranch(branch: String): Boolean = branch.lowercase() in protectedBranches

    private fun observed(code: String, condition: Boolean, detail: String) =
        RafGitFsControlResult(
            code,
            if (condition) RafGitFsSecurityDecision.ALLOW else RafGitFsSecurityDecision.BLOCK,
            detail
        )

    private fun known(code: String, condition: Boolean, known: Boolean, detail: String) =
        RafGitFsControlResult(
            code,
            when {
                !known -> RafGitFsSecurityDecision.TOKEN_VAZIO
                condition -> RafGitFsSecurityDecision.ALLOW
                else -> RafGitFsSecurityDecision.BLOCK
            },
            detail
        )
}
