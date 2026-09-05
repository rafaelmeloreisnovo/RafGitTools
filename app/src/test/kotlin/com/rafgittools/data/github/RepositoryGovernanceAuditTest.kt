package com.rafgittools.data.github

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RepositoryGovernanceAuditTest {

    @Test
    fun hardenedObservedState_hasNoFailOrTokenVazio() {
        val report = RepositoryGovernanceAuditor.evaluate(
            GovernanceAuditInput(
                repository = repositoryDetails(isPrivate = false),
                branchProtectionEnabled = true,
                branchProtection = BranchProtectionSnapshot(
                    requiredStatusChecks = RequiredStatusChecksSnapshot(
                        strict = true,
                        contexts = listOf("build", "test")
                    ),
                    requiredPullRequestReviews = PullRequestReviewProtectionSnapshot(
                        dismissStaleReviews = true,
                        requiredApprovingReviewCount = 1,
                        requireLastPushApproval = true
                    ),
                    enforceAdmins = EnabledFlag(true),
                    requiredLinearHistory = EnabledFlag(true),
                    allowForcePushes = EnabledFlag(false),
                    allowDeletions = EnabledFlag(false),
                    requiredConversationResolution = EnabledFlag(true)
                ),
                rulesets = listOf(
                    RepositoryRulesetSummary(
                        id = 42,
                        name = "main-governance",
                        target = "branch",
                        enforcement = "active"
                    )
                ),
                actionsPermissions = ActionsPermissionsSnapshot(enabled = true, allowedActions = "selected"),
                workflowPermissions = ActionsWorkflowPermissionsSnapshot(
                    defaultWorkflowPermissions = "read",
                    canApprovePullRequestReviews = false
                ),
                vulnerabilityAlertsEnabled = true,
                automatedSecurityFixesEnabled = true,
                privateVulnerabilityReportingEnabled = true,
                appendOnlyChainValid = true
            )
        )

        assertThat(report.failCount).isEqualTo(0)
        assertThat(report.gapCount).isEqualTo(0)
        assertThat(report.scorePercent).isEqualTo(100)
    }

    @Test
    fun unavailableProviderEvidence_isTokenVazioNotFalse() {
        val details = repositoryDetails(isPrivate = false).copy(
            securityAndAnalysis = null
        )
        val report = RepositoryGovernanceAuditor.evaluate(
            GovernanceAuditInput(
                repository = details,
                branchProtectionEnabled = null,
                branchProtection = null,
                rulesets = null,
                actionsPermissions = null,
                workflowPermissions = null,
                vulnerabilityAlertsEnabled = null,
                automatedSecurityFixesEnabled = null,
                privateVulnerabilityReportingEnabled = null,
                appendOnlyChainValid = null
            )
        )

        assertThat(report.gapCount).isGreaterThan(0)
        assertThat(report.controls.any {
            it.id == "ENF-001" && it.state == GovernanceControlState.TOKEN_VAZIO
        }).isTrue()
        assertThat(report.controls.any {
            it.id == "AUD-001" && it.state == GovernanceControlState.TOKEN_VAZIO
        }).isTrue()
    }

    @Test
    fun privateRepository_marksPrivateReportingNotApplicable() {
        val report = RepositoryGovernanceAuditor.evaluate(
            GovernanceAuditInput(
                repository = repositoryDetails(isPrivate = true),
                branchProtectionEnabled = false,
                branchProtection = null,
                rulesets = emptyList(),
                actionsPermissions = ActionsPermissionsSnapshot(),
                workflowPermissions = ActionsWorkflowPermissionsSnapshot("read", false),
                vulnerabilityAlertsEnabled = true,
                automatedSecurityFixesEnabled = true,
                privateVulnerabilityReportingEnabled = null,
                appendOnlyChainValid = true
            )
        )

        val control = report.controls.single { it.id == "SEC-006" }
        assertThat(control.state).isEqualTo(GovernanceControlState.NOT_APPLICABLE)
    }

    private fun repositoryDetails(isPrivate: Boolean) = GovernanceRepositoryDetails(
        name = "RafGitTools",
        fullName = "rafaelmeloreisnovo/RafGitTools",
        owner = GovernanceOwner("rafaelmeloreisnovo"),
        isPrivate = isPrivate,
        defaultBranch = "main",
        hasIssues = true,
        hasProjects = true,
        hasWiki = true,
        hasDiscussions = true,
        allowMergeCommit = true,
        allowSquashMerge = true,
        allowRebaseMerge = true,
        allowAutoMerge = true,
        allowUpdateBranch = true,
        deleteBranchOnMerge = true,
        webCommitSignoffRequired = true,
        securityAndAnalysis = GovernanceSecurityAndAnalysis(
            advancedSecurity = GovernanceFeatureStatus("enabled"),
            secretScanning = GovernanceFeatureStatus("enabled"),
            secretScanningPushProtection = GovernanceFeatureStatus("enabled")
        ),
        permissions = GovernancePermissions(admin = true)
    )
}
