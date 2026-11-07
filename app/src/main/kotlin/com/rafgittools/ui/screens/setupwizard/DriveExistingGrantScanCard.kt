package com.rafgittools.ui.screens.setupwizard

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
 * Exactly one Android folder picker, one Google Drive tree, one automatic
 * metadata scan + index write in that SAME tree. No extra login, second picker,
 * checkbox, phone-local fallback, or separate publish button.
 *
 * The Android DocumentsProvider must expose Drive folders. If it does not,
 * this app cannot force Google Drive into the system picker; fail closed.
 */
@Composable
fun GoogleDriveScannerEntry() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("DRIVE_FOLDER_NOT_SELECTED") }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { selected: Uri? ->
        when {
            selected == null -> status = "DRIVE_SELECTION_CANCELLED"
            busy -> status = "DRIVE_SCAN_ALREADY_RUNNING"
            selected.scheme != "content" ||
                !DocumentsContract.isTreeUri(selected) ||
                !GoogleDriveSafGate.isGoogleDrive(context, selected) -> {
                status = "BLOCKED_SELECTED_PROVIDER_NOT_GOOGLE_DRIVE " +
                    GoogleDriveSafGate.diagnostic(context, selected)
            }
            else -> {
                // Keep the folder grant, if provider supports persistence, so
                // subsequent access does not require Google login or file copies.
                // Persist READ even when Drive grants no persistent WRITE.
                // Requesting both flags as one operation could discard an
                // otherwise valid cloud read grant on providers with read-only
                // folder capabilities.
                val persisted = runCatching {
                    context.contentResolver.takePersistableUriPermission(selected, granted)
                }.isSuccess || runCatching {
                    // Some providers grant persistent READ without persistent WRITE.
                    // A failed combined request must not lose a valid READ grant.
                    context.contentResolver.takePersistableUriPermission(
                        selected, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }.isSuccess
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        selected, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                // Android also grants transient access from the system picker.
                // Do not claim persistence when not granted. Let provider enforce
                // actual read/write permission for the scan and publication.
                scope.launch {
                    busy = true
                    status = if (persisted) "DRIVE_SCANNING_SELECTED_FOLDER"
                        else "DRIVE_SCANNING_WITH_TRANSIENT_PERMISSION"
                    val scan = withContext(Dispatchers.IO) {
                        runCatching {
                            // The SAF ContentResolver must enumerate the Google
                            // Drive tree BEFORE any index folder is created.
                            NovoexportSafInventory.scan(
                                context.contentResolver,
                                selected,
                                maxDocuments = DriveMetadataIndexGate.MAX_DOCUMENTS
                            )
                            // Same selected tree for input and output. The scan
                            // finishes BEFORE creating the HyperAtlas directory.
                            val publication = runCatching {
                                DriveAtlasSafPublisherV3.publish(
                                    resolver, selected, snapshot
                                )
                            }
                            snapshot to publication
                        }
                    }
                    result.onSuccess { (snapshot, publication) ->
                        val counts = " pastas=" + snapshot.allDirectories.size +
                            " arquivos=" + snapshot.allFiles.size
                        publication.onSuccess { receipt ->
                            status = "DRIVE_INDEX_READBACK_VERIFIED" + counts +
                                " indice=" + receipt.runFolderName +
                                " partes=" + receipt.parts +
                                " sha256=" + receipt.manifestSha256
                        }.onFailure { failure ->
                            // The read-only scan genuinely succeeded; do not
                            // misreport it as a scan failure or a saved index.
                            status = "DRIVE_SCAN_PASS_INDEX_WRITE_BLOCKED" + counts +
                                " reason=" + failure.javaClass.simpleName
                        }
                    }.onFailure { failure ->
                        status = "BLOCKED_DRIVE_SAF_READ_" +
                            failure.javaClass.simpleName
                    }
                    busy = false
                }
            }
        }
    }

    // If the APK already has a Drive grant, open the picker there rather than
    // showing phone storage first. The user STILL chooses the folder.
    val initialDriveTree = remember(context) {
        runCatching {
            context.contentResolver.persistedUriPermissions.firstOrNull {
                it.isReadPermission && it.uri.scheme == "content" &&
                    DocumentsContract.isTreeUri(it.uri) &&
                    GoogleDriveSafGate.isGoogleDrive(context, it.uri)
            }?.uri
        }.getOrNull()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Google Drive · indexação", style = MaterialTheme.typography.titleMedium)
            Text(
                "Escolha uma pasta do Google Drive no seletor. " +
                    "A varredura e a gravação do índice na mesma pasta começam automaticamente.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    status = "DRIVE_OPENING_FOLDER_PICKER"
                    folderPicker.launch(initialDriveTree)
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Escaneando o Google Drive…" else "Selecionar pasta no Google Drive")
            }
            Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}
