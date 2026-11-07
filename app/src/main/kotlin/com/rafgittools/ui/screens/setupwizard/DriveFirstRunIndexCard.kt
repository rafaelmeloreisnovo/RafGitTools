package com.rafgittools.ui.screens.setupwizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rafgittools.navigator.NovoexportSafInventory
import com.rafgittools.navigator.SafZipRangeGateV1
import com.rafgittools.navigator.ZipCentralDirectoryRangeV1
import com.rafgittools.setupwizard.DriveMetadataIndexGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * First-run source selector. SAF delegates available providers/accounts to Android.
 * Not Google OAuth and not account-wide Drive access.
 */
@Composable
fun DriveFirstRunIndexCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<NovoexportSafInventory.Result?>(null) }
    var busy by remember { mutableStateOf(false) }
    var privateDestinationConfirmed by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("TOKEN_VAZIO_SOURCE_NOT_SELECTED") }
    var selectedZipIndex by remember { mutableStateOf<ZipCentralDirectoryRangeV1.Index?>(null) }

    val outputPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { output ->
        val selected = snapshot
        if (output == null || selected == null || !privateDestinationConfirmed) {
            status = "TOKEN_VAZIO_EXPORT_NOT_AUTHORIZED"
        } else {
            scope.launch {
                busy = true
                status = "INDEX_EXPORT_IN_PROGRESS"
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val index = DriveMetadataIndexGate.build(selected)
                        val expected = DriveMetadataIndexGate.digest(index)
                        val resolver = context.contentResolver
                        val writer = resolver.openOutputStream(output, "w")
                            ?: error("TOKEN_VAZIO_DESTINATION_NOT_WRITABLE")
                        writer.use { it.write(index); it.flush() }
                        val reader = resolver.openInputStream(output)
                            ?: error("TOKEN_VAZIO_DESTINATION_READBACK_UNAVAILABLE")
                        val digest = MessageDigest.getInstance("SHA-256")
                        var bytes = 0L
                        reader.use { stream ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val count = stream.read(buffer)
                                if (count < 0) break
                                if (count == 0) continue
                                bytes += count
                                require(bytes <= DriveMetadataIndexGate.MAX_INDEX_BYTES.toLong()) {
                                    "TOKEN_VAZIO_READBACK_TOO_LARGE"
                                }
                                digest.update(buffer, 0, count)
                            }
                        }
                        val actual = digest.digest().joinToString("") { "%02x".format(it) }
                        require(bytes == index.size.toLong() && actual == expected) {
                            "TOKEN_VAZIO_INDEX_READBACK_MISMATCH"
                        }
                        "INDEX_METADATA_READBACK_VERIFIED bytes=$bytes sha256=$actual"
                    }
                }
                status = result.getOrElse { "BLOCKED_EXPORT_OR_READBACK_TOKEN_VAZIO" }
                busy = false
            }
        }
    }

    val sourcePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        snapshot = null
        privateDestinationConfirmed = false
        if (uri == null) {
            status = "TOKEN_VAZIO_SOURCE_NOT_SELECTED"
        } else {
            scope.launch {
                busy = true
                status = "METADATA_SCAN_READONLY"
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        NovoexportSafInventory.scan(
                            context.contentResolver,
                            uri,
                            maxDocuments = DriveMetadataIndexGate.MAX_DOCUMENTS
                        )
                    }
                }
                result.onSuccess {
                    snapshot = it
                    status = "METADATA_PREVIEW_READY_NOT_EXPORTED"
                }.onFailure {
                    status = "BLOCKED_SOURCE_PROVIDER_OR_LIMIT_TOKEN_VAZIO"
                }
                busy = false
            }
        }
    }

    val smallArchivePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        selectedZipIndex = null
        if (uri == null) {
            status = "TOKEN_VAZIO_ZIP_SOURCE_NOT_SELECTED"
        } else {
            scope.launch {
                busy = true
                status = "ZIP_POSITIONED_READ_IN_PROGRESS"
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        SafZipRangeGateV1.inspectSelected(context.contentResolver, uri)
                    }
                }
                result.onSuccess {
                    selectedZipIndex = it
                    status = "ZIP_DIRECTORY_METADATA_SCOPED_READ sourceBytesRead=${it.sourceBytesRead}"
                }.onFailure { failure ->
                    status = if (failure is java.io.IOException) {
                        "BLOCKED_${failure.message ?: "SAF_ZIP_READ"}"
                    } else "BLOCKED_SAF_ZIP_UNVERIFIED"
                }
                busy = false
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🗂️ HyperAtlas · Mapear pastas e ZIPs no Drive",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Use o acesso a arquivos que o RafGitTools já possui via seletor Android. " +
                    "Escolha uma pasta do Drive: o app mapeia a árvore e identifica ZIPs/ZIPRAFs sem baixá-los. " +
                    "O acesso cobre apenas a árvore autorizada, não a conta inteira.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { sourcePicker.launch(null) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Escolher conta/pasta pelo Android") }

            snapshot?.let { index ->
                val archives = remember(index) {
                    index.allFiles.filter {
                        DriveMetadataIndexGate.classifyArchive(it.relativePath, it.mimeType) != null
                    }
                }
                Text(
                    "Mapa: ${index.visitedDocuments} documentos, ${index.allDirectories.size} pastas, " +
                        "${index.allFiles.size} arquivos, ${archives.size} compactados ou partes. " +
                        "Tamanhos TOKEN_VAZIO: ${index.unknownSizeFiles}.",
                    style = MaterialTheme.typography.bodySmall
                )
                archives.take(3).forEach { archive ->
                    Text(
                        "• ${archive.relativePath} · " +
                            "${archive.sizeBytes?.toString() ?: "TOKEN_VAZIO"} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    "ZIPs grandes ficam no Drive; suas entradas internas exigem HTTP Range autorizado. " +
                        "A prévia da pasta lê somente metadados.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Só os metadados e relações pasta→arquivo podem ser exportados; " +
                        "nenhuma mensagem de conversa foi lida. O índice não cobre outras contas nem pastas não selecionadas.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row {
                    Checkbox(
                        checked = privateDestinationConfirmed,
                        onCheckedChange = { privateDestinationConfirmed = it }
                    )
                    Text(
                        "Confirmei que o destino escolhido é privado e autorizado; nomes de arquivos também podem ser sensíveis.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = { outputPicker.launch("rafgittools-hyperatlas-drive-map.v1.json") },
                    enabled = !busy && privateDestinationConfirmed,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar mapa na pasta escolhida (SHA-256 verificado)") }
            }

            OutlinedButton(
                onClick = {
                    smallArchivePicker.launch(arrayOf(
                        "application/zip", "application/x-zip",
                        "application/octet-stream"
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) { Text("Inspecionar diretório de ZIP pequeno (até 16 MiB)") }
            Text(
                "Somente após escolher o arquivo. O provedor pode materializar ZIPs pequenos " +
                    "ao abrir o descritor; ZIPs maiores ou sem tamanho conhecido são bloqueados. " +
                    "Nenhuma extração é feita.",
                style = MaterialTheme.typography.bodySmall
            )
            selectedZipIndex?.let { index ->
                Text(
                    "ZIP: ${index.state} · entradas ${index.observedEntries}/${index.declaredEntries} · " +
                        "bytes lidos ${index.sourceBytesRead}",
                    style = MaterialTheme.typography.bodySmall
                )
                index.entries.take(8).forEach { entry ->
                    Text(
                        "• ${entry.path} · ${entry.originalBytes} bytes" +
                            if (entry.unsafePath) " [CAMINHO INSEGURO]" else "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (index.gaps.isNotEmpty()) {
                    Text(
                        "GAPS: ${index.gaps.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text(status, style = MaterialTheme.typography.labelSmall)
            Text(
                "Conversations completas e arestas semânticas exigem uma segunda autorização para ler conteúdo. " +
                    "Indexar este recorte não envia dados ao GitHub ou à CI.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
