package com.rafgittools.ui.screens.settings

import com.rafgittools.data.github.ActionsWorkflowPermissionsSnapshot
import com.rafgittools.data.github.BranchProtectionSnapshot
import com.rafgittools.data.github.EnabledFlag
import com.rafgittools.data.github.GovernanceControlState
import com.rafgittools.data.github.GovernanceOwner
import com.rafgittools.data.github.GovernancePermissions
import com.rafgittools.data.github.GovernanceRepositoryDetails
import com.rafgittools.data.github.GovernanceRepositorySummary
import com.rafgittools.data.github.RepositoryRulesetSummary
import com.rafgittools.data.github.RequiredStatusChecksSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryGovernanceControlCenterTest {

    private fun repository() = GovernanceRepositorySummary(
        name = "termux-app-rafacodephi",
        fullName = "rafaelmeloreisnovo/termux-app-rafacodephi",
        owner = GovernanceOwner("rafaelmeloreisnovo"),
        isPrivate = false,
        archived = false,
        defaultBranch = "master",
        permissions = GovernancePermissions(admin = true)
    )

    private fun details() = GovernanceRepositoryDetails(
        name = "termux-app-rafacodephi",
        fullName = "rafaelmeloreisnovo/termux-app-rafacodephi",
        owner = GovernanceOwner("rafaelmeloreisnovo"),
        archived = false,
        isPrivate = false,
        defaultBranch = "master",
        permissions = GovernancePermissions(admin = true)
    )

    private fun observed(
        branchProtected: Boolean? = true,
        rulesets: List<RepositoryRulesetSummary>? = listOf(
            RepositoryRulesetSummary(
                id = 21908888,
                name = "1",
                target = "branch",
                enforcement = "active"
            )
        ),
        contexts: List<String>? = listOf("provider-protection"),
        strict: Boolean = true,
        workflow: ActionsWorkflowPermissionsSnapshot? =
            ActionsWorkflowPermissionsSnapshot(
                defaultWorkflowPermissions = "read",
                canApprovePullRequestReviews = false
            )
    ) = ObservedRepositoryGovernance(
        details = details(),
        branchProtectionEnabled = branchProtected,
        branchProtection = if (branchProtected == true) {
            BranchProtectionSnapshot(
                requiredStatusChecks = contexts?.let {
                    RequiredStatusChecksSnapshot(strict = strict, contexts = it)
                },
                enforceAdmins = EnabledFlag(true)
            )
        } else {
            null
        },
        rulesets = rulesets,
        actionsPermissions = null,
        workflowPermissions = workflow,
        vulnerabilityAlertsEnabled = null,
        automatedSecurityFixesEnabled = null,
        privateVulnerabilityReportingEnabled = null,
        advancedSecurityEnabled = null,
        secretScanningEnabled = null,
        secretScanningPushProtectionEnabled = null
    )

    @Test
    fun activeRulesetOrBranchProtection_provesProtectionSurface() {
        val snapshot = buildGovernanceControlCenterSnapshot(
            RepositoryGovernanceUiState(
                selectedRepository = repository(),
                observed = observed(branchProtected = null)
            )
        )

        assertEquals(GovernanceControlState.PASS, snapshot.protectionSurfaceState)
        assertEquals(GovernanceControlState.TOKEN_VAZIO, snapshot.branchProtectionState)
        assertEquals(GovernanceControlState.PASS, snapshot.rulesetState)
    }

    @Test
    fun emptyRulesetAndForbiddenBranchProtection_remainTokenVazio() {
        val snapshot = buildGovernanceControlCenterSnapshot(
            RepositoryGovernanceUiState(
                selectedRepository = repository(),
                observed = observed(branchProtected = null, rulesets = emptyList())
            )
        )

        assertEquals(GovernanceControlState.TOKEN_VAZIO, snapshot.protectionSurfaceState)
        assertEquals(GovernanceControlState.FAIL, snapshot.rulesetState)
        assertEquals(GovernanceControlState.TOKEN_VAZIO, snapshot.branchProtectionState)
    }

    @Test
    fun strictRequiredContextAndReadOnlyActions_passTheirOwnGates() {
        val snapshot = buildGovernanceControlCenterSnapshot(
            RepositoryGovernanceUiState(
                selectedRepository = repository(),
                observed = observed()
            )
        )

        assertEquals(GovernanceControlState.PASS, snapshot.requiredStatusChecksState)
        assertEquals(GovernanceControlState.PASS, snapshot.actionsAuthorityState)
        assertEquals(listOf("provider-protection"), snapshot.requiredStatusContexts)
    }

    @Test
    fun noRequiredStatusContext_failsOnlyStatusGate() {
        val snapshot = buildGovernanceControlCenterSnapshot(
            RepositoryGovernanceUiState(
                selectedRepository = repository(),
                observed = observed(contexts = emptyList())
            )
        )

        assertEquals(GovernanceControlState.PASS, snapshot.protectionSurfaceState)
        assertEquals(GovernanceControlState.FAIL, snapshot.requiredStatusChecksState)
    }

    @Test
    fun termuxRepository_hasAllowlistedProviderRouteOnly() {
        val route = canonicalProviderWorkflowRoute(
            "rafaelmeloreisnovo/termux-app-rafacodephi"
        )

        assertEquals("00_START_HERE.yml", route?.workflow)
        assertEquals("master", route?.ref)
        assertEquals("10_PROVIDER", route?.inputs?.get("route"))
        assertEquals("true", route?.inputs?.get("strict_governance"))
        assertEquals(
            null,
            canonicalProviderWorkflowRoute("rafaelmeloreisnovo/RafGitTools")
        )
    }

    @Test
    fun providerSettingsUrl_isDerivedOnlyFromSelectedRepository() {
        val snapshot = buildGovernanceControlCenterSnapshot(
            RepositoryGovernanceUiState(selectedRepository = repository())
        )

        assertTrue(
            snapshot.providerSettingsUrl!!.endsWith(
                "/rafaelmeloreisnovo/termux-app-rafacodephi/settings/branches"
            )
        )
    }
}
