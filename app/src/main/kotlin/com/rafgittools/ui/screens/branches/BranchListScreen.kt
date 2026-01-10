package com.rafgittools.ui.screens.branches

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.GitBranch

/**
 * Branch list screen showing repository branches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchListScreen(
    repoPath: String,
    viewModel: BranchListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedBranchForDelete by remember { mutableStateOf<GitBranch?>(null) }
    
    LaunchedEffect(repoPath) {
        viewModel.loadBranches(repoPath)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Branches") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create branch")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is BranchListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is BranchListUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadBranches(repoPath) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is BranchListUiState.Empty -> {
                    EmptyContent(
                        onCreateBranch = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is BranchListUiState.Success -> {
                    BranchList(
                        branches = branches,
                        onCheckout = { branch -> viewModel.checkout(branch) },
                        onDelete = { branch -> selectedBranchForDelete = branch },
                        onMerge = { branch -> viewModel.merge(branch) }
                    )
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
        
        // Create Branch Dialog
        if (showCreateDialog) {
            CreateBranchDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { branchName ->
                    viewModel.createBranch(branchName)
                    showCreateDialog = false
                }
            )
        }
        
        // Delete Confirmation Dialog
        selectedBranchForDelete?.let { branch ->
            AlertDialog(
                onDismissRequest = { selectedBranchForDelete = null },
                title = { Text("Delete Branch") },
                text = { Text("Are you sure you want to delete '${branch.shortName}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBranch(branch)
                            selectedBranchForDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedBranchForDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun BranchList(
    branches: List<GitBranch>,
    onCheckout: (GitBranch) -> Unit,
    onDelete: (GitBranch) -> Unit,
    onMerge: (GitBranch) -> Unit
) {
    val localBranches = branches.filter { it.isLocal }
    val remoteBranches = branches.filter { it.isRemote }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Local Branches Section
        if (localBranches.isNotEmpty()) {
            item {
                Text(
                    text = "Local Branches (${localBranches.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(localBranches) { branch ->
                BranchCard(
                    branch = branch,
                    onCheckout = { onCheckout(branch) },
                    onDelete = if (!branch.isCurrent) { { onDelete(branch) } } else null,
                    onMerge = if (!branch.isCurrent) { { onMerge(branch) } } else null
                )
            }
        }
        
        // Remote Branches Section
        if (remoteBranches.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Remote Branches (${remoteBranches.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(remoteBranches) { branch ->
                BranchCard(
                    branch = branch,
                    onCheckout = { onCheckout(branch) },
                    onDelete = null,
                    onMerge = null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BranchCard(
    branch: GitBranch,
    onCheckout: () -> Unit,
    onDelete: (() -> Unit)?,
    onMerge: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        onClick = onCheckout,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    branch.isCurrent -> Icons.Default.CheckCircle
                    branch.isRemote -> Icons.Default.Cloud
                    else -> Icons.Default.AccountTree
                },
                contentDescription = null,
                tint = when {
                    branch.isCurrent -> MaterialTheme.colorScheme.primary
                    branch.isRemote -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = branch.shortName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (branch.isCurrent) FontWeight.Bold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                branch.commitSha?.let { sha ->
                    Text(
                        text = sha.take(7),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (branch.upstream != null) {
                    Text(
                        text = "Tracking: ${branch.upstream}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (branch.isCurrent) {
                AssistChip(
                    onClick = { },
                    label = { Text("Current") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else if (onDelete != null || onMerge != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Checkout") },
                            onClick = {
                                showMenu = false
                                onCheckout()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        )
                        
                        onMerge?.let {
                            DropdownMenuItem(
                                text = { Text("Merge into current") },
                                onClick = {
                                    showMenu = false
                                    it()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.MergeType, contentDescription = null)
                                }
                            )
                        }
                        
                        onDelete?.let {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    it()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
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
            text = "Error Loading Branches",
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
private fun EmptyContent(
    onCreateBranch: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Branches",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first branch to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateBranch) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Branch")
        }
    }
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
            Column {
                Text(
                    text = "Enter a name for the new branch",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it },
                    label = { Text("Branch name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("feature/my-feature") }
                )
            }
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
