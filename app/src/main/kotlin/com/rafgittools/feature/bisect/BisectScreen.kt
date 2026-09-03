package com.rafgittools.feature.bisect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BisectScreen(
    viewModel: BisectViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val effects by viewModel.effects.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effects) {
        when (effects) {
            is BisectViewModel.BisectEffect.BisectStarted -> {
                showStartDialog = false
                viewModel.clearEffect()
            }
            is BisectViewModel.BisectEffect.BisectEnded -> {
                viewModel.clearEffect()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git Bisect") },
                actions = {
                    if (state.isInSession) {
                        IconButton(onClick = { viewModel.resetBisect() }) {
                            Icon(Icons.Filled.Close, "End bisect")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (!state.isInSession) {
                FloatingActionButton(
                    onClick = { showStartDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Search, "Start Bisect")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!state.isInSession) {
                // Idle state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            "Start bisect",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Find regressions with Git Bisect",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Start a bisect session to interactively search for the first bad commit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Active bisect session
                Column(modifier = Modifier.padding(16.dp)) {
                    // Progress
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Estimated remaining steps",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                "~${state.estimatedRemaining} steps",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current commit
                    state.currentCommit?.let { commit ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    commit.shortHash,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    commit.message,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${commit.author} • ${commit.date}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.markCommitGood(commit.hash) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Good")
                            }
                            Button(
                                onClick = { viewModel.markCommitBad(commit.hash) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bad")
                            }
                            Button(
                                onClick = { viewModel.skipCommit(commit.hash) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Filled.MoreHoriz, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Skip")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // History summary
                    Text("Session summary", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BisectStatsPill("Good", state.goodCommits.size)
                        BisectStatsPill("Bad", state.badCommits.size)
                        BisectStatsPill("Skipped", state.skippedCommits.size)
                    }
                }
            }
        }
    }

    // Start bisect dialog
    if (showStartDialog) {
        StartBisectDialog(
            onDismiss = { showStartDialog = false },
            onStart = { good, bad ->
                viewModel.startBisect(good, bad)
            }
        )
    }
}

@Composable
fun BisectStatsPill(label: String, count: Int) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(count.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun StartBisectDialog(
    onDismiss: () -> Unit,
    onStart: (goodCommit: String, badCommit: String) -> Unit
) {
    var goodCommit by remember { mutableStateOf("") }
    var badCommit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Bisect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = goodCommit,
                    onValueChange = { goodCommit = it },
                    label = { Text("Good commit (known working)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                TextField(
                    value = badCommit,
                    onValueChange = { badCommit = it },
                    label = { Text("Bad commit (known broken)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Use commit hashes or branch names",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStart(goodCommit, badCommit)
                    onDismiss()
                },
                enabled = goodCommit.isNotEmpty() && badCommit.isNotEmpty()
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
