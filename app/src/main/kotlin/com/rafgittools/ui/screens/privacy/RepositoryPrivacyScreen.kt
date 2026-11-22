package com.rafgittools.ui.screens.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.benchmark.ContextualCreditsInfo
import com.rafgittools.benchmark.RafScreenCredits
import com.rafgittools.data.privacy.PrivacyProvenanceState
import com.rafgittools.data.privacy.PrivacyVisibilityTarget
import com.rafgittools.data.privacy.RepositoryPrivacyCandidate

/**
 * Wizard (not a settings spreadsheet):
 * 1. Owner/account  ->  2. Public/Private  ->  3. Automatic review.
 * Inventory discovery, eligibility and safe selection are automatic.
 * Only the final, explicitly confirmed step may trigger GitHub PATCH.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryPrivacyScreen(
    viewModel: RepositoryPrivacyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(0) }
    var confirmationOpen by remember { mutableStateOf(false) }
    var showExceptions by remember { mutableStateOf(false) }
    val selected = state.selectedCandidates()
    val selectedIds = selected.mapTo(linkedSetOf()) { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raf Privacy · Assistente") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    ContextualCreditsInfo(RafScreenCredits.privacy)
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !state.loading && !state.executing && !confirmationOpen
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar inventário")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.requiresAuthentication -> AuthenticationRequired(onNavigateToAuth, Modifier.align(Alignment.Center))
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Etapa ${step + 1} de 3", style = MaterialTheme.typography.labelLarge)
                        LinearProgressIndicator(
                            progress = (step + 1) / 3f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    when (step) {
                        0 -> item {
                            AccountWizardStep(
                                state = state,
                                onChooseOwner = viewModel::selectOwnerScope
                            )
                        }
                        1 -> item {
                            VisibilityWizardStep(
                                target = state.targetVisibility,
                                executing = state.executing,
                                onChooseTarget = viewModel::selectTarget
                            )
                        }
                        else -> {
                            item {
                                Card(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("3. Revisar plano automático", style = MaterialTheme.typography.titleLarge)
                                        val ownersLabel = state.selectedOwners?.joinToString() ?: "Todas as contas autorizadas"
                                        Text("Conta: $ownersLabel")
                                        Text("Origem: ${state.sourceVisibility.apiValue} → destino: ${state.targetVisibility.apiValue}")
                                        Text("${selected.size} repositório(s) elegível(is) preparado(s) automaticamente.")
                                        val blocked = state.candidates.count {
                                            state.ownerIncluded(it.ownerLogin) && !it.eligibleFor(state.targetVisibility)
                                        }
                                        Text("$blocked fora da operação (já no destino, sem permissão ou protegidos).",
                                            style = MaterialTheme.typography.bodySmall)
                                        if (state.targetVisibility == PrivacyVisibilityTarget.PUBLIC) {
                                            Text(
                                                "ATENÇÃO: publicar expõe arquivos, histórico e código. Confira privacidade, direitos e segredos antes de confirmar.",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(
                                            "Sem alterações ainda. O app verifica permissão e estado real no GitHub antes de cada mudança, com recibo durável.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            if (selected.isNotEmpty()) {
                                item {
                                    Text("Prévia (${selected.size})", fontWeight = FontWeight.SemiBold)
                                    selected.take(12).forEach { candidate ->
                                        Text("• ${candidate.fullName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (selected.size > 12) Text("… e mais ${selected.size - 12}",
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            item {
                                TextButton(
                                    onClick = { showExceptions = !showExceptions },
                                    enabled = !state.executing
                                ) {
                                    Text(if (showExceptions) "Ocultar ajustes avançados"
                                        else "Ajustar exceções (opcional)")
                                }
                                if (showExceptions) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = viewModel::useAutomaticSelection, enabled = !state.executing) {
                                            Text("Automático")
                                        }
                                        TextButton(onClick = viewModel::clearSelection, enabled = !state.executing) {
                                            Text("Nenhum")
                                        }
                                        TextButton(onClick = viewModel::selectAllEligible, enabled = !state.executing) {
                                            Text("Todos")
                                        }
                                    }
                                }
                            }
                            if (showExceptions) {
                                items(
                                    state.candidates.filter { state.isSelectable(it) },
                                    key = { "exception:${it.id}" }
                                ) { candidate ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = candidate.id in selectedIds,
                                            onCheckedChange = { viewModel.toggleRepository(candidate.id) },
                                            enabled = !state.executing
                                        )
                                        Text(candidate.fullName, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    state.error?.let { error ->
                        item {
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    state.lastResult?.let { result ->
                        item {
                            val durable = result.provenanceState == PrivacyProvenanceState.DURABLE ||
                                result.provenanceState == PrivacyProvenanceState.DURABLE_RECOVERED
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Resultado e recibo", style = MaterialTheme.typography.titleMedium)
                                    Text("Atualizados: ${result.receipt.updated} · Falhas: ${result.receipt.failed} · " +
                                        "Ignorados: ${result.receipt.skipped} · Pendentes: ${result.receipt.notAttempted}")
                                    Text(
                                        "Proveniência: ${result.provenanceState} · ${result.provenanceMessage}",
                                        color = if (durable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(result.receiptPath ?: "TOKEN_VAZIO: recibo durável não confirmado",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (step > 0) {
                                OutlinedButton(
                                    onClick = { step -= 1 },
                                    enabled = !state.executing
                                ) { Text("Voltar") }
                            }
                            Button(
                                onClick = {
                                    if (step < 2) step += 1 else confirmationOpen = true
                                },
                                enabled = !state.executing &&
                                    (step != 2 || selected.isNotEmpty()) &&
                                    (step != 0 || state.candidates.isNotEmpty()),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    if (step < 2) "Continuar"
                                    else "Revisar e confirmar ${selected.size}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmationOpen && !state.loading && selected.isNotEmpty()) {
        // Exact IDs and target are bound to what the user reviewed.
        val confirmedIds = selected.mapTo(linkedSetOf()) { it.id }
        val confirmedTarget = state.targetVisibility
        PrivacyConfirmationDialog(
            selected = selected,
            target = confirmedTarget,
            executing = state.executing,
            onDismiss = { if (!state.executing) confirmationOpen = false },
            onConfirm = {
                confirmationOpen = false
                viewModel.executeChangeVisibility(
                    confirmation = RepositoryPrivacyViewModel.confirmationPhrase(confirmedIds.size, confirmedTarget),
                    expectedRepositoryIds = confirmedIds,
                    expectedTarget = confirmedTarget
                )
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
        Text("Conecte sua conta GitHub", style = MaterialTheme.typography.titleLarge)
        Text("O assistente carrega somente repositórios visíveis à sua autenticação.")
        Button(onClick = onNavigateToAuth) { Text("Entrar") }
    }
}

@Composable
private fun AccountWizardStep(
    state: RepositoryPrivacyUiState,
    onChooseOwner: (String?) -> Unit
) {
    val owners = state.candidates.map { it.ownerLogin }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("1. Onde aplicar?", style = MaterialTheme.typography.titleLarge)
            Text("Suas contas e organizações aparecem automaticamente.")
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    enabled = !state.executing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(state.selectedOwners?.singleOrNull() ?: "Todas minhas contas e organizações",
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Todas minhas contas e organizações") },
                        onClick = { onChooseOwner(null); expanded = false }
                    )
                    owners.forEach { owner ->
                        DropdownMenuItem(
                            text = { Text(owner) },
                            onClick = { onChooseOwner(owner); expanded = false }
                        )
                    }
                }
            }
            Text(
                "${state.candidates.count { state.ownerIncluded(it.ownerLogin) }} repositórios encontrados. " +
                    "Nenhum repositório será alterado sem sua confirmação.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun VisibilityWizardStep(
    target: PrivacyVisibilityTarget,
    executing: Boolean,
    onChooseTarget: (PrivacyVisibilityTarget) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("2. Como quer deixar?", style = MaterialTheme.typography.titleLarge)
            Text("O assistente identifica automaticamente a origem; você escolhe apenas o destino.")
            Button(
                onClick = { onChooseTarget(PrivacyVisibilityTarget.PRIVATE) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !executing,
                colors = if (target == PrivacyVisibilityTarget.PRIVATE)
                    ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Privado · restringir acesso")
            }
            OutlinedButton(
                onClick = { onChooseTarget(PrivacyVisibilityTarget.PUBLIC) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !executing
            ) {
                Icon(Icons.Default.Public, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Público · permitir acesso a todos")
            }
            Text(
                if (target == PrivacyVisibilityTarget.PRIVATE)
                    "Selecionado: Público → Privado"
                else "Selecionado: Privado → Público (exposição de código, histórico e arquivos)",
                color = if (target == PrivacyVisibilityTarget.PUBLIC)
                    MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyConfirmationDialog(
    selected: List<RepositoryPrivacyCandidate>,
    target: PrivacyVisibilityTarget,
    executing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val pages = selected.count { it.hasPages }
    val social = selected.count { it.stars > 0 || it.watchers > 0 || it.forks > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = { Text("Confirmar ${selected.size} repositórios") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (target == PrivacyVisibilityTarget.PRIVATE)
                        "Tornar privados ${selected.size} repositórios pode afetar Pages, forks e outros usuários."
                    else "ATENÇÃO: tornar públicos ${selected.size} repositórios expõe código, arquivos e histórico para qualquer pessoa.",
                    color = if (target == PrivacyVisibilityTarget.PUBLIC)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Impactos detectados: Pages=$pages · atividade/forks=$social")
                selected.take(5).forEach { Text("• ${it.fullName}", style = MaterialTheme.typography.bodySmall) }
                if (selected.size > 5) Text("… mais ${selected.size - 5}")
                Text("Confirmação manual desta operação. Verificação GitHub por item e recibo antes de cada PATCH.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !executing && selected.isNotEmpty()) {
                Text(if (target == PrivacyVisibilityTarget.PRIVATE) "Sim, tornar privado" else "Sim, tornar público")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !executing) { Text("Cancelar") }
        }
    )
}
