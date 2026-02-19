package com.rafgittools.ui.screens.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.GitDiff
import com.rafgittools.domain.model.DiffHunk
import com.rafgittools.domain.model.DiffLine

private val ADD_BG    = Color(0xFF1B5E20).copy(alpha = 0.20f)
private val DEL_BG    = Color(0xFFB71C1C).copy(alpha = 0.20f)
private val CTX_BG    = Color.Transparent
private val ADD_COLOR = Color(0xFF4CAF50)
private val DEL_COLOR = Color(0xFFEF9A9A)
private val CTX_COLOR = Color(0xFFCFD8DC)
private val HEADER_BG = Color(0xFF263238)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(
    repoPath: String,
    viewModel: DiffViewerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val diffs by viewModel.diffs.collectAsState()
    val showStagedOnly by viewModel.showStagedOnly.collectAsState()
    var viewMode by remember { mutableStateOf(DiffViewMode.UNIFIED) }

    LaunchedEffect(repoPath) { viewModel.loadDiff(repoPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diff Viewer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Staged/Unstaged toggle
                    FilterChip(
                        selected = showStagedOnly,
                        onClick = { viewModel.toggleStagedFilter() },
                        label = { Text(if (showStagedOnly) "Staged" else "Unstaged") },
                        leadingIcon = {
                            Icon(
                                if (showStagedOnly) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                null, modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    // View mode toggle
                    IconButton(onClick = {
                        viewMode = if (viewMode == DiffViewMode.UNIFIED) DiffViewMode.SIDE_BY_SIDE else DiffViewMode.UNIFIED
                    }) {
                        Icon(
                            if (viewMode == DiffViewMode.UNIFIED) Icons.Default.ViewColumn else Icons.Default.ViewStream,
                            contentDescription = "Toggle view mode"
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DiffViewerUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DiffViewerUiState.Empty -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, null,
                            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("No changes", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (showStagedOnly) "No staged changes" else "Working tree is clean",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            is DiffViewerUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
            }
            is DiffViewerUiState.Success -> {
                when (viewMode) {
                    DiffViewMode.UNIFIED -> UnifiedDiffView(diffs = diffs, modifier = Modifier.padding(padding))
                    DiffViewMode.SIDE_BY_SIDE -> SideBySideDiffView(diffs = diffs, modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

// ─── Unified view ───────────────────────────────────────────────────────────
@Composable
private fun UnifiedDiffView(diffs: List<GitDiff>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        diffs.forEach { diff ->
            item {
                DiffFileHeader(diff.filePath, diff.isNew, diff.isDeleted)
            }
            diff.hunks.forEach { hunk ->
                item {
                    HunkHeader(hunk.header)
                }
                itemsIndexed(hunk.lines) { _, line ->
                    UnifiedDiffLine(line)
                }
            }
        }
    }
}

@Composable
private fun UnifiedDiffLine(line: String) {
    val (bg, color) = when {
        line.startsWith("+") -> ADD_BG to ADD_COLOR
        line.startsWith("-") -> DEL_BG to DEL_COLOR
        line.startsWith("\\") -> Color(0xFF37474F).copy(alpha = 0.3f) to Color(0xFF90A4AE)
        else -> CTX_BG to CTX_COLOR
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        Text(
            text = line,
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            softWrap = false
        )
    }
}

// ─── Side-by-side view (BUG FIX: panels were blank) ───────────────────────
@Composable
private fun SideBySideDiffView(diffs: List<GitDiff>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        diffs.forEach { diff ->
            item {
                DiffFileHeader(diff.filePath, diff.isNew, diff.isDeleted)
            }
            diff.hunks.forEach { hunk ->
                item {
                    HunkHeader(hunk.header)
                }
                // Build paired lines (old | new) from raw hunk lines
                val pairs = buildSideBySidePairs(hunk.lines)
                itemsIndexed(pairs) { _, pair ->
                    SideBySideLine(pair)
                }
            }
        }
    }
}

/**
 * Pairs deletion and addition lines side-by-side.
 * Context lines appear in both columns.
 * Unpaired additions/deletions get an empty opposing column.
 *
 * BUG ORIGINAL: os painéis ficavam em branco porque este emparelhamento
 * não existia — o código apenas comentava "Empty placeholder for additions/deletions".
 */
private fun buildSideBySidePairs(lines: List<String>): List<LinePair> {
    val result = mutableListOf<LinePair>()
    val deletions = ArrayDeque<String>()
    val additions = ArrayDeque<String>()

    fun flush() {
        val maxLen = maxOf(deletions.size, additions.size)
        for (i in 0 until maxLen) {
            result.add(LinePair(
                left = deletions.removeFirstOrNull(),
                right = additions.removeFirstOrNull()
            ))
        }
        deletions.clear()
        additions.clear()
    }

    for (line in lines) {
        when {
            line.startsWith("-") -> deletions.add(line)
            line.startsWith("+") -> additions.add(line)
            else -> {
                flush()
                result.add(LinePair(left = line, right = line, isContext = true))
            }
        }
    }
    flush()
    return result
}

@Composable
private fun SideBySideLine(pair: LinePair) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // LEFT panel (deletions / context)
        val leftBg = when {
            pair.isContext -> CTX_BG
            pair.left != null -> DEL_BG
            else -> Color(0xFF1A1A1A).copy(alpha = 0.3f) // empty slot
        }
        val leftColor = if (pair.left?.startsWith("-") == true) DEL_COLOR else CTX_COLOR

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(leftBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = pair.left ?: "",
                color = leftColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0xFF30363D)))

        // RIGHT panel (additions / context)
        val rightBg = when {
            pair.isContext -> CTX_BG
            pair.right != null -> ADD_BG
            else -> Color(0xFF1A1A1A).copy(alpha = 0.3f) // empty slot
        }
        val rightColor = if (pair.right?.startsWith("+") == true) ADD_COLOR else CTX_COLOR

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(rightBg)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            Text(
                text = pair.right ?: "",
                color = rightColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                softWrap = false
            )
        }
    }
}

// ─── Shared components ───────────────────────────────────────────────────────
@Composable
private fun DiffFileHeader(path: String, isNew: Boolean, isDeleted: Boolean) {
    val badge = when {
        isNew -> "ADDED" to MaterialTheme.colorScheme.tertiary
        isDeleted -> "DELETED" to MaterialTheme.colorScheme.error
        else -> "MODIFIED" to MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(path, fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        Badge(containerColor = badge.second) { Text(badge.first, fontSize = 9.sp) }
    }
}

@Composable
private fun HunkHeader(header: String) {
    Text(
        text = header,
        modifier = Modifier
            .fillMaxWidth()
            .background(HEADER_BG)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = Color(0xFF79C0FF),
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp
    )
}

// ─── Data structures ─────────────────────────────────────────────────────────
private data class LinePair(
    val left: String? = null,
    val right: String? = null,
    val isContext: Boolean = false
)

private enum class DiffViewMode { UNIFIED, SIDE_BY_SIDE }
