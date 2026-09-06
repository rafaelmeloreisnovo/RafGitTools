package com.rafgittools.ui.screens.settings

import java.security.MessageDigest

enum class GovernancePlanMode {
    APPLY,
    ROLLBACK
}

enum class GovernancePlanDisposition {
    MUTATE,
    BLOCKED_TOKEN_VAZIO,
    BLOCKED_NON_REVERSIBLE,
    BLOCKED_DRIFT
}

enum class GovernanceRollbackClass {
    REVERSIBLE,
    LOSSY_UNSAFE,
    NOT_AVAILABLE
}

data class GovernanceMutationPlanItem(
    val field: GovernanceField,
    val before: Boolean?,
    val after: Boolean,
    val disposition: GovernancePlanDisposition,
    val rollbackClass: GovernanceRollbackClass,
    val reason: String
)

data class GovernanceMutationPlan(
    val repository: String,
    val mode: GovernancePlanMode,
    val generatedAtEpochMs: Long,
    val sourceFingerprint: String? = null,
    val fingerprint: String,
    val authorityProven: Boolean,
    val archived: Boolean,
    val items: List<GovernanceMutationPlanItem>
) {
    val blockingItems: List<GovernanceMutationPlanItem>
        get() = items.filter { it.disposition != GovernancePlanDisposition.MUTATE }

    val reversibleItems: List<GovernanceMutationPlanItem>
        get() = items.filter {
            it.disposition == GovernancePlanDisposition.MUTATE &&
                it.rollbackClass == GovernanceRollbackClass.REVERSIBLE
        }

    /**
     * Every provider write must have a proven pre-image and a non-lossy rollback path.
     */
    val executable: Boolean
        get() = authorityProven &&
            !archived &&
            items.isNotEmpty() &&
            blockingItems.isEmpty() &&
            reversibleItems.size == items.size

    fun receiptDetails(): String = buildString {
        append("mode=").append(mode.name)
        append("; fingerprint=").append(fingerprint)
        sourceFingerprint?.let { append("; source_fingerprint=").append(it) }
        append("; executable=").append(executable)
        append("; items=")
        items.sortedBy { it.field.name }.forEachIndexed { index, item ->
            if (index > 0) append(" | ")
            append(item.field.name)
            append(':').append(item.before ?: "TOKEN_VAZIO")
            append("->").append(item.after)
            append(':').append(item.disposition.name)
            append(':').append(item.rollbackClass.name)
            append(':').append(item.reason)
        }
    }
}

object RepositoryGovernanceMutationPlanner {

    fun buildApplyPlan(
        repository: String,
        observed: ObservedRepositoryGovernance,
        desired: DesiredRepositoryGovernance,
        dirtyFields: Set<GovernanceField>,
        authorityProven: Boolean,
        archived: Boolean,
        nowEpochMs: Long = System.currentTimeMillis()
    ): GovernanceMutationPlan {
        val items = dirtyFields
            .sortedBy { it.name }
            .map { field ->
                val before = observedValue(field, observed)
                val after = desiredValue(field, desired)
                val rollbackClass = rollbackClass(field, before, after)
                val disposition = when {
                    before == null -> GovernancePlanDisposition.BLOCKED_TOKEN_VAZIO
                    rollbackClass != GovernanceRollbackClass.REVERSIBLE -> GovernancePlanDisposition.BLOCKED_NON_REVERSIBLE
                    else -> GovernancePlanDisposition.MUTATE
                }
                GovernanceMutationPlanItem(
                    field = field,
                    before = before,
                    after = after,
                    disposition = disposition,
                    rollbackClass = rollbackClass,
                    reason = when (disposition) {
                        GovernancePlanDisposition.MUTATE -> "PREIMAGE_PROVEN_AND_ROLLBACK_AVAILABLE"
                        GovernancePlanDisposition.BLOCKED_TOKEN_VAZIO -> "PROVIDER_PRESTATE_NOT_PROVEN"
                        GovernancePlanDisposition.BLOCKED_NON_REVERSIBLE -> "LOSSLESS_ROLLBACK_NOT_PROVEN"
                        GovernancePlanDisposition.BLOCKED_DRIFT -> "UNEXPECTED"
                    }
                )
            }

        return plan(
            repository = repository,
            mode = GovernancePlanMode.APPLY,
            nowEpochMs = nowEpochMs,
            sourceFingerprint = null,
            authorityProven = authorityProven,
            archived = archived,
            items = items
        )
    }

    /**
     * A rollback is generated only from a previously applied plan and fresh provider state.
     * If the provider drifted after the apply, rollback is blocked rather than overwriting drift.
     */
    fun buildRollbackPlan(
        repository: String,
        appliedPlan: GovernanceMutationPlan,
        currentObserved: ObservedRepositoryGovernance,
        authorityProven: Boolean,
        archived: Boolean,
        nowEpochMs: Long = System.currentTimeMillis()
    ): GovernanceMutationPlan {
        val items = appliedPlan.items
            .sortedBy { it.field.name }
            .map { original ->
                val providerNow = observedValue(original.field, currentObserved)
                val target = original.before
                val disposition = when {
                    original.rollbackClass != GovernanceRollbackClass.REVERSIBLE ->
                        GovernancePlanDisposition.BLOCKED_NON_REVERSIBLE
                    target == null || providerNow == null ->
                        GovernancePlanDisposition.BLOCKED_TOKEN_VAZIO
                    providerNow != original.after ->
                        GovernancePlanDisposition.BLOCKED_DRIFT
                    else -> GovernancePlanDisposition.MUTATE
                }
                GovernanceMutationPlanItem(
                    field = original.field,
                    before = providerNow,
                    after = target ?: false,
                    disposition = disposition,
                    rollbackClass = if (target != null && providerNow != null) {
                        GovernanceRollbackClass.REVERSIBLE
                    } else {
                        GovernanceRollbackClass.NOT_AVAILABLE
                    },
                    reason = when (disposition) {
                        GovernancePlanDisposition.MUTATE -> "APPLIED_PREIMAGE_RECOVERABLE"
                        GovernancePlanDisposition.BLOCKED_TOKEN_VAZIO -> "ROLLBACK_PRESTATE_NOT_PROVEN"
                        GovernancePlanDisposition.BLOCKED_NON_REVERSIBLE -> "ORIGINAL_WRITE_WAS_NOT_LOSSLESSLY_REVERSIBLE"
                        GovernancePlanDisposition.BLOCKED_DRIFT -> "PROVIDER_DRIFT_AFTER_APPLY"
                    }
                )
            }

        return plan(
            repository = repository,
            mode = GovernancePlanMode.ROLLBACK,
            nowEpochMs = nowEpochMs,
            sourceFingerprint = appliedPlan.fingerprint,
            authorityProven = authorityProven,
            archived = archived,
            items = items
        )
    }

    fun applyPlanToDesired(
        base: DesiredRepositoryGovernance,
        plan: GovernanceMutationPlan
    ): DesiredRepositoryGovernance = plan.items
        .filter { it.disposition == GovernancePlanDisposition.MUTATE }
        .fold(base) { desired, item -> desired.withFieldForPlan(item.field, item.after) }

    private fun plan(
        repository: String,
        mode: GovernancePlanMode,
        nowEpochMs: Long,
        sourceFingerprint: String?,
        authorityProven: Boolean,
        archived: Boolean,
        items: List<GovernanceMutationPlanItem>
    ): GovernanceMutationPlan {
        val fingerprint = sha256(
            buildString {
                append(repository).append('\n')
                append(mode.name).append('\n')
                append(sourceFingerprint.orEmpty()).append('\n')
                append(authorityProven).append('\n')
                append(archived).append('\n')
                items.sortedBy { it.field.name }.forEach { item ->
                    append(item.field.name).append('|')
                    append(item.before ?: "TOKEN_VAZIO").append('|')
                    append(item.after).append('|')
                    append(item.disposition.name).append('|')
                    append(item.rollbackClass.name).append('|')
                    append(item.reason).append('\n')
                }
            }
        )
        return GovernanceMutationPlan(
            repository = repository,
            mode = mode,
            generatedAtEpochMs = nowEpochMs,
            sourceFingerprint = sourceFingerprint,
            fingerprint = fingerprint,
            authorityProven = authorityProven,
            archived = archived,
            items = items
        )
    }

    private fun rollbackClass(
        field: GovernanceField,
        before: Boolean?,
        after: Boolean
    ): GovernanceRollbackClass = when {
        before == null -> GovernanceRollbackClass.NOT_AVAILABLE
        field == GovernanceField.BRANCH_PROTECTION && before && !after ->
            GovernanceRollbackClass.LOSSY_UNSAFE
        else -> GovernanceRollbackClass.REVERSIBLE
    }

    private fun observedValue(field: GovernanceField, observed: ObservedRepositoryGovernance): Boolean? = when (field) {
        GovernanceField.HAS_ISSUES -> observed.details.hasIssues
        GovernanceField.HAS_PROJECTS -> observed.details.hasProjects
        GovernanceField.HAS_WIKI -> observed.details.hasWiki
        GovernanceField.HAS_DISCUSSIONS -> observed.details.hasDiscussions
        GovernanceField.ALLOW_MERGE_COMMIT -> observed.details.allowMergeCommit
        GovernanceField.ALLOW_SQUASH_MERGE -> observed.details.allowSquashMerge
        GovernanceField.ALLOW_REBASE_MERGE -> observed.details.allowRebaseMerge
        GovernanceField.ALLOW_AUTO_MERGE -> observed.details.allowAutoMerge
        GovernanceField.ALLOW_UPDATE_BRANCH -> observed.details.allowUpdateBranch
        GovernanceField.DELETE_BRANCH_ON_MERGE -> observed.details.deleteBranchOnMerge
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> observed.details.webCommitSignoffRequired
        GovernanceField.BRANCH_PROTECTION -> observed.branchProtectionEnabled
        GovernanceField.VULNERABILITY_ALERTS -> observed.vulnerabilityAlertsEnabled
        GovernanceField.AUTOMATED_SECURITY_FIXES -> observed.automatedSecurityFixesEnabled
        GovernanceField.PRIVATE_VULNERABILITY_REPORTING -> observed.privateVulnerabilityReportingEnabled
        GovernanceField.ADVANCED_SECURITY -> observed.advancedSecurityEnabled
        GovernanceField.SECRET_SCANNING -> observed.secretScanningEnabled
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> observed.secretScanningPushProtectionEnabled
        GovernanceField.ACTIONS_READ_ONLY_DEFAULT -> observed.workflowPermissions?.defaultWorkflowPermissions?.let { it == "read" }
        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS -> observed.workflowPermissions?.canApprovePullRequestReviews
    }

    private fun desiredValue(field: GovernanceField, desired: DesiredRepositoryGovernance): Boolean = when (field) {
        GovernanceField.HAS_ISSUES -> desired.hasIssues
        GovernanceField.HAS_PROJECTS -> desired.hasProjects
        GovernanceField.HAS_WIKI -> desired.hasWiki
        GovernanceField.HAS_DISCUSSIONS -> desired.hasDiscussions
        GovernanceField.ALLOW_MERGE_COMMIT -> desired.allowMergeCommit
        GovernanceField.ALLOW_SQUASH_MERGE -> desired.allowSquashMerge
        GovernanceField.ALLOW_REBASE_MERGE -> desired.allowRebaseMerge
        GovernanceField.ALLOW_AUTO_MERGE -> desired.allowAutoMerge
        GovernanceField.ALLOW_UPDATE_BRANCH -> desired.allowUpdateBranch
        GovernanceField.DELETE_BRANCH_ON_MERGE -> desired.deleteBranchOnMerge
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> desired.webCommitSignoffRequired
        GovernanceField.BRANCH_PROTECTION -> desired.branchProtectionEnabled
        GovernanceField.VULNERABILITY_ALERTS -> desired.vulnerabilityAlertsEnabled
        GovernanceField.AUTOMATED_SECURITY_FIXES -> desired.automatedSecurityFixesEnabled
        GovernanceField.PRIVATE_VULNERABILITY_REPORTING -> desired.privateVulnerabilityReportingEnabled
        GovernanceField.ADVANCED_SECURITY -> desired.advancedSecurityEnabled
        GovernanceField.SECRET_SCANNING -> desired.secretScanningEnabled
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> desired.secretScanningPushProtectionEnabled
        GovernanceField.ACTIONS_READ_ONLY_DEFAULT -> desired.actionsReadOnlyDefault
        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS -> desired.actionsCanApprovePullRequests
    }

    private fun DesiredRepositoryGovernance.withFieldForPlan(
        field: GovernanceField,
        enabled: Boolean
    ): DesiredRepositoryGovernance = when (field) {
        GovernanceField.HAS_ISSUES -> copy(hasIssues = enabled)
        GovernanceField.HAS_PROJECTS -> copy(hasProjects = enabled)
        GovernanceField.HAS_WIKI -> copy(hasWiki = enabled)
        GovernanceField.HAS_DISCUSSIONS -> copy(hasDiscussions = enabled)
        GovernanceField.ALLOW_MERGE_COMMIT -> copy(allowMergeCommit = enabled)
        GovernanceField.ALLOW_SQUASH_MERGE -> copy(allowSquashMerge = enabled)
        GovernanceField.ALLOW_REBASE_MERGE -> copy(allowRebaseMerge = enabled)
        GovernanceField.ALLOW_AUTO_MERGE -> copy(allowAutoMerge = enabled)
        GovernanceField.ALLOW_UPDATE_BRANCH -> copy(allowUpdateBranch = enabled)
        GovernanceField.DELETE_BRANCH_ON_MERGE -> copy(deleteBranchOnMerge = enabled)
        GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED -> copy(webCommitSignoffRequired = enabled)
        GovernanceField.BRANCH_PROTECTION -> copy(branchProtectionEnabled = enabled)
        GovernanceField.VULNERABILITY_ALERTS -> copy(vulnerabilityAlertsEnabled = enabled)
        GovernanceField.AUTOMATED_SECURITY_FIXES -> copy(automatedSecurityFixesEnabled = enabled)
        GovernanceField.PRIVATE_VULNERABILITY_REPORTING -> copy(privateVulnerabilityReportingEnabled = enabled)
        GovernanceField.ADVANCED_SECURITY -> copy(advancedSecurityEnabled = enabled)
        GovernanceField.SECRET_SCANNING -> copy(secretScanningEnabled = enabled)
        GovernanceField.SECRET_SCANNING_PUSH_PROTECTION -> copy(secretScanningPushProtectionEnabled = enabled)
        GovernanceField.ACTIONS_READ_ONLY_DEFAULT -> copy(actionsReadOnlyDefault = enabled)
        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS -> copy(actionsCanApprovePullRequests = enabled)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
