package com.rafgittools.ui.screens.createissue

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIssueScreen(
    owner: String,
    repo: String,
    viewModel: CreateIssueViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onIssueCreated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.title.collectAsState()
    val body by viewModel.body.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is CreateIssueViewModel.UiState.Success) {
            onIssueCreated()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Issue") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("Issue title") }
            )
            
            OutlinedTextField(
                value = body,
                onValueChange = { viewModel.setBody(it) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Description") },
                placeholder = { Text("Describe the issue...") },
                minLines = 10
            )
            
            Button(
                onClick = { viewModel.createIssue() },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("Create Issue")
            }
        }
    }
}
