package com.rafgittools.ui.screens.createpr

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
fun CreatePullRequestScreen(
    owner: String,
    repo: String,
    viewModel: CreatePullRequestViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onPullRequestCreated: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val title by viewModel.title.collectAsState()
    val body by viewModel.body.collectAsState()
    val baseBranch by viewModel.baseBranch.collectAsState()
    val headBranch by viewModel.headBranch.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is CreatePullRequestViewModel.UiState.Success) {
            onPullRequestCreated()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Pull Request") },
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
                placeholder = { Text("Pull request title") }
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = baseBranch,
                    onValueChange = { viewModel.setBaseBranch(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Base") }
                )
                Text("←", modifier = Modifier.padding(top = 16.dp))
                OutlinedTextField(
                    value = headBranch,
                    onValueChange = { viewModel.setHeadBranch(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Compare") }
                )
            }
            
            OutlinedTextField(
                value = body,
                onValueChange = { viewModel.setBody(it) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Description") },
                placeholder = { Text("Describe your changes...") },
                minLines = 10
            )
            
            Button(
                onClick = { viewModel.createPullRequest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && headBranch.isNotBlank()
            ) {
                Text("Create Pull Request")
            }
        }
    }
}
