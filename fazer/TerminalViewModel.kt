package com.rafgittools.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * TerminalViewModel — feature completamente ausente
 *
 * Provides a basic interactive terminal running in the app's process.
 * Uses ProcessBuilder to execute shell commands in the context of
 * the repository working directory.
 *
 * Security: only runs inside app sandbox (no root). Commands are
 * prefixed with sh -c to support pipes, redirects, etc.
 *
 * For a richer terminal (PTY + ANSI colors) consider integrating
 * the Termux terminal-view library as a future enhancement.
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
        if (_workingDir.value == null) {
            appendLine(TerminalLine.Error("No working directory. Open a repository first."))
            return
        }
        // Safety: only allow git commands and safe read-only commands in prod mode
        val baseCmd = command.split(" ").firstOrNull()?.lowercase() ?: ""
        if (baseCmd !in GIT_SAFE_COMMANDS.map { it.split(" ").first() }) {
            appendLine(TerminalLine.Error("Command '$baseCmd' not allowed. Permitted: git, ls, cat, grep, find, pwd, echo, diff, stat, head, tail, wc"))
            return
        }

        viewModelScope.launch {
            _isRunning.value = true
            try {
                val result = withContext(Dispatchers.IO) {
                    executeProcess(command, _workingDir.value!!)
                }
                if (result.stdout.isNotBlank()) {
                    result.stdout.lines().take(500).forEach { line ->
                        appendLine(TerminalLine.Output(line))
                    }
                    if (result.stdout.lines().size > 500) {
                        appendLine(TerminalLine.Info("... output truncated at 500 lines"))
                    }
                }
                if (result.stderr.isNotBlank()) {
                    result.stderr.lines().take(100).forEach { line ->
                        appendLine(TerminalLine.Error(line))
                    }
                }
                if (result.stdout.isBlank() && result.stderr.isBlank()) {
                    appendLine(TerminalLine.Output("(no output)"))
                }
            } catch (e: Exception) {
                appendLine(TerminalLine.Error("Failed to execute: ${e.message}"))
            } finally {
                _isRunning.value = false
            }
        }
    }

    private fun executeProcess(command: String, dir: File): ProcessResult {
        val pb = ProcessBuilder("sh", "-c", command)
            .directory(dir)
            .redirectErrorStream(false)
        val process = pb.start()

        val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
        val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        stdoutReader.forEachLine { stdout.appendLine(it) }
        stderrReader.forEachLine { stderr.appendLine(it) }

        process.waitFor()
        return ProcessResult(stdout.toString().trimEnd(), stderr.toString().trimEnd())
    }

    private fun showHelp() {
        val help = """
Available commands:
  Git operations:
    git status          — show working tree status
    git log --oneline   — compact commit history
    git diff            — show unstaged changes
    git branch          — list branches
    git add <file>      — stage a file
    git commit -m "..."  — commit staged changes
    git push            — push to remote
    git pull            — pull from remote
    git clone <url>     — clone a repository

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

    private data class ProcessResult(val stdout: String, val stderr: String)
}

sealed class TerminalLine {
    data class Input(val text: String) : TerminalLine()
    data class Output(val text: String) : TerminalLine()
    data class Error(val text: String) : TerminalLine()
    data class Info(val text: String) : TerminalLine()
}
