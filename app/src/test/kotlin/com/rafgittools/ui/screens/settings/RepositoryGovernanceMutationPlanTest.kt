package com.rafgittools.ui.screens.settings

import com.rafgittools.data.github.ActionsWorkflowPermissionsSnapshot
import com.rafgittools.data.github.GovernanceOwner
import com.rafgittools.data.github.GovernancePermissions
import com.rafgittools.data.github.GovernanceRepositoryDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryGovernanceMutationPlanTest {

    @Test
    fun knownReversibleDelta_isExecutableAndRollbackRestoresPreimage() {
        val observed = observed(deleteBranchOnMerge = false)
        val desired = desired(deleteBranchOnMerge = true)
        val plan = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed,
            desired = desired,
            dirtyFields = setOf(GovernanceField.DELETE_BRANCH_ON_MERGE),
            authorityProven = true,
            archived = false,
            nowEpochMs = 1L
        )

        assertTrue(plan.executable)
        assertEquals(GovernanceRollbackClass.REVERSIBLE, plan.items.single().rollbackClass)

        val postApply = observed(deleteBranchOnMerge = true)
        val rollback = RepositoryGovernanceMutationPlanner.buildRollbackPlan(
            repository = REPO,
            appliedPlan = plan,
            currentObserved = postApply,
            authorityProven = true,
            archived = false,
            nowEpochMs = 2L
        )

        assertTrue(rollback.executable)
        assertEquals(false, rollback.items.single().after)
        assertEquals(plan.fingerprint, rollback.sourceFingerprint)
    }

    @Test
    fun tokenVazioPrestate_blocksWrite() {
        val plan = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed(vulnerabilityAlerts = null),
            desired = desired(vulnerabilityAlerts = true),
            dirtyFields = setOf(GovernanceField.VULNERABILITY_ALERTS),
            authorityProven = true,
            archived = false,
            nowEpochMs = 1L
        )

        assertFalse(plan.executable)
        assertEquals(GovernancePlanDisposition.BLOCKED_TOKEN_VAZIO, plan.items.single().disposition)
    }

    @Test
    fun disablingExistingBranchProtection_isBlockedBecauseLosslessRollbackIsNotProven() {
        val plan = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed(branchProtection = true),
            desired = desired(branchProtection = false),
            dirtyFields = setOf(GovernanceField.BRANCH_PROTECTION),
            authorityProven = true,
            archived = false,
            nowEpochMs = 1L
        )

        assertFalse(plan.executable)
        assertEquals(GovernanceRollbackClass.LOSSY_UNSAFE, plan.items.single().rollbackClass)
        assertEquals(GovernancePlanDisposition.BLOCKED_NON_REVERSIBLE, plan.items.single().disposition)
    }

    @Test
    fun providerDrift_blocksRollback() {
        val observed = observed(deleteBranchOnMerge = false)
        val applied = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed,
            desired = desired(deleteBranchOnMerge = true),
            dirtyFields = setOf(GovernanceField.DELETE_BRANCH_ON_MERGE),
            authorityProven = true,
            archived = false,
            nowEpochMs = 1L
        )

        // Provider no longer matches the exact post-apply state expected by the capsule.
        val drifted = observed(deleteBranchOnMerge = false)
        val rollback = RepositoryGovernanceMutationPlanner.buildRollbackPlan(
            repository = REPO,
            appliedPlan = applied,
            currentObserved = drifted,
            authorityProven = true,
            archived = false,
            nowEpochMs = 2L
        )

        assertFalse(rollback.executable)
        assertEquals(GovernancePlanDisposition.BLOCKED_DRIFT, rollback.items.single().disposition)
    }

    @Test
    fun fingerprint_isDeterministicForSameSemanticPlan() {
        val a = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed(),
            desired = desired(deleteBranchOnMerge = true, webCommitSignoff = true),
            dirtyFields = linkedSetOf(
                GovernanceField.DELETE_BRANCH_ON_MERGE,
                GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED
            ),
            authorityProven = true,
            archived = false,
            nowEpochMs = 1L
        )
        val b = RepositoryGovernanceMutationPlanner.buildApplyPlan(
            repository = REPO,
            observed = observed(),
            desired = desired(deleteBranchOnMerge = true, webCommitSignoff = true),
            dirtyFields = linkedSetOf(
                GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED,
                GovernanceField.DELETE_BRANCH_ON_MERGE
            ),
            authorityProven = true,
            archived = false,
            nowEpochMs = 99L
        )

        assertEquals(a.fingerprint, b.fingerprint)
    }

    private fun observed(
        deleteBranchOnMerge: Boolean = false,
        webCommitSignoff: Boolean = false,
        branchProtection: Boolean? = false,
        vulnerabilityAlerts: Boolean? = false
    ) = ObservedRepositoryGovernance(
        details = GovernanceRepositoryDetails(
            name = "repo",
            fullName = REPO,
            owner = GovernanceOwner("owner"),
            archived = false,
            isPrivate = false,
            defaultBranch = "main",
            deleteBranchOnMerge = deleteBranchOnMerge,
            webCommitSignoffRequired = webCommitSignoff,
            permissions = GovernancePermissions(admin = true)
        ),
        branchProtectionEnabled = branchProtection,
        branchProtection = null,
        rulesets = emptyList(),
        actionsPermissions = null,
        workflowPermissions = ActionsWorkflowPermissionsSnapshot(
            defaultWorkflowPermissions = "write",
            canApprovePullRequestReviews = true
        ),
        vulnerabilityAlertsEnabled = vulnerabilityAlerts,
        automatedSecurityFixesEnabled = false,
        privateVulnerabilityReportingEnabled = false,
        advancedSecurityEnabled = false,
        secretScanningEnabled = false,
        secretScanningPushProtectionEnabled = false
    )

    private fun desired(
        deleteBranchOnMerge: Boolean = false,
        webCommitSignoff: Boolean = false,
        branchProtection: Boolean = false,
        vulnerabilityAlerts: Boolean = false
    ) = DesiredRepositoryGovernance(
        hasIssues = true,
        hasProjects = true,
        hasWiki = true,
        hasDiscussions = false,
        allowMergeCommit = true,
        allowSquashMerge = true,
        allowRebaseMerge = true,
        allowAutoMerge = false,
        allowUpdateBranch = false,
        deleteBranchOnMerge = deleteBranchOnMerge,
        webCommitSignoffRequired = webCommitSignoff,
        branchProtectionEnabled = branchProtection,
        vulnerabilityAlertsEnabled = vulnerabilityAlerts,
        automatedSecurityFixesEnabled = false,
        privateVulnerabilityReportingEnabled = false,
        advancedSecurityEnabled = false,
        secretScanningEnabled = false,
        secretScanningPushProtectionEnabled = false,
        actionsReadOnlyDefault = false,
        actionsCanApprovePullRequests = true
    )

    companion object {
        private const val REPO = "owner/repo"
    }
}
