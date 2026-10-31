package com.rafgittools

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rafgittools.benchmark.ContextualCreditsInfo
import com.rafgittools.benchmark.RafBenchmarkCore
import com.rafgittools.benchmark.RafBenchmarkExchange
import com.rafgittools.benchmark.RafApplicationsCatalogView
import com.rafgittools.benchmark.RafBenchmarkCredits
import com.rafgittools.benchmark.RafBenchmarkReceiptStore
import com.rafgittools.ui.theme.RafGitToolsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.io.ByteArrayOutputStream

/**
 * Raf Benchmark: additional launcher INSIDE RafGitTools, not another APK/UID.
 * No arbitrary source execution, token reads, network requests or auto-sync.
 * Run requires a human click; receipts stay in private app storage.
 */
class RafBenchmarkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RafGitToolsTheme { RafBenchmarkScreen(onBack = { finish() }) } }
    }
}

private fun devicePreflight(context: Context, bufferKiB: Int) {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    require(!info.lowMemory && info.availMem >= bufferKiB.toLong() * 1024L * 3L) {
        "BLOCKED_RESOURCE: memória livre insuficiente"
    }
    val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val percent = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    require(percent !in 0..14) { "BLOCKED_BATTERY: bateria abaixo de 15%" }
    val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    require(!power.isPowerSaveMode) { "BLOCKED_POWER_SAVE: economia de energia ativa" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RafBenchmarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var workers by remember { mutableStateOf(1) }
    var bufferKiB by remember { mutableStateOf(256) }
    var workload by remember { mutableStateOf(RafBenchmarkCore.Workload.STREAM_WRITE) }
    var running by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("NOT_EXECUTED · sem acesso remoto e sem sincronização") }
    var receipt by remember { mutableStateOf("") }
    var lastReceiptId by remember { mutableStateOf("") }
    var proposal by remember { mutableStateOf<RafBenchmarkExchange.Proposal?>(null) }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream()
                    val chunk = ByteArray(4096)
                    while (true) {
                        val n = input.read(chunk)
                        if (n < 0) break
                        require(n > 0 && out.size() + n <= RafBenchmarkExchange.MAX_BYTES) {
                            "BLOCKED_INPUT_SIZE"
                        }
                        out.write(chunk, 0, n)
                    }
                    out.toByteArray()
                } ?: error("SOURCE_UNAVAILABLE")
                RafBenchmarkExchange.parse(bytes)
            }.onSuccess { item ->
                proposal = item
                workers = item.workers
                bufferKiB = item.bufferKiB
                workload = item.workload
                output = "PROPOSED (não executado) · " + item.id +
                    " · source=" + item.producer + "@" + item.sourceRevision.take(12) +
                    " · digest=" + item.digest.take(12)
            }.onFailure { e ->
                proposal = null
                output = "HOLD_IMPORT · " + (e.message ?: "invalid proposal")
            }
        }
    }
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && lastReceiptId.isNotEmpty()) {
            runCatching {
                val text = RafBenchmarkReceiptStore(context).exportJson(lastReceiptId)
                context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                    stream.write(text.toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: error("SAF_DESTINATION_UNAVAILABLE")
            }.onSuccess {
                output = "SAF_EXPORT_COMPLETE · destino escolhido pelo usuário; " +
                    "isso não atesta o processamento do provedor."
            }.onFailure { e ->
                output = "HOLD_EXPORT · " + (e.message ?: "falha de escrita")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raf Benchmark") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
                actions = {
                    ContextualCreditsInfo(RafBenchmarkCredits.benchmarkScreen)
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
                Text("Silício · buffers × workers", style = MaterialTheme.typography.titleMedium)
                Text("Medição local exploratória. 1, 2, 4 ou 8 workers lógicos não representam " +
                    "necessariamente 1, 2, 4 ou 8 núcleos físicos. " +
                    "Não mede largura de banda DRAM nem L3 diretamente.",
                    style = MaterialTheme.typography.bodySmall)
            }
            item { RafApplicationsCatalogView() }
            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Workers (threads)", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RafBenchmarkCore.allowedWorkers.forEach { candidate ->
                                FilterChip(selected = workers == candidate, enabled = !running,
                                    onClick = { workers = candidate; proposal = null },
                                    label = { Text("$candidate") })
                            }
                        }
                        Text("Buffer TOTAL do teste", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RafBenchmarkCore.allowedBufferKiB.forEach { candidate ->
                                FilterChip(selected = bufferKiB == candidate, enabled = !running,
                                    onClick = { bufferKiB = candidate; proposal = null },
                                    label = { Text(if (candidate < 1024) "256K"
                                        else "${candidate / 1024}M") })
                            }
                        }
                        Text("Carga de teste (mesma entrada/saída, independente de workers)",
                            style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RafBenchmarkCore.allowedWorkloads.forEach { candidate ->
                                FilterChip(
                                    selected = workload == candidate,
                                    enabled = !running,
                                    onClick = { workload = candidate; proposal = null },
                                    label = { Text(when (candidate) {
                                        RafBenchmarkCore.Workload.STREAM_WRITE -> "Escrita"
                                        RafBenchmarkCore.Workload.CACHE_REUSE -> "Reuso"
                                        RafBenchmarkCore.Workload.COMPUTE_ONLY -> "CPU"
                                    }) }
                                )
                            }
                        }
                        Text("4 rodadas determinísticas · sintéticos. Throughput de trabalho efetivo, " +
                            "não banda DRAM/L3. JIT, escalonamento e temperatura afetam medições.",
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            importPicker.launch(arrayOf("application/json", "text/plain"))
                        }, enabled = !running) {
                            Text("Importar proposta do Drive / arquivo (SAF)")
                        }
                        if (proposal != null) {
                            Text("Proposta importada (somente parâmetros), requer clique humano. " +
                                "SHA-256: " + (proposal?.digest ?: "TOKEN_VAZIO"),
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            enabled = !running,
                            onClick = {
                                running = true
                                output = "RUNNING · aguarde encerramento local"
                                receipt = ""
                                lastReceiptId = ""
                                val selectedProposal = proposal
                                val selectedWorkload = workload
                                scope.launch {
                                    val outcome = withContext(Dispatchers.Default) {
                                        runCatching {
                                            if (selectedProposal != null) {
                                                require(selectedProposal.workers == workers &&
                                                    selectedProposal.bufferKiB == bufferKiB &&
                                                    selectedProposal.workload == selectedWorkload) {
                                                    "BLOCKED_PROPOSAL_MISMATCH"
                                                }
                                            }
                                            devicePreflight(context, bufferKiB)
                                            val result = RafBenchmarkCore.run(workers, bufferKiB, selectedWorkload)
                                            val stored = RafBenchmarkReceiptStore(context).append(result,
                                                selectedProposal?.digest ?: "TOKEN_VAZIO_MANUAL_JOB")
                                            val verified = RafBenchmarkReceiptStore(context).verify()
                                            Triple(result, stored, verified)
                                        }
                                    }
                                    outcome.onSuccess { (result, stored, verified) ->
                                        output = String.format(Locale.US,
                                            "EVIDENCED_LOCAL_ONLY [%s] · %.2f MiB/s efetivos · %.3f ms · " +
                                            "checksum=%016x · recebimentos locais=%d · claim_allowed=false",
                                            result.workload.name, result.throughputMiBs,
                                            result.elapsedNs / 1_000_000.0,
                                            result.checksum, verified)
                                        lastReceiptId = stored.id
                                        receipt = "SHA-256 chain: " + stored.hash +
                                            "\nID: " + stored.id +
                                            "\nSem âncora externa ou atestação de hardware."
                                    }.onFailure { error ->
                                        output = "HOLD · " + (error.message ?: error.javaClass.simpleName)
                                    }
                                    running = false
                                }
                            }
                        ) { Text("Autorizar e executar ESTE teste") }
                        if (running) CircularProgressIndicator()
                    }
                }
            }
            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resultado e recibo", style = MaterialTheme.typography.titleSmall)
                        Text(output, style = MaterialTheme.typography.bodySmall)
                        if (receipt.isNotEmpty()) {
                            Text(receipt, style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                exportPicker.launch("rafbenchmark-" + lastReceiptId + ".json")
                            }, enabled = !running && lastReceiptId.isNotEmpty()) {
                                Text("Exportar recibo (escolher pasta no Drive/SAF)")
                            }
                        }
                    }
                }
            }
            item {
                Text("ⓘ Preto: esta tela usa APIs externas, com créditos contextuais. " +
                    "Cinza/desabilitado: somente se todos os componentes do escopo forem " +
                    "verificados como autorais. Unknown → informação ativa.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Não usa GitHub secrets, não chama agentes, não grava no Drive, " +
                    "não importa B7/Vectras/Termux automaticamente. Propostas via SAF " +
                    "são dados não confiáveis e nunca autorizam execução por si. " +
                    "A exportação de receipt depende da escolha explícita do usuário.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
