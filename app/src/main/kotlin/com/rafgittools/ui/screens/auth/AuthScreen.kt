package com.rafgittools.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.data.auth.AuthMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel(), onAuthSuccess: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val username by viewModel.username.collectAsState()
    val selectedMethod by viewModel.selectedMethod.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success || uiState is AuthUiState.Offline) onAuthSuccess()
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("GitHub Authentication") },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isAuthenticated && username != null -> AuthenticatedContent(username!!, onLogout = viewModel::logout)
                selectedMethod == null -> AuthMethodSelection(onSelect = viewModel::selectMethod, onStartDeviceCode = viewModel::startDeviceCodeLogin, onStartOauthWeb = viewModel::startOAuthWebLogin, onImportGh = viewModel::importGhCliToken, onSsh = viewModel::authenticateWithSshKey, onOffline = viewModel::continueOffline)
                selectedMethod == AuthMethod.PAT -> PatLoginForm(viewModel, uiState)
                else -> MethodPlaceholder(selectedMethod = selectedMethod, uiState = uiState, onBack = viewModel::clearSelectedMethod)
            }
        }
    }
}

@Composable
private fun AuthMethodSelection(
    onSelect: (AuthMethod) -> Unit,
    onStartDeviceCode: () -> Unit,
    onStartOauthWeb: () -> Unit,
    onImportGh: () -> Unit,
    onSsh: () -> Unit,
    onOffline: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Escolha o método de login", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartDeviceCode, modifier = Modifier.fillMaxWidth()) { Text("Login com GitHub pelo navegador / código") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStartOauthWeb, modifier = Modifier.fillMaxWidth()) { Text("OAuth Web (browser)") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onSelect(AuthMethod.PAT) }, modifier = Modifier.fillMaxWidth()) { Text("Usar Personal Access Token") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onImportGh, modifier = Modifier.fillMaxWidth()) { Text("Importar sessão do gh CLI / Termux") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSsh, modifier = Modifier.fillMaxWidth()) { Text("Usar chave SSH") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) { Text("Continuar offline/local") }
    }
}

@Composable
private fun MethodPlaceholder(selectedMethod: AuthMethod?, uiState: AuthUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Método: ${selectedMethod?.name}")
        if (uiState is AuthUiState.Loading) CircularProgressIndicator()
        if (uiState is AuthUiState.DeviceCodePending) {
            Text("Abra o navegador e confirme o login:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("User code", style = MaterialTheme.typography.labelMedium)
                    Text(uiState.userCode, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Text("Verification URL", style = MaterialTheme.typography.labelMedium)
                    Text(uiState.verificationUri, style = MaterialTheme.typography.bodyMedium)
                    if (selectedMethod == AuthMethod.OAUTH_WEB) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { LocalUriHandler.current.openUri(uiState.verificationUri) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Abrir navegador")
                        }
                    }
                }
            }
        }
        if (uiState is AuthUiState.DeviceCodePolling) {
            Text("Aguardando autorização no GitHub (${uiState.attempt}/${uiState.max})")
        }
        if (uiState is AuthUiState.Error) Text(uiState.message, color = MaterialTheme.colorScheme.error)
        if (uiState is AuthUiState.Offline) Text("Modo offline ativo. Recursos locais liberados.")
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
private fun PatLoginForm(viewModel: AuthViewModel, uiState: AuthUiState) { /* keep old form */
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Code, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Sign in with GitHub", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Enter your Personal Access Token (PAT) to authenticate", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Personal Access Token") }, placeholder = { Text("ghp_xxxxxxxxxxxx") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (token.isNotBlank()) viewModel.authenticateWithPat(token) }), trailingIcon = { IconButton(onClick = { showToken = !showToken }) { Icon(if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }, leadingIcon = { Icon(Icons.Default.Key, null) }, isError = uiState is AuthUiState.Error)
        if (uiState is AuthUiState.Error) { Spacer(Modifier.height(8.dp)); Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.authenticateWithPat(token) }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = token.isNotBlank() && uiState !is AuthUiState.Loading) {
            if (uiState is AuthUiState.Loading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else { Icon(Icons.Default.Login, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Sign In") }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("How to create a PAT:"); Spacer(Modifier.height(8.dp)); Text("1. Go to GitHub Settings → Developer settings → Personal access tokens\n2. Click 'Generate new token (classic)' or 'Fine-grained tokens'\n3. Select scopes: repo, read:user, read:org\n4. Copy the generated token", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); TextButton(onClick = { uriHandler.openUri("https://github.com/settings/tokens") }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Create Token") } } }
    }
}

@Composable
private fun AuthenticatedContent(username: String, onLogout: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.CheckCircle, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(24.dp)); Text("Authenticated", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp)); Text("Signed in as @$username", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(32.dp)); OutlinedButton(onClick = onLogout, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Logout, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Sign Out") } } }
