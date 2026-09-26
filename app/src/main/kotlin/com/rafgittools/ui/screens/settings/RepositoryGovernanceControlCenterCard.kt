package com.rafgittools.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rafgittools.R
import com.rafgittools.data.github.GovernanceControlState

/**
 * Primary human control surface for provider governance.
 *
 * Every action delegates to an existing fail-closed ViewModel operation.
 * The card never promotes a provider denial or missing observation to PASS.
 */
@Composable
fun GovernanceControlCenterCard(
    snapshot: GovernanceControlCenterSnapshot,
    onRefresh: () -> Unit,
    onAudit: () -> Unit,
    onDryRun: () -> Unit,
    onApply: () -> Unit,
    onRollback: () -> Unit,
    onOpenProvider: () -> Unit,
    onDispatchProviderGate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null)
                Column {
                    Text(
                        text = stringResource(R.string.repo_governance_control_center),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = snapshot.repository
                            ?: stringResource(R.string.repo_governance_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            StatusRow(
                firstLabel = stringResource(R.string.repo_governance_surface),
                firstState = snapshot.protectionSurfaceState,
                secondLabel = stringResource(R.string.repo_governance_rulesets),
                secondState = snapshot.rulesetState
            )
            StatusRow(
                firstLabel = stringResource(R.string.repo_governance_required_checks),
                firstState = snapshot.requiredStatusChecksState,
                secondLabel = stringResource(R.string.repo_governance_actions_short),
                secondState = snapshot.actionsAuthorityState
            )

            Text(
                text = stringResource(
                    R.string.repo_governance_ruleset_count,
                    snapshot.rulesetCount?.toString() ?: "TOKEN_VAZIO",
                    snapshot.activeRulesetCount?.toString() ?: "TOKEN_VAZIO"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.repo_governance_status_contexts,
                    snapshot.requiredStatusContexts?.joinToString(", ")
                        ?.ifBlank { "[]" }
                        ?: "TOKEN_VAZIO"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = snapshot.canAudit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(stringResource(R.string.repo_governance_action_refresh))
                }
                OutlinedButton(
                    onClick = onAudit,
                    enabled = snapshot.canAudit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.repo_governance_action_audit))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDryRun,
                    enabled = snapshot.canDryRun,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.repo_governance_action_dry_run))
                }
                Button(
                    onClick = onApply,
                    enabled = snapshot.canApply,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.repo_governance_action_apply))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRollback,
                    enabled = snapshot.canRollback,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Text(stringResource(R.string.repo_governance_action_rollback))
                }
                OutlinedButton(
                    onClick = onOpenProvider,
                    enabled = snapshot.providerSettingsUrl != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.repo_governance_action_provider))
                }
            }

            Text(
                text = stringResource(
                    R.string.repo_governance_dispatch_state,
                    snapshot.providerDispatchState.name
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onDispatchProviderGate,
                enabled = snapshot.providerWorkflowRoute != null &&
                    snapshot.providerDispatchState != ProviderDispatchState.DISPATCHING,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (snapshot.providerWorkflowRoute != null) {
                        stringResource(
                            R.string.repo_governance_action_dispatch,
                            snapshot.providerWorkflowRoute.label
                        )
                    } else {
                        stringResource(R.string.repo_governance_action_dispatch_unavailable)
                    }
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    firstLabel: String,
    firstState: GovernanceControlState,
    secondLabel: String,
    secondState: GovernanceControlState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatusChip(firstLabel, firstState, Modifier.weight(1f))
        StatusChip(secondLabel, secondState, Modifier.weight(1f))
    }
}

@Composable
private fun StatusChip(
    label: String,
    state: GovernanceControlState,
    modifier: Modifier = Modifier
) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        enabled = false,
        label = { Text(label + ": " + state.name) }
    )
}
