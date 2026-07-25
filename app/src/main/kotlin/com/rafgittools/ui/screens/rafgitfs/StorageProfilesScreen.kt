package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.rafgitfs.data.StorageProfileEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProfilesScreen(
    onNavigateBack: () -> Unit,
    onOpenRepositories: (String) -> Unit,
    onOpenSettings: (String) -> Unit,
    viewModel: StorageProfilesViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RafGitFS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(
                            "GitHub as governed virtual storage",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Browse repositories like folders while preserving branches, SHAs, evidence and audit boundaries.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    RafGitFsStatusBanner(status)
                }
            }

            if (profiles.isEmpty()) {
                item {
                    RafGitFsEmptyState(
                        title = "Preparing secure profile",
                        detail = "The local read-only profile is being created."
                    )
                }
            } else {
                items(profiles, key = { it.profileId }) { profile ->
                    StorageProfileCard(
                        profile = profile,
                        onOpen = { if (profile.isEnabled) onOpenRepositories(profile.profileId) },
                        onSettings = { onOpenSettings(profile.profileId) },
                        onEnabledChange = { viewModel.setEnabled(profile.profileId, it) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageProfileCard(
    profile: StorageProfileEntity,
    onOpen: () -> Unit,
    onSettings: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        onClick = onOpen,
        enabled = profile.isEnabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${profile.provider} · ${profile.scope}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = profile.isEnabled,
                    onCheckedChange = onEnabledChange
                )
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Storage settings")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Cache: ${profile.cachePolicy}", style = MaterialTheme.typography.bodySmall)
                    Text("Default ref: ${profile.defaultRef}", style = MaterialTheme.typography.bodySmall)
                }
                RafGitFsReadOnlyBadge()
            }
        }
    }
}
