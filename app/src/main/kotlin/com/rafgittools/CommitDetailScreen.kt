package com.rafgittools.ui.screens.commits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitDiff
import com.rafgittools.domain.model.DiffLineType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommitDetailScreen(
    repoPath: String,
    commitSha: String,
    viewModel: CommitDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val commit by viewModel.commit.collectAsState()
    val diffs by viewModel.diffs.collectAsState()
    val changedFiles by viewModel.changedFiles.collectAsState()

    LaunchedEffect(repoPath, commitSha) {
        viewModel.loadCommit(repoPath, commitSha)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commit ${commitSha.take(7)}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is CommitDetailViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CommitDetailViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
            }
            is CommitDetailViewModel.UiState.Success -> {
                commit?.let { c ->
                    CommitDetailContent(
                        commit = c,
                        diffs = diffs,
                        changedFiles = changedFiles,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitDetailContent(
    commit: GitCommit,
    diffs: List<GitDiff>,
    changedFiles: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Files (${changedFiles.size})", "Diff")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        when (selectedTab) {
            0 -> CommitInfoTab(commit)
            1 -> CommitFilesTab(changedFiles)
            2 -> CommitDiffTab(diffs)
        }
    }
}

@Composable
private fun CommitInfoTab(commit: GitCommit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Commit SHA
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tag, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Commit SHA", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                            Text(
                                commit.sha,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider()
                    // Author
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Author", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                            Text(commit.author.name, fontWeight = FontWeight.SemiBold)
                            Text(commit.author.email, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    // Timestamp
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Date", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                            Text(
                                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    .format(java.util.Date(commit.timestamp))
                            )
                        }
                    }
                    // Parents
                    if (commit.parents.isNotEmpty()) {
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.AccountTree, null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Parents", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline)
                                commit.parents.forEach { parent ->
                                    Text(parent.take(12), fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = commit.message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CommitFilesTab(files: List<String>) {
    if (files.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No file changes available", color = MaterialTheme.colorScheme.outline)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(files) { path ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.InsertDriveFile, null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = path,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitDiffTab(diffs: List<GitDiff>) {
    if (diffs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Code, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))
                Text("No diff available for this commit", color = MaterialTheme.colorScheme.outline)
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(diffs) { diff ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // File header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            diff.newPath ?: diff.oldPath ?: "unknown",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Hunks
                    diff.hunks.forEach { hunk ->
                        hunk.lines.forEach { line ->
                            val (bg, lineColor) = when (line.type) {
                                DiffLineType.ADD ->
                                    Color(0xFF1B5E20).copy(alpha = 0.15f) to Color(0xFF2E7D32)
                                DiffLineType.DELETE ->
                                    Color(0xFFB71C1C).copy(alpha = 0.15f) to Color(0xFFC62828)
                                DiffLineType.CONTEXT ->
                                    Color.Transparent to MaterialTheme.colorScheme.onSurface
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg)
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    line.content,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = lineColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
