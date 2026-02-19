package com.rafgittools.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val TERMINAL_BG = Color(0xFF0D1117)
private val TERMINAL_OUTPUT = Color(0xFFE6EDF3)
private val TERMINAL_ERROR = Color(0xFFFF7B72)
private val TERMINAL_INFO = Color(0xFF79C0FF)
private val TERMINAL_INPUT = Color(0xFF3FB950)
private val TERMINAL_PROMPT = Color(0xFFF78166)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    repoPath: String = "",
    viewModel: TerminalViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val lines by viewModel.lines.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentInput by viewModel.currentInput.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(repoPath) {
        if (repoPath.isNotBlank()) viewModel.setWorkingDirectory(repoPath)
    }

    // Auto-scroll to bottom when new lines appear
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal", style = MaterialTheme.typography.titleMedium)
                        if (repoPath.isNotBlank()) {
                            Text(
                                repoPath.split("/").lastOrNull() ?: repoPath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.executeCommand("clear") }) {
                        Icon(Icons.Default.ClearAll, "Clear terminal")
                    }
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = TERMINAL_INPUT
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161B22),
                    titleContentColor = TERMINAL_OUTPUT
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TERMINAL_BG)
                .padding(padding)
        ) {
            // Output area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(lines) { line ->
                    TerminalLineItem(line)
                }
            }

            HorizontalDivider(color = Color(0xFF30363D))

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // History navigation
                IconButton(
                    onClick = { viewModel.navigateHistory(up = true) },
                    modifier = Modifier.size(32.dp),
                    enabled = !isRunning
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Previous command",
                        tint = TERMINAL_INFO, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { viewModel.navigateHistory(up = false) },
                    modifier = Modifier.size(32.dp),
                    enabled = !isRunning
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Next command",
                        tint = TERMINAL_INFO, modifier = Modifier.size(18.dp))
                }

                Text(
                    "$ ",
                    color = TERMINAL_PROMPT,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { viewModel.setInput(it) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                    enabled = !isRunning,
                    placeholder = {
                        Text("type a command…", color = Color(0xFF8B949E),
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TERMINAL_OUTPUT,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF30363D),
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = TERMINAL_INPUT
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!isRunning) viewModel.executeCommand()
                    })
                )

                IconButton(
                    onClick = { if (!isRunning) viewModel.executeCommand() },
                    enabled = !isRunning && currentInput.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        "Run command",
                        tint = if (!isRunning && currentInput.isNotBlank()) TERMINAL_INPUT
                               else Color(0xFF8B949E)
                    )
                }
            }

            // Quick command chips
            QuickCommandsRow(isRunning = isRunning, onCommand = { viewModel.executeCommand(it) })
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    val (text, color, prefix) = when (line) {
        is TerminalLine.Input -> Triple(line.text, TERMINAL_INPUT, "")
        is TerminalLine.Output -> Triple(line.text, TERMINAL_OUTPUT, "")
        is TerminalLine.Error -> Triple(line.text, TERMINAL_ERROR, "✖ ")
        is TerminalLine.Info -> Triple(line.text, TERMINAL_INFO, "ℹ ")
    }
    Text(
        text = "$prefix$text",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    )
}

@Composable
private fun QuickCommandsRow(isRunning: Boolean, onCommand: (String) -> Unit) {
    val quickCommands = listOf(
        "git status" to "status",
        "git log --oneline -10" to "log",
        "git diff" to "diff",
        "git branch" to "branch",
        "ls -la" to "ls"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        quickCommands.forEach { (cmd, label) ->
            SuggestionChip(
                onClick = { if (!isRunning) onCommand(cmd) },
                label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.height(28.dp),
                enabled = !isRunning,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = Color(0xFF21262D),
                    labelColor = TERMINAL_INFO
                )
            )
        }
    }
}
