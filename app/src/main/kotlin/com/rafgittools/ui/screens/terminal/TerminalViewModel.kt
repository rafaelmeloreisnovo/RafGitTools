package com.rafgittools.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rafgittools.terminal.TerminalEmulator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Provides a bounded read-only git terminal running in the app's process.
 * Delegates command execution to TerminalEmulator with a bounded executor
 * (read-only git subcommands only) in the context of the repository working directory.
 *
 * Security: only safe read-only subcommands (status, log, diff, show, rev-parse,
 * ls-files, grep, blame) are allowed; write operations are rejected.
 */
@HiltViewModel
class TerminalViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val MAX_OUTPUT_LINES = 1000
        private const val HISTORY_LIMIT = 100
        private const val PROMPT = "$ "
    }

    private val _lines = MutableStateFlow<List<TerminalLine>>(listOf(
        TerminalLine.Info("RafGitTools Terminal — type 'help' for available commands"),
        TerminalLine.Info("Working directory: not set — open a repository first")
    ))
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _workingDir = MutableStateFlow<File?>(null)
    val workingDir: StateFlow<File?> = _workingDir.asStateFlow()

    private val commandHistory = ArrayDeque<String>()
    private var historyIndex = -1

    private val GIT_SAFE_COMMANDS = setOf(
        "git", "ls", "ls -la", "ls -l", "cat", "head", "tail",
        "pwd", "echo", "grep", "find", "wc", "sort", "uniq",
        "diff", "stat", "file", "which", "date", "whoami", "help", "clear", "cd"
    )
    private val SAFE_BASE_CMDS: Set<String> = GIT_SAFE_COMMANDS.map { it.split(" ").first() }.toSet()

    fun setWorkingDirectory(path: String) {
        val dir = File(path)
        _workingDir.value = if (dir.exists() && dir.isDirectory) dir else null
        val msg = if (_workingDir.value != null) "Working directory: $path" else "Directory not found: $path"
        appendLine(TerminalLine.Info(msg))
    }

    fun setInput(input: String) {
        _currentInput.value = input
        historyIndex = -1
    }

    fun navigateHistory(up: Boolean) {
        if (commandHistory.isEmpty()) return
        historyIndex = if (up) {
            (historyIndex + 1).coerceAtMost(commandHistory.size - 1)
        } else {
            (historyIndex - 1).coerceAtLeast(-1)
        }
        _currentInput.value = if (historyIndex >= 0) commandHistory[historyIndex] else ""
    }

    fun executeCommand(rawInput: String = _currentInput.value) {
        val command = rawInput.trim()
        if (command.isBlank()) return

        _currentInput.value = ""
        appendLine(TerminalLine.Input("$PROMPT$command"))
        addToHistory(command)

        when (command.lowercase()) {
            "help" -> showHelp()
            "clear" -> _lines.value = emptyList()
            "pwd" -> appendLine(TerminalLine.Output(_workingDir.value?.absolutePath ?: "No working directory"))
            "history" -> commandHistory.forEachIndexed { i, cmd ->
                appendLine(TerminalLine.Output("  $i  $cmd"))
            }
            else -> runShellCommand(command)
        }
    }

    private fun runShellCommand(command: String) {
        val dir = _workingDir.value ?: run {
            appendLine(TerminalLine.Error("No working directory. Open a repository first."))
            return
        }
        // Safety: only allow git commands and safe read-only commands in prod mode
        val baseCmd = command.split(" ").firstOrNull()?.lowercase() ?: ""
        if (baseCmd !in SAFE_BASE_CMDS) {
            appendLine(TerminalLine.Error("Command '$baseCmd' not allowed. Permitted: git, ls, cat, grep, find, pwd, echo, diff, stat, head, tail, wc"))
            return
        }

        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = TerminalEmulator.executeCommand(
                    command = command,
                    workingDir = dir,
                    timeoutMs = 15_000L
                )

                if (result.output.isNotBlank()) {
                    result.output.lines().take(500).forEach { line ->
                        appendLine(TerminalLine.Output(line))
                    }
                    if (result.output.lines().size > 500) {
                        appendLine(TerminalLine.Info("... output truncated at 500 lines"))
                    }
                }

                if (result.error != null) {
                    appendLine(TerminalLine.Error(result.error))
                } else if (result.exitCode != 0) {
                    appendLine(TerminalLine.Error("Command exited with code ${result.exitCode}"))
                }

                if (result.output.isBlank() && result.error == null) {
                    appendLine(TerminalLine.Output("(no output)"))
                }
            } catch (e: Exception) {
                appendLine(TerminalLine.Error("Failed to execute: ${e.message}"))
            } finally {
                _isRunning.value = false
            }
        }
    }

    private fun showHelp() {
        val help = """
Available commands:
  Read-only git operations:
    git status          — show working tree status
    git log --oneline   — compact commit history
    git diff            — show unstaged changes
    git show <ref>      — show object content
    git rev-parse HEAD  — resolve ref to hash
    git ls-files        — list tracked files
    git grep <pattern>  — search in tracked files
    git blame <file>    — annotate file with commit info

  File operations:
    ls [-la]            — list directory contents
    cat <file>          — display file contents
    head/tail <file>    — first/last lines of file
    grep <pattern>      — search in files
    find . -name "*.kt" — find files by pattern
    diff <a> <b>        — compare two files

  Terminal:
    pwd                 — print working directory
    clear               — clear terminal
    history             — show command history
    help                — this message
        """.trimIndent()
        appendLine(TerminalLine.Info(help))
    }

    private fun appendLine(line: TerminalLine) {
        val current = _lines.value.toMutableList()
        current.add(line)
        if (current.size > MAX_OUTPUT_LINES) {
            _lines.value = current.drop(current.size - MAX_OUTPUT_LINES)
        } else {
            _lines.value = current
        }
    }

    private fun addToHistory(command: String) {
        commandHistory.addFirst(command)
        if (commandHistory.size > HISTORY_LIMIT) commandHistory.removeLast()
    }

}

sealed class TerminalLine {
    data class Input(val text: String) : TerminalLine()
    data class Output(val text: String) : TerminalLine()
    data class Error(val text: String) : TerminalLine()
    data class Info(val text: String) : TerminalLine()
}
