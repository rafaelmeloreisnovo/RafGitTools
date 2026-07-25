package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceEditorScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkspaceEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RafGitFS Workspace")
                        Text(
                            "${viewModel.repositoryFullName}@${viewModel.refName}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return to virtual repository")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RafGitFsStatusBanner(state.status)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Private workspace", style = MaterialTheme.typography.titleMedium)
                        Text(
                            state.workspace?.workspaceId?.take(12) ?: "TOKEN_VAZIO",
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "State: ${state.workspace?.state ?: "creating"} · claim_allowed=false",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "GitHub writes are limited to a generated rafgitfs/* branch and a draft pull request.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stage a text file", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.path,
                            onValueChange = viewModel::setPath,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Repository path") },
                            supportingText = { Text("Example: docs/architecture.md · .git paths are blocked") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.baseBlobSha,
                            onValueChange = viewModel::setBaseBlobSha,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Observed base blob SHA (blank for new file)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.content,
                            onValueChange = viewModel::setContent,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("UTF-8 content") },
                            minLines = 6
                        )
                        Button(
                            onClick = viewModel::stageFile,
                            enabled = !state.busy && state.workspace != null && state.path.isNotBlank()
                        ) {
                            Text("Stage locally")
                        }
                    }
                }
            }
            item {
                Text("Staged files (${state.stagedFiles.size})", style = MaterialTheme.typography.titleMedium)
            }
            if (state.stagedFiles.isEmpty()) {
                item {
                    RafGitFsEmptyState(
                        title = "Nothing staged",
                        detail = "No branch, commit, push or pull request can occur without staged content."
                    )
                }
            } else {
                items(state.stagedFiles, key = { it.operationId }) { operation ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(operation.path ?: "TOKEN_VAZIO", fontFamily = FontFamily.Monospace)
                                Text(
                                    "base ${operation.baseSha?.take(12) ?: "new"} · local ${operation.localSha?.take(12) ?: "TOKEN_VAZIO"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = { viewModel.removeStaged(operation.operationId) },
                                enabled = !state.busy
                            ) { Text("Undo local") }
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = viewModel::preparePlan,
                    enabled = !state.busy && state.stagedFiles.isNotEmpty()
                ) {
                    Text("Generate plan and dry-run")
                }
            }
            state.plan?.let { plan ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Exact governed plan", style = MaterialTheme.typography.titleMedium)
                            Text("Hash ${plan.planHash}", fontFamily = FontFamily.Monospace)
                            Text("Base commit ${plan.baseCommitSha ?: "TOKEN_VAZIO"}")
                            Text("Conflicts ${plan.conflicts.size} · steps ${plan.steps.size}")
                            plan.steps.forEach { step ->
                                Text(
                                    "${step.order}. ${step.action} · ${if (step.executableNow) "ready" else "blocked"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                if (state.conflicts.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Conflicts block execution", style = MaterialTheme.typography.titleMedium)
                                state.conflicts.filter { it.resolvedAt == null }.forEach { conflict ->
                                    Text("${conflict.path}: ${conflict.conflictState}")
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.approvalText,
                        onValueChange = viewModel::setApprovalText,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Exact approval") },
                        supportingText = { Text(state.expectedApproval ?: "TOKEN_VAZIO") },
                        singleLine = true
                    )
                }
                item {
                    Button(
                        onClick = viewModel::approveAndPublish,
                        enabled = !state.busy && plan.conflicts.isEmpty() && state.approvalText == state.expectedApproval
                    ) {
                        Text("Approve branch + commit + push + draft PR")
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.rollbackText,
                        onValueChange = viewModel::setRollbackText,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rollback confirmation") },
                        supportingText = { Text(state.expectedRollback ?: "TOKEN_VAZIO") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::rollbackPublishedBranch,
                        enabled = !state.busy && state.rollbackText == state.expectedRollback
                    ) {
                        Text("Create rollback commit on RafGitFS branch")
                    }
                }
            }
            if (state.dryRun.isNotEmpty()) {
                item {
                    Text("Latest outcomes", style = MaterialTheme.typography.titleMedium)
                }
                items(state.dryRun) { outcome ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${outcome.step.action}: ${outcome.result}")
                            Text(
                                "evidence=${outcome.evidenceState} · ${outcome.detail.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
