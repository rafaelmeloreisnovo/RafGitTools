package com.rafgittools.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.R
import com.rafgittools.data.github.GovernanceAuditControl
import com.rafgittools.data.github.GovernanceControlState

/**
 * Provider-bound repository governance control center.
 *
 * Configuration, enforcement evidence and audit state are deliberately separated.
 * Nothing is promoted to applied until the provider accepts a mutation and a re-probe runs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryGovernanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: RepositoryGovernanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var repositoryMenuExpanded by remember { mutableStateOf(false) }
    var showApplyConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.repo_governance_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshSelected,
                        enabled = state.selectedRepository != null &&
                            state.evidenceState != GovernanceEvidenceState.APPLYING
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.repo_governance_refresh)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.repo_governance_contract),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                RepositorySelector(
                    state = state,
                    expanded = repositoryMenuExpanded,
                    onExpandedChange = { repositoryMenuExpanded = it },
                    onSelect = {
                        repositoryMenuExpanded = false
                        viewModel.selectRepository(it)
                    }
                )
            }

            item { GovernanceEvidenceCard(state) }

            item {
                OutlinedButton(
                    onClick = viewModel::runDeepAudit,
                    enabled = state.selectedRepository != null &&
                        !state.isAuditing &&
                        state.evidenceState != GovernanceEvidenceState.APPLYING,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isAuditing) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(R.string.repo_governance_deep_audit))
                }
            }

            state.auditReport?.let { report ->
                item { GovernanceAuditSummaryCard(state) }
                report.controls.forEach { control ->
                    item { GovernanceAuditControlRow(control) }
                }
            }

            if (state.isLoadingRepository) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            val desired = state.desired
            val observed = state.observed
            if (desired != null && observed != null) {
                item {
                    SectionHeader(
                        icon = Icons.Default.SettingsSuggest,
                        title = stringResource(R.string.repo_governance_structure)
                    )
                }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_issues), observedLabel(observed.details.hasIssues), desired.hasIssues, GovernanceField.HAS_ISSUES in state.dirtyFields) { viewModel.setField(GovernanceField.HAS_ISSUES, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_projects), observedLabel(observed.details.hasProjects), desired.hasProjects, GovernanceField.HAS_PROJECTS in state.dirtyFields) { viewModel.setField(GovernanceField.HAS_PROJECTS, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_wiki), observedLabel(observed.details.hasWiki), desired.hasWiki, GovernanceField.HAS_WIKI in state.dirtyFields) { viewModel.setField(GovernanceField.HAS_WIKI, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_discussions), observedLabel(observed.details.hasDiscussions), desired.hasDiscussions, GovernanceField.HAS_DISCUSSIONS in state.dirtyFields) { viewModel.setField(GovernanceField.HAS_DISCUSSIONS, it) } }

                item {
                    SectionHeader(
                        icon = Icons.Default.SettingsSuggest,
                        title = stringResource(R.string.repo_governance_merge_policy)
                    )
                }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_merge_commit), observedLabel(observed.details.allowMergeCommit), desired.allowMergeCommit, GovernanceField.ALLOW_MERGE_COMMIT in state.dirtyFields) { viewModel.setField(GovernanceField.ALLOW_MERGE_COMMIT, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_squash_merge), observedLabel(observed.details.allowSquashMerge), desired.allowSquashMerge, GovernanceField.ALLOW_SQUASH_MERGE in state.dirtyFields) { viewModel.setField(GovernanceField.ALLOW_SQUASH_MERGE, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_rebase_merge), observedLabel(observed.details.allowRebaseMerge), desired.allowRebaseMerge, GovernanceField.ALLOW_REBASE_MERGE in state.dirtyFields) { viewModel.setField(GovernanceField.ALLOW_REBASE_MERGE, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_auto_merge), observedLabel(observed.details.allowAutoMerge), desired.allowAutoMerge, GovernanceField.ALLOW_AUTO_MERGE in state.dirtyFields) { viewModel.setField(GovernanceField.ALLOW_AUTO_MERGE, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_update_branch), observedLabel(observed.details.allowUpdateBranch), desired.allowUpdateBranch, GovernanceField.ALLOW_UPDATE_BRANCH in state.dirtyFields) { viewModel.setField(GovernanceField.ALLOW_UPDATE_BRANCH, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_delete_branch), observedLabel(observed.details.deleteBranchOnMerge), desired.deleteBranchOnMerge, GovernanceField.DELETE_BRANCH_ON_MERGE in state.dirtyFields) { viewModel.setField(GovernanceField.DELETE_BRANCH_ON_MERGE, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_signoff), observedLabel(observed.details.webCommitSignoffRequired), desired.webCommitSignoffRequired, GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED in state.dirtyFields) { viewModel.setField(GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED, it) } }

                item {
                    SectionHeader(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.repo_governance_security)
                    )
                }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_branch_protection), observedLabel(observed.branchProtectionEnabled), desired.branchProtectionEnabled, GovernanceField.BRANCH_PROTECTION in state.dirtyFields) { viewModel.setField(GovernanceField.BRANCH_PROTECTION, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_vulnerability_alerts), observedLabel(observed.vulnerabilityAlertsEnabled), desired.vulnerabilityAlertsEnabled, GovernanceField.VULNERABILITY_ALERTS in state.dirtyFields) { viewModel.setField(GovernanceField.VULNERABILITY_ALERTS, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_dependabot), observedLabel(observed.automatedSecurityFixesEnabled), desired.automatedSecurityFixesEnabled, GovernanceField.AUTOMATED_SECURITY_FIXES in state.dirtyFields) { viewModel.setField(GovernanceField.AUTOMATED_SECURITY_FIXES, it) } }
                if (!observed.details.isPrivate) {
                    item { GovernanceSwitchRow(stringResource(R.string.repo_governance_private_vulnerability), observedLabel(observed.privateVulnerabilityReportingEnabled), desired.privateVulnerabilityReportingEnabled, GovernanceField.PRIVATE_VULNERABILITY_REPORTING in state.dirtyFields) { viewModel.setField(GovernanceField.PRIVATE_VULNERABILITY_REPORTING, it) } }
                }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_advanced_security), observedLabel(observed.advancedSecurityEnabled), desired.advancedSecurityEnabled, GovernanceField.ADVANCED_SECURITY in state.dirtyFields) { viewModel.setField(GovernanceField.ADVANCED_SECURITY, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_secret_scanning), observedLabel(observed.secretScanningEnabled), desired.secretScanningEnabled, GovernanceField.SECRET_SCANNING in state.dirtyFields) { viewModel.setField(GovernanceField.SECRET_SCANNING, it) } }
                item { GovernanceSwitchRow(stringResource(R.string.repo_governance_push_protection), observedLabel(observed.secretScanningPushProtectionEnabled), desired.secretScanningPushProtectionEnabled, GovernanceField.SECRET_SCANNING_PUSH_PROTECTION in state.dirtyFields) { viewModel.setField(GovernanceField.SECRET_SCANNING_PUSH_PROTECTION, it) } }

                item {
                    SectionHeader(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.repo_governance_actions)
                    )
                }
                item {
                    GovernanceSwitchRow(
                        stringResource(R.string.repo_governance_actions_read_only),
                        observedLabel(observed.workflowPermissions?.defaultWorkflowPermissions?.let { it == "read" }),
                        desired.actionsReadOnlyDefault,
                        GovernanceField.ACTIONS_READ_ONLY_DEFAULT in state.dirtyFields
                    ) { viewModel.setField(GovernanceField.ACTIONS_READ_ONLY_DEFAULT, it) }
                }
                item {
                    GovernanceSwitchRow(
                        stringResource(R.string.repo_governance_actions_approve_pr),
                        observedLabel(observed.workflowPermissions?.canApprovePullRequestReviews),
                        desired.actionsCanApprovePullRequests,
                        GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS in state.dirtyFields
                    ) { viewModel.setField(GovernanceField.ACTIONS_CAN_APPROVE_PULL_REQUESTS, it) }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::useRecommendedBaseline,
                            enabled = state.adminAuthorityProven && state.evidenceState != GovernanceEvidenceState.APPLYING,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.repo_governance_baseline))
                        }
                        OutlinedButton(
                            onClick = viewModel::discardChanges,
                            enabled = state.dirtyFields.isNotEmpty() && state.evidenceState != GovernanceEvidenceState.APPLYING,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.repo_governance_discard))
                        }
                    }
                }

                item {
                    Button(
                        onClick = { showApplyConfirmation = true },
                        enabled = state.canApply,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.repo_governance_apply_count, state.dirtyFields.size))
                    }
                }

                if (state.lastReceiptId != null) {
                    item { GovernanceReceiptCard(state) }
                }
            }
        }
    }

    if (showApplyConfirmation) {
        AlertDialog(
            onDismissRequest = { showApplyConfirmation = false },
            title = { Text(stringResource(R.string.repo_governance_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.repo_governance_confirm_body,
                        state.selectedRepository?.fullName.orEmpty(),
                        state.dirtyFields.size
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showApplyConfirmation = false
                        viewModel.applyChanges()
                    }
                ) {
                    Text(stringResource(R.string.repo_governance_confirm_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun RepositorySelector(
    state: RepositoryGovernanceUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !state.isLoadingRepositories) { onExpandedChange(true) }
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        state.selectedRepository?.fullName
                            ?: stringResource(R.string.repo_governance_select_repository)
                    )
                },
                supportingContent = {
                    val selected = state.selectedRepository
                    Text(
                        when {
                            state.isLoadingRepositories -> stringResource(R.string.repo_governance_loading)
                            selected == null -> stringResource(R.string.repo_governance_none)
                            selected.archived -> stringResource(R.string.repo_governance_archived)
                            selected.permissions?.admin == true -> stringResource(R.string.repo_governance_admin_proven)
                            else -> stringResource(R.string.repo_governance_admin_unproven)
                        }
                    )
                }
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            state.repositories.forEach { repository ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(repository.fullName)
                            Text(
                                if (repository.permissions?.admin == true) {
                                    stringResource(R.string.repo_governance_admin)
                                } else {
                                    stringResource(R.string.repo_governance_read_only)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSelect(repository.fullName) }
                )
            }
        }
    }
}

@Composable
private fun GovernanceEvidenceCard(state: RepositoryGovernanceUiState) {
    val icon = when (state.evidenceState) {
        GovernanceEvidenceState.OBSERVED,
        GovernanceEvidenceState.APPLIED -> Icons.Default.CheckCircle
        GovernanceEvidenceState.READY,
        GovernanceEvidenceState.APPLYING -> Icons.Default.SettingsSuggest
        GovernanceEvidenceState.FAILED,
        GovernanceEvidenceState.TOKEN_VAZIO -> Icons.Default.Warning
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Text(
                    text = state.evidenceState.name,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (state.dirtyFields.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Δ ${state.dirtyFields.size}") },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            state.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GovernanceAuditSummaryCard(state: RepositoryGovernanceUiState) {
    val report = state.auditReport ?: return
    val chain = state.receiptChainStatus
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.repo_governance_audit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(
                    R.string.repo_governance_audit_score,
                    report.scorePercent,
                    report.passCount,
                    report.failCount,
                    report.gapCount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            if (chain != null) {
                Text(
                    if (chain.valid) {
                        stringResource(R.string.repo_governance_chain_valid)
                    } else {
                        stringResource(R.string.repo_governance_chain_invalid)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    stringResource(
                        R.string.repo_governance_chain_records,
                        chain.chainedRecords,
                        chain.legacyRecords
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                chain.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun GovernanceAuditControlRow(control: GovernanceAuditControl) {
    val icon = when (control.state) {
        GovernanceControlState.PASS -> Icons.Default.CheckCircle
        GovernanceControlState.FAIL,
        GovernanceControlState.TOKEN_VAZIO -> Icons.Default.Warning
        GovernanceControlState.NOT_APPLICABLE -> Icons.Default.SettingsSuggest
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = { Icon(icon, contentDescription = null) },
            headlineContent = {
                Text("${control.id} · ${control.title}")
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${control.domain} · ${control.state.name}", style = MaterialTheme.typography.labelSmall)
                    Text(control.evidence, style = MaterialTheme.typography.bodySmall)
                    control.remediation?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

@Composable
private fun GovernanceReceiptCard(state: RepositoryGovernanceUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.lastReceiptId?.let {
                Text(
                    stringResource(R.string.repo_governance_receipt, it),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            state.receiptPath?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.receiptChainStatus?.headHash?.let {
                Text(
                    text = "SHA-256 head: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            title,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun GovernanceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    dirty: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                if (dirty) {
                    Text(
                        "  Δ",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        supportingContent = {
            Text(
                subtitle,
                color = if (subtitle.contains("TOKEN_VAZIO")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

private fun observedLabel(value: Boolean?): String = when (value) {
    true -> "OBSERVED: enabled"
    false -> "OBSERVED: disabled"
    null -> "TOKEN_VAZIO: provider state not proven"
}
