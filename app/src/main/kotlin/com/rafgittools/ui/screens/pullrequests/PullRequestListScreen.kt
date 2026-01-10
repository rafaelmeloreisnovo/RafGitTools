package com.rafgittools.ui.screens.pullrequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.github.GithubPullRequest
import com.rafgittools.ui.theme.GitHubColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pull request list screen showing PRs for a repository
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestListScreen(
    owner: String,
    repo: String,
    viewModel: PullRequestListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onPullRequestClick: (GithubPullRequest) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRequests by viewModel.pullRequests.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    LaunchedEffect(owner, repo) {
        viewModel.loadPullRequests(owner, repo)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pull Requests")
                        Text(
                            text = "$owner/$repo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is PullRequestListUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is PullRequestListUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.loadPullRequests(owner, repo) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is PullRequestListUiState.Empty -> {
                        EmptyContent(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is PullRequestListUiState.Success -> {
                        PullRequestList(
                            pullRequests = pullRequests,
                            onPullRequestClick = onPullRequestClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: PullRequestFilter,
    onFilterSelected: (PullRequestFilter) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedFilter.ordinal
    ) {
        PullRequestFilter.entries.forEach { filter ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (filter) {
                                PullRequestFilter.OPEN -> Icons.Default.CallMerge
                                PullRequestFilter.CLOSED -> Icons.Default.Close
                                PullRequestFilter.ALL -> Icons.Default.List
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(filter.displayName)
                    }
                }
            )
        }
    }
}

@Composable
private fun PullRequestList(
    pullRequests: List<GithubPullRequest>,
    onPullRequestClick: (GithubPullRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${pullRequests.size} pull requests",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(pullRequests) { pr ->
            PullRequestCard(
                pullRequest = pr,
                onClick = { onPullRequestClick(pr) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullRequestCard(
    pullRequest: GithubPullRequest,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with number, state, and draft indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            pullRequest.draft -> Icons.Default.Edit
                            pullRequest.mergedAt != null -> Icons.Default.MergeType
                            pullRequest.state == "open" -> Icons.Default.CallMerge
                            else -> Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = when {
                            pullRequest.draft -> GitHubColors.DraftGray
                            pullRequest.mergedAt != null -> GitHubColors.MergedPurple
                            pullRequest.state == "open" -> GitHubColors.OpenGreen
                            else -> GitHubColors.ClosedRed
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "#${pullRequest.number}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (pullRequest.draft) {
                        AssistChip(
                            onClick = { },
                            label = { Text("Draft", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                
                Text(
                    text = dateFormat.format(parseIsoDate(pullRequest.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = pullRequest.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Branch info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = pullRequest.base.ref,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
                
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AssistChip(
                    onClick = { },
                    label = {
                        Text(
                            text = pullRequest.head.ref,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer with author
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pullRequest.user.login,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status badge
                Text(
                    text = when {
                        pullRequest.mergedAt != null -> "Merged"
                        pullRequest.draft -> "Draft"
                        pullRequest.state == "open" -> "Open"
                        else -> "Closed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        pullRequest.mergedAt != null -> GitHubColors.MergedPurple
                        pullRequest.draft -> GitHubColors.DraftGray
                        pullRequest.state == "open" -> GitHubColors.OpenGreen
                        else -> GitHubColors.ClosedRed
                    }
                )
            }
        }
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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error Loading Pull Requests",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CallMerge,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Pull Requests",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No pull requests match your current filter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function
private fun parseIsoDate(isoDate: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(isoDate) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

/**
 * Pull request filter options
 */
enum class PullRequestFilter(val displayName: String, val apiValue: String) {
    OPEN("Open", "open"),
    CLOSED("Closed", "closed"),
    ALL("All", "all")
}
