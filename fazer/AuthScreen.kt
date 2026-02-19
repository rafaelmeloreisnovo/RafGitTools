package com.rafgittools.ui.screens.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val oauthState by viewModel.oauthState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Personal Access Token", "OAuth Device Flow")

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthSuccess()
    }
    LaunchedEffect(oauthState) {
        if (oauthState is OAuthUiState.Authorized) onAuthSuccess()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GitHub Authentication") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PatLoginTab(viewModel = viewModel, uiState = uiState)
                1 -> OAuthDeviceFlowTab(viewModel = viewModel, oauthState = oauthState)
            }
        }
    }
}

@Composable
private fun PatLoginTab(viewModel: AuthViewModel, uiState: AuthUiState) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Personal Access Token",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Generate a PAT at GitHub → Settings → Developer settings → Personal access tokens",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("GitHub Personal Access Token") },
            placeholder = { Text("ghp_xxxxxxxxxxxx") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                if (token.isNotBlank()) viewModel.saveToken(token)
            }),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showToken) "Hide token" else "Show token"
                    )
                }
            },
            isError = uiState is AuthUiState.Error
        )
        if (uiState is AuthUiState.Error) {
            Text(
                uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Button(
            onClick = { viewModel.saveToken(token) },
            modifier = Modifier.fillMaxWidth(),
            enabled = token.isNotBlank() && uiState !is AuthUiState.Loading
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Sign In with Token")
        }
        TextButton(onClick = { uriHandler.openUri("https://github.com/settings/tokens/new") }) {
            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Generate a new token")
        }
    }
}

@Composable
private fun OAuthDeviceFlowTab(viewModel: AuthViewModel, oauthState: OAuthUiState) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            Icons.Default.DevicesOther,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "OAuth Device Flow",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        when (val state = oauthState) {
            is OAuthUiState.Idle -> {
                Text(
                    "Authenticate without entering your password. We'll give you a code to enter on GitHub.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
                Button(
                    onClick = { viewModel.startOAuthFlow() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start OAuth Flow")
                }
            }
            is OAuthUiState.PendingUserAction -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Step 1 — Copy this code", fontWeight = FontWeight.SemiBold)
                        Text(
                            state.userCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("GitHub Code", state.userCode))
                        }) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Code")
                        }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Step 2 — Open GitHub and enter the code above", fontWeight = FontWeight.SemiBold)
                        Text(state.verificationUri, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                        Button(
                            onClick = { uriHandler.openUri(state.verificationUri) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open GitHub")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Waiting for authorization…", color = MaterialTheme.colorScheme.outline)
                }
                Text(
                    "Expires in ${state.expiresInSeconds / 60} minutes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                TextButton(onClick = { viewModel.cancelOAuth() }) { Text("Cancel") }
            }
            is OAuthUiState.Polling -> {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("Polling GitHub… (${state.attempt}/${state.max})",
                    color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = { viewModel.cancelOAuth() }) { Text("Cancel") }
            }
            is OAuthUiState.Authorized -> {
                Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Text("Authorized as ${state.username}!", fontWeight = FontWeight.Bold)
            }
            is OAuthUiState.Error -> {
                Icon(Icons.Default.ErrorOutline, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Button(onClick = { viewModel.startOAuthFlow() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Try Again")
                }
            }
        }
    }
}
