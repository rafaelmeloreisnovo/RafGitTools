package com.rafgittools.ui.screens.actions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.data.github.ActionsControlPolicy
import com.rafgittools.data.github.ActionsRun
import com.rafgittools.data.github.ActionsWorkflow

private data class PendingAction(
    val operation: ActionsMutation,
    val id: Long,
    val label: String,
    val repository: String,
    val reference: String
) {
    val phrase: String get() = when (operation) {
        ActionsMutation.DISPATCH -> "DISPARAR $id"
        ActionsMutation.CANCEL -> "CANCELAR $id"
        ActionsMutation.RERUN -> "REEXECUTAR $id"
        ActionsMutation.RERUN_FAILED -> "REEXECUTAR FALHOS $id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsControlScreen(
    onBack: () -> Unit,
    onAuthentication: () -> Unit,
    viewModel: ActionsControlViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var repoMenu by remember { mutableStateOf(false) }
    var inputs by remember { mutableStateOf("{}") }
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raf Actions · Workflows") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
                actions = {
                    TextButton(onClick = viewModel::refresh, enabled = !state.busy) { Text("Atualizar") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Painel de controle GitHub Actions", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "A sessão GitHub do RafGitTools executa as solicitações. " +
                                "Segredos PAT_ACTIONS dos runners CI não entram no APK. " +
                                "Cada operação exige confirmação textual e preflight no GitHub.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box {
                            OutlinedButton(onClick = { repoMenu = true }, enabled = !state.busy) {
                                Text("Repositório: ${state.repository} ▾")
                            }
                            DropdownMenu(expanded = repoMenu, onDismissRequest = { repoMenu = false }) {
                                ActionsControlPolicy.repositories.forEach { repo ->
                                    DropdownMenuItem(
                                        text = { Text(repo) },
                                        onClick = {
                                            repoMenu = false
                                            viewModel.setRepository(repo)
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = state.reference,
                            onValueChange = viewModel::setReference,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            label = { Text("Git ref para disparar (branch ou tag)") },
                            singleLine = true,
                            supportingText = { Text("Padrão: branch principal observada no GitHub") }
                        )
                        OutlinedTextField(
                            value = inputs,
                            onValueChange = { inputs = it.take(4096) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.busy,
                            label = { Text("Entradas opcionais do workflow (JSON)") },
                            minLines = 1,
                            maxLines = 4,
                            supportingText = { Text("Ex.: {\"mode\":\"fast\"}. Não cole tokens, senhas ou segredos.") }
                        )
                        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(state.message, style = MaterialTheme.typography.bodySmall)
                        if (state.authRequired) {
                            Button(onClick = onAuthentication) { Text("Conectar conta GitHub") }
                        }
                    }
                }
            }

            state.lastReceipt?.let { receipt ->
                item {
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Último receipt · ${receipt.state}", fontWeight = FontWeight.Bold)
                            Text(
                                "repo=${receipt.repository} · ${receipt.operation} #${receipt.targetId} · HTTP ${receipt.providerHttpCode}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "source_sha=${receipt.sourceSha} · ref=${receipt.targetRef} · local_receipt=" +
                                    (if (state.receiptDurable) "DURABLE" else "TOKEN_VAZIO"),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Solicitação aceita ≠ run concluído ≠ PASS. Atualize para ler o estado real.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Text("Workflows (${state.workflows.size} nesta página)",
                    style = MaterialTheme.typography.titleMedium)
            }
            items(state.workflows, key = { "workflow-${it.id}" }) { workflow ->
                WorkflowCard(
                    workflow = workflow,
                    disabled = state.busy || state.authRequired,
                    onDispatch = {
                        pending = PendingAction(
                            ActionsMutation.DISPATCH, workflow.id,
                            workflow.name ?: "Workflow", state.repository, state.reference
                        )
                    }
                )
            }
            item {
                Text("Execuções (${state.runs.size} nesta página)",
                    style = MaterialTheme.typography.titleMedium)
            }
            items(state.runs, key = { "run-${it.id}" }) { run ->
                RunCard(
                    run = run,
                    repository = state.repository,
                    disabled = state.busy || state.authRequired,
                    onAction = { op ->
                        pending = PendingAction(
                            op, run.id, run.name ?: "Run", state.repository, state.reference
                        )
                    }
                )
            }
        }
    }
    pending?.let { action ->
        var typed by remember(action.id, action.operation) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!state.busy) pending = null },
            title = { Text("Confirmar ${action.operation.name} #${action.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Repositório: ${state.repository}\nAlvo: ${action.label} (#${action.id})")
                    if (action.operation == ActionsMutation.DISPATCH) {
                        Text("Ref: ${state.reference}\nInputs: fornecidos no formulário, não incluídos no receipt.")
                    } else {
                        Text("Essa operação pode interromper ou repetir trabalho e consumir cota.")
                    }
                    Text("Digite exatamente: ${action.phrase}", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        label = { Text("Confirmação obrigatória") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.mutate(action.operation, action.id, inputs)
                        pending = null
                    },
                    enabled = !state.busy && typed == action.phrase &&
                        action.repository == state.repository && action.reference == state.reference
                ) { Text("Solicitar ao GitHub") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Não executar") } }
        )
    }
}

@Composable
private fun WorkflowCard(workflow: ActionsWorkflow, disabled: Boolean, onDispatch: () -> Unit) {
    OutlinedCard {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(workflow.name ?: "Workflow #${workflow.id}", fontWeight = FontWeight.SemiBold)
                Text("id=${workflow.id} · ${workflow.state ?: "UNKNOWN"}",
                    style = MaterialTheme.typography.bodySmall)
                Text(workflow.path ?: "path=TOKEN_VAZIO", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onDispatch,
                enabled = !disabled && ActionsControlPolicy.workflowAllowed(workflow)
            ) { Text("Disparar") }
        }
    }
}

@Composable
private fun RunCard(
    run: ActionsRun,
    repository: String,
    disabled: Boolean,
    onAction: (ActionsMutation) -> Unit
) {
    val uri = LocalUriHandler.current
    OutlinedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(run.name ?: "Run #${run.id}", fontWeight = FontWeight.SemiBold)
            Text(
                "#${run.id} · ${run.status ?: "UNKNOWN"} · ${run.conclusion ?: "TOKEN_VAZIO"}" +
                    " · tentativa=${run.runAttempt ?: 1}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("branch=${run.headBranch ?: "TOKEN_VAZIO"} · sha=${run.headSha ?: "TOKEN_VAZIO"}",
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                uri.openUri("https://github.com/${ActionsControlPolicy.OWNER}/$repository/actions/runs/${run.id}")
            }) { Text("Ver execução / logs no GitHub") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ActionsControlPolicy.runCanCancel(run)) {
                    OutlinedButton(onClick = { onAction(ActionsMutation.CANCEL) }, enabled = !disabled) {
                        Text("Cancelar")
                    }
                }
                if (ActionsControlPolicy.runCanRerun(run)) {
                    OutlinedButton(onClick = { onAction(ActionsMutation.RERUN) }, enabled = !disabled) {
                        Text("Reexecutar")
                    }
                }
            }
            if (ActionsControlPolicy.runCanRerunFailedJobs(run)) {
                TextButton(onClick = { onAction(ActionsMutation.RERUN_FAILED) }, enabled = !disabled) {
                    Text("Reexecutar apenas jobs com falha")
                }
            }
        }
    }
}
