package com.rafgittools.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rafgittools.domain.model.github.GithubRepository
import com.rafgittools.domain.model.github.GithubUser

/**
 * Home screen showing GitHub repositories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAuth: () -> Unit = {},
    onNavigateToRepository: (GithubRepository) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val user by viewModel.user.collectAsState()
    val repositories by viewModel.repositories.collectAsState()
    
    var showMenu by remember { mutableStateOf(false) }
    
    // Navigate to auth if not authenticated
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated && uiState is HomeUiState.NotAuthenticated) {
            // Don't auto-navigate, let user click the button
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RafGitTools") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (isAuthenticated) {
                        // User avatar and menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                if (user != null) {
                                    AsyncImage(
                                        model = user!!.avatarUrl,
                                        contentDescription = "User avatar",
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Account"
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                user?.let { u ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(
                                                    text = u.name ?: u.login,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = "@${u.login}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = { showMenu = false },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    HorizontalDivider()
                                }
                                DropdownMenuItem(
                                    text = { Text("Refresh") },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.refresh()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    onClick = { 
                                        showMenu = false
                                        viewModel.logout()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        TextButton(onClick = onNavigateToAuth) {
                            Text("Sign In")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is HomeUiState.NotAuthenticated -> {
                    NotAuthenticatedContent(
                        onSignIn = onNavigateToAuth,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is HomeUiState.Empty -> {
                    EmptyRepositoriesContent(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                is HomeUiState.Success -> {
                    RepositoryList(
                        repositories = repositories,
                        onRepositoryClick = onNavigateToRepository
                    )
                }
                
                is HomeUiState.Error -> {
                    ErrorContent(
                        message = (uiState as HomeUiState.Error).message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryList(
    repositories: List<GithubRepository>,
    onRepositoryClick: (GithubRepository) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Your Repositories (${repositories.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(repositories) { repo ->
            RepositoryCard(
                repository = repo,
                onClick = { onRepositoryClick(repo) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryCard(
    repository: GithubRepository,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (repository.isPrivate) {
                        Icons.Default.Lock
                    } else {
                        Icons.Default.Public
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = repository.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (repository.isFork) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = "Fork",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            repository.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repository.language?.let { lang ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = getLanguageColor(lang)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = repository.stargazersCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = repository.forksCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NotAuthenticatedContent(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to RafGitTools",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in with your GitHub account to access your repositories",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onSignIn) {
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign In with GitHub")
        }
    }
}

@Composable
private fun EmptyRepositoriesContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "No Repositories",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You don't have any repositories yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Get color for programming language
 */
@Composable
private fun getLanguageColor(language: String): androidx.compose.ui.graphics.Color {
    return when (language.lowercase()) {
        "kotlin" -> androidx.compose.ui.graphics.Color(0xFFA97BFF)
        "java" -> androidx.compose.ui.graphics.Color(0xFFB07219)
        "javascript" -> androidx.compose.ui.graphics.Color(0xFFF1E05A)
        "typescript" -> androidx.compose.ui.graphics.Color(0xFF3178C6)
        "python" -> androidx.compose.ui.graphics.Color(0xFF3572A5)
        "go" -> androidx.compose.ui.graphics.Color(0xFF00ADD8)
        "rust" -> androidx.compose.ui.graphics.Color(0xFFDEA584)
        "c" -> androidx.compose.ui.graphics.Color(0xFF555555)
        "c++" -> androidx.compose.ui.graphics.Color(0xFFF34B7D)
        "c#" -> androidx.compose.ui.graphics.Color(0xFF178600)
        "swift" -> androidx.compose.ui.graphics.Color(0xFFFFAC45)
        "ruby" -> androidx.compose.ui.graphics.Color(0xFF701516)
        "php" -> androidx.compose.ui.graphics.Color(0xFF4F5D95)
        "html" -> androidx.compose.ui.graphics.Color(0xFFE34C26)
        "css" -> androidx.compose.ui.graphics.Color(0xFF563D7C)
        "shell" -> androidx.compose.ui.graphics.Color(0xFF89E051)
        else -> MaterialTheme.colorScheme.primary
    }
}
