package com.rafgittools.ui.screens.repository

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository detail screen showing Git operations and status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryDetailScreen(
    repoPath: String,
    viewModel: RepositoryDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToCommits: (String) -> Unit = {},
    onNavigateToBranches: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val status by viewModel.status.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val commits by viewModel.recentCommits.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()
    
    var showBranchDialog by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(repoPath) {
        viewModel.loadRepository(repoPath)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.let { 
                                if (it is RepositoryDetailUiState.Success) it.repository.name
                                else "Repository"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        status?.let {
                            Text(
                                text = "Branch: ${it.branch}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is RepositoryDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is RepositoryDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadRepository(repoPath) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is RepositoryDetailUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Repository Status Card
                        item {
                            StatusCard(
                                status = status,
                                onStageAll = { viewModel.stageAllChanges() },
                                onUnstageAll = { viewModel.unstageAllChanges() }
                            )
                        }
                        
                        // Quick Actions Card
                        item {
                            QuickActionsCard(
                                onPull = { viewModel.pull() },
                                onPush = { viewModel.push() },
                                onCommit = { showCommitDialog = true },
                                onBranch = { showBranchDialog = true },
                                onFetch = { viewModel.fetch() },
                                hasChanges = status?.hasUncommittedChanges == true
                            )
                        }
                        
                        // Branches Section
                        item {
                            SectionHeader(
                                title = "Branches",
                                count = branches.size,
                                onSeeAll = { onNavigateToBranches(repoPath) }
                            )
                        }
                        
                        items(branches.take(5)) { branch ->
                            BranchItem(
                                branch = branch,
                                onCheckout = { viewModel.checkout(branch.shortName) }
                            )
                        }
                        
                        // Recent Commits Section
                        item {
                            SectionHeader(
                                title = "Recent Commits",
                                count = commits.size,
                                onSeeAll = { onNavigateToCommits(repoPath) }
                            )
                        }
                        
                        items(commits.take(5)) { commit ->
                            CommitItem(commit = commit)
                        }
                        
                        // Changed Files Section
                        status?.let { s ->
                            val allChanges = s.modified + s.added + s.removed + s.untracked
                            if (allChanges.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Changed Files",
                                        count = allChanges.size,
                                        onSeeAll = null
                                    )
                                }
                                
                                items(allChanges.take(10)) { file ->
                                    ChangedFileItem(
                                        file = file,
                                        status = when {
                                            s.modified.contains(file) -> "Modified"
                                            s.added.contains(file) -> "Added"
                                            s.removed.contains(file) -> "Deleted"
                                            s.untracked.contains(file) -> "Untracked"
                                            else -> "Unknown"
                                        },
                                        onStage = { viewModel.stageFile(file) },
                                        onUnstage = { viewModel.unstageFile(file) }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Operation Status Snackbar
                    operationStatus?.let { status ->
                        Snackbar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            action = {
                                TextButton(onClick = { viewModel.clearOperationStatus() }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text(status)
                        }
                    }
                }
            }
        }
        
        // Commit Dialog
        if (showCommitDialog) {
            CommitDialog(
                onDismiss = { showCommitDialog = false },
                onCommit = { message ->
                    viewModel.commit(message)
                    showCommitDialog = false
                }
            )
        }
        
        // Branch Dialog
        if (showBranchDialog) {
            CreateBranchDialog(
                onDismiss = { showBranchDialog = false },
                onCreate = { branchName ->
                    viewModel.createBranch(branchName)
                    showBranchDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatusCard(
    status: GitStatus?,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Repository Status",
                    style = MaterialTheme.typography.titleMedium
                )
                if (status?.hasUncommittedChanges == true) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Uncommitted Changes") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            status?.let { s ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatusCount(
                        icon = Icons.Default.Add,
                        count = s.added.size,
                        label = "Added",
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatusCount(
                        icon = Icons.Default.Edit,
                        count = s.modified.size,
                        label = "Modified",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    StatusCount(
                        icon = Icons.Default.Delete,
                        count = s.removed.size,
                        label = "Deleted",
                        color = MaterialTheme.colorScheme.error
                    )
                    StatusCount(
                        icon = Icons.Default.HelpOutline,
                        count = s.untracked.size,
                        label = "Untracked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (s.hasUncommittedChanges) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onStageAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stage All")
                        }
                        OutlinedButton(
                            onClick = onUnstageAll,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Unstage All")
                        }
                    }
                }
            } ?: Text(
                text = "Loading status...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsCard(
    onPull: () -> Unit,
    onPush: () -> Unit,
    onCommit: () -> Unit,
    onBranch: () -> Unit,
    onFetch: () -> Unit,
    hasChanges: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Default.CloudDownload,
                    label = "Pull",
                    onClick = onPull
                )
                ActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Push",
                    onClick = onPush
                )
                ActionButton(
                    icon = Icons.Default.Save,
                    label = "Commit",
                    onClick = onCommit,
                    enabled = hasChanges
                )
                ActionButton(
                    icon = Icons.Default.AccountTree,
                    label = "Branch",
                    onClick = onBranch
                )
                ActionButton(
                    icon = Icons.Default.Sync,
                    label = "Fetch",
                    onClick = onFetch
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Icon(imageVector = icon, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$title ($count)",
            style = MaterialTheme.typography.titleMedium
        )
        onSeeAll?.let {
            TextButton(onClick = it) {
                Text("See All")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchItem(
    branch: GitBranch,
    onCheckout: () -> Unit
) {
    Card(
        onClick = onCheckout,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (branch.isCurrent) Icons.Default.CheckCircle else Icons.Default.AccountTree,
                contentDescription = null,
                tint = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = branch.shortName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (branch.isCurrent) androidx.compose.ui.text.font.FontWeight.Bold else null
                )
                if (branch.isRemote) {
                    Text(
                        text = "Remote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (branch.isCurrent) {
                AssistChip(
                    onClick = { },
                    label = { Text("Current") }
                )
            }
        }
    }
}

@Composable
private fun CommitItem(commit: GitCommit) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = commit.sha.take(7),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateFormat.format(Date(commit.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = commit.message.lines().first(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = commit.author.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChangedFileItem(
    file: String,
    status: String,
    onStage: () -> Unit,
    onUnstage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    "Added" -> Icons.Default.Add
                    "Modified" -> Icons.Default.Edit
                    "Deleted" -> Icons.Default.Delete
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when (status) {
                    "Added" -> MaterialTheme.colorScheme.primary
                    "Modified" -> MaterialTheme.colorScheme.tertiary
                    "Deleted" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.split("/").last(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onStage) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Stage",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun CommitDialog(
    onDismiss: () -> Unit,
    onCommit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Commit") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Commit message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCommit(message) },
                enabled = message.isNotBlank()
            ) {
                Text("Commit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CreateBranchDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var branchName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Branch") },
        text = {
            OutlinedTextField(
                value = branchName,
                onValueChange = { branchName = it },
                label = { Text("Branch name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(branchName) },
                enabled = branchName.isNotBlank()
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
