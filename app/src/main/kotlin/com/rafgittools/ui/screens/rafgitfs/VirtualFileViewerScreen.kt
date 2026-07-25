package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualFileViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: VirtualFileViewerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = state.snapshot

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            viewModel.path.substringAfterLast('/'),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${viewModel.repositoryFullName}@${viewModel.refName}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload blob")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            RafGitFsStatusBanner(state.status)
            Spacer(Modifier.height(10.dp))
            snapshot?.let { content ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(content.path, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Text("${RafGitFsUiPaths.formatBytes(content.sizeBytes)}")
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "SHA ${content.blobSha.take(12)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        RafGitFsReadOnlyBadge()
                    }
                }
                Spacer(Modifier.height(10.dp))
                val display = remember(content.blobSha, content.textUtf8) {
                    content.textUtf8 ?: buildString {
                        append("Binary preview (first 256 bytes)\n\n")
                        content.bytes.take(256).chunked(16).forEachIndexed { row, bytes ->
                            append(row.times(16).toString(16).padStart(8, '0'))
                            append("  ")
                            append(bytes.joinToString(" ") { byte ->
                                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
                            })
                            append('\n')
                        }
                    }
                }
                Card(Modifier.fillMaxSize()) {
                    SelectionContainer {
                        Text(
                            text = display,
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } ?: RafGitFsEmptyState(
                title = "No content observed",
                detail = "The file may be too large, unavailable, binary-only, rate-limited or not indexed."
            )
        }
    }
}
