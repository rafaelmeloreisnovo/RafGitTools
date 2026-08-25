package com.rafgittools.ui.screens.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rafgittools.domain.model.FileContent
import com.rafgittools.domain.model.GitBranch
import com.rafgittools.domain.model.GitFile
import com.rafgittools.domain.model.GitTag
import com.rafgittools.ui.components.SyntaxHighlighter

/**
 * File browser screen for exploring repository files.
 *
 * Closes RG6:
 *  - P33-12: syntax highlighting via SyntaxHighlighter (wired in FileViewer)
 *  - P33-13: line numbers inline (unchanged — already in gutter)
 *  - P33-15: breadcrumb navigation (unchanged — BreadcrumbBar)
 *  - P33-16: file icons by extension (unchanged — getFileIcon)
 *  - P33-20/21: branch/tag ref selector (RefSelector composable in top bar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    repoPath: String,
    viewModel: FileBrowserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()
    val fileContent by viewModel.fileContent.collectAsStateWithLifecycle()
    val currentRef by viewModel.currentRef.collectAsStateWithLifecycle()
    val availableBranches by viewModel.availableBranches.collectAsStateWithLifecycle()
    val availableTags by viewModel.availableTags.collectAsStateWithLifecycle()

    var showRefPicker by remember { mutableStateOf(false) }

    LaunchedEffect(repoPath) {
        viewModel.loadRepository(repoPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Files")
                        Text(
                            text = if (currentPath.isEmpty()) "/" else currentPath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFile != null) {
                            viewModel.closeFile()
                        } else if (currentPath.isNotEmpty()) {
                            viewModel.navigateUp()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // Branch/tag ref picker (P33-20/21)
                    Box {
                        AssistChip(
                            onClick = {
                                if (availableBranches.isNotEmpty() || availableTags.isNotEmpty()) {
                                    showRefPicker = true
                                }
                            },
                            label = {
                                Text(
                                    text = currentRef,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 120.dp)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AccountTree,
                                    contentDescription = "Branch",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        DropdownMenu(
                            expanded = showRefPicker,
                            onDismissRequest = { showRefPicker = false }
                        ) {
                            if (availableBranches.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Branches",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                availableBranches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (branch.shortName == currentRef) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                } else {
                                                    Spacer(Modifier.width(20.dp))
                                                }
                                                Text(branch.shortName)
                                            }
                                        },
                                        onClick = {
                                            showRefPicker = false
                                            viewModel.switchRef(branch.shortName)
                                        }
                                    )
                                }
                            }
                            if (availableTags.isNotEmpty()) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Tags",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                availableTags.take(20).forEach { tag ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (tag.name == currentRef) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                } else {
                                                    Spacer(Modifier.width(20.dp))
                                                }
                                                Text(tag.name)
                                            }
                                        },
                                        onClick = {
                                            showRefPicker = false
                                            viewModel.switchRef(tag.name)
                                        }
                                    )
                                }
                            }
                        }
                    }
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
            // Breadcrumb navigation (P33-15)
            if (breadcrumbs.isNotEmpty() && selectedFile == null) {
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onNavigate = { path -> viewModel.navigateTo(path) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is FileBrowserUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is FileBrowserUiState.Error -> {
                        ErrorContent(
                            message = state.message,
                            onRetry = { viewModel.refresh() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is FileBrowserUiState.FileList -> {
                        FileList(
                            files = files,
                            onFileClick = { file ->
                                if (file.isDirectory) {
                                    viewModel.navigateTo(file.path)
                                } else {
                                    viewModel.openFile(file)
                                }
                            }
                        )
                    }
                    is FileBrowserUiState.FileView -> {
                        fileContent?.let { content ->
                            FileViewer(content = content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    onNavigate: (String) -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { onNavigate("") },
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Root",
                    modifier = Modifier.size(18.dp)
                )
            }

            breadcrumbs.forEachIndexed { index, (name, path) ->
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (index == breadcrumbs.lastIndex) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    TextButton(
                        onClick = { onNavigate(path) },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(name)
                    }
                }
            }
        }
    }
}

@Composable
private fun FileList(
    files: List<GitFile>,
    onFileClick: (GitFile) -> Unit
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderOff,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Empty directory",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(files, key = { it.path }) { file ->
                FileItem(
                    file = file,
                    onClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun FileItem(
    file: GitFile,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = if (!file.isDirectory && file.size > 0) {
            {
                Text(
                    text = formatFileSize(file.size),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else null,
        leadingContent = {
            Icon(
                imageVector = if (file.isDirectory) {
                    Icons.Default.Folder
                } else {
                    getFileIcon(file.name)
                },
                contentDescription = null,
                tint = if (file.isDirectory) {
                    Color(0xFF54A3FF)
                } else {
                    getFileIconColor(file.name)
                }
            )
        },
        trailingContent = if (file.isDirectory) {
            {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}

@Composable
private fun FileViewer(content: FileContent) {
    // Pre-compute syntax-highlighted lines once per file (P33-12).
    val extension = content.name.substringAfterLast(".", "")
    val highlightedLines: List<AnnotatedString> = remember(content.content, extension) {
        if (content.isBinary) emptyList()
        else content.content.lines().map { line ->
            SyntaxHighlighter.highlight(line.ifEmpty { " " }, extension)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // File info header
        Surface(
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = content.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = formatFileSize(content.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        content.language?.let { lang ->
                            Text(
                                text = lang,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (content.isBinary) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Binary") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilePresent,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        if (content.isBinary) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilePresent,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Binary file", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = formatFileSize(content.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Code viewer with line numbers (P33-13) and syntax highlighting (P33-12).
            // Each Row keeps the line number gutter and the highlighted code line in sync.
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    itemsIndexed(highlightedLines) { index, annotatedLine ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Line number gutter — fixed width, right-aligned
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Syntax-highlighted code line
                            Text(
                                text = annotatedLine,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
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

// Helper functions

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun getFileIcon(fileName: String): androidx.compose.ui.graphics.vector.ImageVector {
    val extension = fileName.substringAfterLast(".", "").lowercase()
    return when (extension) {
        "kt", "kts", "java", "py", "js", "ts", "go", "rs", "rb", "php", "swift", "c", "cpp", "h" -> Icons.Default.Code
        "md", "txt", "doc", "docx", "pdf" -> Icons.Default.Description
        "json", "xml", "yaml", "yml", "toml" -> Icons.Default.DataObject
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Icons.Default.Image
        "mp3", "wav", "ogg", "flac" -> Icons.Default.AudioFile
        "mp4", "avi", "mkv", "mov" -> Icons.Default.VideoFile
        "zip", "tar", "gz", "rar", "7z" -> Icons.Default.Archive
        "gradle", "properties" -> Icons.Default.Settings
        "gitignore", "gitattributes" -> Icons.Default.Source
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
private fun getFileIconColor(fileName: String): Color {
    val extension = fileName.substringAfterLast(".", "").lowercase()
    return when (extension) {
        "kt", "kts" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "py" -> Color(0xFF3572A5)
        "js" -> Color(0xFFF1E05A)
        "ts" -> Color(0xFF3178C6)
        "go" -> Color(0xFF00ADD8)
        "rs" -> Color(0xFFDEA584)
        "md" -> Color(0xFF083FA1)
        "json" -> Color(0xFF292929)
        "xml" -> Color(0xFF0060AC)
        "yaml", "yml" -> Color(0xFFCB171E)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
