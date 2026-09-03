package com.rafgittools.feature.ssh

import androidx.compose.foundation.background
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
import com.rafgittools.core.security.SshKeyInfo

/**
 * SSH Key Manager Screen
 *
 * Provides UI for:
 * - List stored SSH keys
 * - Generate new keys
 * - Import existing keys
 * - Delete keys
 * - Export public keys
 */
@Composable
fun SshKeyManagerScreen(
    viewModel: SshKeyManagerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val effects by viewModel.effects.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var expandedKeyName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(effects) {
        when (effects) {
            is SshKeyManagerViewModel.SshKeyManagerEffect.ShowToast -> {
                // Handle toast
            }
            is SshKeyManagerViewModel.SshKeyManagerEffect.KeyGeneratedSuccess -> {
                showGenerateDialog = false
                viewModel.clearEffect()
            }
            is SshKeyManagerViewModel.SshKeyManagerEffect.KeyDeletedSuccess -> {
                expandedKeyName = null
                viewModel.clearEffect()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Keys") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { showGenerateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, "Generate Key")
                }
                FloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Filled.Upload, "Import Key")
                }
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
            } else if (state.keys.isEmpty()) {
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
                            Icons.Filled.Key,
                            "No keys",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No SSH keys found",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Generate or import an SSH key to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Keys list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.keys) { keyInfo ->
                        SshKeyCard(
                            keyInfo = keyInfo,
                            isExpanded = expandedKeyName == keyInfo.name,
                            onExpandChange = { expanded ->
                                expandedKeyName = if (expanded) keyInfo.name else null
                            },
                            onDelete = { viewModel.deleteKey(keyInfo.name) },
                            onExport = { viewModel.exportPublicKey(keyInfo.name) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showGenerateDialog) {
        GenerateKeyDialog(
            onDismiss = { showGenerateDialog = false },
            onGenerate = { keyType, keyName, passphrase, comment ->
                viewModel.generateKey(keyType, keyName, passphrase, comment)
            }
        )
    }

    if (showImportDialog) {
        ImportKeyDialog(
            onDismiss = { showImportDialog = false },
            onImport = { keyName, privateKey, passphrase ->
                viewModel.importKey(keyName, privateKey, passphrase)
            }
        )
    }
}

@Composable
fun SshKeyCard(
    keyInfo: SshKeyInfo,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
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
                        keyInfo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge { Text(keyInfo.type, fontSize = 11.sp) }
                        if (keyInfo.hasPassphrase) {
                            Badge { Text("Protected", fontSize = 11.sp) }
                        }
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
                    // Fingerprint
                    Text("Fingerprint:", style = MaterialTheme.typography.labelSmall)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            keyInfo.fingerprint,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    // Comment
                    if (keyInfo.comment.isNotEmpty()) {
                        Text("Comment:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            keyInfo.comment,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExport,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
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
fun GenerateKeyDialog(
    onDismiss: () -> Unit,
    onGenerate: (keyType: Int, keyName: String, passphrase: String?, comment: String) -> Unit
) {
    var keyName by remember { mutableStateOf("id_ed25519") }
    var passphrase by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var selectedKeyType by remember { mutableStateOf("ED25519") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate SSH Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Key type
                Text("Key Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ED25519", "RSA", "ECDSA").forEach { type ->
                        FilterChip(
                            selected = selectedKeyType == type,
                            onClick = { selectedKeyType = type },
                            label = { Text(type) }
                        )
                    }
                }

                // Key name
                TextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Key Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Passphrase
                TextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Comment
                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val keyType = when (selectedKeyType) {
                        "RSA" -> 1 // KEY_TYPE_RSA
                        "ECDSA" -> 3 // KEY_TYPE_ECDSA
                        else -> 4 // KEY_TYPE_ED25519
                    }
                    onGenerate(keyType, keyName, passphrase.ifEmpty { null }, comment)
                    onDismiss()
                }
            ) {
                Text("Generate")
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
fun ImportKeyDialog(
    onDismiss: () -> Unit,
    onImport: (keyName: String, privateKey: String, passphrase: String?) -> Unit
) {
    var keyName by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import SSH Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Key Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                TextField(
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    label = { Text("Private Key Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    maxLines = 10
                )

                TextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase (if encrypted)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImport(keyName, privateKey, passphrase.ifEmpty { null })
                    onDismiss()
                },
                enabled = keyName.isNotEmpty() && privateKey.isNotEmpty()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
