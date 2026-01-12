package com.rafgittools.ui.screens.releases

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDetailScreen(
    owner: String,
    repo: String,
    releaseId: Long,
    viewModel: ReleaseDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val release by viewModel.release.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Release Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Release details coming soon", color = MaterialTheme.colorScheme.outline)
        }
    }
}
