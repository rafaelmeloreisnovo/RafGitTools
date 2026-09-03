package com.rafgittools.feature.worktree

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.core.vcs.WorktreeInfo

@Composable
fun WorktreeScreen(
    viewModel: WorktreeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val effects by viewModel.effects.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedWorktreePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(effects) {
        when (effects) {
            is WorktreeViewModel.WorktreeEffect.ShowToast -> {
                // Handle toast
            }
            is WorktreeViewModel.WorktreeEffect.WorktreeCreatedSuccess -> {
                showCreateDialog = false
                viewModel.clearEffect()
            }
            is WorktreeViewModel.WorktreeEffect.WorktreeDeletedSuccess -> {
                expandedWorktreePath = null
                viewModel.clearEffect()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git Worktrees") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Create Worktree")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error message
            state.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Filled.Close, "Dismiss")
                        }
                    }
                }
            }

            // Success message
            state.successMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            // Loading
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.worktrees.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            "No worktrees",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No worktrees found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Create a new worktree to enable parallel development",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Worktrees list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.worktrees) { worktreeInfo ->
                        WorktreeCard(
                            worktreeInfo = worktreeInfo,
                            isExpanded = expandedWorktreePath == worktreeInfo.path,
                            onExpandChange = { expanded ->
                                expandedWorktreePath = if (expanded) worktreeInfo.path else null
                            },
                            onDelete = { viewModel.deleteWorktree(worktreeInfo.path) }
                        )
                    }
                }
            }
        }
    }

    // Dialog
    if (showCreateDialog) {
        CreateWorktreeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { path, branch, commit ->
                viewModel.createWorktree(path, branch, commit)
            }
        )
    }
}

@Composable
fun WorktreeCard(
    worktreeInfo: WorktreeInfo,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        worktreeInfo.path.substringAfterLast("/"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        worktreeInfo.branch,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (worktreeInfo.isPrunable) {
                        Badge { Text("Prunable", fontSize = 10.sp) }
                    }
                }
                IconButton(onClick = { onExpandChange(!isExpanded) }) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        "Expand"
                    )
                }
            }

            // Expanded content
            if (isExpanded) {
                Divider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Path
                    Text("Path:", style = MaterialTheme.typography.labelSmall)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            worktreeInfo.path,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    // Commit
                    Text("Commit:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        worktreeInfo.commitHash.take(12),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateWorktreeDialog(
    onDismiss: () -> Unit,
    onCreate: (path: String, branch: String, commitHash: String?) -> Unit
) {
    var path by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var commitHash by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Worktree") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Worktree Path") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = commitHash,
                    onValueChange = { commitHash = it },
                    label = { Text("Commit Hash (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(path, branch, commitHash.ifEmpty { null })
                    onDismiss()
                },
                enabled = path.isNotEmpty() && branch.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
