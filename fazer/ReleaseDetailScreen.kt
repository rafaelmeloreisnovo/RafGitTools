package com.rafgittools.ui.screens.releases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.github.GithubRelease

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailScreen(
    owner: String,
    repo: String,
    releaseId: Long,
    viewModel: ReleaseDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val release by viewModel.release.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(owner, repo, releaseId) {
        viewModel.loadRelease(owner, repo, releaseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(release?.name ?: release?.tagName ?: "Release") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    release?.htmlUrl?.let { url ->
                        IconButton(onClick = { uriHandler.openUri(url) }) {
                            Icon(Icons.Default.OpenInBrowser, "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ReleaseDetailViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is ReleaseDetailViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
            }
            is ReleaseDetailViewModel.UiState.Success -> {
                release?.let { rel ->
                    ReleaseDetailContent(
                        release = rel,
                        modifier = Modifier.padding(paddingValues),
                        onDownloadAsset = { url -> uriHandler.openUri(url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseDetailContent(
    release: GithubRelease,
    modifier: Modifier = Modifier,
    onDownloadAsset: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (release.prerelease) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Pre-release") },
                    icon = { Icon(Icons.Default.Warning, null, Modifier.size(16.dp)) }
                )
            }
            if (release.draft) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Draft") },
                    icon = { Icon(Icons.Default.Edit, null, Modifier.size(16.dp)) }
                )
            }
            if (!release.prerelease && !release.draft) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Latest") },
                    icon = { Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp)) }
                )
            }
        }

        // Tag and target
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalOffer, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tag: ", fontWeight = FontWeight.SemiBold)
                    Text(release.tagName, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Branch: ", fontWeight = FontWeight.SemiBold)
                    Text(release.targetCommitish, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Author: ", fontWeight = FontWeight.SemiBold)
                    Text(release.author.login, style = MaterialTheme.typography.bodyMedium)
                }
                release.publishedAt?.let { date ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Published: ", fontWeight = FontWeight.SemiBold)
                        Text(date.take(10), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Release notes / body
        if (!release.body.isNullOrBlank()) {
            Text("Release Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = release.body,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Assets
        if (release.assets.isNotEmpty()) {
            Text("Assets (${release.assets.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            release.assets.forEach { asset ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(asset.name, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${formatBytes(asset.size)} · ${asset.downloadCount} downloads",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { onDownloadAsset(asset.browserDownloadUrl) }) {
                            Icon(Icons.Default.Download, "Download ${asset.name}",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
}
