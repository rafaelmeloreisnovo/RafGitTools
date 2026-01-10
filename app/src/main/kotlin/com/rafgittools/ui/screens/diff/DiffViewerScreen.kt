package com.rafgittools.ui.screens.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.*
import com.rafgittools.ui.theme.GitHubColors

/**
 * Diff viewer screen for showing repository changes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(
    repoPath: String,
    viewModel: DiffViewerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val diffs by viewModel.diffs.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val showStagedOnly by viewModel.showStagedOnly.collectAsState()
    
    LaunchedEffect(repoPath) {
        viewModel.loadDiff(repoPath)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changes") },
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
                    // Toggle staged/unstaged
                    FilterChip(
                        selected = showStagedOnly,
                        onClick = { viewModel.toggleStagedFilter() },
                        label = { Text("Staged") },
                        leadingIcon = {
                            if (showStagedOnly) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                    
                    // View mode toggle
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (viewMode == DiffViewMode.UNIFIED) 
                                Icons.Default.ViewStream 
                            else 
                                Icons.Default.ViewColumn,
                            contentDescription = "Toggle view mode"
                        )
                    }
                    
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
                is DiffViewerUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DiffViewerUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DiffViewerUiState.Empty -> {
                    EmptyContent(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DiffViewerUiState.Success -> {
                    DiffList(
                        diffs = diffs,
                        viewMode = viewMode
                    )
                }
            }
        }
    }
}

@Composable
private fun DiffList(
    diffs: List<GitDiff>,
    viewMode: DiffViewMode
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary
        item {
            DiffSummaryCard(diffs = diffs)
        }
        
        // Individual diffs
        items(diffs) { diff ->
            DiffCard(diff = diff, viewMode = viewMode)
        }
    }
}

@Composable
private fun DiffSummaryCard(diffs: List<GitDiff>) {
    val additions = diffs.sumOf { diff ->
        diff.hunks.sumOf { hunk ->
            hunk.lines.count { it.type == DiffLineType.ADD }
        }
    }
    val deletions = diffs.sumOf { diff ->
        diff.hunks.sumOf { hunk ->
            hunk.lines.count { it.type == DiffLineType.DELETE }
        }
    }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = diffs.size.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Files changed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+$additions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GitHubColors.OpenGreen
                )
                Text(
                    text = "Additions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "-$deletions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = GitHubColors.ClosedRed
                )
                Text(
                    text = "Deletions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiffCard(
    diff: GitDiff,
    viewMode: DiffViewMode
) {
    var expanded by remember { mutableStateOf(true) }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // File header
            Surface(
                onClick = { expanded = !expanded },
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val (icon, iconColor) = when (diff.changeType) {
                            DiffChangeType.ADD -> Icons.Default.Add to GitHubColors.OpenGreen
                            DiffChangeType.DELETE -> Icons.Default.Remove to GitHubColors.ClosedRed
                            DiffChangeType.MODIFY -> Icons.Default.Edit to MaterialTheme.colorScheme.tertiary
                            DiffChangeType.RENAME -> Icons.Default.DriveFileRenameOutline to Color(0xFF9333EA)
                            DiffChangeType.COPY -> Icons.Default.ContentCopy to MaterialTheme.colorScheme.secondary
                        }
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = diff.changeType.name,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = (diff.newPath ?: diff.oldPath)?.substringAfterLast("/") ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = (diff.newPath ?: diff.oldPath)?.substringBeforeLast("/", "") ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    // Line counts
                    val additions = diff.hunks.sumOf { it.lines.count { l -> l.type == DiffLineType.ADD } }
                    val deletions = diff.hunks.sumOf { it.lines.count { l -> l.type == DiffLineType.DELETE } }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "+$additions",
                            style = MaterialTheme.typography.labelMedium,
                            color = GitHubColors.OpenGreen
                        )
                        Text(
                            text = "-$deletions",
                            style = MaterialTheme.typography.labelMedium,
                            color = GitHubColors.ClosedRed
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Diff content
            if (expanded) {
                when (viewMode) {
                    DiffViewMode.UNIFIED -> UnifiedDiffView(diff = diff)
                    DiffViewMode.SPLIT -> SplitDiffView(diff = diff)
                }
            }
        }
    }
}

@Composable
private fun UnifiedDiffView(diff: GitDiff) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        diff.hunks.forEach { hunk ->
            // Hunk header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "@@ -${hunk.oldStart},${hunk.oldLines} +${hunk.newStart},${hunk.newLines} @@",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Lines
            hunk.lines.forEach { line ->
                DiffLine(line = line)
            }
        }
    }
}

@Composable
private fun DiffLine(line: DiffLine) {
    val (backgroundColor, textColor, prefix) = when (line.type) {
        DiffLineType.ADD -> Triple(
            Color(0xFF0D5D21).copy(alpha = 0.2f),
            GitHubColors.OpenGreen,
            "+"
        )
        DiffLineType.DELETE -> Triple(
            Color(0xFF67060C).copy(alpha = 0.2f),
            GitHubColors.ClosedRed,
            "-"
        )
        DiffLineType.CONTEXT -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            " "
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Line numbers
        Text(
            text = (line.oldLineNumber?.toString() ?: "").padStart(4),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = (line.newLineNumber?.toString() ?: "").padStart(4),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        
        // Prefix
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp)
        )
        
        // Content
        Text(
            text = line.content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
private fun SplitDiffView(diff: GitDiff) {
    // For split view, show old and new side by side
    val scrollState = rememberScrollState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        // Old side
        Column(modifier = Modifier.weight(1f)) {
            diff.hunks.forEach { hunk ->
                hunk.lines.forEach { line ->
                    if (line.type != DiffLineType.ADD) {
                        SplitDiffLine(
                            lineNumber = line.oldLineNumber,
                            content = line.content,
                            isDelete = line.type == DiffLineType.DELETE
                        )
                    } else {
                        // Empty placeholder for additions
                        SplitDiffLine(
                            lineNumber = null,
                            content = "",
                            isDelete = false,
                            isEmpty = true
                        )
                    }
                }
            }
        }
        
        // Divider
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        
        // New side
        Column(modifier = Modifier.weight(1f)) {
            diff.hunks.forEach { hunk ->
                hunk.lines.forEach { line ->
                    if (line.type != DiffLineType.DELETE) {
                        SplitDiffLine(
                            lineNumber = line.newLineNumber,
                            content = line.content,
                            isAdd = line.type == DiffLineType.ADD
                        )
                    } else {
                        // Empty placeholder for deletions
                        SplitDiffLine(
                            lineNumber = null,
                            content = "",
                            isAdd = false,
                            isEmpty = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitDiffLine(
    lineNumber: Int?,
    content: String,
    isDelete: Boolean = false,
    isAdd: Boolean = false,
    isEmpty: Boolean = false
) {
    val backgroundColor = when {
        isEmpty -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isDelete -> Color(0xFF67060C).copy(alpha = 0.2f)
        isAdd -> Color(0xFF0D5D21).copy(alpha = 0.2f)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isDelete -> GitHubColors.ClosedRed
        isAdd -> GitHubColors.OpenGreen
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = lineNumber?.toString()?.padStart(4) ?: "    ",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = GitHubColors.OpenGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Changes",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your working directory is clean",
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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
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

/**
 * Diff view mode
 */
enum class DiffViewMode {
    UNIFIED,
    SPLIT
}
