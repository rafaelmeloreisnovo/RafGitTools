package com.rafgittools.ui.screens.rafgitfs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RafGitFS settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RafGitFsStatusBanner(state.status)
            if (profile == null) {
                RafGitFsEmptyState(
                    title = "Profile unavailable",
                    detail = "Return to RafGitFS and recreate the default profile."
                )
                return@Column
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text(
                            "  Remote write policy is permanently blocked in V1.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Access mode: READ_ONLY")
                    Text("Receipts required: true")
                    Text("Protected branch writes: false")
                    Text("Claim allowed: false")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cache policy", style = MaterialTheme.typography.titleSmall)
                    StorageSettingsViewModel.ALLOWED_CACHE_POLICIES.forEach { policy ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = profile.cachePolicy == policy,
                                onClick = { viewModel.setCachePolicy(policy) }
                            )
                            Column {
                                Text(policy)
                                Text(
                                    cachePolicyDescription(policy),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.maxCacheMiBText,
                onValueChange = viewModel::setMaxCacheMiB,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Maximum cache (MiB)") },
                supportingText = { Text("Allowed local budget: 16–4096 MiB") }
            )

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save local settings")
            }
        }
    }
}

private fun cachePolicyDescription(policy: String): String = when (policy) {
    "METADATA_ONLY" -> "Keep only repository, ref and tree metadata."
    "SELECTIVE_OFFLINE" -> "Permit explicitly selected offline content in Prompt 5."
    else -> "Load content only when the user opens it."
}
