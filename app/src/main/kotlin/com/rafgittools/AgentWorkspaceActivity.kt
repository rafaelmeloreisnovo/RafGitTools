package com.rafgittools

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rafgittools.ui.theme.RafGitToolsTheme
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/**
 * First operational slice of the RAFAELIA agent workspace.
 *
 * Commands are proposals, never shell scripts. This activity does not connect
 * to model APIs, GitHub admin, Supabase service-role or background workers.
 * Android's Storage Access Framework lets the user choose a Drive folder
 * without storing a Google credential or downloading their corpus.
 */
class AgentWorkspaceActivity : ComponentActivity() {
    private var pendingExport: String? = null

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            val snapshot = pendingExport
            pendingExport = null
            if (uri == null || snapshot == null) return@registerForActivityResult
            try {
                contentResolver.openOutputStream(uri, "w")?.use { stream ->
                    stream.write(snapshot.toByteArray(Charsets.UTF_8))
                    stream.flush()
                } ?: error("Provider rejected write")
                Toast.makeText(this, "Proposta salva; ainda não executada", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                Toast.makeText(this, "Falha ao salvar: proposta continua local", Toast.LENGTH_LONG).show()
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RafGitToolsTheme {
                var intent by remember { mutableStateOf("") }
                var preparedIntent by remember { mutableStateOf("") }
                var proposedJson by remember { mutableStateOf<String?>(null) }
                val valid = intent.isNotBlank() &&
                    intent.toByteArray(Charsets.UTF_8).size <= MAX_INTENT_BYTES
                val readyToExport = proposedJson != null && intent == preparedIntent

                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(topBar = { TopAppBar(title = { Text("RAFAELIA Agent Workspace") }) }) { padding ->
                        Column(
                            modifier = Modifier.fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Escreva seu comando", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "A proposta só será exportada depois da sua revisão. " +
                                    "Nenhum agente ou build será acionado nesta etapa.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = intent,
                                onValueChange = { intent = it; proposedJson = null },
                                label = { Text("Intenção / tarefa") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Destino inicial: rafaelmeloreisnovo/RafGitTools. " +
                                    "Não inclua PATs, senhas ou dados privados no comando.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                enabled = valid,
                                onClick = {
                                    preparedIntent = intent.trim()
                                    proposedJson = envelope(preparedIntent)
                                }
                            ) { Text("Preparar proposta para revisão") }
                            proposedJson?.let { proposal ->
                                Text("Estado: PENDING_HUMAN / NOT_EXECUTED")
                                Text(proposal, style = MaterialTheme.typography.bodySmall)
                                Button(
                                    enabled = readyToExport,
                                    onClick = {
                                        pendingExport = proposal
                                        val id = JSONObject(proposal).getString("interaction_id")
                                        createDocument.launch("RAFAELIA-REQ-$id.json")
                                    }
                                ) { Text("Salvar JSON no Drive ou armazenamento escolhido") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "A exportação é somente um envelope de intenção. " +
                                    "Uma aprovação confiável, um executor autenticado e " +
                                    "receipts verificados continuam obrigatórios.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    private fun envelope(intent: String): String {
        require(intent.isNotBlank() && intent.toByteArray(Charsets.UTF_8).size <= MAX_INTENT_BYTES)
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(intent.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return JSONObject()
            .put("schema", "rafaelia.agent-interaction.v1")
            .put("interaction_id", UUID.randomUUID().toString())
            .put("origin", "RAFGITTOOLS_APK")
            .put("target_repository", "rafaelmeloreisnovo/RafGitTools")
            .put("requested_action", "PROPOSE_CHANGE")
            .put("intent", intent)
            .put("intent_sha256", sha)
            .put("created_at_utc", Instant.now().toString())
            .put("approval_state", "PENDING_HUMAN")
            .put("execution_state", "NOT_EXECUTED")
            .put("claim_allowed", false)
            .put("receipt_ref", "TOKEN_VAZIO")
            .toString(2)
    }

    private companion object {
        const val MAX_INTENT_BYTES = 4096
    }
}
