package com.rafgittools.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rafgittools.RafGitFsActivity
import com.rafgittools.domain.model.github.GithubRepository
import com.rafgittools.navigator.LocalGithubInventoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

/** GitHub list becomes an explicitly saved, app-private offline catalogue, not a clone. */
@Composable
fun LocalGithubInventoryCard(
    githubList: List<GithubRepository>,
    profile: String?,
    listingComplete: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { LocalGithubInventoryStore(context.applicationContext) }
    var saved by remember { mutableStateOf<LocalGithubInventoryStore.Saved?>(null) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("TOKEN_VAZIO_LOCAL_SNAPSHOT_NOT_OBSERVED") }
    var search by remember { mutableStateOf("") }
    var acceptsPrivateLocalMetadata by remember { mutableStateOf(false) }
    LaunchedEffect(store) {
        val previous = withContext(Dispatchers.IO) { runCatching { store.latest() } }
        previous.onSuccess { saved = it }
            .onFailure { result = "TOKEN_VAZIO_LOCAL_READBACK_FAILED" }
    }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🦉 Inventário GitHub guardado no celular",
                style = MaterialTheme.typography.titleMedium)
            Text(
                "Salva nomes, descrições, linguagem, branch, visibilidade e contagens " +
                    "de repositórios no espaço privado do APK. Não faz clone, não " +
                    "abre conversas, não envia dados ao Drive nem à CI.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Fonte atual: " + githubList.size + " repositórios" +
                    (if (listingComplete) " · paginação concluída" else " · TOKEN_VAZIO_COBERTURA"),
                style = MaterialTheme.typography.bodySmall
            )
            Row {
                Checkbox(
                    checked = acceptsPrivateLocalMetadata,
                    onCheckedChange = { acceptsPrivateLocalMetadata = it }
                )
                Text("Entendo que nomes de repositórios privados continuarão guardados no aparelho, " +
                    "inclusive para pesquisa sem internet.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    if (profile.isNullOrBlank()) {
                        result = "TOKEN_VAZIO_GITHUB_PROFILE_REQUIRED"
                    } else {
                        scope.launch {
                            busy = true
                            result = "LOCAL_INDEX_WRITING"
                            val attempt = withContext(Dispatchers.IO) {
                                runCatching {
                                    val record = LocalGithubInventoryStore.observe(
                                        profile, githubList, listingComplete, System.currentTimeMillis()
                                    )
                                    store.save(record)
                                }
                            }
                            attempt.onSuccess {
                                saved = it
                                result = "LOCAL_INDEX_SHA256_READBACK_PASS"
                            }.onFailure {
                                result = "BLOCKED_LOCAL_SNAPSHOT_OR_QUOTA"
                            }
                            busy = false
                        }
                    }
                },
                enabled = !busy && acceptsPrivateLocalMetadata && profile != null && githubList.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Verificando…" else "Guardar inventário atual no Local") }

            saved?.let { item ->
                val snapshot = item.snapshot
                val publicCount = snapshot.repositories.count { it.visibility == "PUBLIC" }
                val privateCount = snapshot.repositories.count { it.visibility == "PRIVATE" }
                Text("Último snapshot · @" + snapshot.profile + " · " + item.bytes + " bytes",
                    style = MaterialTheme.typography.bodySmall)
                Text(
                    snapshot.observed_repositories.toString() + " repositórios observados · " +
                        publicCount + " públicos · " + privateCount + " privados · " +
                        snapshot.repositories.count { it.is_fork } + " forks",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("SHA-256 " + item.sha256.take(24) + "… · " + snapshot.enumeration,
                    style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it.take(100) },
                    label = { Text("Pesquisar repos, linguagem ou descrição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val hits = snapshot.repositories.filter {
                    search.isBlank() ||
                        it.full_name.contains(search, ignoreCase = true) ||
                        it.language.orEmpty().contains(search, ignoreCase = true) ||
                        it.description.orEmpty().contains(search, ignoreCase = true)
                }
                Text("Encontrados: " + hits.size + " · exibidos: " + hits.take(20).size,
                    style = MaterialTheme.typography.bodySmall)
                hits.take(20).forEach { repo ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(repo.full_name, style = MaterialTheme.typography.titleSmall)
                        Text(repo.visibility + " · " + repo.default_branch +
                            " · " + (repo.language ?: "TOKEN_VAZIO_LANGUAGE") +
                            " · issues " + repo.open_issues_reported,
                            style = MaterialTheme.typography.bodySmall)
                        repo.description?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
                Text(
                    "Não verificado neste snapshot: workflows, arquivos, checks, dependências, " +
                        "segurança, outras contas e conteúdo das conversas. A pesquisa funciona " +
                        "offline sobre estes metadados.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, RafGitFsActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Abrir RafGitFS — branches, arquivos e busca") }
            Text(result, style = MaterialTheme.typography.labelSmall)
            Text("Local significa armazenamento privado do app. " +
                "Uma cópia em Drive/GitHub exige autorização separada.",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
