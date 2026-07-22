package com.rafgittools.ui.screens.bisect

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisectScreen(
    repoPath: String,
    viewModel: BisectViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Git Bisect") },
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
                    if (uiState == BisectUiState.Running) {
                        IconButton(onClick = { viewModel.finish() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Reset bisect")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is BisectUiState.NotStarted -> NotStartedContent(onStart = { good, bad ->
                    viewModel.start(good, bad)
                })
                is BisectUiState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                is BisectUiState.Running -> RunningContent(
                    log = log,
                    onGood = { viewModel.markGood() },
                    onBad = { viewModel.markBad() },
                    onSkip = { viewModel.skip() },
                    onReset = { viewModel.finish() }
                )
            }
        }
    }
}

@Composable
private fun NotStartedContent(onStart: (good: String, bad: String) -> Unit) {
    var good by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf("HEAD") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Start Bisect Session", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Binary-search between good and bad commits to find the regression.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = good,
                onValueChange = { good = it },
                label = { Text("Last known good (commit / tag)") },
                placeholder = { Text("v1.0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
            )
            OutlinedTextField(
                value = bad,
                onValueChange = { bad = it },
                label = { Text("Known bad (commit / tag)") },
                placeholder = { Text("HEAD") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) }
            )
            Button(
                onClick = { onStart(good, bad) },
                modifier = Modifier.fillMaxWidth(),
                enabled = good.isNotBlank() && bad.isNotBlank()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Bisect")
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    HowItWorks()
}

@Composable
private fun RunningContent(
    log: String,
    onGood: () -> Unit,
    onBad: () -> Unit,
    onSkip: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bisect In Progress", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Test the current commit and mark it as good or bad.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onGood,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Good")
        }
        Button(
            onClick = onBad,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Bad")
        }
        OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
            Text("Skip")
        }
    }

    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Reset Bisect")
    }

    if (log.isNotBlank()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bisect Log", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = log,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HowItWorks() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("How it works", style = MaterialTheme.typography.labelLarge)
            Text("1. Git checks out the midpoint commit", style = MaterialTheme.typography.bodySmall)
            Text("2. Test your code and mark the commit good or bad", style = MaterialTheme.typography.bodySmall)
            Text("3. Git narrows the range until the first-bad commit is found", style = MaterialTheme.typography.bodySmall)
            Text("4. Press Reset to return to your original branch", style = MaterialTheme.typography.bodySmall)
        }
    }
}
