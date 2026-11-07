package com.rafgittools.ui.screens.setupwizard

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.rafgittools.setupwizard.DriveAtlasSafPublisherV3
import com.rafgittools.setupwizard.DriveMetadataIndexGate
import com.rafgittools.setupwizard.GoogleDriveSafGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reuses folder access ALREADY granted to RafGitTools by Google Drive's own
 * DocumentsProvider. No second Google login, new picker or local staging.
 * This does not borrow another app's account token or bypass Android consent.
 */
object ExistingDriveTreeGrant {
    data class Grant(val tree: Uri, val writable: Boolean)

    fun find(context: Context): List<Grant> =
        context.contentResolver.persistedUriPermissions.asSequence()
            .filter { it.isReadPermission }
            .mapNotNull { permission ->
                val uri = permission.uri
                if (uri.scheme != "content" ||
                    !DocumentsContract.isTreeUri(uri) ||
                    !GoogleDriveSafGate.isGoogleDrive(context, uri)) {
                    null
                } else Grant(uri, permission.isWritePermission)
            }
            .distinctBy { it.tree.toString() }
            .toList()
}

/**
 * Existing Drive grant -> scan metadata -> create a NEW index folder in Drive.
 * Never downloads ZIP contents, deletes source or writes phone-local files.
 */
@Composable
fun DriveAlreadyConnectedScanCard(grants: List<ExistingDriveTreeGrant.Grant>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("DRIVE_EXISTING_ACCESS_READY") }
    var selection by remember(grants) { mutableStateOf(grants.first()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Google Drive · acesso já autorizado", style = MaterialTheme.typography.titleMedium)
            Text("Usar o Drive que já estava conectado ao RafGitTools. " +
                "Escanear apenas nomes, pastas, tamanhos e tipos; " +
                "salvar o HyperAtlas numa nova subpasta do mesmo Drive.",
                style = MaterialTheme.typography.bodyMedium)
            if (grants.size > 1) {
                grants.forEachIndexed { i, grant ->
                    OutlinedButton(
                        enabled = !busy,
                        onClick = { selection = grant },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((if (selection.tree == grant.tree) "✓ " else "") +
                            "Pasta Google Drive autorizada " + (i + 1))
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy && selection.writable,
                onClick = {
                    val tree = selection.tree
                    scope.launch {
                        busy = true
                        status = "DRIVE_EXISTING_FOLDER_SCANNING"
                        val result = withContext(Dispatchers.IO) {
                            runCatching {
                                require(GoogleDriveSafGate.isGoogleDrive(context, tree)) {
                                    "BLOCKED_NON_GOOGLE_DRIVE_PROVIDER"
                                }
                                val snapshot = NovoexportSafInventory.scan(
                                    context.contentResolver, tree,
                                    maxDocuments = DriveMetadataIndexGate.MAX_DOCUMENTS
                                )
                                val receipt = DriveAtlasSafPublisherV3.publish(
                                    context.contentResolver, tree, snapshot
                                )
                                Pair(snapshot, receipt)
                            }
                        }
                        result.onSuccess { pair ->
                            val snapshot = pair.first
                            val receipt = pair.second
                            status = "DRIVE_ALREADY_CONNECTED_INDEX_VERIFIED " +
                                "pastas=" + snapshot.allDirectories.size +
                                " arquivos=" + snapshot.allFiles.size +
                                " indice=" + receipt.runFolderName +
                                " sha256=" + receipt.manifestSha256
                        }.onFailure { error ->
                            status = "BLOCKED_DRIVE_EXISTING_SCAN_" +
                                (error.message ?: "TOKEN_VAZIO")
                        }
                        busy = false
                    }
                }
            ) { Text("Escanear esta pasta do Google Drive") }
            if (!selection.writable) {
                Text("A autorização anterior permite somente leitura. " +
                    "Para salvar o índice nesta pasta é preciso permissão de gravação.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Text(status, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Prefer the Drive tree already authorized to this APK over new Google OAuth. */
@Composable
fun GoogleDriveScannerEntry() {
    val context = LocalContext.current
    val grants = remember(context) {
        runCatching { ExistingDriveTreeGrant.find(context) }.getOrDefault(emptyList())
    }
    if (grants.isNotEmpty()) {
        DriveAlreadyConnectedScanCard(grants)
    } else {
        // No pre-existing app-specific SAF grant: use Drive REST authorization.
        GoogleDriveCloudCard()
    }
}
