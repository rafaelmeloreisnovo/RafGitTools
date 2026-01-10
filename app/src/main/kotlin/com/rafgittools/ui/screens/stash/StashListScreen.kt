package com.rafgittools.ui.screens.stash

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.GitStash
import java.text.SimpleDateFormat
import java.util.*

/**
 * Stash list screen for managing Git stashes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StashListScreen(
    repoPath: String,
    viewModel: StashListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val stashes by viewModel.stashes.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var stashToApply by remember { mutableStateOf<GitStash?>(null) }
    var stashToDrop by remember { mutableStateOf<GitStash?>(null) }
    
    LaunchedEffect(repoPath) {
        viewModel.loadStashes(repoPath)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stashes") },
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
                Icon(Icons.Default.Add, contentDescription = "Create stash")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is StashListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is StashListUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is StashListUiState.Empty -> {
                    EmptyContent(
                        onCreateStash = { showCreateDialog = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is StashListUiState.Success -> {
                    StashList(
                        stashes = stashes,
                        onApply = { stashToApply = it },
                        onPop = { viewModel.popStash(it.index) },
                        onDrop = { stashToDrop = it }
                    )
                }
            }
        }
    }
    
    // Create stash dialog
    if (showCreateDialog) {
        CreateStashDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { message, includeUntracked ->
                viewModel.createStash(message, includeUntracked)
                showCreateDialog = false
            }
        )
    }
    
    // Apply stash confirmation
    stashToApply?.let { stash ->
        AlertDialog(
            onDismissRequest = { stashToApply = null },
            title = { Text("Apply Stash") },
            text = { 
                Text("Apply stash ${stash.index}? This will restore the stashed changes to your working directory.") 
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyStash(stash.index)
                    stashToApply = null
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { stashToApply = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Drop stash confirmation
    stashToDrop?.let { stash ->
        AlertDialog(
            onDismissRequest = { stashToDrop = null },
            title = { Text("Drop Stash") },
            text = { 
                Text("Drop stash ${stash.index}? This cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dropStash(stash.index)
                        stashToDrop = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Drop")
                }
            },
            dismissButton = {
                TextButton(onClick = { stashToDrop = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StashList(
    stashes: List<GitStash>,
    onApply: (GitStash) -> Unit,
    onPop: (GitStash) -> Unit,
    onDrop: (GitStash) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${stashes.size} stashes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(stashes) { stash ->
            StashCard(
                stash = stash,
                dateFormat = dateFormat,
                onApply = { onApply(stash) },
                onPop = { onPop(stash) },
                onDrop = { onDrop(stash) }
            )
        }
    }
}

@Composable
private fun StashCard(
    stash: GitStash,
    dateFormat: SimpleDateFormat,
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDrop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "stash@{${stash.index}}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    Text(
                        text = stash.sha.take(7),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                Text(
                    text = dateFormat.format(Date(stash.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stash.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            if (stash.branch.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stash.branch,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDrop) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Drop")
                }
                TextButton(onClick = onApply) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply")
                }
                FilledTonalButton(onClick = onPop) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pop")
                }
            }
        }
    }
}

@Composable
private fun CreateStashDialog(
    onDismiss: () -> Unit,
    onCreate: (String?, Boolean) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var includeUntracked by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Stash") },
        text = {
            Column {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeUntracked,
                        onCheckedChange = { includeUntracked = it }
                    )
                    Text(
                        text = "Include untracked files",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(message.ifBlank { null }, includeUntracked) }) {
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

@Composable
private fun EmptyContent(
    onCreateStash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Stashes",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You haven't stashed any changes yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateStash) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Stash")
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
