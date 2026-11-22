package com.rafgittools.ui.screens.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rafgittools.bridge.CorpusCatalogTreeGate
import com.rafgittools.bridge.PrivateGitHubTreeIndexGate
import com.rafgittools.domain.model.github.GithubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Explicit user gesture gates both private GitHub metadata read and private Drive SAF export. */
@Composable
fun PrivateGithubTreeIndexCard(
    repository: GithubRepository?,
    onBuild: suspend (GithubRepository) -> Result<PrivateGitHubTreeIndexGate.Snapshot>
) {
    val repo = repository?.takeIf { it.isPrivate }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<PrivateGitHubTreeIndexGate.Snapshot?>(null) }
    var outcome by remember { mutableStateOf("TOKEN_VAZIO_NOT_RUN") }
    var destinationConfirmed by remember { mutableStateOf(false) }

    val destination = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val current = snapshot
        if (uri != null && current != null) {
            scope.launch {
                busy = true
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // This operation may still be permitted by the transient SAF grant.
                    }
                    val exported = withContext(Dispatchers.IO) {
                        CorpusCatalogTreeGate.exportCatalog(
                            context = context,
                            treeUri = uri,
                            artifacts = listOf(current.indexFile)
                        )
                    }
                    outcome = if (exported.exportedArtifactCount == 1) {
                        "SAF_READBACK_VERIFIED_SHARING_UNVERIFIED"
                    } else "BLOCKED_INCOMPLETE_EXPORT"
                } catch (_: Exception) {
                    outcome = "BLOCKED_DRIVE_EXPORT"
                } finally {
                    busy = false
                }
            }
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("GitHub privado → Índice no Google Drive", style = MaterialTheme.typography.titleMedium)
            Text(
                "Indexa somente metadados do commit do repositório privado selecionado. " +
                    "Nenhum texto das conversas entra na CI pública. A exportação usa " +
                    "uma pasta escolhida no Android/Drive e confirma SHA-256 por leitura. " +
                    "O SAF não comprova as permissões de compartilhamento da pasta.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                enabled = repo != null && !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val target = repo ?: return@Button
                    scope.launch {
                        busy = true
                        outcome = "INDEXING_PRIVATE_METADATA"
                        snapshot = null
                        val result = onBuild(target)
                        result.onSuccess {
                            snapshot = it
                            outcome = "INDEXED_LOCALLY_PRIVATE"
                        }.onFailure {
                            outcome = "BLOCKED_GITHUB_INDEX"
                        }
                        busy = false
                    }
                }
            ) {
                Text(if (busy) "Processando…" else "Indexar GitHub privado")
            }
            snapshot?.let { current ->
                Text(
                    "Arquivos: ${current.fileCount} · bytes referenciados: ${current.byteCount} · " +
                        "arquivos acima de 2 MiB: ${current.largeBlobCount}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row {
                    Checkbox(checked = destinationConfirmed, onCheckedChange = { destinationConfirmed = it })
                    Text(
                        "Confirmo que verifiquei no Google Drive que a pasta de destino não é pública nem compartilhada.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { destination.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && destinationConfirmed
                ) {
                    Text("Exportar índice para pasta privada do Drive")
                }
            }
            Text(outcome, style = MaterialTheme.typography.labelSmall)
            if (repo == null) {
                Text("Conecte o GitHub e confirme o acesso privado ao repositório.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
