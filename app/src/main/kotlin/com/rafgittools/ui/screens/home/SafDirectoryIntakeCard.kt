package com.rafgittools.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rafgittools.bridge.SafDirectoryCopyResult
import com.rafgittools.bridge.SafDirectoryIntakeGate
import com.rafgittools.bridge.SafDirectoryPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SafDirectoryIntakeCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTreeUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<SafDirectoryPreview?>(null) }
    var copied by remember { mutableStateOf<SafDirectoryCopyResult?>(null) }
    var reviewing by remember { mutableStateOf(false) }
    var copying by remember { mutableStateOf(false) }
    var copyAcknowledged by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(selectedTreeUri) {
        val grantedUri = selectedTreeUri
        onDispose {
            if (grantedUri != null) releaseSafReadGrant(context, grantedUri)
        }
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            selectedTreeUri?.takeIf { it != uri }?.let { releaseSafReadGrant(context, it) }
            selectedTreeUri = uri
            preview = null
            copied = null
            copyAcknowledged = false
            error = null
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // The current SAF grant may still allow this bounded operation.
            }
            scope.launch {
                reviewing = true
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        SafDirectoryIntakeGate.preview(
                            resolver = context.contentResolver,
                            treeUri = uri
                        )
                    }
                }
                result.onSuccess { preview = it }
                    .onFailure { error = safDirectoryFailure(it) }
                reviewing = false
            }
        }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar e indexar uma pasta", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Escolha uma pasta no seletor do Android. Se o Google Drive estiver disponível no aparelho, ele aparece como provedor. O RafGitTools usa somente a permissão de leitura concedida pelo seletor; não recebe sua senha Google.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Primeiro o app mostra um inventário de metadados. A cópia só começa depois da sua confirmação e fica no armazenamento privado do aplicativo, preservando a árvore selecionada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { folderPicker.launch(null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !reviewing && !copying
                ) {
                    if (reviewing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Revisando metadados…")
                    } else {
                        androidx.compose.material3.Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar pasta para revisar")
                    }
                }
            }

            preview?.let { inventory ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Prévia do inventário", style = MaterialTheme.typography.titleSmall)
                        Text("Provedor: " + inventory.providerAuthority)
                        Text(
                            "Documentos: " + inventory.visitedDocumentCount +
                                " · pastas: " + inventory.directoryCount +
                                " · arquivos: " + inventory.fileCount,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Tamanho conhecido: " + formatSafBytes(inventory.knownBytes) +
                                " · tamanhos não informados: " + inventory.unknownSizeFileCount,
                            style = MaterialTheme.typography.bodySmall
                        )
                        inventory.samplePaths.forEach { path ->
                            Text(
                                "• " + path,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            "Fingerprint da prévia: " + inventory.inventoryFingerprint.take(16) + "…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "O inventário inclui todos os arquivos e diretórios encontrados; a visibilidade compartilhada da origem não é exposta pelo SAF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = copyAcknowledged,
                                onCheckedChange = { copyAcknowledged = it },
                                enabled = !copying
                            )
                            Text(
                                "Confirmo a cópia local privada. A origem não será alterada e nada será enviado ao Drive ou GitHub nesta etapa.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = {
                                val uri = selectedTreeUri
                                if (uri == null) {
                                    error = "Selecione a pasta novamente antes de copiar."
                                } else {
                                    scope.launch {
                                        copying = true
                                        error = null
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching {
                                                SafDirectoryIntakeGate.copyAndIndex(
                                                    context = context,
                                                    treeUri = uri,
                                                    expectedInventoryFingerprint =
                                                        inventory.inventoryFingerprint
                                                )
                                            }
                                        }
                                        result.onSuccess {
                                            copied = it
                                            releaseSafReadGrant(context, uri)
                                            selectedTreeUri = null
                                            preview = null
                                            copyAcknowledged = false
                                        }.onFailure {
                                            error = safDirectoryFailure(it)
                                        }
                                        copying = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = copyAcknowledged && !copying && !reviewing
                        ) {
                            if (copying) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Copiando e verificando…")
                            } else {
                                androidx.compose.material3.Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Confirmar cópia privada e gerar índice")
                            }
                        }
                    }
                }
            }

            copied?.let { result ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Cópia e índice verificados", style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "Arquivos: " + result.fileCount +
                                " · pastas: " + result.directoryCount +
                                " · dados copiados: " + formatSafBytes(result.totalBytes),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "SHA-256 do manifesto: " + result.manifestSha256,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Receipt local: " + result.receiptFile.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Snapshot privado: " + result.snapshotDirectory.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Política: PRIVATE_LOCAL_ONLY · fonte intacta · compartilhamento da origem desconhecido · transferência externa NONE.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Destino Drive/GitHub: TOKEN_VAZIO. A composição e aplicação das regras do repositório continuam bloqueadas até a próxima etapa explícita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            error?.let { message ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatSafBytes(bytes: Long): String {
    if (bytes < 1024L) return bytes.toString() + " B"
    val units = listOf("KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    return String.format(java.util.Locale.ROOT, "%.2f %s", value, units[unit])
}


private fun releaseSafReadGrant(context: android.content.Context, uri: Uri) {
    try {
        context.contentResolver.releasePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // A non-persisted, one-shot grant needs no release.
    }
}


private fun safDirectoryFailure(failure: Throwable): String {
    val code = failure.message
        ?.takeIf { it.matches(Regex("^[A-Z0-9_]{1,80}$")) }
        ?: "SAF_OPERATION_FAILED"
    return when (code) {
        "SOURCE_CHANGED_AFTER_PREVIEW" ->
            "A pasta mudou desde a prévia. Revise a seleção novamente antes de copiar."
        "SAF_MAX_DOCUMENTS_EXCEEDED", "SOURCE_EXCEEDS_MAX_TOTAL_BYTES",
        "SAF_COPY_BYTE_LIMIT_EXCEEDED", "SOURCE_FILE_EXCEEDS_MAX_FILE_BYTES" ->
            "A pasta excede o limite desta operação (50.000 itens ou 512 MiB)."
        "INSUFFICIENT_PRIVATE_STORAGE_FOR_KNOWN_SOURCE_BYTES" ->
            "O espaço privado disponível no aparelho não comporta o tamanho informado pela origem."
        "SAF_PATH_TRAVERSAL_BLOCKED", "SAF_PATH_SEPARATOR_BLOCKED",
        "SAF_PATH_CONTROL_CHARACTER_BLOCKED", "SAF_PATH_SEGMENT_TOO_LONG",
        "SAF_RELATIVE_PATH_TOO_LONG", "SAF_RELATIVE_PATH_COLLISION",
        "SAF_DUPLICATE_DOCUMENT_REFERENCE" ->
            "A pasta contém um caminho ambíguo ou inseguro; nada foi publicado."
        "STAGED_READBACK_MISMATCH", "FINAL_READBACK_MISMATCH",
        "SAF_SOURCE_SIZE_CHANGED_DURING_COPY" ->
            "A verificação de integridade falhou. A cópia incompleta foi descartada e a origem permanece intacta."
        else -> "Falha na operação SAF (" + code + "). A origem não foi alterada."
    }
}
