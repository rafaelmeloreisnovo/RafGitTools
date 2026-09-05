package com.rafgittools.data.github

/**
 * Deterministic repository-governance audit profile.
 *
 * This is a CIS-style structured evidence profile for operational auditing. It is not
 * a declaration of CIS certification and must not be promoted beyond observed evidence.
 */
enum class GovernanceControlState {
    PASS,
    FAIL,
    TOKEN_VAZIO,
    NOT_APPLICABLE
}

data class GovernanceAuditControl(
    val id: String,
    val domain: String,
    val title: String,
    val state: GovernanceControlState,
    val evidence: String,
    val remediation: String? = null
)

data class GovernanceAuditInput(
    val repository: GovernanceRepositoryDetails,
    val branchProtectionEnabled: Boolean?,
    val branchProtection: BranchProtectionSnapshot?,
    val rulesets: List<RepositoryRulesetSummary>?,
    val actionsPermissions: ActionsPermissionsSnapshot?,
    val workflowPermissions: ActionsWorkflowPermissionsSnapshot?,
    val vulnerabilityAlertsEnabled: Boolean?,
    val automatedSecurityFixesEnabled: Boolean?,
    val privateVulnerabilityReportingEnabled: Boolean?,
    val appendOnlyChainValid: Boolean?
)

data class GovernanceAuditReport(
    val profileId: String,
    val repository: String,
    val generatedAtEpochMs: Long,
    val controls: List<GovernanceAuditControl>
) {
    val passCount: Int get() = controls.count { it.state == GovernanceControlState.PASS }
    val failCount: Int get() = controls.count { it.state == GovernanceControlState.FAIL }
    val gapCount: Int get() = controls.count { it.state == GovernanceControlState.TOKEN_VAZIO }
    val notApplicableCount: Int get() = controls.count { it.state == GovernanceControlState.NOT_APPLICABLE }
    val assessedCount: Int get() = passCount + failCount
    val scorePercent: Int
        get() = if (assessedCount == 0) 0 else (passCount * 100) / assessedCount
}

object RepositoryGovernanceAuditor {
    const val PROFILE_ID = "RAFGITTOOLS_GOVERNANCE_CIS_STYLE_V1"

    fun evaluate(input: GovernanceAuditInput): GovernanceAuditReport {
        val repo = input.repository
        val protection = input.branchProtection
        val reviews = protection?.requiredPullRequestReviews
        val statusChecks = protection?.requiredStatusChecks
        val controls = buildList {
            add(bool(
                "SET-001", "SETTINGS", "Delete head branches after merge",
                repo.deleteBranchOnMerge,
                "provider.delete_branch_on_merge=${repo.deleteBranchOnMerge}",
                "Enable automatic deletion of merged head branches."
            ))
            add(bool(
                "SET-002", "SETTINGS", "Require web commit sign-off",
                repo.webCommitSignoffRequired,
                "provider.web_commit_signoff_required=${repo.webCommitSignoffRequired}",
                "Require sign-off for commits created in the web UI."
            ))
            add(bool(
                "SET-003", "INTEROPERABILITY", "Allow pull-request branch updates",
                repo.allowUpdateBranch,
                "provider.allow_update_branch=${repo.allowUpdateBranch}",
                "Enable update-branch support when compatible with repository policy."
            ))

            add(nullableBool(
                "ENF-001", "ENFORCEMENT", "Default branch protection is enforced",
                input.branchProtectionEnabled,
                "provider.branch_protection=${token(input.branchProtectionEnabled)}",
                "Protect the default branch or apply an equivalent active ruleset."
            ))
            add(protectionBool(
                "ENF-002", "ENFORCEMENT", "Administrators are included in branch enforcement",
                input.branchProtectionEnabled,
                protection?.enforceAdmins?.enabled,
                "provider.enforce_admins=${token(protection?.enforceAdmins?.enabled)}",
                "Enable administrator enforcement."
            ))
            add(reviewCount(
                "ENF-003", "ENFORCEMENT", "At least one approving review is required",
                input.branchProtectionEnabled,
                reviews?.requiredApprovingReviewCount,
                minimum = 1,
                remediation = "Require at least one independent approving review."
            ))
            add(protectionBool(
                "ENF-004", "ENFORCEMENT", "Stale approvals are dismissed",
                input.branchProtectionEnabled,
                reviews?.dismissStaleReviews,
                "dismiss_stale_reviews=${token(reviews?.dismissStaleReviews)}",
                "Dismiss stale pull-request approvals after new changes."
            ))
            add(protectionBool(
                "ENF-005", "ENFORCEMENT", "Last-push approval is required",
                input.branchProtectionEnabled,
                reviews?.requireLastPushApproval,
                "require_last_push_approval=${token(reviews?.requireLastPushApproval)}",
                "Require approval after the most recent push."
            ))
            add(protectionBool(
                "ENF-006", "ENFORCEMENT", "Conversation resolution is required",
                input.branchProtectionEnabled,
                protection?.requiredConversationResolution?.enabled,
                "required_conversation_resolution=${token(protection?.requiredConversationResolution?.enabled)}",
                "Require review conversations to be resolved before merge."
            ))
            add(invertedProtectionBool(
                "ENF-007", "ENFORCEMENT", "Force pushes are blocked",
                input.branchProtectionEnabled,
                protection?.allowForcePushes?.enabled,
                "allow_force_pushes=${token(protection?.allowForcePushes?.enabled)}",
                "Disable force pushes on the protected default branch."
            ))
            add(invertedProtectionBool(
                "ENF-008", "ENFORCEMENT", "Branch deletion is blocked",
                input.branchProtectionEnabled,
                protection?.allowDeletions?.enabled,
                "allow_deletions=${token(protection?.allowDeletions?.enabled)}",
                "Disable deletion of the protected default branch."
            ))
            add(protectionBool(
                "ENF-009", "ENFORCEMENT", "Linear history is required",
                input.branchProtectionEnabled,
                protection?.requiredLinearHistory?.enabled,
                "required_linear_history=${token(protection?.requiredLinearHistory?.enabled)}",
                "Require linear history when compatible with the repository merge model."
            ))
            add(protectionBool(
                "ENF-010", "ENFORCEMENT", "Required status checks use strict mode",
                input.branchProtectionEnabled,
                statusChecks?.strict,
                "required_status_checks.strict=${token(statusChecks?.strict)}",
                "Require the branch to be current before merging."
            ))
            add(statusContexts(
                input.branchProtectionEnabled,
                statusChecks?.contexts
            ))
            add(rulesetControl(input.rulesets))

            add(nullableBool(
                "SEC-001", "SECURITY", "Vulnerability alerts are enabled",
                input.vulnerabilityAlertsEnabled,
                "vulnerability_alerts=${token(input.vulnerabilityAlertsEnabled)}",
                "Enable dependency/vulnerability alerts."
            ))
            add(nullableBool(
                "SEC-002", "SECURITY", "Automated security fixes are enabled",
                input.automatedSecurityFixesEnabled,
                "automated_security_fixes=${token(input.automatedSecurityFixesEnabled)}",
                "Enable automated security updates when supported."
            ))
            add(nullableBool(
                "SEC-003", "SECURITY", "Advanced Security is enabled when observable",
                repo.securityAndAnalysis?.advancedSecurity?.isEnabledOrNull(),
                "advanced_security=${token(repo.securityAndAnalysis?.advancedSecurity?.isEnabledOrNull())}",
                "Enable Advanced Security when available for this repository/plan."
            ))
            add(nullableBool(
                "SEC-004", "SECURITY", "Secret scanning is enabled",
                repo.securityAndAnalysis?.secretScanning?.isEnabledOrNull(),
                "secret_scanning=${token(repo.securityAndAnalysis?.secretScanning?.isEnabledOrNull())}",
                "Enable secret scanning when supported."
            ))
            add(nullableBool(
                "SEC-005", "SECURITY", "Secret-scanning push protection is enabled",
                repo.securityAndAnalysis?.secretScanningPushProtection?.isEnabledOrNull(),
                "secret_scanning_push_protection=${token(repo.securityAndAnalysis?.secretScanningPushProtection?.isEnabledOrNull())}",
                "Enable push protection for detected secrets when supported."
            ))
            if (repo.isPrivate) {
                add(GovernanceAuditControl(
                    "SEC-006", "SECURITY", "Private vulnerability reporting",
                    GovernanceControlState.NOT_APPLICABLE,
                    "repository_visibility=private",
                    null
                ))
            } else {
                add(nullableBool(
                    "SEC-006", "SECURITY", "Private vulnerability reporting is enabled",
                    input.privateVulnerabilityReportingEnabled,
                    "private_vulnerability_reporting=${token(input.privateVulnerabilityReportingEnabled)}",
                    "Enable private vulnerability reporting for coordinated disclosure."
                ))
            }

            add(actionsReadOnly(input.workflowPermissions))
            add(actionsApproval(input.workflowPermissions))
            add(actionsSurface(input.actionsPermissions))

            add(nullableBool(
                "AUD-001", "AUDIT", "Local append-only governance chain verifies",
                input.appendOnlyChainValid,
                "local_receipt_chain_valid=${token(input.appendOnlyChainValid)}",
                "Repair only by preserving the original log and starting a successor chain; never rewrite history."
            ))
        }

        return GovernanceAuditReport(
            profileId = PROFILE_ID,
            repository = repo.fullName,
            generatedAtEpochMs = System.currentTimeMillis(),
            controls = controls
        )
    }

    private fun bool(
        id: String,
        domain: String,
        title: String,
        value: Boolean,
        evidence: String,
        remediation: String
    ) = GovernanceAuditControl(
        id, domain, title,
        if (value) GovernanceControlState.PASS else GovernanceControlState.FAIL,
        evidence,
        if (value) null else remediation
    )

    private fun nullableBool(
        id: String,
        domain: String,
        title: String,
        value: Boolean?,
        evidence: String,
        remediation: String
    ) = GovernanceAuditControl(
        id, domain, title,
        when (value) {
            true -> GovernanceControlState.PASS
            false -> GovernanceControlState.FAIL
            null -> GovernanceControlState.TOKEN_VAZIO
        },
        evidence,
        if (value == true) null else remediation
    )

    private fun protectionBool(
        id: String,
        domain: String,
        title: String,
        protectionEnabled: Boolean?,
        value: Boolean?,
        evidence: String,
        remediation: String
    ) = when (protectionEnabled) {
        false -> GovernanceAuditControl(id, domain, title, GovernanceControlState.FAIL, "branch_protection=false; $evidence", remediation)
        null -> GovernanceAuditControl(id, domain, title, GovernanceControlState.TOKEN_VAZIO, "branch_protection=TOKEN_VAZIO; $evidence", remediation)
        true -> nullableBool(id, domain, title, value, evidence, remediation)
    }

    private fun invertedProtectionBool(
        id: String,
        domain: String,
        title: String,
        protectionEnabled: Boolean?,
        providerAllows: Boolean?,
        evidence: String,
        remediation: String
    ) = protectionBool(id, domain, title, protectionEnabled, providerAllows?.not(), evidence, remediation)

    private fun reviewCount(
        id: String,
        domain: String,
        title: String,
        protectionEnabled: Boolean?,
        value: Int?,
        minimum: Int,
        remediation: String
    ): GovernanceAuditControl = when (protectionEnabled) {
        false -> GovernanceAuditControl(id, domain, title, GovernanceControlState.FAIL, "branch_protection=false", remediation)
        null -> GovernanceAuditControl(id, domain, title, GovernanceControlState.TOKEN_VAZIO, "branch_protection=TOKEN_VAZIO", remediation)
        true -> when {
            value == null -> GovernanceAuditControl(id, domain, title, GovernanceControlState.TOKEN_VAZIO, "required_approving_review_count=TOKEN_VAZIO", remediation)
            value >= minimum -> GovernanceAuditControl(id, domain, title, GovernanceControlState.PASS, "required_approving_review_count=$value")
            else -> GovernanceAuditControl(id, domain, title, GovernanceControlState.FAIL, "required_approving_review_count=$value", remediation)
        }
    }

    private fun statusContexts(
        protectionEnabled: Boolean?,
        contexts: List<String>?
    ): GovernanceAuditControl = when (protectionEnabled) {
        false -> GovernanceAuditControl(
            "ENF-011", "ENFORCEMENT", "At least one required status check is bound",
            GovernanceControlState.FAIL, "branch_protection=false",
            "Bind the exact required CI checks before claiming server-side enforcement."
        )
        null -> GovernanceAuditControl(
            "ENF-011", "ENFORCEMENT", "At least one required status check is bound",
            GovernanceControlState.TOKEN_VAZIO, "branch_protection=TOKEN_VAZIO",
            "Read back required status checks from the provider."
        )
        true -> when {
            contexts == null -> GovernanceAuditControl(
                "ENF-011", "ENFORCEMENT", "At least one required status check is bound",
                GovernanceControlState.TOKEN_VAZIO, "required_status_checks.contexts=TOKEN_VAZIO",
                "Read back required status checks from the provider."
            )
            contexts.isEmpty() -> GovernanceAuditControl(
                "ENF-011", "ENFORCEMENT", "At least one required status check is bound",
                GovernanceControlState.FAIL, "required_status_checks.contexts=[]",
                "Bind exact required CI checks to the protected branch or equivalent ruleset."
            )
            else -> GovernanceAuditControl(
                "ENF-011", "ENFORCEMENT", "At least one required status check is bound",
                GovernanceControlState.PASS, "required_status_checks.contexts=${contexts.joinToString(",")}"
            )
        }
    }

    private fun rulesetControl(rulesets: List<RepositoryRulesetSummary>?): GovernanceAuditControl {
        if (rulesets == null) {
            return GovernanceAuditControl(
                "ENF-012", "ENFORCEMENT", "Repository ruleset inventory is observable",
                GovernanceControlState.TOKEN_VAZIO, "rulesets=TOKEN_VAZIO",
                "Read repository rulesets with provider authority."
            )
        }
        val active = rulesets.filter { it.enforcement.equals("active", ignoreCase = true) }
        return GovernanceAuditControl(
            "ENF-012", "ENFORCEMENT", "At least one active repository ruleset is present",
            if (active.isNotEmpty()) GovernanceControlState.PASS else GovernanceControlState.FAIL,
            "rulesets=${rulesets.size}; active=${active.size}",
            if (active.isEmpty()) "Create or activate a ruleset when branch protection alone is not the intended policy surface." else null
        )
    }

    private fun actionsReadOnly(value: ActionsWorkflowPermissionsSnapshot?): GovernanceAuditControl = when (value?.defaultWorkflowPermissions) {
        null -> GovernanceAuditControl(
            "ACT-001", "ACTIONS", "Default GITHUB_TOKEN permission is read-only",
            GovernanceControlState.TOKEN_VAZIO, "default_workflow_permissions=TOKEN_VAZIO",
            "Read provider Actions workflow permissions."
        )
        "read" -> GovernanceAuditControl(
            "ACT-001", "ACTIONS", "Default GITHUB_TOKEN permission is read-only",
            GovernanceControlState.PASS, "default_workflow_permissions=read"
        )
        else -> GovernanceAuditControl(
            "ACT-001", "ACTIONS", "Default GITHUB_TOKEN permission is read-only",
            GovernanceControlState.FAIL, "default_workflow_permissions=${value.defaultWorkflowPermissions}",
            "Set default workflow permissions to read and grant write permissions explicitly per workflow/job only when required."
        )
    }

    private fun actionsApproval(value: ActionsWorkflowPermissionsSnapshot?): GovernanceAuditControl = when (value?.canApprovePullRequestReviews) {
        null -> GovernanceAuditControl(
            "ACT-002", "ACTIONS", "GitHub Actions cannot approve pull requests by default",
            GovernanceControlState.TOKEN_VAZIO, "can_approve_pull_request_reviews=TOKEN_VAZIO",
            "Read provider Actions workflow permissions."
        )
        false -> GovernanceAuditControl(
            "ACT-002", "ACTIONS", "GitHub Actions cannot approve pull requests by default",
            GovernanceControlState.PASS, "can_approve_pull_request_reviews=false"
        )
        true -> GovernanceAuditControl(
            "ACT-002", "ACTIONS", "GitHub Actions cannot approve pull requests by default",
            GovernanceControlState.FAIL, "can_approve_pull_request_reviews=true",
            "Disable workflow-generated approving reviews unless a separately audited use case requires them."
        )
    }

    private fun actionsSurface(value: ActionsPermissionsSnapshot?): GovernanceAuditControl = if (value == null) {
        GovernanceAuditControl(
            "ACT-003", "ACTIONS", "Actions policy surface is observable",
            GovernanceControlState.TOKEN_VAZIO, "actions_permissions=TOKEN_VAZIO",
            "Read repository Actions permission policy."
        )
    } else {
        GovernanceAuditControl(
            "ACT-003", "ACTIONS", "Actions policy surface is observable",
            GovernanceControlState.PASS,
            "enabled=${value.enabled}; allowed_actions=${value.allowedActions ?: "TOKEN_VAZIO"}"
        )
    }

    private fun token(value: Boolean?): String = value?.toString() ?: "TOKEN_VAZIO"
}
