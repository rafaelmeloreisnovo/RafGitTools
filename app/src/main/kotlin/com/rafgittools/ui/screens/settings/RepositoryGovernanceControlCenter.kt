package com.rafgittools.ui.screens.settings

import com.rafgittools.data.github.GovernanceControlState

/**
 * Compact, deterministic projection for the human governance control surface.
 *
 * This is a UI projection only:
 * OBSERVATION != POLICY_TARGET != PROVIDER_MUTATION != ENFORCEMENT_CLAIM.
 */
data class GovernanceControlCenterSnapshot(
    val repository: String?,
    val protectionSurfaceState: GovernanceControlState,
    val branchProtectionState: GovernanceControlState,
    val rulesetState: GovernanceControlState,
    val requiredStatusChecksState: GovernanceControlState,
    val actionsAuthorityState: GovernanceControlState,
    val rulesetCount: Int?,
    val activeRulesetCount: Int?,
    val requiredStatusContexts: List<String>?,
    val canAudit: Boolean,
    val canDryRun: Boolean,
    val canApply: Boolean,
    val canRollback: Boolean,
    val providerSettingsUrl: String?
)

fun buildGovernanceControlCenterSnapshot(
    state: RepositoryGovernanceUiState
): GovernanceControlCenterSnapshot {
    val repository = state.selectedRepository
    val observed = state.observed
    val rulesets = observed?.rulesets
    val activeRulesets = rulesets?.count { it.enforcement.equals("active", ignoreCase = true) }

    val branchState = when (observed?.branchProtectionEnabled) {
        true -> GovernanceControlState.PASS
        false -> GovernanceControlState.FAIL
        null -> GovernanceControlState.TOKEN_VAZIO
    }

    val rulesetState = when {
        rulesets == null -> GovernanceControlState.TOKEN_VAZIO
        activeRulesets != null && activeRulesets > 0 -> GovernanceControlState.PASS
        else -> GovernanceControlState.FAIL
    }

    val protectionSurface = when {
        branchState == GovernanceControlState.PASS || rulesetState == GovernanceControlState.PASS ->
            GovernanceControlState.PASS
        branchState == GovernanceControlState.TOKEN_VAZIO || rulesetState == GovernanceControlState.TOKEN_VAZIO ->
            GovernanceControlState.TOKEN_VAZIO
        else -> GovernanceControlState.FAIL
    }

    val statusContexts = observed?.branchProtection?.requiredStatusChecks?.contexts
    val statusChecksState = when (observed?.branchProtectionEnabled) {
        null -> GovernanceControlState.TOKEN_VAZIO
        false -> {
            // An active ruleset can still be the authoritative enforcement surface,
            // but this branch-protection endpoint does not prove its status contexts.
            if (rulesetState == GovernanceControlState.PASS) {
                GovernanceControlState.TOKEN_VAZIO
            } else {
                GovernanceControlState.FAIL
            }
        }
        true -> when {
            observed.branchProtection?.requiredStatusChecks == null ->
                GovernanceControlState.TOKEN_VAZIO
            statusContexts.isNullOrEmpty() -> GovernanceControlState.FAIL
            observed.branchProtection.requiredStatusChecks.strict ->
                GovernanceControlState.PASS
            else -> GovernanceControlState.FAIL
        }
    }

    val workflow = observed?.workflowPermissions
    val actionsState = when {
        workflow?.defaultWorkflowPermissions == null ||
            workflow.canApprovePullRequestReviews == null ->
            GovernanceControlState.TOKEN_VAZIO
        workflow.defaultWorkflowPermissions == "read" &&
            workflow.canApprovePullRequestReviews == false ->
            GovernanceControlState.PASS
        else -> GovernanceControlState.FAIL
    }

    val settingsUrl = repository?.fullName?.let {
        "https://github.com/$it/settings/branches"
    }

    return GovernanceControlCenterSnapshot(
        repository = repository?.fullName,
        protectionSurfaceState = protectionSurface,
        branchProtectionState = branchState,
        rulesetState = rulesetState,
        requiredStatusChecksState = statusChecksState,
        actionsAuthorityState = actionsState,
        rulesetCount = rulesets?.size,
        activeRulesetCount = activeRulesets,
        requiredStatusContexts = statusContexts,
        canAudit = repository != null &&
            state.evidenceState != GovernanceEvidenceState.APPLYING,
        canDryRun = state.dirtyFields.isNotEmpty() &&
            state.evidenceState != GovernanceEvidenceState.APPLYING,
        canApply = state.canApply,
        canRollback = state.canRollback,
        providerSettingsUrl = settingsUrl
    )
}
