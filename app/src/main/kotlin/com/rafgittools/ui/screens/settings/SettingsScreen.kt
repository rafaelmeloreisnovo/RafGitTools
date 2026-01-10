package com.rafgittools.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val gitConfig by viewModel.gitConfig.collectAsState()
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showGitConfigDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                SettingsSectionHeader(title = "Appearance")
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = currentLanguage.displayName,
                    onClick = { showLanguageDialog = true }
                )
            }
            
            // Git Configuration Section
            item {
                SettingsSectionHeader(title = "Git Configuration")
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Person,
                    title = "User Name",
                    subtitle = gitConfig.userName.ifBlank { "Not set" },
                    onClick = { showGitConfigDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Email,
                    title = "User Email",
                    subtitle = gitConfig.userEmail.ifBlank { "Not set" },
                    onClick = { showGitConfigDialog = true }
                )
            }
            
            item {
                SwitchSettingsItem(
                    icon = Icons.Default.Commit,
                    title = "Sign Commits",
                    subtitle = "Sign commits with GPG key",
                    checked = gitConfig.signCommits,
                    onCheckedChange = { viewModel.setSignCommits(it) }
                )
            }
            
            // Storage Section
            item {
                SettingsSectionHeader(title = "Storage")
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Clear Cache",
                    subtitle = "Free up storage space",
                    onClick = { showCacheDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Repository Location",
                    subtitle = "/storage/emulated/0/RafGitTools/repositories",
                    onClick = { /* Could open folder picker */ }
                )
            }
            
            // About Section
            item {
                SettingsSectionHeader(title = "About")
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Info,
                    title = "About RafGitTools",
                    subtitle = "Version 1.0.0-alpha",
                    onClick = { showAboutDialog = true }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Policy,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { viewModel.openPrivacyPolicy() }
                )
            }
            
            item {
                ClickableSettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
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
                title = { Text("Clear Cache") },
                text = { Text("This will clear all cached data including repository metadata. This cannot be undone.") },
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
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCacheDialog = false }) {
                        Text("Cancel")
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
        title = { Text("Select Language") },
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
                Text("Cancel")
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
        title = { Text("Git Configuration") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("User Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("User Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, email) }) {
                Text("Save")
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
private fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About RafGitTools") },
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
                    text = "RafGitTools",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Text(
                    text = "Version 1.0.0-alpha",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "A unified Git/GitHub Android client combining the best features from FastHub, MGit, PuppyGit, and Termux.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "© 2024 RafGitTools Team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Licensed under GPL-3.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
