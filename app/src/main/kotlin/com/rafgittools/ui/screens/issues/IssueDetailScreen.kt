package com.rafgittools.ui.screens.issues

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rafgittools.domain.model.github.GithubComment
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.ui.theme.GitHubColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Issue detail with IME-safe comment composer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    owner: String,
    repo: String,
    issueNumber: Int,
    viewModel: IssueDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val issue by viewModel.issue.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
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
                        Text("#$issueNumber", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$owner/$repo",
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
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (uiState is IssueDetailUiState.Success) {
                CommentComposer(
                    value = newComment,
                    submitting = isSubmitting,
                    onValueChange = { newComment = it },
                    onSend = {
                        if (newComment.isNotBlank() && !isSubmitting) {
                            isSubmitting = true
                            viewModel.addComment(newComment.trim()) {
                                newComment = ""
                                isSubmitting = false
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                IssueDetailUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is IssueDetailUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadIssue(owner, repo, issueNumber) },
                    modifier = Modifier.align(Alignment.Center)
                )
                IssueDetailUiState.Success -> issue?.let { data ->
                    IssueContent(issue = data, comments = comments)
                }
            }
        }
    }
}

@Composable
private fun CommentComposer(
    value: String,
    submitting: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment…") },
                minLines = 1,
                maxLines = 3,
                enabled = !submitting
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !submitting
            ) {
                if (submitting) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun IssueContent(issue: GithubIssue, comments: List<GithubComment>) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { IssueHeader(issue, dateFormat) }

        item {
            Card(Modifier.fillMaxWidth()) {
                SelectionContainer {
                    Text(
                        text = issue.body?.takeIf { it.isNotBlank() } ?: "No description provided.",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (issue.body.isNullOrBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }

        if (issue.labels.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(issue.labels, key = { it.id }) { label ->
                        AssistChip(onClick = {}, label = { Text(label.name) })
                    }
                }
            }
        }

        item {
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Text(
                "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (comments.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        null,
                        Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("No comments yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentCard(comment, dateFormat)
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun IssueHeader(issue: GithubIssue, dateFormat: SimpleDateFormat) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (issue.state == "open") {
                    GitHubColors.OpenGreen.copy(alpha = 0.18f)
                } else {
                    GitHubColors.MergedPurple.copy(alpha = 0.18f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (issue.state == "open") Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
                        null,
                        Modifier.size(15.dp),
                        tint = if (issue.state == "open") GitHubColors.OpenGreen else GitHubColors.MergedPurple
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(issue.state.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("#${issue.number}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))
        Text(issue.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = issue.user.avatarUrl,
                contentDescription = "Author avatar",
                modifier = Modifier.size(24.dp).clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(6.dp))
            Text(issue.user.login, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(
                "opened on ${dateFormat.format(parseIsoDate(issue.createdAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommentCard(comment: GithubComment, dateFormat: SimpleDateFormat) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = comment.user.avatarUrl,
                    contentDescription = "Comment author avatar",
                    modifier = Modifier.size(28.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(comment.user.login, fontWeight = FontWeight.Medium)
                    Text(
                        dateFormat.format(parseIsoDate(comment.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                Text(comment.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Error, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(10.dp))
        Text("Error Loading Issue", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(6.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

private fun parseIsoDate(isoDate: String): Date = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).parse(isoDate) ?: Date()
} catch (_: Exception) {
    Date()
}
