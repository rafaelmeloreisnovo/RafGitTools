package com.rafgittools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rafgittools.lens.*
import com.rafgittools.ui.theme.RafGitToolsTheme
import java.io.ByteArrayOutputStream
import org.json.JSONObject

/**
 * Isolated, read-only metadata lens. Does not use PATs, dispatch workflows,
 * open external network sessions, write Drive or export private references.
 */
class FederatedVectorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RafGitToolsTheme { FederatedVectorScreen(onBack = { finish() }) } }
    }
}

private const val MAX_IMPORT_BYTES = 65536

private fun readBounded(input: java.io.InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buf = ByteArray(4096)
    while (true) {
        val n = input.read(buf)
        if (n < 0) return out.toByteArray()
        if (n == 0) continue
        require(out.size() + n <= MAX_IMPORT_BYTES) { "manifest maior que 64 KiB" }
        out.write(buf, 0, n)
    }
}

private fun importManifest(bytes: ByteArray): List<LensNode> {
    val root = JSONObject(String(bytes, Charsets.UTF_8))
    require(root.length() == 2 && root.has("schema") && root.has("nodes")) {
        "manifest deve conter somente schema + nodes"
    }
    require(root.getString("schema") == "rafgittools.federated-lens.metadata.v1") {
        "schema não reconhecido"
    }
    val rows = root.getJSONArray("nodes")
    require(rows.length() in 1..FederatedLensCore.MAX_NODES) { "1..64 nós" }
    val result = ArrayList<LensNode>(rows.length())
    for (i in 0 until rows.length()) {
        val row = rows.getJSONObject(i)
        require(row.length() == 4 &&
            row.has("logical_id") && row.has("source_kind") &&
            row.has("tokens") && row.has("gap_state")) { "campos extras ou ausentes" }
        val inputTokens = row.getJSONArray("tokens")
        val tokens = (0 until inputTokens.length()).map { inputTokens.getString(it) }
        val node = LensNode(
            row.getString("logical_id"),
            LensSource.valueOf(row.getString("source_kind")),
            tokens,
            LensGap.valueOf(row.getString("gap_state"))
        )
        FederatedLensCore.validate(node)
        result.add(node)
    }
    require(result.map { it.logicalId }.distinct().size == result.size) { "IDs duplicados" }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FederatedVectorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val nodes = remember { mutableStateListOf<LensNode>() }
    var logicalId by remember { mutableStateOf("") }
    var tokens by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(LensSource.DRIVE_PRIVATE) }
    var gap by remember { mutableStateOf(LensGap.TOKEN_VAZIO) }
    var sourceMenu by remember { mutableStateOf(false) }
    var gapMenu by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pronto: leitura local de metadados; nenhuma integração remota ativa.") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use(::readBounded)
                    ?: error("fonte SAF indisponível")
                importManifest(bytes)
            }.onSuccess { imported ->
                // Replace, never silently merge contradictory snapshots.
                nodes.clear()
                nodes.addAll(imported)
                status = "Manifest lido via SAF: ${imported.size} nós; sem escrita ou upload."
            }.onFailure { status = "HOLD: ${it.message ?: "arquivo inválido"}" }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Raf · Lente Federada Ω") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Catálogo × índices × vetores estruturais", style = MaterialTheme.typography.titleMedium)
                Text("Drive e repositórios privados: selecionar um manifest de metadados pelo SAF. " +
                    "Mapa público: somente rota lógica. Sem OAuth próprio, sem varredura automática, " +
                    "sem importar texto bruto, sem embedding semântico, sem permissão para executar pipelines.",
                    style = MaterialTheme.typography.bodySmall)
                Text("SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM · TOKEN_VAZIO ≠ 0",
                    style = MaterialTheme.typography.labelSmall)
            }
            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { picker.launch(arrayOf("application/json", "text/plain")) }) {
                            Text("Abrir manifest seguro (Drive / SAF)")
                        }
                        Text("Somente schema tipado; 64 KiB; 64 nós; 16 tokens/nó. Nenhum ID privado no APK.")
                        OutlinedTextField(value = logicalId, onValueChange = { logicalId = it.take(64) },
                            label = { Text("ID lógico, ex.: CORPUS_SHARD_000") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = tokens, onValueChange = { tokens = it.take(500) },
                            label = { Text("Tokens taxonômicos separados por vírgula (sem dados brutos)") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
                        Box {
                            OutlinedButton(onClick = { sourceMenu = true }) { Text("Fonte: ${source.name} ▾") }
                            DropdownMenu(expanded = sourceMenu, onDismissRequest = { sourceMenu = false }) {
                                LensSource.values().forEach { value ->
                                    DropdownMenuItem(text = { Text(value.name) },
                                        onClick = { source = value; sourceMenu = false })
                                }
                            }
                        }
                        Box {
                            OutlinedButton(onClick = { gapMenu = true }) { Text("Estado: ${gap.name} ▾") }
                            DropdownMenu(expanded = gapMenu, onDismissRequest = { gapMenu = false }) {
                                LensGap.values().forEach { value ->
                                    DropdownMenuItem(text = { Text(value.name) },
                                        onClick = { gap = value; gapMenu = false })
                                }
                            }
                        }
                        Button(onClick = {
                            runCatching {
                                require(nodes.size < FederatedLensCore.MAX_NODES) { "64 nós máximo" }
                                val parsed = tokens.split(',').map { it.trim() }
                                val candidate = LensNode(logicalId, source, parsed, gap)
                                FederatedLensCore.validate(candidate)
                                require(nodes.none { it.logicalId == candidate.logicalId }) { "ID duplicado" }
                                candidate
                            }.onSuccess {
                                nodes.add(it)
                                status = "Nó local registrado; persistência e custódia remota NÃO executadas."
                                logicalId = ""; tokens = ""
                            }.onFailure { status = "HOLD: ${it.message ?: "entrada inválida"}" }
                        }) { Text("Vetorizar metadados locais") }
                        Text(status, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                val edges = FederatedLensCore.candidates(nodes.toList())
                Text("Nós ${nodes.size} · relações lexicais candidatas ${edges.size}/64",
                    style = MaterialTheme.typography.titleSmall)
                Text("Relações são somente sobreposição de tokens; não implicam causalidade, verdade ou validade científica.",
                    style = MaterialTheme.typography.bodySmall)
                if (edges.isNotEmpty()) {
                    Text(edges.take(5).joinToString("\n") {
                        "${it.from} ↔ ${it.to} · ${it.commonTokens}/${it.unionTokens} (candidata)"
                    }, style = MaterialTheme.typography.bodySmall)
                }
            }
            items(nodes, key = { it.logicalId }) { node ->
                val v = FederatedLensCore.vectorize(node)
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(node.logicalId, style = MaterialTheme.typography.titleSmall)
                        Text("${node.source} · ${node.gap} · ${FederatedLensCore.urgency(node)}")
                        Text("vetor=64 dimensões; ocupados=${v.nonzeroBuckets}; digest local=${v.digestSha256.take(16)}…",
                            style = MaterialTheme.typography.bodySmall)
                        Text("GATE: ${FederatedLensCore.operationGate(node)} · no remote write · claim=false",
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            item {
                TextButton(onClick = { nodes.clear(); status = "Memória volátil limpa; nenhuma fonte alterada." }) {
                    Text("Limpar catálogo temporário")
                }
                Text("Para Actions/PATs e PRs, usar o Raf Actions com preflight independente. " +
                    "Autoridade do produtor e recibos exatos obrigatórios.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
