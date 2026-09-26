package com.rafgittools.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.R
import com.rafgittools.core.feature.FeatureFlags
import com.rafgittools.core.localization.Language

/**
 * Settings screen for app configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onLanguageChange: (Language) -> Unit = {}
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val gitConfig by viewModel.gitConfig.collectAsStateWithLifecycle()
    val repositoryTreeUri by viewModel.repositoryTreeUri.collectAsStateWithLifecycle()
    val repositoryImportState by viewModel.repositoryImportState.collectAsStateWithLifecycle()

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is SettingsNavEvent.OpenUrl -> uriHandler.openUri(event.url)
                is SettingsNavEvent.OpenLicenses -> uriHandler.openUri("https://github.com/rafaelmeloreisnovo/RafGitTools/blob/main/License2.md")
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // The provider may grant only the current session. Persist the
                // selection itself so the UI never silently discards it.
            }
            viewModel.setRepositoryTreeUri(uri.toString())
        }
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGitConfigDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showRepositoryGovernance by remember { mutableStateOf(false) }

    if (showRepositoryGovernance) {
        RepositoryGovernanceScreen(
            onNavigateBack = { showRepositoryGovernance = false }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Appearance Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    subtitle = stringResource(R.string.settings_use_dark_theme),
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = currentLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )
            }
            
            // Git Configuration Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_git_configuration))
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.settings_user_name),
                    subtitle = gitConfig.userName.ifBlank { stringResource(R.string.settings_not_set) },
                    onClick = { showGitConfigDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Email,
                    title = stringResource(R.string.settings_user_email),
                    subtitle = gitConfig.userEmail.ifBlank { stringResource(R.string.settings_not_set) },
                    onClick = { showGitConfigDialog = true }
                )
            }
            
            if (FeatureFlags.ENABLE_GPG) {
                item {
                    SwitchSettingsItem(
                        icon = Icons.Default.Commit,
                        title = stringResource(R.string.settings_sign_commits),
                        subtitle = stringResource(R.string.settings_sign_commits_subtitle),
                        checked = gitConfig.signCommits,
                        onCheckedChange = { viewModel.setSignCommits(it) }
                    )
                }
            }

            // Repository governance: provider-bound repository settings + security.
            item {
                SettingsSectionHeader(title = stringResource(R.string.repo_governance_title))
            }

            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.repo_governance_entry_title),
                    subtitle = stringResource(R.string.repo_governance_entry_subtitle),
                    onClick = { showRepositoryGovernance = true }
                )
            }
            
            // Storage Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_storage))
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Storage,
                    title = stringResource(R.string.settings_clear_cache),
                    subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                    onClick = { showCacheDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.settings_repository_location),
                    subtitle = if (repositoryTreeUri.isBlank()) {
                        "TOKEN_VAZIO — nenhuma pasta persistida"
                    } else {
                        val provider = Uri.parse(repositoryTreeUri).authority ?: "TOKEN_VAZIO"
                        "SAF provider: $provider · permissão de leitura persistida"
                    },
                    onClick = { folderPickerLauncher.launch(null) }
                )
            }

            item {
                RepositoryImportCard(
                    treeSelected = repositoryTreeUri.isNotBlank(),
                    state = repositoryImportState,
                    onImport = viewModel::importSelectedRepositoryTree
                )
            }
            
            // About Section
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_rafgittools),
                    subtitle = stringResource(R.string.settings_version_alpha),
                    onClick = { showAboutDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Policy,
                    title = stringResource(R.string.settings_privacy_policy),
                    subtitle = stringResource(R.string.settings_privacy_policy_subtitle),
                    onClick = { viewModel.openPrivacyPolicy() }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Code,
                    title = stringResource(R.string.settings_open_source_licenses),
                    subtitle = stringResource(R.string.settings_open_source_licenses_subtitle),
                    onClick = { viewModel.openLicenses() }
                )
            }
        }
        
        // Language Dialog
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = currentLanguage,
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { language ->
                    viewModel.setLanguage(language)
                    onLanguageChange(language)
                    showLanguageDialog = false
                }
            )
        }
        
        // Git Config Dialog
        if (showGitConfigDialog) {
            GitConfigDialog(
                currentName = gitConfig.userName,
                currentEmail = gitConfig.userEmail,
                onDismiss = { showGitConfigDialog = false },
                onSave = { name, email ->
                    viewModel.setGitConfig(name, email)
                    showGitConfigDialog = false
                }
            )
        }
        
        // Cache Dialog
        if (showCacheDialog) {
            AlertDialog(
                onDismissRequest = { showCacheDialog = false },
                title = { Text(stringResource(R.string.settings_clear_cache)) },
                text = { Text(stringResource(R.string.settings_clear_cache_warning)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearCache()
                            showCacheDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.settings_clear_cache_action))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCacheDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
        
        // About Dialog
        if (showAboutDialog) {
            AboutDialog(onDismiss = { showAboutDialog = false })
        }
    }
}

@Composable
private fun RepositoryImportCard(
    treeSelected: Boolean,
    state: RepositoryImportState,
    onImport: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar workspace Git", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Cria um snapshot somente-leitura da origem SAF dentro do armazenamento privado do app, valida .git com JGit e só então registra no workspace Local.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Origem SAF: sem escrita · limite: 50.000 arquivos / 512 MiB · cada import gera nova versão",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (state) {
                RepositoryImportState.Idle -> Text("IMPORT: TOKEN_VAZIO")
                RepositoryImportState.Running -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("IMPORT: executando…")
                    }
                }
                is RepositoryImportState.Passed -> {
                    val receipt = state.receipt
                    Text("IMPORT: PASS · ${receipt.repositoryName}")
                    Text(
                        "${receipt.fileCount} arquivos · ${receipt.directoryCount} diretórios · ${receipt.byteCount} bytes · branch ${receipt.branch}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Receipt: ${receipt.receiptPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Workspace: ${receipt.repositoryPath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is RepositoryImportState.Failed -> {
                    Text(
                        "IMPORT: FAIL · ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = onImport,
                enabled = treeSelected && state !is RepositoryImportState.Running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Importar snapshot Git")
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ClickableSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SwitchSettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: Language,
    onDismiss: () -> Unit,
    onLanguageSelected: (Language) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_select)) },
        text = {
            Column {
                Language.entries.forEach { language ->
                    ListItem(
                        headlineContent = { Text(language.displayName) },
                        leadingContent = {
                            RadioButton(
                                selected = language == currentLanguage,
                                onClick = { onLanguageSelected(language) }
                            )
                        },
                        modifier = Modifier.clickable { onLanguageSelected(language) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun GitConfigDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_git_configuration)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.settings_user_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.settings_user_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, email) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_about_rafgittools)) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    text = stringResource(R.string.settings_version_alpha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.settings_about_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = stringResource(R.string.settings_about_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}
