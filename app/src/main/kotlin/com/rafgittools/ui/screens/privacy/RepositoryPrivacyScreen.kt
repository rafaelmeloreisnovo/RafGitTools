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
import com.rafgittools.data.privacy.PrivacyProvenanceState
import com.rafgittools.data.privacy.RepositoryPrivacyCandidate
import com.rafgittools.data.privacy.PrivacyVisibilityTarget

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
                    onSelectOwner = viewModel::selectOwner,
                    onSelectTarget = viewModel::selectTarget,
                    onSelectAll = viewModel::selectAllEligible,
                    onClear = viewModel::clearSelection,
                    onExecute = { showConfirmation = true }
                )
            }
        }
    }

    if (showConfirmation) {
        PrivacyConfirmationDialog(
            selected = state.candidates.filter {
                it.id in state.selectedIds && it.eligibleFor(state.targetVisibility) &&
                    (state.selectedOwner == null || it.ownerLogin == state.selectedOwner)
            },
            target = state.targetVisibility,
            executing = state.executing,
            onDismiss = { if (!state.executing) showConfirmation = false },
            onConfirm = { phrase ->
                viewModel.executeChangeVisibility(phrase)
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
    onSelectOwner: (String?) -> Unit,
    onSelectTarget: (PrivacyVisibilityTarget) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onExecute: () -> Unit
) {
    val visible = state.candidates.filter {
        state.selectedOwner == null || it.ownerLogin == state.selectedOwner
    }
    val eligible = visible.count { it.eligibleFor(state.targetVisibility) }
    val alreadyTarget = visible.count { it.currentVisibility == state.targetVisibility.apiValue }
    val blocked = visible.size - eligible - alreadyTarget
    val owners = visible.groupBy { it.ownerLogin }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
    val ownerOptions = state.candidates.map { it.ownerLogin }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    var ownerMenuExpanded by remember { mutableStateOf(false) }
    var targetMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repositórios pessoais e organizações", style = MaterialTheme.typography.titleMedium)
                    Text("Mostrando ${visible.size} de ${state.candidates.size} · elegíveis=$eligible · bloqueados=$blocked")
                    Text("1. Selecione conta/organização", fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(
                            onClick = { ownerMenuExpanded = true },
                            enabled = !state.executing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(state.selectedOwner ?: "Todas as contas / organizações", modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = ownerMenuExpanded, onDismissRequest = { ownerMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Todas as contas / organizações") },
                                onClick = { onSelectOwner(null); ownerMenuExpanded = false }
                            )
                            ownerOptions.forEach { owner ->
                                DropdownMenuItem(
                                    text = { Text(owner) },
                                    onClick = { onSelectOwner(owner); ownerMenuExpanded = false }
                                )
                            }
                        }
                    }
                    Text("2. Visibilidade desejada", fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(
                            onClick = { targetMenuExpanded = true },
                            enabled = !state.executing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (state.targetVisibility == PrivacyVisibilityTarget.PRIVATE) "Privado" else "Público",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = targetMenuExpanded, onDismissRequest = { targetMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Privado — restringir acesso") },
                                onClick = { onSelectTarget(PrivacyVisibilityTarget.PRIVATE); targetMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Público — conteúdo acessível a todos") },
                                onClick = { onSelectTarget(PrivacyVisibilityTarget.PUBLIC); targetMenuExpanded = false }
                            )
                        }
                    }
                    Text(
                        "Only GitHub-confirmed admin repositories are eligible. Forks are always blocked. " +
                            "Every PATCH is preceded by a fresh live preflight and durable receipt checkpoint.",
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
                        Icon(
                            if (state.targetVisibility == PrivacyVisibilityTarget.PRIVATE) Icons.Default.Lock else Icons.Default.Public,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.targetVisibility == PrivacyVisibilityTarget.PRIVATE)
                                "Tornar privados (${state.selectedIds.size})"
                            else "Tornar públicos (${state.selectedIds.size})"
                        )
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
                val provenanceHealthy = result.provenanceState == PrivacyProvenanceState.DURABLE ||
                    result.provenanceState == PrivacyProvenanceState.DURABLE_RECOVERED
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Last receipt", fontWeight = FontWeight.SemiBold)
                        Text(
                            "phase=${result.receipt.phase} updated=${result.receipt.updated} " +
                                "failed=${result.receipt.failed} skipped=${result.receipt.skipped} " +
                                "not_attempted=${result.receipt.notAttempted} attempting=${result.receipt.attempting}"
                        )
                        Text(
                            "provenance=${result.provenanceState}: ${result.provenanceMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (provenanceHealthy) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Text(
                            result.receiptPath ?: "TOKEN_VAZIO: no durable receipt checkpoint available",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        owners.forEach { (owner, repos) ->
            val ownerEligible = repos.filter { it.eligibleFor(state.targetVisibility) }
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
                RepositoryRow(repo, repo.id in state.selectedIds, state.executing, state.targetVisibility) {
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
    target: PrivacyVisibilityTarget,
    onToggle: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                enabled = candidate.eligibleFor(target) && !executing
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(candidate.name, style = MaterialTheme.typography.titleSmall)
                Text(candidate.fullName, style = MaterialTheme.typography.bodySmall)
                Text(
                    candidate.blockReasonFor(target) ?: "Elegível · admin confirmado",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (candidate.eligibleFor(target)) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                candidate.isPrivate -> Icon(Icons.Default.Lock, contentDescription = "Private")
                candidate.hasPublicImpactWarning && candidate.eligibleFor(target) ->
                    Icon(Icons.Default.Warning, contentDescription = "Impact warning")
            }
        }
    }
}

@Composable
private fun PrivacyConfirmationDialog(
    selected: List<RepositoryPrivacyCandidate>,
    target: PrivacyVisibilityTarget,
    executing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var confirmation by remember(selected.size, target) { mutableStateOf("") }
    val phrase = RepositoryPrivacyViewModel.confirmationPhrase(selected.size, target)
    val pages = selected.count { it.hasPages }
    val social = selected.count { it.stars > 0 || it.watchers > 0 }
    val forks = selected.count { it.forks > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Confirm visibility change") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Alterar ${selected.size} repositórios para ${target.apiValue}.")
                Text(
                    if (target == PrivacyVisibilityTarget.PRIVATE)
                        "Privatizar pode afetar estrelas, forks, Pages e recursos do plano."
                    else
                        "ATENÇÃO: tornar público permite que qualquer pessoa leia, copie e indexe código, histórico e arquivos. Revise segredos e dados pessoais ANTES de confirmar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (target == PrivacyVisibilityTarget.PUBLIC) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("impact: pages=$pages stars/watchers=$social forks=$forks")
                Text("A live GitHub preflight and durable journal checkpoint run before each PATCH.")
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
            ) { Text(if (target == PrivacyVisibilityTarget.PRIVATE) "Tornar privado" else "Tornar público") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !executing) { Text("Cancel") }
        }
    )
}
