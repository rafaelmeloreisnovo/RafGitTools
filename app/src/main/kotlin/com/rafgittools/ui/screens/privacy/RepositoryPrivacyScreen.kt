package com.rafgittools.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.data.privacy.RepositoryPrivacyCandidate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryPrivacyScreen(
    viewModel: RepositoryPrivacyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raf Privacy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.executing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.requiresAuthentication -> AuthenticationRequired(
                    onNavigateToAuth,
                    Modifier.align(Alignment.Center)
                )
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> PrivacyInventory(
                    state = state,
                    onToggleRepository = viewModel::toggleRepository,
                    onToggleOwner = viewModel::toggleOwner,
                    onSelectAll = viewModel::selectAllEligible,
                    onClear = viewModel::clearSelection,
                    onExecute = { showConfirmation = true }
                )
            }
        }
    }

    if (showConfirmation) {
        PrivacyConfirmationDialog(
            selected = state.candidates.filter { it.id in state.selectedIds && it.eligible },
            executing = state.executing,
            onDismiss = { if (!state.executing) showConfirmation = false },
            onConfirm = { phrase ->
                viewModel.executeMakePrivate(phrase)
                showConfirmation = false
            }
        )
    }
}

@Composable
private fun AuthenticationRequired(onNavigateToAuth: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.Security, contentDescription = null)
        Text("GitHub authentication required", style = MaterialTheme.typography.titleLarge)
        Text("Use the existing RafGitTools OAuth/PAT login, then return here.")
        Button(onClick = onNavigateToAuth) { Text("Sign in") }
    }
}

@Composable
private fun PrivacyInventory(
    state: RepositoryPrivacyUiState,
    onToggleRepository: (Long) -> Unit,
    onToggleOwner: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onExecute: () -> Unit
) {
    val eligible = state.candidates.count { it.eligible }
    val privateCount = state.candidates.count { it.isPrivate || it.currentVisibility == "private" }
    val blocked = state.candidates.size - eligible - privateCount
    val owners = state.candidates.groupBy { it.ownerLogin }.toSortedMap(String.CASE_INSENSITIVE_ORDER)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Personal + organization repositories", style = MaterialTheme.typography.titleMedium)
                    Text("inventory=${state.candidates.size} eligible=$eligible private=$privateCount blocked=$blocked")
                    Text(
                        "Only GitHub-confirmed admin repositories are eligible. Forks are always blocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSelectAll, enabled = eligible > 0 && !state.executing) {
                            Text("Select all eligible")
                        }
                        TextButton(onClick = onClear, enabled = state.selectedIds.isNotEmpty() && !state.executing) {
                            Text("Clear")
                        }
                    }
                    Button(
                        onClick = onExecute,
                        enabled = state.selectedIds.isNotEmpty() && !state.executing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Make selected private (${state.selectedIds.size})")
                    }
                }
            }
        }

        state.error?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        state.lastResult?.let { result ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Last receipt", fontWeight = FontWeight.SemiBold)
                        Text(
                            "updated=${result.receipt.updated} failed=${result.receipt.failed} " +
                                "skipped=${result.receipt.skipped} not_attempted=${result.receipt.notAttempted}"
                        )
                        Text(
                            result.receiptPath ?: "TOKEN_VAZIO: receipt persistence failed",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        owners.forEach { (owner, repos) ->
            val ownerEligible = repos.filter { it.eligible }
            val ownerSelected = ownerEligible.isNotEmpty() && ownerEligible.all { it.id in state.selectedIds }
            item(key = "owner:$owner") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (repos.firstOrNull()?.organizationOwned == true) Icons.Default.Business else Icons.Default.Person,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(owner, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = ownerSelected,
                        onCheckedChange = { onToggleOwner(owner) },
                        enabled = ownerEligible.isNotEmpty() && !state.executing
                    )
                }
            }
            items(repos, key = { it.id }) { repo ->
                RepositoryRow(repo, repo.id in state.selectedIds, state.executing) {
                    onToggleRepository(repo.id)
                }
            }
            item(key = "divider:$owner") { HorizontalDivider() }
        }
    }
}

@Composable
private fun RepositoryRow(
    candidate: RepositoryPrivacyCandidate,
    selected: Boolean,
    executing: Boolean,
    onToggle: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = candidate.eligible && !executing
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(candidate.name, style = MaterialTheme.typography.titleSmall)
                Text(candidate.fullName, style = MaterialTheme.typography.bodySmall)
                Text(
                    candidate.blockReason ?: "Eligible · admin confirmed",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (candidate.eligible) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                candidate.isPrivate -> Icon(Icons.Default.Lock, contentDescription = "Private")
                candidate.hasPublicImpactWarning && candidate.eligible ->
                    Icon(Icons.Default.Warning, contentDescription = "Impact warning")
            }
        }
    }
}

@Composable
private fun PrivacyConfirmationDialog(
    selected: List<RepositoryPrivacyCandidate>,
    executing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var confirmation by remember(selected.size) { mutableStateOf("") }
    val phrase = RepositoryPrivacyViewModel.confirmationPhrase(selected.size)
    val pages = selected.count { it.hasPages }
    val social = selected.count { it.stars > 0 || it.watchers > 0 }
    val forks = selected.count { it.forks > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Confirm visibility change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Make ${selected.size} repositories private.")
                Text(
                    "GitHub may erase stars/watchers, detach existing public forks, and unpublish Pages depending on plan.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("impact: pages=$pages stars/watchers=$social forks=$forks")
                Text("Type exactly: $phrase", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    singleLine = true,
                    enabled = !executing,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(confirmation) },
                enabled = !executing && confirmation.trim() == phrase
            ) { Text("Make private") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !executing) { Text("Cancel") }
        }
    )
}
