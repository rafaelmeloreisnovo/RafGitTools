package com.rafgittools.ui.screens.issues

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
import com.rafgittools.domain.model.github.GithubIssue
import java.text.SimpleDateFormat
import java.util.*

/**
 * Issue list screen showing GitHub issues for a repository
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueListScreen(
    owner: String,
    repo: String,
    viewModel: IssueListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onIssueClick: (GithubIssue) -> Unit = {},
    onCreateIssue: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val issues by viewModel.issues.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    
    LaunchedEffect(owner, repo) {
        viewModel.loadIssues(owner, repo)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Issues")
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateIssue) {
                Icon(Icons.Default.Add, contentDescription = "Create issue")
            }
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
                    is IssueListUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is IssueListUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.loadIssues(owner, repo) },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is IssueListUiState.Empty -> {
                        EmptyContent(
                            onCreateIssue = onCreateIssue,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is IssueListUiState.Success -> {
                        IssueList(
                            issues = issues,
                            onIssueClick = onIssueClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    selectedFilter: IssueFilter,
    onFilterSelected: (IssueFilter) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedFilter.ordinal
    ) {
        IssueFilter.entries.forEach { filter ->
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
                                IssueFilter.OPEN -> Icons.Default.RadioButtonUnchecked
                                IssueFilter.CLOSED -> Icons.Default.CheckCircle
                                IssueFilter.ALL -> Icons.Default.List
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
private fun IssueList(
    issues: List<GithubIssue>,
    onIssueClick: (GithubIssue) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "${issues.size} issues",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(issues) { issue ->
            IssueCard(
                issue = issue,
                onClick = { onIssueClick(issue) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueCard(
    issue: GithubIssue,
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
            // Header with number and state
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
                        imageVector = if (issue.state == "open") {
                            Icons.Default.RadioButtonUnchecked
                        } else {
                            Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = if (issue.state == "open") {
                            Color(0xFF238636)  // GitHub green
                        } else {
                            Color(0xFF8957E5)  // GitHub purple
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "#${issue.number}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = dateFormat.format(parseIsoDate(issue.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = issue.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Labels
            if (issue.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    issue.labels.take(3).forEach { label ->
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = parseHexColor(label.color)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    if (issue.labels.size > 3) {
                        Text(
                            text = "+${issue.labels.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer with author and comments
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
                        text = issue.user.login,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (issue.commentsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = issue.commentsCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
            text = "Error Loading Issues",
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
    onCreateIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Issues",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No issues match your current filter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateIssue) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Issue")
        }
    }
}

// Helper functions
private fun parseIsoDate(isoDate: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(isoDate) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex")).copy(alpha = 0.3f)
    } catch (e: Exception) {
        Color.Gray.copy(alpha = 0.3f)
    }
}

/**
 * Issue filter options
 */
enum class IssueFilter(val displayName: String, val apiValue: String) {
    OPEN("Open", "open"),
    CLOSED("Closed", "closed"),
    ALL("All", "all")
}
