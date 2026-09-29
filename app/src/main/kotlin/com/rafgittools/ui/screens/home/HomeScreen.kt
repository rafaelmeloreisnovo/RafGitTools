package com.rafgittools.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rafgittools.bridge.CatalogTreeExportResult
import com.rafgittools.bridge.CorpusCatalogTreeGate
import com.rafgittools.bridge.CorpusIntakeGate
import com.rafgittools.bridge.CorpusIntakeResult
import com.rafgittools.bridge.DriveStagingGate
import com.rafgittools.navigator.ConversationManifoldProcessor
import com.rafgittools.navigator.ConversationManifoldPublication
import com.rafgittools.ui.components.ResponsiveContentFrame
import com.rafgittools.domain.model.github.GithubRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Main source dashboard.
 *
 * GitHub is one source, Google Drive is a source bridge, and Local is the
 * staging/workbench layer. Drive uses Android's Storage Access Framework so
 * the user's Google account stays under the system/Drive provider; RafGitTools
 * never asks for a Google password.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToRepository: (GithubRepository) -> Unit = {},
    onNavigateToLocalRepo: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val user by viewModel.user.collectAsStateWithLifecycle()
    val remoteRepositories by viewModel.remoteRepositories.collectAsStateWithLifecycle()
    val localRepositories by viewModel.localRepositories.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val githubProbeState by viewModel.githubProbeState.collectAsStateWithLifecycle()
    val remoteReceiptState by viewModel.remoteReceiptState.collectAsStateWithLifecycle()
    val privateProcessingPublishState by viewModel.privateProcessingPublishState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RafGitTools") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    if (isAuthenticated) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                val u = user
                                if (u != null) {
                                    AsyncImage(
                                        model = u.avatarUrl,
                                        contentDescription = "User avatar",
                                        modifier = Modifier.size(30.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.AccountCircle, contentDescription = "Account")
                                }
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                user?.let { u ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(u.name ?: u.login, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    "@${u.login}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { showMenu = false },
                                        leadingIcon = { Icon(Icons.Default.Person, null) }
                                    )
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text("Atualizar") },
                                    onClick = { showMenu = false; viewModel.refresh() },
                                    leadingIcon = { Icon(Icons.Default.Refresh, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Desconectar GitHub") },
                                    onClick = { showMenu = false; viewModel.logout() },
                                    leadingIcon = { Icon(Icons.Default.Logout, null) }
                                )
                            }
                        }
                    } else {
                        TextButton(onClick = onNavigateToAuth) { Text("GitHub") }
                    }
                }
            )
        }
    ) { padding ->
        ResponsiveContentFrame(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                HomeUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is HomeUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = viewModel::refresh,
                    modifier = Modifier.align(Alignment.Center)
                )
                HomeUiState.NotAuthenticated,
                HomeUiState.Empty,
                HomeUiState.Success -> SourceDashboard(
                    activeTab = activeTab,
                    isGithubAuthenticated = isAuthenticated,
                    remoteRepositories = remoteRepositories,
                    localRepositories = localRepositories,
                    githubProbeState = githubProbeState,
                    remoteReceiptState = remoteReceiptState,
                    privateProcessingPublishState = privateProcessingPublishState,
                    onTabSelected = viewModel::setActiveTab,
                    onNavigateToAuth = onNavigateToAuth,
                    onRepositoryClick = onNavigateToRepository,
                    onLocalRepositoryClick = onNavigateToLocalRepo,
                    onRunGithubTest = viewModel::runGithubConnectivityTest,
                    onCreateRemoteReceipt = viewModel::createRemoteConnectivityReceipt,
                    onPublishPrivateProcessingReceipt = viewModel::publishPrivateProcessingReceipt,
                    onPublishManifold = viewModel::publishConversationManifold
                )
            }
        }
    }
}

@Composable
private fun SourceDashboard(
    activeTab: HomeViewModel.HomeTab,
    isGithubAuthenticated: Boolean,
    remoteRepositories: List<GithubRepository>,
    localRepositories: List<LocalRepoSummary>,
    githubProbeState: GithubConnectivityState,
    remoteReceiptState: RemoteReceiptState,
    privateProcessingPublishState: PrivateProcessingPublishState,
    onTabSelected: (HomeViewModel.HomeTab) -> Unit,
    onNavigateToAuth: () -> Unit,
    onRepositoryClick: (GithubRepository) -> Unit,
    onLocalRepositoryClick: (String) -> Unit,
    onRunGithubTest: () -> Unit,
    onCreateRemoteReceipt: () -> Unit,
    onPublishPrivateProcessingReceipt: (
        GithubRepository,
        Long,
        String,
        String,
        CorpusIntakeResult
    ) -> Unit,
    onPublishManifold: suspend (GithubRepository, ConversationManifoldPublication.Plan, String, android.content.ContentResolver) -> Result<ConversationManifoldPublication.Published>
) {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = activeTab.ordinal) {
            Tab(
                selected = activeTab == HomeViewModel.HomeTab.REMOTE,
                onClick = { onTabSelected(HomeViewModel.HomeTab.REMOTE) },
                text = { Text("GitHub (${remoteRepositories.size})", maxLines = 1) }
            )
            Tab(
                selected = activeTab == HomeViewModel.HomeTab.DRIVE,
                onClick = { onTabSelected(HomeViewModel.HomeTab.DRIVE) },
                text = { Text("Drive", maxLines = 1) }
            )
            Tab(
                selected = activeTab == HomeViewModel.HomeTab.LOCAL,
                onClick = { onTabSelected(HomeViewModel.HomeTab.LOCAL) },
                text = { Text("Local (${localRepositories.size})", maxLines = 1) }
            )
        }

        when (activeTab) {
            HomeViewModel.HomeTab.REMOTE -> {
                if (isGithubAuthenticated) {
                    RepositoryList(
                        repositories = remoteRepositories,
                        githubProbeState = githubProbeState,
                        remoteReceiptState = remoteReceiptState,
                        onRunGithubTest = onRunGithubTest,
                        onCreateRemoteReceipt = onCreateRemoteReceipt,
                        onRepositoryClick = onRepositoryClick
                    )
                } else {
                    GithubDisconnectedContent(onNavigateToAuth)
                }
            }
            HomeViewModel.HomeTab.DRIVE -> DriveBridgeContent(
                privateRepositories = remoteRepositories.filter { it.isPrivate },
                publishState = privateProcessingPublishState,
                onPublishReceipt = onPublishPrivateProcessingReceipt,
                onPublishManifold = onPublishManifold
            )
            HomeViewModel.HomeTab.LOCAL -> LocalRepositoryList(localRepositories, onLocalRepositoryClick)
        }
    }
}

@Composable
private fun GithubDisconnectedContent(onNavigateToAuth: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Key, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("GitHub desconectado", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "O Drive e o ambiente local continuam disponíveis. Para repositórios remotos, conecte com um token gerado no GitHub.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToAuth) {
            Icon(Icons.Default.Login, null)
            Spacer(Modifier.width(8.dp))
            Text("Conectar GitHub")
        }
    }
}

@Composable
private fun DriveBridgeContent(
    privateRepositories: List<GithubRepository>,
    publishState: PrivateProcessingPublishState,
    onPublishReceipt: (GithubRepository, Long, String, String, CorpusIntakeResult) -> Unit,
    onPublishManifold: suspend (GithubRepository, ConversationManifoldPublication.Plan, String, android.content.ContentResolver) -> Result<ConversationManifoldPublication.Published>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var repositoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedPrivateRepository by remember(privateRepositories) {
        mutableStateOf(privateRepositories.firstOrNull())
    }
    var staging by remember { mutableStateOf(false) }
    var staged by remember { mutableStateOf<DriveStageResult?>(null) }
    var cataloging by remember { mutableStateOf(false) }
    var cataloged by remember { mutableStateOf<CorpusIntakeResult?>(null) }
    var exportingCatalog by remember { mutableStateOf(false) }
    var exportedCatalog by remember { mutableStateOf<CatalogTreeExportResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var processingManifold by remember { mutableStateOf(false) }
    var manifoldResult by remember { mutableStateOf<ConversationManifoldProcessor.Result?>(null) }
    var manifoldPlan by remember { mutableStateOf<ConversationManifoldPublication.Plan?>(null) }
    var publishingManifold by remember { mutableStateOf(false) }
    var manifoldPublishSummary by remember { mutableStateOf<String?>(null) }

    val catalogFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val currentCatalog = cataloged
            if (currentCatalog == null) {
                error = "Catalogue o corpus antes de escolher a pasta de exportação"
            } else {
                error = null
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Some providers grant only the current operation; export can still proceed now.
                }

                scope.launch {
                    exportingCatalog = true
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            CorpusCatalogTreeGate.exportCatalog(
                                context = context,
                                treeUri = uri,
                                artifacts = listOf(
                                    currentCatalog.manifestFile,
                                    currentCatalog.publicProjectionFile
                                )
                            )
                        }
                    }
                    result.onSuccess { exportedCatalog = it }
                        .onFailure { error = it.message ?: "Falha ao exportar catálogo privado" }
                    exportingCatalog = false
                }
            }
        }
    }

    val manifoldDestinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                error = "O provedor não concedeu acesso persistente de gravação; selecione uma pasta acessível no Drive."
            }
            val processed = manifoldResult
            val target = selectedPrivateRepository
            if (processed == null) error = "Processe primeiro um conversations*.json ou codex*.json."
            else if (target == null) error = "Selecione um repositório GitHub privado antes de planejar a publicação."
            else scope.launch {
                manifoldPlan = null
                val planned = withContext(Dispatchers.IO) {
                    runCatching {
                        ConversationManifoldPublication.plan(
                            generationId = "g${System.currentTimeMillis()}",
                            driveFolderUri = uri,
                            githubOwner = target.owner.login,
                            githubRepository = target.name,
                            artifact = processed.outputFile
                        )
                    }
                }
                planned.onSuccess { manifoldPlan = it }
                    .onFailure { error = it.message ?: "Falha ao criar plano de publicação" }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            error = null
            cataloged = null
            exportedCatalog = null
            manifoldResult = null
            manifoldPlan = null
            manifoldPublishSummary = null
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers grant only the current read; staging can still proceed now.
            }

            scope.launch {
                staging = true
                val result = stageDriveDocument(context, uri)
                result.onSuccess { staged = it }
                    .onFailure { error = it.message ?: "Falha ao importar arquivo do Drive" }
                staging = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Drive / SAF → staging local", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Este botão abre o seletor de documentos do Android; ele não faz login Google dentro do RafGitTools. Se o provedor Google Drive estiver instalado e com conta disponível no aparelho, ele aparece no seletor. O app recebe somente acesso de leitura ao arquivo escolhido.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            picker.launch(
                                arrayOf(
                                    "application/json",
                                    "application/zip",
                                    "text/plain",
                                    "application/octet-stream"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !staging
                    ) {
                        if (staging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizando…")
                        } else {
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir seletor de arquivos")
                        }
                    }
                }
            }
        }

        staged?.let { item ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Staging pronto", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${formatBytes(item.bytes)} · SHA-256 ${item.sha256.take(16)}…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Local privado: ${item.path}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Provider: ${item.providerAuthority}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Gate: STAGED_VERIFIED · GitHub recipient = TOKEN_VAZIO",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Receipt local: ${item.receiptPath}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    cataloging = true
                                    error = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            CorpusIntakeGate.catalog(
                                                stagedFile = File(item.path),
                                                sourceProviderAuthority = item.providerAuthority,
                                                sourceDisplayName = item.name,
                                                expectedSha256 = item.sha256
                                            )
                                        }
                                    }
                                    result.onSuccess { cataloged = it }
                                        .onFailure { error = it.message ?: "Falha ao catalogar corpus" }
                                    cataloging = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !cataloging
                        ) {
                            if (cataloging) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Catalogando…")
                            } else {
                                Icon(Icons.Default.FactCheck, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Catalogar corpus (privado)")
                            }
                        }
                    }
                }
            }
        }

        staged?.let { stagedItem ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Processar no RafGitTools", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "O telefone lê somente este arquivo selecionado e gera JSONL privado. Para processar outros shards, selecione cada arquivo; inventário recursivo da pasta ainda não está ligado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    processingManifold = true
                                    manifoldResult = null
                                    manifoldPlan = null
                                    manifoldPublishSummary = null
                                    error = null
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching {
                                            FileInputStream(File(stagedItem.path)).use { source ->
                                                ConversationManifoldProcessor(
                                                    outputRoot = File(context.filesDir, "conversation-manifold")
                                                ).process(stagedItem.name, source)
                                            }
                                        }
                                    }
                                    result.onSuccess { manifoldResult = it }
                                        .onFailure { error = it.message ?: "Falha no processamento JSON" }
                                    processingManifold = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !processingManifold && !publishingManifold
                        ) {
                            if (processingManifold) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Processando no telefone…")
                            } else {
                                Icon(Icons.Default.AccountTree, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Processar JSON selecionado")
                            }
                        }
                    }
                }
            }
        }

        manifoldResult?.let { processed ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Processamento local concluído", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${processed.sourceName} · ${formatBytes(processed.sourceBytes)} · SHA-256 ${processed.sourceSha256}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Registros ${processed.records} · nós ${processed.nodes} · mensagens ${processed.messages} · Codex ${processed.codexRecords}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Saída derivada SHA-256: ${processed.outputSha256}", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { manifoldDestinationPicker.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedPrivateRepository?.isPrivate == true && !publishingManifold
                        ) {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Escolher pasta de saída no Drive")
                        }
                    }
                }
            }
        }

        manifoldPlan?.let { plan ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Plano de publicação", style = MaterialTheme.typography.titleSmall)
                        Text("Destino Git privado: ${plan.githubOwner}/${plan.githubRepository}")
                        Text("Partes: ${plan.parts.size} · SHA-256 do plano: ${plan.planSha256}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Ao confirmar, o APK grava as partes derivadas na pasta Drive selecionada e em memory_bridge/private_processing/conversation_manifold/. A verificação de leitura de volta ainda está pendente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val target = selectedPrivateRepository
                                if (target == null) {
                                    error = "O repositório privado selecionado desapareceu."
                                } else {
                                    scope.launch {
                                        publishingManifold = true
                                        error = null
                                        val result = onPublishManifold(
                                            target, plan, plan.planSha256, context.contentResolver
                                        )
                                        result.onSuccess { published ->
                                            manifoldPublishSummary =
                                                "PUBLISHED_UNVERIFIED_READBACK_PENDING · Drive ${published.driveUris.size} itens · Git ${published.githubPaths.size} caminhos · receipt SHA-256 ${published.artifactSha256}"
                                        }.onFailure {
                                            error = it.message ?: "Falha ao publicar o manifold"
                                        }
                                        publishingManifold = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !publishingManifold && selectedPrivateRepository?.isPrivate == true
                        ) {
                            if (publishingManifold) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Publicando no Drive e no Git privado…")
                            } else {
                                Text("Confirmar plano e publicar")
                            }
                        }
                    }
                }
            }
        }

        manifoldPublishSummary?.let { summary ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(summary, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        cataloged?.let { result ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Catálogo privado pronto", style = MaterialTheme.typography.titleSmall)
                        }
                        Text("Intake: ${result.intakeId}", style = MaterialTheme.typography.bodySmall)
                        Text("Tipo: ${result.contentKind}", style = MaterialTheme.typography.bodySmall)
                        result.structuralVector?.let { vector ->
                            Text(
                                "Vetor estrutural: obj=${vector.objects}, arrays=${vector.arrays}, nomes=${vector.names}, strings=${vector.strings}, números=${vector.numbers}, depth=${vector.maxDepth}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Risk handle público opaco: ${result.publicRiskHandle}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Manifesto privado: ${result.manifestFile.absolutePath}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Embedding semântico: TOKEN_VAZIO_EXPLICIT_PROVIDER_REQUIRED",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { catalogFolderPicker.launch(null) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !exportingCatalog
                        ) {
                            if (exportingCatalog) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Exportando…")
                            } else {
                                Icon(Icons.Default.Upload, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Exportar catálogo para pasta privada")
                            }
                        }
                        exportedCatalog?.let { exported ->
                            Text(
                                "Export: PASS · ${exported.exportedArtifactCount} artefatos · provider ${exported.destinationProviderAuthority}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Receipt local: ${exported.localReceiptFile.absolutePath}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        HorizontalDivider()
                        Text(
                            "GitHub privado · receipt de atividade",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (privateRepositories.isEmpty()) {
                            Text(
                                "Destino: TOKEN_VAZIO — conecte o GitHub e carregue um repositório privado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Box {
                                OutlinedButton(
                                    onClick = { repositoryMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Lock, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        selectedPrivateRepository?.fullName
                                            ?: "Selecionar repositório privado",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DropdownMenu(
                                    expanded = repositoryMenuExpanded,
                                    onDismissRequest = { repositoryMenuExpanded = false }
                                ) {
                                    privateRepositories.forEach { repository ->
                                        DropdownMenuItem(
                                            text = { Text(repository.fullName) },
                                            onClick = {
                                                selectedPrivateRepository = repository
                                                repositoryMenuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.Lock, null) }
                                        )
                                    }
                                }
                            }

                            val stage = staged
                            Button(
                                onClick = {
                                    val target = selectedPrivateRepository
                                    if (target != null && stage != null) {
                                        onPublishReceipt(
                                            target,
                                            stage.bytes,
                                            stage.sha256,
                                            stage.providerAuthority,
                                            result
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedPrivateRepository != null &&
                                    stage != null &&
                                    publishState !is PrivateProcessingPublishState.Running
                            ) {
                                if (publishState is PrivateProcessingPublishState.Running) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Publicando receipt…")
                                } else {
                                    Icon(Icons.Default.Upload, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Publicar receipt privado")
                                }
                            }
                        }

                        when (publishState) {
                            PrivateProcessingPublishState.Idle -> Text(
                                "GitHub privado: TOKEN_VAZIO",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PrivateProcessingPublishState.Running -> Unit
                            is PrivateProcessingPublishState.Passed -> Text(
                                "GitHub privado: PASS · ${publishState.operationId} · commit ${publishState.commitSha.take(12)}…",
                                style = MaterialTheme.typography.bodySmall
                            )
                            is PrivateProcessingPublishState.Failed -> Text(
                                "GitHub privado: FAIL · ${publishState.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        error?.let { message ->
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Text(
                "O local funciona como meio de campo: o arquivo escolhido é copiado em streaming para o armazenamento privado do app, sem carregar o corpus inteiro na memória.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class DriveStageResult(
    val name: String,
    val path: String,
    val bytes: Long,
    val sha256: String,
    val receiptPath: String,
    val providerAuthority: String
)

private suspend fun stageDriveDocument(context: Context, uri: Uri): Result<DriveStageResult> =
    withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val displayName = resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "drive_import_${System.currentTimeMillis()}.bin"

            val safeName = displayName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .take(120)
                .ifBlank { "drive_import_${System.currentTimeMillis()}.bin" }

            val stagingDir = File(context.filesDir, "drive-staging")
            if (!stagingDir.exists() && !stagingDir.mkdirs()) {
                throw IOException("Não foi possível criar o staging local")
            }

            val finalFile = File(stagingDir, safeName)
            val partFile = File(stagingDir, "$safeName.part")
            if (partFile.exists() && !partFile.delete()) {
                throw IOException("Não foi possível limpar staging anterior")
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            val buffer = ByteArray(64 * 1024)

            val input = resolver.openInputStream(uri)
                ?: throw IOException("O provedor não abriu o arquivo selecionado")
            input.use { source ->
                partFile.outputStream().buffered(64 * 1024).use { target ->
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        target.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        total += read.toLong()
                    }
                    target.flush()
                }
            }

            if (finalFile.exists() && !finalFile.delete()) {
                throw IOException("Não foi possível substituir o staging anterior")
            }
            if (!partFile.renameTo(finalFile)) {
                throw IOException("Não foi possível promover o arquivo .part para staging completo")
            }

            val sourceStreamSha256 = digest.digest().joinToString("") { "%02x".format(it) }
            val receiptFile = try {
                DriveStagingGate.verifyAndWriteReceipt(
                    stagedFile = finalFile,
                    sourceProviderAuthority = uri.authority,
                    sourceDisplayName = displayName,
                    expectedBytes = total,
                    expectedSha256 = sourceStreamSha256
                )
            } catch (error: Exception) {
                finalFile.delete()
                throw error
            }

            DriveStageResult(
                name = safeName,
                path = finalFile.absolutePath,
                bytes = total,
                sha256 = sourceStreamSha256,
                receiptPath = receiptFile.absolutePath,
                providerAuthority = uri.authority ?: "TOKEN_VAZIO"
            )
        }
    }

@Composable
private fun RepositoryList(
    repositories: List<GithubRepository>,
    githubProbeState: GithubConnectivityState,
    remoteReceiptState: RemoteReceiptState,
    onRunGithubTest: () -> Unit,
    onCreateRemoteReceipt: () -> Unit,
    onRepositoryClick: (GithubRepository) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GithubConnectivityCard(
                probeState = githubProbeState,
                receiptState = remoteReceiptState,
                onRunProbe = onRunGithubTest,
                onCreateRemoteReceipt = onCreateRemoteReceipt
            )
        }
        item {
            Text(
                "Repositórios (${repositories.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        items(repositories, key = { it.id }) { repo ->
            RepositoryCard(repo) { onRepositoryClick(repo) }
        }
    }
}

@Composable
private fun GithubConnectivityCard(
    probeState: GithubConnectivityState,
    receiptState: RemoteReceiptState,
    onRunProbe: () -> Unit,
    onCreateRemoteReceipt: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Diagnóstico GitHub", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "READ faz uma chamada remota sem aceitar cache. WRITE cria, somente após seu toque, uma Issue de receipt no próprio RafGitTools. Nenhum token, arquivo local ou conteúdo do Drive é enviado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (probeState) {
                GithubConnectivityState.Idle -> Text("GitHub READ: TOKEN_VAZIO")
                GithubConnectivityState.Running -> Text("GitHub READ: testando…")
                is GithubConnectivityState.Passed ->
                    Text("GitHub READ: PASS · @${probeState.login} · ${probeState.loadedRepositories} repositórios carregados")
                is GithubConnectivityState.Failed ->
                    Text("GitHub READ: FAIL · ${probeState.message}", color = MaterialTheme.colorScheme.error)
            }

            when (receiptState) {
                RemoteReceiptState.Idle -> Text("GitHub WRITE receipt: TOKEN_VAZIO")
                RemoteReceiptState.Running -> Text("GitHub WRITE receipt: enviando…")
                is RemoteReceiptState.Passed ->
                    Text("GitHub WRITE receipt: PASS · Issue #${receiptState.issueNumber}")
                is RemoteReceiptState.Failed ->
                    Text("GitHub WRITE receipt: FAIL · ${receiptState.message}", color = MaterialTheme.colorScheme.error)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRunProbe,
                    enabled = probeState !is GithubConnectivityState.Running,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.NetworkCheck, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Testar READ")
                }
                Button(
                    onClick = onCreateRemoteReceipt,
                    enabled = receiptState !is RemoteReceiptState.Running,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Receipt WRITE")
                }
            }
        }
    }
}

private data class LocalPrivateStorageResult(
    val path: String,
    val sha256: String,
    val bytes: Long
)

@Composable
private fun LocalPrivateStorageTestCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<LocalPrivateStorageResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FactCheck, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Teste de armazenamento privado", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Grava um receipt pequeno em files/connectivity-receipts/, relê o arquivo e confere SHA-256. Não publica nada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            result?.let {
                Text("LOCAL PRIVATE: PASS · ${it.bytes} B")
                Text(
                    it.path,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text("SHA-256 ${it.sha256.take(16)}…", style = MaterialTheme.typography.bodySmall)
            } ?: Text(if (running) "LOCAL PRIVATE: testando…" else "LOCAL PRIVATE: TOKEN_VAZIO")
            error?.let {
                Text("LOCAL PRIVATE: FAIL · $it", color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    running = true
                    error = null
                    scope.launch {
                        runLocalPrivateStorageTest(context)
                            .onSuccess { result = it }
                            .onFailure { error = it.message ?: "Falha no armazenamento privado" }
                        running = false
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Testar e gerar receipt local")
            }
        }
    }
}

private suspend fun runLocalPrivateStorageTest(context: Context): Result<LocalPrivateStorageResult> =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "connectivity-receipts")
            if (!dir.exists() && !dir.mkdirs()) {
                throw IOException("Não foi possível criar connectivity-receipts")
            }

            val epochMs = System.currentTimeMillis()
            val payload = buildString {
                appendLine("RAFGITTOOLS_LOCAL_PRIVATE_RECEIPT_V1")
                appendLine("epoch_ms=$epochMs")
                appendLine("storage=ANDROID_APP_PRIVATE_FILES")
                appendLine("write=REQUESTED_BY_USER")
                appendLine("claim_allowed=false")
            }.toByteArray(Charsets.UTF_8)

            val file = File(dir, "connectivity-$epochMs.receipt")
            file.outputStream().use { it.write(payload) }

            val readBack = file.inputStream().use { it.readBytes() }
            if (!payload.contentEquals(readBack)) {
                file.delete()
                throw IOException("Read-back diferente do payload gravado")
            }

            val sha256 = MessageDigest.getInstance("SHA-256")
                .digest(readBack)
                .joinToString("") { "%02x".format(it) }

            LocalPrivateStorageResult(
                path = file.absolutePath,
                sha256 = sha256,
                bytes = readBack.size.toLong()
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryCard(repository: GithubRepository, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (repository.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                    null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    repository.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (repository.isFork) {
                    Icon(Icons.Default.CallSplit, "Fork", Modifier.size(15.dp))
                }
            }

            repository.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(6.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repository.language?.let { lang ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Circle, null, Modifier.size(10.dp), tint = getLanguageColor(lang))
                        Spacer(Modifier.width(4.dp))
                        Text(lang, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(repository.stargazersCount.toString(), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, null, Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(repository.forksCount.toString(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun LocalRepositoryList(
    repositories: List<LocalRepoSummary>,
    onRepositoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            LocalPrivateStorageTestCard()
        }
        item {
            Column {
                Text("Workspace local (${repositories.size})", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Camada de staging e trabalho; Drive/GitHub continuam como fontes externas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (repositories.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderOff, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nenhum repositório local indexado ainda.")
                    }
                }
            }
        }
        items(repositories, key = { it.path }) { repo ->
            LocalRepositoryCard(repo) { onRepositoryClick(repo.path) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalRepositoryCard(repository: LocalRepoSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    repository.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, null, Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(repository.currentBranch, style = MaterialTheme.typography.bodySmall)
            }
            if (repository.lastCommitMessage.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    repository.lastCommitMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Error, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text("Erro", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(6.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("Tentar novamente")
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.2f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> "%.2f MiB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.2f KiB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun getLanguageColor(language: String): androidx.compose.ui.graphics.Color = when (language.lowercase()) {
    "kotlin" -> androidx.compose.ui.graphics.Color(0xFFA97BFF)
    "java" -> androidx.compose.ui.graphics.Color(0xFFB07219)
    "javascript" -> androidx.compose.ui.graphics.Color(0xFFF1E05A)
    "typescript" -> androidx.compose.ui.graphics.Color(0xFF3178C6)
    "python" -> androidx.compose.ui.graphics.Color(0xFF3572A5)
    "go" -> androidx.compose.ui.graphics.Color(0xFF00ADD8)
    "rust" -> androidx.compose.ui.graphics.Color(0xFFDEA584)
    "c" -> androidx.compose.ui.graphics.Color(0xFF555555)
    "c++" -> androidx.compose.ui.graphics.Color(0xFFF34B7D)
    "c#" -> androidx.compose.ui.graphics.Color(0xFF178600)
    "swift" -> androidx.compose.ui.graphics.Color(0xFFFFAC45)
    "ruby" -> androidx.compose.ui.graphics.Color(0xFF701516)
    "php" -> androidx.compose.ui.graphics.Color(0xFF4F5D95)
    "html" -> androidx.compose.ui.graphics.Color(0xFFE34C26)
    "css" -> androidx.compose.ui.graphics.Color(0xFF563D7C)
    "shell" -> androidx.compose.ui.graphics.Color(0xFF89E051)
    else -> MaterialTheme.colorScheme.primary
}
