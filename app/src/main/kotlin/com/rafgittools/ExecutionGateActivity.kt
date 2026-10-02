package com.rafgittools

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.rafgittools.execution.ExecutionGateContractV1
import com.rafgittools.library.processing.LibraryJobEnvironment
import com.rafgittools.library.processing.LibraryJobInput
import com.rafgittools.library.processing.LibraryJobState
import com.rafgittools.library.processing.LibraryLocalJobExecutor
import com.rafgittools.ui.theme.RafGitToolsTheme
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Isolated launcher surface for the bounded one-button WorkGroup.
 *
 * V1 is deliberately local/read-only: one SAF source, fixed STRUCTURAL rigor,
 * no network, no arbitrary commands and no claim promotion.
 */
class ExecutionGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RafGitToolsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ExecutionGateScreen(onClose = { finish() })
                }
            }
        }
    }
}

private data class GateSource(
    val uri: Uri,
    val displayName: String,
    val mediaType: String?,
    val declaredSize: Long?
)

private data class GateRun(
    val finalState: String,
    val lines: List<String>
)

private data class LoadedSource(
    val bytes: ByteArray,
    val sha256: String,
    val opaqueLocatorSha256: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExecutionGateScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<GateSource?>(null) }
    var running by remember { mutableStateOf(false) }
    var finalState by remember { mutableStateOf("TOKEN_VAZIO") }
    val syslog = remember { mutableStateListOf<String>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            source = describeSource(context, uri)
            finalState = "PENDING"
            syslog.clear()
            syslog += "[SOURCE] selecionada · ${source?.displayName ?: "TOKEN_VAZIO"}"
            syslog += "[POLICY] READ_ONLY · OFFLINE_ONLY · claim_allowed=false"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("RAFAELIA · Portão de Execução") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Um botão abre um WorkGroup limitado. Cada resultado abaixo vem do executor/receipt; ausência permanece TOKEN_VAZIO.",
                style = MaterialTheme.typography.bodyMedium
            )

            val selected = source
            if (selected == null) {
                Text("SOURCE: TOKEN_VAZIO")
            } else {
                Text("SOURCE: ${selected.displayName}")
                Text(
                    "tipo=${selected.mediaType ?: "TOKEN_VAZIO"} · tamanho=${selected.declaredSize?.toString() ?: "TOKEN_VAZIO"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !running) {
                    Text("Selecionar fonte")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = selected != null && !running,
                    onClick = {
                        val target = selected ?: return@Button
                        running = true
                        finalState = "RUNNING"
                        syslog.clear()
                        syslog += "[00] GATE · RUNNING"
                        syslog += "[01] SOURCE_LOCK · RUNNING"
                        scope.launch {
                            val run = withContext(Dispatchers.IO) {
                                runGate(context, target)
                            }
                            syslog += run.lines
                            finalState = run.finalState
                            running = false
                        }
                    }
                ) {
                    Text("ABRIR PORTÃO")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("STATE: $finalState", style = MaterialTheme.typography.titleMedium)
                if (running) {
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Text("SYSLOG / RECEIPT", style = MaterialTheme.typography.titleSmall)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(syslog) { _, line ->
                    Text(
                        text = line,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth(), enabled = !running) {
                Text("Fechar")
            }
        }
    }
}

private fun runGate(context: Context, source: GateSource): GateRun {
    val lines = mutableListOf<String>()
    return try {
        if (source.declaredSize != null && source.declaredSize > ExecutionGateContractV1.MAX_SOURCE_BYTES) {
            return GateRun(
                "BLOCKED_RESOURCE",
                listOf("[01] SOURCE_LOCK · BLOCKED_RESOURCE · size>${ExecutionGateContractV1.MAX_SOURCE_BYTES}")
            )
        }

        val loaded = loadBounded(context, source.uri)
        lines += "[01] SOURCE_LOCK · PASS · sha256=${loaded.sha256.take(16)}… · bytes=${loaded.bytes.size}"
        lines += "[02] PREFLIGHT · RUNNING"

        val now = System.currentTimeMillis()
        val job = ExecutionGateContractV1.buildJob(
            displayName = source.displayName,
            mediaType = source.mediaType,
            sizeBytes = loaded.bytes.size.toLong(),
            contentSha256 = loaded.sha256,
            opaqueLocatorSha256 = loaded.opaqueLocatorSha256,
            nowEpochMs = now
        )
        val availableMemory = availableWorkingMemoryBytes()
        val battery = batteryPercent(context)
        lines += "[02] PREFLIGHT · observed_memory=$availableMemory · battery=$battery%"
        lines += "[03] WORKGROUP · ${job.requestedStages.joinToString("→") { it.name }}"

        val result = LibraryLocalJobExecutor().execute(
            job = job,
            input = LibraryJobInput(bytes = loaded.bytes, observedBytes = loaded.bytes.size.toLong()),
            environment = LibraryJobEnvironment(
                availableWorkingMemoryBytes = availableMemory,
                batteryPercent = battery,
                nowEpochMs = now
            )
        )

        result.receipt.completedStages.forEachIndexed { index, stage ->
            lines += "[03.${index + 1}] ${stage.name} · PASS_RECEIPT"
        }
        lines += "[04] RECEIPT · state=${result.receipt.finalState.name} · bytes=${result.receipt.bytesConsumed}"
        val descriptor = result.receipt.descriptorSha256
        lines += "[05] EVIDENCE · descriptor_sha256=${descriptor ?: "TOKEN_VAZIO"}"
        if (result.receipt.gaps.isEmpty()) {
            lines += "[06] GAPS · ∅"
        } else {
            result.receipt.gaps.forEach { lines += "[06] GAP · $it" }
        }
        lines += "[07] CLAIM_GATE · claim_allowed=${result.receipt.claimAllowed}"
        lines += "[08] CLOSE · ${if (result.receipt.finalState == LibraryJobState.SUCCEEDED) "PASS_LIMITED" else result.receipt.finalState.name}"

        GateRun(
            finalState = if (result.receipt.finalState == LibraryJobState.SUCCEEDED) "PASS_LIMITED" else result.receipt.finalState.name,
            lines = lines
        )
    } catch (exc: GateInputException) {
        GateRun("BLOCKED_RESOURCE", lines + "[FAIL] ${exc.message ?: "bounded input rejected"}")
    } catch (exc: Exception) {
        GateRun("FAIL_CLOSED", lines + "[FAIL_CLOSED] ${exc::class.java.simpleName}: ${exc.message ?: "TOKEN_VAZIO"}")
    }
}

private fun describeSource(context: Context, uri: Uri): GateSource {
    var displayName = uri.lastPathSegment ?: "source"
    var size: Long? = null
    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return GateSource(uri, displayName, context.contentResolver.getType(uri), size)
}

private fun loadBounded(context: Context, uri: Uri): LoadedSource {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    context.contentResolver.openInputStream(uri)?.use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (out.size().toLong() + read > ExecutionGateContractV1.MAX_SOURCE_BYTES) {
                throw GateInputException("SOURCE_TOO_LARGE · fail-closed before partial promotion")
            }
            out.write(buffer, 0, read)
        }
    } ?: throw GateInputException("SOURCE_OPEN_FAILED")
    val bytes = out.toByteArray()
    val sha = sha256(bytes)
    val locator = ExecutionGateContractV1.sha256(uri.toString())
    return LoadedSource(bytes, sha, locator)
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private fun availableWorkingMemoryBytes(): Long {
    val runtime = Runtime.getRuntime()
    val used = runtime.totalMemory() - runtime.freeMemory()
    return (runtime.maxMemory() - used).coerceAtLeast(0L)
}

private fun batteryPercent(context: Context): Int {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
    val level = intent.getIntExtra("level", -1)
    val scale = intent.getIntExtra("scale", -1)
    if (level < 0 || scale <= 0) return 0
    return ((level * 100L) / scale).toInt().coerceIn(0, 100)
}

private class GateInputException(message: String) : RuntimeException(message)
