package com.rafgittools.ui.screens.rafgitfs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.rafgitfs.data.VirtualTreeEntryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualFileBrowserScreen(
    onNavigateBack: () -> Unit,
    onOpenFile: (profileId: String, repository: String, ref: String, path: String) -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenWorkspace: (profileId: String, repository: String, ref: String) -> Unit,
    viewModel: VirtualFileBrowserViewModel = hiltViewModel()
) {
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val refs by viewModel.refs.collectAsStateWithLifecycle()
    val currentRef by viewModel.currentRef.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    var showRefs by remember { mutableStateOf(false) }

    BackHandler(enabled = currentPath.isNotEmpty()) { viewModel.navigateUp() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            viewModel.repositoryFullName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (currentPath.isEmpty()) "/" else "/$currentPath",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.navigateUp()) onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onOpenWorkspace(
                                viewModel.profileId,
                                viewModel.repositoryFullName,
                                currentRef
                            )
                        },
                        enabled = currentRef.isNotBlank()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Open governed Git workspace")
                    }
                    IconButton(onClick = viewModel::refreshTree) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh tree")
                    }
                    IconButton(onClick = { onOpenSettings(viewModel.profileId) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Storage settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            TextButton(onClick = { showRefs = true }) {
                                Icon(Icons.Default.CallSplit, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(currentRef.ifBlank { "Select ref" })
                            }
                            DropdownMenu(
                                expanded = showRefs,
                                onDismissRequest = { showRefs = false }
                            ) {
                                refs.forEach { ref ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                buildString {
                                                    append(ref.refName)
                                                    if (ref.isDefault) append(" · default")
                                                    append(" · ")
                                                    append(ref.refType.lowercase())
                                                }
                                            )
                                        },
                                        onClick = {
                                            showRefs = false
                                            viewModel.selectRef(ref.refName)
                                        }
                                    )
                                }
                            }
                        }
                        RafGitFsReadOnlyBadge()
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search this folder") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Spacer(Modifier.height(8.dp))
                    RafGitFsStatusBanner(status)
                }
            }
            item {
                RafGitFsBreadcrumbBar(
                    path = currentPath,
                    onNavigate = viewModel::navigateTo
                )
            }
            if (entries.isEmpty()) {
                item {
                    RafGitFsEmptyState(
                        title = "No entries here",
                        detail = "Refresh the selected ref or navigate to another folder."
                    )
                }
            } else {
                items(entries, key = { "${it.refName}:${it.path}" }) { entry ->
                    VirtualEntryCard(
                        entry = entry,
                        onOpen = {
                            if (entry.entryType == "DIRECTORY") viewModel.navigateTo(entry.path)
                            else onOpenFile(
                                viewModel.profileId,
                                viewModel.repositoryFullName,
                                currentRef,
                                entry.path
                            )
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(entry) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VirtualEntryCard(
    entry: VirtualTreeEntryEntity,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (entry.entryType) {
        "DIRECTORY" -> Icons.Default.Folder
        "SYMLINK", "SUBMODULE" -> Icons.Default.Link
        else -> Icons.Default.InsertDriveFile
    }
    Card(onClick = onOpen, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = entry.entryType, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        entry.entryType.lowercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.entryType != "DIRECTORY") {
                        Text(
                            RafGitFsUiPaths.formatBytes(entry.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        entry.cacheState,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (entry.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (entry.isFavorite) "Remove favorite" else "Add favorite"
                )
            }
        }
    }
}
