package com.rafgittools.ui.screens.issues

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rafgittools.domain.model.github.GithubComment
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.domain.model.github.GithubLabel
import com.rafgittools.ui.theme.GitHubColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Issue detail screen showing full issue info with comments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    owner: String,
    repo: String,
    issueNumber: Int,
    viewModel: IssueDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val issue by viewModel.issue.collectAsState()
    val comments by viewModel.comments.collectAsState()
    var newComment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    LaunchedEffect(owner, repo, issueNumber) {
        viewModel.loadIssue(owner, repo, issueNumber)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "#$issueNumber",
                            style = MaterialTheme.typography.titleMedium
                        )
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
        bottomBar = {
            // Comment input bar
            if (uiState is IssueDetailUiState.Success) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newComment,
                            onValueChange = { newComment = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add a comment...") },
                            maxLines = 4,
                            enabled = !isSubmitting
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newComment.isNotBlank()) {
                                    isSubmitting = true
                                    viewModel.addComment(newComment) {
                                        newComment = ""
                                        isSubmitting = false
                                    }
                                }
                            },
                            enabled = newComment.isNotBlank() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (newComment.isNotBlank()) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is IssueDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is IssueDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadIssue(owner, repo, issueNumber) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is IssueDetailUiState.Success -> {
                    issue?.let { issueData ->
                        IssueContent(
                            issue = issueData,
                            comments = comments
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueContent(
    issue: GithubIssue,
    comments: List<GithubComment>
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Issue Header
        item {
            IssueHeader(issue = issue, dateFormat = dateFormat)
        }
        
        // Issue Body
        item {
            IssueBody(body = issue.body)
        }
        
        // Labels
        if (issue.labels.isNotEmpty()) {
            item {
                IssueLabels(labels = issue.labels)
            }
        }
        
        // Assignees
        if (issue.assignees.isNotEmpty()) {
            item {
                IssueAssignees(assignees = issue.assignees)
            }
        }
        
        // Comments Section
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (comments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No comments yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(comments) { comment ->
                CommentCard(comment = comment, dateFormat = dateFormat)
            }
        }
        
        // Bottom spacing for the input bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IssueHeader(
    issue: GithubIssue,
    dateFormat: SimpleDateFormat
) {
    Column {
        // State badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (issue.state == "open") 
                    GitHubColors.OpenGreen.copy(alpha = 0.2f) 
                else 
                    GitHubColors.MergedPurple.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (issue.state == "open") 
                            Icons.Default.RadioButtonUnchecked 
                        else 
                            Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (issue.state == "open") 
                            GitHubColors.OpenGreen 
                        else 
                            GitHubColors.MergedPurple
                    )
                    Text(
                        text = issue.state.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (issue.state == "open") 
                            GitHubColors.OpenGreen 
                        else 
                            GitHubColors.MergedPurple
                    )
                }
            }
            
            Text(
                text = "#${issue.number}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = issue.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Author and date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = issue.user.avatarUrl,
                contentDescription = "Author avatar",
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Text(
                text = issue.user.login,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "opened on ${dateFormat.format(parseIsoDate(issue.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IssueBody(body: String?) {
    if (body.isNullOrBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "No description provided.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            SelectionContainer {
                Text(
                    text = body,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun IssueLabels(labels: List<GithubLabel>) {
    Column {
        Text(
            text = "Labels",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { label ->
                val backgroundColor = try {
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#${label.color}"))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }
                
                Surface(
                    color = backgroundColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = label.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = backgroundColor
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueAssignees(assignees: List<com.rafgittools.domain.model.github.GithubUser>) {
    Column {
        Text(
            text = "Assignees",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            assignees.forEach { assignee ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AsyncImage(
                        model = assignee.avatarUrl,
                        contentDescription = assignee.login,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = assignee.login,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: GithubComment,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Comment header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = comment.user.avatarUrl,
                        contentDescription = "Comment author avatar",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = comment.user.login,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = dateFormat.format(parseIsoDate(comment.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Author association badge if available
                comment.authorAssociation?.let { association ->
                    if (association != "NONE" && association != "CONTRIBUTOR") {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = association.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Comment body
            SelectionContainer {
                Text(
                    text = comment.body,
                    style = MaterialTheme.typography.bodyMedium
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
            text = "Error Loading Issue",
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

// Helper function
private fun parseIsoDate(isoDate: String): Date {
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(isoDate) ?: Date()
    } catch (e: Exception) {
        Date()
    }
}
