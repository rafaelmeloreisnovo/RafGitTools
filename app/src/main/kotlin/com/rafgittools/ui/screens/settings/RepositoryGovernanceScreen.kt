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
import androidx.compose.material3.Divider
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

/**
 * Provider-bound repository governance screen.
 *
 * This screen deliberately distinguishes observed provider state from staged desired state.
 * Nothing is promoted to applied until the provider accepts the mutation and a re-probe runs.
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

            item {
                GovernanceEvidenceCard(state)
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

                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_issues),
                        subtitle = observedLabel(observed.details.hasIssues),
                        checked = desired.hasIssues,
                        dirty = GovernanceField.HAS_ISSUES in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.HAS_ISSUES, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_projects),
                        subtitle = observedLabel(observed.details.hasProjects),
                        checked = desired.hasProjects,
                        dirty = GovernanceField.HAS_PROJECTS in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.HAS_PROJECTS, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_wiki),
                        subtitle = observedLabel(observed.details.hasWiki),
                        checked = desired.hasWiki,
                        dirty = GovernanceField.HAS_WIKI in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.HAS_WIKI, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_discussions),
                        subtitle = observedLabel(observed.details.hasDiscussions),
                        checked = desired.hasDiscussions,
                        dirty = GovernanceField.HAS_DISCUSSIONS in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.HAS_DISCUSSIONS, it) }
                    )
                }

                item { Divider() }

                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_merge_commit),
                        subtitle = observedLabel(observed.details.allowMergeCommit),
                        checked = desired.allowMergeCommit,
                        dirty = GovernanceField.ALLOW_MERGE_COMMIT in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.ALLOW_MERGE_COMMIT, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_squash_merge),
                        subtitle = observedLabel(observed.details.allowSquashMerge),
                        checked = desired.allowSquashMerge,
                        dirty = GovernanceField.ALLOW_SQUASH_MERGE in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.ALLOW_SQUASH_MERGE, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_rebase_merge),
                        subtitle = observedLabel(observed.details.allowRebaseMerge),
                        checked = desired.allowRebaseMerge,
                        dirty = GovernanceField.ALLOW_REBASE_MERGE in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.ALLOW_REBASE_MERGE, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_delete_branch),
                        subtitle = observedLabel(observed.details.deleteBranchOnMerge),
                        checked = desired.deleteBranchOnMerge,
                        dirty = GovernanceField.DELETE_BRANCH_ON_MERGE in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.DELETE_BRANCH_ON_MERGE, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_signoff),
                        subtitle = observedLabel(observed.details.webCommitSignoffRequired),
                        checked = desired.webCommitSignoffRequired,
                        dirty = GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.WEB_COMMIT_SIGNOFF_REQUIRED, it) }
                    )
                }

                item {
                    SectionHeader(
                        icon = Icons.Default.Security,
                        title = stringResource(R.string.repo_governance_security)
                    )
                }

                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_branch_protection),
                        subtitle = observedLabel(observed.branchProtectionEnabled),
                        checked = desired.branchProtectionEnabled,
                        dirty = GovernanceField.BRANCH_PROTECTION in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.BRANCH_PROTECTION, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_vulnerability_alerts),
                        subtitle = observedLabel(observed.vulnerabilityAlertsEnabled),
                        checked = desired.vulnerabilityAlertsEnabled,
                        dirty = GovernanceField.VULNERABILITY_ALERTS in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.VULNERABILITY_ALERTS, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_dependabot),
                        subtitle = observedLabel(observed.automatedSecurityFixesEnabled),
                        checked = desired.automatedSecurityFixesEnabled,
                        dirty = GovernanceField.AUTOMATED_SECURITY_FIXES in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.AUTOMATED_SECURITY_FIXES, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_advanced_security),
                        subtitle = observedLabel(observed.advancedSecurityEnabled),
                        checked = desired.advancedSecurityEnabled,
                        dirty = GovernanceField.ADVANCED_SECURITY in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.ADVANCED_SECURITY, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_secret_scanning),
                        subtitle = observedLabel(observed.secretScanningEnabled),
                        checked = desired.secretScanningEnabled,
                        dirty = GovernanceField.SECRET_SCANNING in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.SECRET_SCANNING, it) }
                    )
                }
                item {
                    GovernanceSwitchRow(
                        title = stringResource(R.string.repo_governance_push_protection),
                        subtitle = observedLabel(observed.secretScanningPushProtectionEnabled),
                        checked = desired.secretScanningPushProtectionEnabled,
                        dirty = GovernanceField.SECRET_SCANNING_PUSH_PROTECTION in state.dirtyFields,
                        onCheckedChange = { viewModel.setField(GovernanceField.SECRET_SCANNING_PUSH_PROTECTION, it) }
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::useRecommendedBaseline,
                            enabled = state.selectedRepository?.permissions?.admin == true &&
                                state.evidenceState != GovernanceEvidenceState.APPLYING,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.repo_governance_baseline))
                        }
                        OutlinedButton(
                            onClick = viewModel::discardChanges,
                            enabled = state.dirtyFields.isNotEmpty() &&
                                state.evidenceState != GovernanceEvidenceState.APPLYING,
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
                        Text(
                            stringResource(
                                R.string.repo_governance_apply_count,
                                state.dirtyFields.size
                            )
                        )
                    }
                }

                if (state.lastReceiptId != null) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    stringResource(R.string.repo_governance_receipt, state.lastReceiptId),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                state.receiptPath?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
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
