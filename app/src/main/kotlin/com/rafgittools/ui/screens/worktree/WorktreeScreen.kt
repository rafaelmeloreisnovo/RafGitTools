package com.rafgittools.ui.screens.worktree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.worktree.WorktreeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorktreeScreen(
    repoPath: String,
    viewModel: WorktreeViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val worktrees by viewModel.worktrees.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(repoPath) { viewModel.load(repoPath) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Worktrees") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.pruneWorktrees() }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Prune")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add worktree")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is WorktreeUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is WorktreeUiState.Empty -> EmptyContent(
                    onAdd = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.Center)
                )
                is WorktreeUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
                is WorktreeUiState.Success -> WorktreeList(
                    worktrees = worktrees,
                    onRemove = { viewModel.removeWorktree(it) },
                    onLock = { path, reason -> viewModel.lockWorktree(path, reason) },
                    onUnlock = { viewModel.unlockWorktree(it) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddWorktreeDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { path, branch, create ->
                viewModel.addWorktree(path, branch, create)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun WorktreeList(
    worktrees: List<WorktreeManager.WorktreeInfo>,
    onRemove: (String) -> Unit,
    onLock: (String, String) -> Unit,
    onUnlock: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${worktrees.size} worktree${if (worktrees.size == 1) "" else "s"}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        items(worktrees, key = { it.path }) { wt ->
            WorktreeCard(wt = wt, onRemove = onRemove, onLock = onLock, onUnlock = onUnlock)
        }
    }
}

@Composable
private fun WorktreeCard(
    wt: WorktreeManager.WorktreeInfo,
    onRemove: (String) -> Unit,
    onLock: (String, String) -> Unit,
    onUnlock: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (wt.bare) Icons.Default.FolderOpen else Icons.Default.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = wt.path,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (wt.branch.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = wt.branch.removePrefix("refs/heads/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (wt.head.isNotBlank()) {
                        Text(
                            text = wt.head.take(8),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (wt.bare) Badge { Text("bare") }
                        if (wt.locked) Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                            Text("locked", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (wt.locked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                leadingIcon = { Icon(Icons.Default.LockOpen, null) },
                                onClick = { showMenu = false; onUnlock(wt.path) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Lock") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                onClick = { showMenu = false; onLock(wt.path, "") }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; confirmRemove = true }
                        )
                    }
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove Worktree?") },
            text = { Text("Remove worktree at ${wt.path}? This only removes the Git admin link, not the directory.") },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; onRemove(wt.path) }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AddWorktreeDialog(
    onDismiss: () -> Unit,
    onAdd: (path: String, branch: String, create: Boolean) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var createBranch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Worktree") },
        text = {
            Column {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Path") },
                    placeholder = { Text("/path/to/worktree") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch") },
                    placeholder = { Text("feature/my-branch") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = createBranch, onCheckedChange = { createBranch = it })
                    Text("Create new branch", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(path, branch, createBranch) },
                enabled = path.isNotBlank() && branch.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EmptyContent(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountTree,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("No Worktrees", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "No linked worktrees. Add one to work on multiple branches simultaneously.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Worktree")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text("Error", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
