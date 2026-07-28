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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rafgittools.data.auth.AuthMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onAuthSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val selectedMethod by viewModel.selectedMethod.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success || uiState is AuthUiState.Offline) {
            onAuthSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conectar ao GitHub") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val currentUsername = username
            when {
                isAuthenticated && currentUsername != null -> AuthenticatedContent(
                    username = currentUsername,
                    onLogout = viewModel::logout
                )

                selectedMethod == null -> AuthMethodSelection(
                    onStartOauthWeb = viewModel::startOAuthWebLogin,
                    onStartDeviceCode = viewModel::startDeviceCodeLogin,
                    onSelectPat = { viewModel.selectMethod(AuthMethod.PAT) },
                    onImportGh = viewModel::importGhCliToken,
                    onSsh = viewModel::authenticateWithSshKey,
                    onOffline = viewModel::continueOffline
                )

                selectedMethod == AuthMethod.PAT -> PatLoginForm(viewModel, uiState)
                else -> MethodStatus(
                    selectedMethod = selectedMethod,
                    uiState = uiState,
                    onBack = viewModel::clearSelectedMethod
                )
            }
        }
    }
}

@Composable
private fun AuthMethodSelection(
    onStartOauthWeb: () -> Unit,
    onStartDeviceCode: () -> Unit,
    onSelectPat: () -> Unit,
    onImportGh: () -> Unit,
    onSsh: () -> Unit,
    onOffline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Escolha como conectar sua conta",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O RafGitTools nunca pede nem armazena sua senha do GitHub.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Button(onClick = onStartOauthWeb, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Entrar pelo navegador")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onStartDeviceCode, modifier = Modifier.fillMaxWidth()) {
            Text("Entrar com código do dispositivo")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onSelectPat, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Key, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Usar token de acesso — não é senha")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onImportGh, modifier = Modifier.fillMaxWidth()) {
            Text("Importar sessão do gh CLI / Termux")
        }
        Spacer(Modifier.height(12.dp))

        OutlinedButton(onClick = onSsh, modifier = Modifier.fillMaxWidth()) {
            Text("Usar chave SSH para Git local/remoto")
        }
        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onOffline, modifier = Modifier.fillMaxWidth()) {
            Text("Continuar somente com repositórios locais")
        }
    }
}

@Composable
private fun MethodStatus(
    selectedMethod: AuthMethod?,
    uiState: AuthUiState,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (uiState) {
            AuthUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Preparando conexão segura com o GitHub…")
            }

            is AuthUiState.DeviceCodePending -> {
                Text(
                    "Autorize o RafGitTools no GitHub",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Código", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.userCode, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(12.dp))
                        Text("Endereço oficial", style = MaterialTheme.typography.labelMedium)
                        Text(uiState.verificationUri, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { uriHandler.openUri(uiState.verificationUri) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir GitHub no navegador")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Digite o código no site do GitHub. A senha permanece apenas no GitHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            is AuthUiState.DeviceCodePolling -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Aguardando autorização no GitHub…")
                Text(
                    "Verificação ${uiState.attempt} de ${uiState.max}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is AuthUiState.Error -> {
                Text(
                    uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            is AuthUiState.Offline -> Text("Modo local ativado.")
            else -> Text("Método selecionado: ${selectedMethod?.name.orEmpty()}")
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onBack) { Text("Voltar") }
    }
}

@Composable
private fun PatLoginForm(viewModel: AuthViewModel, uiState: AuthUiState) {
    var token by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Token de acesso do GitHub",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cole um Personal Access Token. Não informe sua senha.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Personal Access Token") },
            placeholder = { Text("github_pat_… ou ghp_…") },
            supportingText = { Text("A credencial será validada em api.github.com/user e cifrada no Android Keystore.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (token.isNotBlank()) viewModel.authenticateWithPat(token.trim())
                }
            ),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showToken) "Ocultar token" else "Mostrar token"
                    )
                }
            },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
            isError = uiState is AuthUiState.Error
        )

        if (uiState is AuthUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.authenticateWithPat(token.trim()) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = token.isNotBlank() && uiState !is AuthUiState.Loading
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Login, contentDescription = null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Validar e conectar")
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Permissões recomendadas", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Para cliente completo: acesso aos repositórios escolhidos, leitura do perfil, issues, pull requests e notificações. Prefira token fine-grained e conceda apenas os repositórios necessários.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { uriHandler.openUri("https://github.com/settings/personal-access-tokens") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Criar token no GitHub")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = viewModel::clearSelectedMethod) { Text("Voltar") }
    }
}

@Composable
private fun AuthenticatedContent(username: String, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text("GitHub conectado", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Sessão ativa como @$username",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Encerrar sessão")
        }
    }
}
