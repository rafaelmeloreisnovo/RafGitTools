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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🗝️ Primeiro: escolher o Drive e observar os arquivos",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "O seletor do Android permite escolher um provedor e, quando disponível, uma conta Google Drive. " +
                    "Não há login Google dentro do RafGitTools nem acesso automático ao Drive inteiro.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { sourcePicker.launch(null) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Escolher conta/pasta pelo Android") }

            snapshot?.let { index ->
                Text(
                    "Prévia: ${index.visitedDocuments} documentos, ${index.allDirectories.size} pastas, " +
                        "${index.allFiles.size} arquivos, ${index.candidateFiles.size} possíveis conversations/codex. " +
                        "Tamanhos ausentes: ${index.unknownSizeFiles}.",
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
                    onClick = { outputPicker.launch("rafaelia-drive-metadata-index.v1.json") },
                    enabled = !busy && privateDestinationConfirmed,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar índice no Drive (com readback SHA-256)") }
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
