package com.rafgittools.ui.screens.pullrequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rafgittools.R
import com.rafgittools.domain.model.github.*
import com.rafgittools.ui.theme.GitHubColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pull request detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestDetailScreen(
    owner: String,
    repo: String,
    prNumber: Int,
    viewModel: PullRequestDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRequest by viewModel.pullRequest.collectAsState()
    val files by viewModel.files.collectAsState()
    val commits by viewModel.commits.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    
    LaunchedEffect(owner, repo, prNumber) {
        viewModel.loadPullRequest(owner, repo, prNumber)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "#$prNumber",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
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
            when (val state = uiState) {
                is PullRequestDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PullRequestDetailUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadPullRequest(owner, repo, prNumber) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PullRequestDetailUiState.Success -> {
                    pullRequest?.let { pr ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            // PR Header
                            PullRequestHeader(
                                pullRequest = pr,
                                modifier = Modifier.padding(16.dp)
                            )
                            
                            // Tab Row
                            TabRow(
                                selectedTabIndex = selectedTab.ordinal
                            ) {
                                PullRequestTab.entries.forEach { tab ->
                                    Tab(
                                        selected = tab == selectedTab,
                                        onClick = { viewModel.selectTab(tab) },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(stringResource(tab.titleRes))
                                                val count = when (tab) {
                                                    PullRequestTab.FILES -> files.size
                                                    PullRequestTab.COMMITS -> commits.size
                                                    PullRequestTab.REVIEWS -> reviews.size
                                                    else -> null
                                                }
                                                count?.let {
                                                    Badge { Text(it.toString()) }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // Tab Content
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (selectedTab) {
                                    PullRequestTab.CONVERSATION -> {
                                        ConversationTab(pullRequest = pr)
                                    }
                                    PullRequestTab.FILES -> {
                                        FilesTab(files = files)
                                    }
                                    PullRequestTab.COMMITS -> {
                                        CommitsTab(commits = commits)
                                    }
                                    PullRequestTab.REVIEWS -> {
                                        ReviewsTab(reviews = reviews)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PullRequestHeader(
    pullRequest: GithubPullRequest,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    
    Column(modifier = modifier) {
        // State badge and PR info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (stateColor, stateIcon, stateText) = when {
                pullRequest.mergedAt != null -> Triple(
                    GitHubColors.MergedPurple,
                    Icons.Default.CallMerge,
                    stringResource(R.string.pr_state_merged)
                )
                pullRequest.state == "closed" -> Triple(
                    GitHubColors.ClosedRed,
                    Icons.Default.Close,
                    stringResource(R.string.pr_state_closed)
                )
                pullRequest.draft -> Triple(
                    Color.Gray,
                    Icons.Default.Edit,
                    stringResource(R.string.pr_state_draft)
                )
                else -> Triple(
                    GitHubColors.OpenGreen,
                    Icons.Default.MergeType,
                    stringResource(R.string.pr_state_open)
                )
            }
            
            Surface(
                color = stateColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = stateIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = stateColor
                    )
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.labelMedium,
                        color = stateColor
                    )
                }
            }
            
            Text(
                text = "#${pullRequest.number}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = pullRequest.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Branch info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = pullRequest.head.ref,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = pullRequest.base.ref,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Author and date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = pullRequest.user.avatarUrl,
                contentDescription = stringResource(R.string.cd_author_avatar),
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
            Text(
                text = pullRequest.user.login,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.pr_opened_on_date, dateFormat.format(parseIsoDate(pullRequest.createdAt))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConversationTab(pullRequest: GithubPullRequest) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.pr_description),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = pullRequest.body ?: stringResource(R.string.pr_no_description),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = if (pullRequest.body.isNullOrBlank()) 
                                androidx.compose.ui.text.font.FontStyle.Italic 
                            else 
                                androidx.compose.ui.text.font.FontStyle.Normal,
                            color = if (pullRequest.body.isNullOrBlank()) 
                                MaterialTheme.colorScheme.onSurfaceVariant 
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilesTab(files: List<GithubPullRequestFile>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary
        item {
            val totalAdditions = files.sumOf { it.additions }
            val totalDeletions = files.sumOf { it.deletions }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = files.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.pr_files_changed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "+$totalAdditions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GitHubColors.OpenGreen
                        )
                        Text(
                            text = stringResource(R.string.pr_additions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "-$totalDeletions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GitHubColors.ClosedRed
                        )
                        Text(
                            text = stringResource(R.string.pr_deletions),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        items(files) { file ->
            FileChangeCard(file = file)
        }
    }
}

@Composable
private fun FileChangeCard(file: GithubPullRequestFile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val (statusIcon, statusColor) = when (file.status) {
                        "added" -> Icons.Default.Add to GitHubColors.OpenGreen
                        "removed" -> Icons.Default.Remove to GitHubColors.ClosedRed
                        "modified" -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                        "renamed" -> Icons.Default.DriveFileRenameOutline to Color(0xFF9333EA)
                        else -> Icons.Default.InsertDriveFile to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = stringResource(fileStatusLabelRes(file.status)),
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.filename.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = file.filename.substringBeforeLast("/", ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "+${file.additions}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GitHubColors.OpenGreen
                    )
                    Text(
                        text = "-${file.deletions}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GitHubColors.ClosedRed
                    )
                }
            }
            
            // Diff preview (if patch is available and short)
            file.patch?.let { patch ->
                if (patch.length < 500) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = patch.lines().take(10).joinToString("\n"),
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitsTab(commits: List<GithubCommit>) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.pr_commits_count, commits.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        items(commits) { commit ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = commit.sha.take(7),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = dateFormat.format(parseIsoDate(commit.commit.author.date)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = commit.commit.message.lines().first(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        commit.author?.let { author ->
                            AsyncImage(
                                model = author.avatarUrl,
                                contentDescription = stringResource(R.string.cd_author_avatar),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = commit.commit.author.name,
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
private fun ReviewsTab(reviews: List<GithubReview>) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (reviews.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.pr_no_reviews_yet),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(reviews) { review ->
                ReviewCard(review = review, dateFormat = dateFormat)
            }
        }
    }
}

@Composable
private fun ReviewCard(
    review: GithubReview,
    dateFormat: SimpleDateFormat
) {
    val (stateColor, stateIcon, stateText) = when (review.state) {
        "APPROVED" -> Triple(GitHubColors.OpenGreen, Icons.Default.CheckCircle, stringResource(R.string.pr_review_approved))
        "CHANGES_REQUESTED" -> Triple(GitHubColors.ClosedRed, Icons.Default.Cancel, stringResource(R.string.pr_review_changes_requested))
        "COMMENTED" -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Comment, stringResource(R.string.pr_review_commented))
        "PENDING" -> Triple(Color(0xFFF59E0B), Icons.Default.Schedule, stringResource(R.string.pr_review_pending))
        "DISMISSED" -> Triple(Color.Gray, Icons.Default.RemoveCircle, stringResource(R.string.pr_review_dismissed))
        else -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.RateReview, review.state)
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        model = review.user.avatarUrl,
                        contentDescription = stringResource(R.string.cd_reviewer_avatar),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = review.user.login,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        review.submittedAt?.let {
                            Text(
                                text = dateFormat.format(parseIsoDate(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Surface(
                    color = stateColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = stateIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = stateColor
                        )
                        Text(
                            text = stateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = stateColor
                        )
                    }
                }
            }
            
            review.body?.let { body ->
                if (body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodyMedium
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
            text = stringResource(R.string.pr_error_loading_title),
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
            Text(stringResource(R.string.action_retry))
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

/**
 * Tab options for pull request detail
 */
enum class PullRequestTab(val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONVERSATION(R.string.pr_tab_conversation, Icons.Default.Chat),
    FILES(R.string.pr_tab_files, Icons.Default.InsertDriveFile),
    COMMITS(R.string.pr_tab_commits, Icons.Default.History),
    REVIEWS(R.string.pr_tab_reviews, Icons.Default.RateReview)
}

private fun fileStatusLabelRes(status: String): Int = when (status.lowercase(Locale.ROOT)) {
    "added" -> R.string.status_added
    "modified" -> R.string.status_modified
    "removed" -> R.string.status_deleted
    "renamed" -> R.string.pr_status_renamed
    "copied" -> R.string.pr_status_copied
    "changed" -> R.string.pr_status_changed
    "unchanged" -> R.string.pr_status_unchanged
    else -> R.string.status_unknown
}
