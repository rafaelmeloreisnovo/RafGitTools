package com.rafgittools.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rafgittools.bridge.CatalogTreeExportResult
import com.rafgittools.library.LibraryCatalogMaterialization
import com.rafgittools.library.LibraryCatalogMaterializer
import com.rafgittools.library.LibraryCatalogTreeExporter
import com.rafgittools.navigator.NovoexportLibraryCatalogComposer
import com.rafgittools.navigator.NovoexportSafInventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun NovoexportLibraryCatalogCard(inventory: NovoexportSafInventory.Result) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filter by remember(inventory) { mutableStateOf("") }
    var materialization by remember(inventory) {
        mutableStateOf<LibraryCatalogMaterialization?>(null)
    }
    var outputFolder by remember { mutableStateOf<Uri?>(null) }
    var askExportConfirmation by remember { mutableStateOf(false) }
    var permissionWarning by remember { mutableStateOf(false) }
    var materializing by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var error by remember(inventory) { mutableStateOf<String?>(null) }
    var exportSummary by remember { mutableStateOf<CatalogTreeExportResult?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            outputFolder = uri
            exportSummary = null
            permissionWarning = false
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                permissionWarning = true
                error = "Permissão persistente indisponível; a exportação ainda será tentada nesta sessão."
            }
            askExportConfirmation = true
        }
    }

    val candidates = remember(inventory, filter) {
        val query = filter.trim()
        if (query.isEmpty()) inventory.candidateFiles
        else inventory.candidateFiles.filter { it.name.contains(query, ignoreCase = true) }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Catálogo NOVOexport", style = MaterialTheme.typography.titleMedium)
            Text(
                "Prévia pesquisável dos nomes e tamanhos informados pelo provedor. Esta etapa não abre nem copia o conteúdo dos arquivos.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Acesso/compartilhamento: TOKEN_VAZIO. O seletor SAF não confirma quem pode ver a pasta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Itens encontrados: ${inventory.candidateFiles.size} · filtrados: ${candidates.size} · catalogados: ${materialization?.let { inventory.candidateFiles.size } ?: 0}",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Filtrar por nome") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Default.Search, null) }
            )
            candidates.take(12).forEach { candidate ->
                Text(
                    "${candidate.name} · ${candidate.sizeBytes?.let(::formatBytes) ?: "tamanho TOKEN_VAZIO"}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (candidates.size > 12) {
                Text(
                    "Mais ${candidates.size - 12} itens no resultado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "O catálogo mantém IDs de origem como hashes SHA-256; isso é uma impressão técnica, não anonimização. Conteúdo, vetores e relações semânticas não são calculados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        materializing = true
                        materialization = null
                        error = null
                        exportSummary = null
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                val bundle = NovoexportLibraryCatalogComposer.compose(
                                    inventory = inventory,
                                    createdAtEpochMs = System.currentTimeMillis()
                                )
                                LibraryCatalogMaterializer.materialize(
                                    destinationDir = File(
                                        context.filesDir,
                                        "library-catalogs/novoexport"
                                    ),
                                    bundle = bundle
                                )
                            }
                        }
                        result.onSuccess { materialization = it }
                            .onFailure { error = it.message ?: "Falha ao compor o catálogo local" }
                        materializing = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !materializing && !exporting
            ) {
                if (materializing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Compondo catálogo…")
                } else {
                    Text("Indexar e compor catálogo local")
                }
            }

            materialization?.let { ready ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Materialização local verificada", style = MaterialTheme.typography.titleSmall)
                        Text(ready.catalogFile.name, style = MaterialTheme.typography.bodySmall)
                        Text("SHA-256 catálogo: ${ready.catalogSha256}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Claim permitido: false · acesso: TOKEN_VAZIO · fonte bruta copiada: não",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Button(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !exporting
                ) {
                    androidx.compose.material3.Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Escolher destino e revisar exportação")
                }
            }

            exportSummary?.let { result ->
                Text(
                    "Exportação confirmada pelo gate · arquivos=${result.exportedArtifactCount} · provedor=${result.destinationProviderAuthority} · recibo local=${result.localReceiptFile.name}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    if (askExportConfirmation) {
        val destination = outputFolder
        val ready = materialization
        AlertDialog(
            onDismissRequest = {
                askExportConfirmation = false
                outputFolder = null
            },
            title = { Text("Revisar exportação para o Drive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Serão gravados somente o catálogo e o recibo de exportação; nenhum arquivo NOVOexport será copiado.")
                    if (ready != null) {
                        Text("Catálogo: ${ready.catalogFile.name}")
                        Text("Recibo de materialização: ${ready.receiptFile.name}")
                        Text("SHA-256 do catálogo: ${ready.catalogSha256}")
                    }
                    Text("Destino (provedor): ${destination?.authority ?: "TOKEN_VAZIO"}")
                    Text("Compartilhamento do destino: TOKEN_VAZIO; confirme as permissões diretamente no Google Drive.")
                    if (permissionWarning) {
                        Text("Acesso persistente não confirmado pelo Android; a tentativa desta sessão pode falhar.")
                    }
                    Text("Claim permitido: false.")
                }
            },
            confirmButton = {
                TextButton(
                    enabled = destination != null && ready != null && !exporting,
                    onClick = {
                        if (destination != null && ready != null) {
                            askExportConfirmation = false
                            scope.launch {
                                exporting = true
                                error = null
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        LibraryCatalogTreeExporter.export(
                                            context = context,
                                            destinationTree = destination,
                                            materialization = ready
                                        )
                                    }
                                }
                                result.onSuccess { exportSummary = it }
                                    .onFailure { error = it.message ?: "Falha ao exportar catálogo e recibo" }
                                outputFolder = null
                                exporting = false
                            }
                        }
                    }
                ) {
                    if (exporting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(6.dp))
                    }
                    Text("Confirmar exportação")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    askExportConfirmation = false
                    outputFolder = null
                }) { Text("Cancelar") }
            }
        )
    }
}
