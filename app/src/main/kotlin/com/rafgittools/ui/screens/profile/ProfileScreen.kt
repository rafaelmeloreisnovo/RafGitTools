package com.rafgittools.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.saveable.rememberSaveable
import com.rafgittools.domain.model.github.GithubEvent
import com.rafgittools.domain.model.github.GithubRepository as GithubRepoModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    username: String,
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewModelUsername by viewModel.username.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val bio by viewModel.bio.collectAsStateWithLifecycle()
    val publicRepos by viewModel.publicRepos.collectAsStateWithLifecycle()
    val followers by viewModel.followers.collectAsStateWithLifecycle()
    val following by viewModel.following.collectAsStateWithLifecycle()
    val repositories by viewModel.repositories.collectAsStateWithLifecycle()
    val starred by viewModel.starred.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val tabLoading by viewModel.tabLoading.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val resolvedUsername = viewModelUsername.ifBlank { username }

    LaunchedEffect(username) {
        viewModel.loadProfile(username)
    }

    LaunchedEffect(selectedTab, resolvedUsername) {
        if (resolvedUsername.isNotBlank()) {
            viewModel.loadTab(selectedTab, resolvedUsername)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolvedUsername) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is ProfileViewModel.UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Profile header
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Column {
                                        Text(
                                            resolvedUsername,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        name?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                                bio?.let {
                                    HorizontalDivider()
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // Stats row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCard("Repos", publicRepos.toString())
                            StatCard("Followers", followers.toString())
                            StatCard("Following", following.toString())
                        }
                    }

                    // Tab row
                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Repositories") },
                                icon = { Icon(Icons.Default.Folder, null) }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Stars") },
                                icon = { Icon(Icons.Default.Star, null) }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Activity") },
                                icon = { Icon(Icons.Default.History, null) }
                            )
                        }
                    }

                    // Tab content
                    if (tabLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }
                    } else when (selectedTab) {
                        0 -> {
                            if (repositories.isEmpty()) {
                                item { EmptyTabContent("No public repositories") }
                            } else {
                                items(repositories, key = { it.id }) { repo ->
                                    RepoCard(repo = repo, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                }
                            }
                        }
                        1 -> {
                            if (starred.isEmpty()) {
                                item { EmptyTabContent("No starred repositories yet") }
                            } else {
                                items(starred, key = { it.id }) { repo ->
                                    RepoCard(repo = repo, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                }
                            }
                        }
                        2 -> {
                            if (events.isEmpty()) {
                                item { EmptyTabContent("No recent activity") }
                            } else {
                                items(events, key = { it.id }) { event ->
                                    EventItem(event = event, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
            is ProfileViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.loadProfile(username) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepoCard(repo: GithubRepoModel, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (repo.isPrivate) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Private", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
            repo.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repo.language?.let { lang ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Circle, null, modifier = Modifier.size(10.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(lang, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(repo.stargazersCount.toString(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.ForkRight, null, modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(repo.forksCount.toString(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: GithubEvent, modifier: Modifier = Modifier) {
    val (icon, description) = eventDescription(event.type)
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(description, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(
                event.repo.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            Text(
                formatEventDate(event.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
    HorizontalDivider()
}

@Composable
private fun EmptyTabContent(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun eventDescription(type: String?): Pair<androidx.compose.ui.graphics.vector.ImageVector, String> {
    return when (type) {
        "PushEvent"            -> Icons.Default.Upload to "Pushed commits"
        "PullRequestEvent"     -> Icons.Default.CallMerge to "Pull request activity"
        "IssuesEvent"          -> Icons.Default.BugReport to "Issue activity"
        "CreateEvent"          -> Icons.Default.Add to "Created branch or tag"
        "DeleteEvent"          -> Icons.Default.Delete to "Deleted branch or tag"
        "ForkEvent"            -> Icons.Default.ForkRight to "Forked repository"
        "WatchEvent"           -> Icons.Default.Star to "Starred repository"
        "IssueCommentEvent"    -> Icons.Default.Comment to "Commented on issue"
        "PullRequestReviewEvent" -> Icons.Default.RateReview to "Reviewed pull request"
        "ReleaseEvent"         -> Icons.Default.NewReleases to "Published release"
        else                   -> Icons.Default.Circle to (type?.replace("Event", "") ?: "Activity")
    }
}

private fun formatEventDate(isoDate: String): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        val date = sdf.parse(isoDate) ?: return isoDate
        val now = java.util.Date()
        val diffMs = now.time - date.time
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
        when {
            diffDays == 0 -> "Today"
            diffDays == 1 -> "Yesterday"
            diffDays < 7  -> "${diffDays}d ago"
            diffDays < 30 -> "${diffDays / 7}w ago"
            else          -> java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        isoDate.take(10)
    }
}
