package com.rafgittools.ui.screens.setupwizard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.rafgittools.data.drive.GoogleDriveCloudRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * First-party Drive browser. There is NO SAF OpenDocumentTree, phone-local
 * staging, hidden-local toggle or GitHub PAT dependency in this UI.
 */
@Composable
fun GoogleDriveCloudCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authorization = remember(context) { Identity.getAuthorizationClient(context) }
    val drive = remember { GoogleDriveCloudRepository() }

    var accessToken by remember { mutableStateOf<String?>(null) } // volatile, never persisted/logged
    var status by remember { mutableStateOf("GOOGLE_DRIVE_NOT_CONNECTED") }
    var busy by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<GoogleDriveCloudRepository.Item>>(emptyList()) }
    var nextPage by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<GoogleDriveCloudRepository.Selection?>(null) }
    var destination by remember { mutableStateOf<GoogleDriveCloudRepository.Selection?>(null) }
    val folders = remember { mutableStateListOf(GoogleDriveCloudRepository.Selection("root", "Meu Drive")) }

    fun load(folderId: String, pageToken: String? = null) {
        val token = accessToken
        if (token.isNullOrBlank()) {
            status = "BLOCKED_GOOGLE_DRIVE_AUTH_REQUIRED"
            return
        }
        scope.launch {
            busy = true
            status = "DRIVE_REST_LISTING"
            val result = withContext(Dispatchers.IO) {
                runCatching { drive.list(token, folderId, pageToken) }
            }
            result.onSuccess { page ->
                items = if (pageToken == null) page.items else items + page.items
                nextPage = page.next
                status = if (nextPage == null) "DRIVE_REST_PAGE_READY"
                    else "DRIVE_REST_PAGE_PARTIAL_MORE_AVAILABLE"
            }.onFailure { error ->
                status = when {
                    error.message?.contains("HTTP_401") == true -> {
                        accessToken = null
                        "BLOCKED_DRIVE_TOKEN_EXPIRED_RECONNECT"
                    }
                    else -> "BLOCKED_" + (error.message ?: "GOOGLE_DRIVE_LISTING")
                }
                nextPage = null
            }
            busy = false
        }
    }

    fun acceptAuthorization(token: String?) {
        if (token.isNullOrBlank()) {
            status = "BLOCKED_DRIVE_AUTH_TOKEN_VAZIO"
            return
        }
        accessToken = token
        source = null
        destination = null
        folders.clear()
        folders.add(GoogleDriveCloudRepository.Selection("root", "Meu Drive"))
        load("root")
    }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val intent = activityResult.data
        if (activityResult.resultCode != Activity.RESULT_OK || intent == null) {
            status = "BLOCKED_DRIVE_CONSENT_CANCELLED"
        } else {
            val result = runCatching { authorization.getAuthorizationResultFromIntent(intent) }
            result.onSuccess { granted -> acceptAuthorization(granted.accessToken) }
                .onFailure { status = "BLOCKED_DRIVE_GOOGLE_AUTH_RESULT" }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("☁️ Google Drive · pastas da nuvem",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Navegação pela API Google Drive, dentro do RafGitTools. " +
                    "Não abre seletor de arquivos do celular, não usa Google Pay e " +
                    "não copia seus ZIPs. A autorização Google é independente do PAT GitHub.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Para listar e criar índices em qualquer pasta escolhida, é solicitada " +
                    "permissão Google Drive. O Google exige configuração OAuth e, " +
                    "para distribuição pública, verificação do escopo restrito.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = {
                    busy = true
                    status = "GOOGLE_DRIVE_AUTHORIZING"
                    val request = AuthorizationRequest.builder()
                        .setRequestedScopes(listOf(Scope(GoogleDriveCloudRepository.SCOPE)))
                        .build()
                    authorization.authorize(request)
                        .addOnSuccessListener { answer ->
                            busy = false
                            if (answer.hasResolution()) {
                                val pending = answer.pendingIntent
                                if (pending == null) {
                                    status = "BLOCKED_DRIVE_CONSENT_UNAVAILABLE"
                                } else {
                                    runCatching {
                                        consentLauncher.launch(
                                            IntentSenderRequest.Builder(pending.intentSender).build()
                                        )
                                    }.onFailure { status = "BLOCKED_DRIVE_CONSENT_LAUNCH" }
                                }
                            } else {
                                acceptAuthorization(answer.accessToken)
                            }
                        }
                        .addOnFailureListener {
                            busy = false
                            status = "BLOCKED_GOOGLE_DRIVE_AUTH_OR_CLOUD_CONFIGURATION"
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            ) { Text(if (accessToken == null) "Conectar conta Google Drive" else "Reconectar / trocar conta Google") }

            if (accessToken != null) {
                val current = folders.last()
                Text("Pasta atual: " + folders.joinToString(" / ") { it.name },
                    style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !busy && folders.size > 1,
                        onClick = {
                            folders.removeAt(folders.lastIndex)
                            load(folders.last().id)
                        }
                    ) { Text("Voltar") }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { load(current.id) }
                    ) { Text("Atualizar") }
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        source = current
                        status = "DRIVE_SOURCE_FOLDER_SELECTED"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Usar esta pasta do Drive como origem") }
                Button(
                    enabled = !busy,
                    onClick = {
                        destination = current
                        status = "DRIVE_DESTINATION_FOLDER_SELECTED"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Usar esta pasta do Drive como destino") }
                Text("Origem: " + (source?.name ?: "TOKEN_VAZIO"),
                    style = MaterialTheme.typography.bodySmall)
                Text("Destino: " + (destination?.name ?: "TOKEN_VAZIO"),
                    style = MaterialTheme.typography.bodySmall)

                // Small provider page prevents nested unbounded lists in a scrolling parent.
                items.forEach { item ->
                    if (item.folder) {
                        OutlinedButton(
                            onClick = {
                                folders.add(GoogleDriveCloudRepository.Selection(item.id, item.name))
                                load(item.id)
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("📁 " + item.name, maxLines = 1) }
                    } else {
                        Text("📄 " + item.name + " · " + (item.size?.toString() ?: "TOKEN_VAZIO") + " bytes",
                            style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
                if (nextPage != null) {
                    OutlinedButton(
                        onClick = { load(current.id, nextPage) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy
                    ) { Text("Carregar próxima página do Google Drive") }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && source != null && destination != null,
                    onClick = {
                        val origin = source
                        val target = destination
                        val token = accessToken
                        if (origin != null && target != null && token != null) {
                            scope.launch {
                                busy = true
                                status = "DRIVE_CLOUD_ATLAS_SCANNING_AND_PUBLISHING"
                                val published = withContext(Dispatchers.IO) {
                                    runCatching { drive.publish(token, origin, target) }
                                }
                                published.onSuccess { receipt ->
                                    status = "DRIVE_CLOUD_ATLAS_READBACK_VERIFIED folder=" +
                                        receipt.folderName + " records=" + receipt.records +
                                        " shards=" + receipt.shards +
                                        " manifest_sha256=" + receipt.manifestSha256
                                }.onFailure { error ->
                                    status = "BLOCKED_DRIVE_CLOUD_PUBLISH_" +
                                        (error.message ?: "TOKEN_VAZIO")
                                }
                                busy = false
                            }
                        } else status = "BLOCKED_DRIVE_EXPLICIT_SELECTION_REQUIRED"
                    }
                ) { Text("Criar HyperAtlas nesta pasta do Google Drive") }
                Text(
                    "Somente índices de metadados. Um MANIFEST.json é criado por último, " +
                        "após conferir hashes por leitura da API Google. Os ZIPs originais " +
                        "continuam intocados e nenhum dado vai ao armazenamento do celular.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(status, style = MaterialTheme.typography.labelSmall)
        }
    }
}
