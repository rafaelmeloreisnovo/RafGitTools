package com.rafgittools.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Bounded command executor for local diagnostic and read-only Git operations.
 *
 * This is deliberately not presented as a PTY/VT100 terminal. It executes a
 * small, validated command vocabulary without invoking a shell. Interactive
 * programs, pipelines, redirects, command substitution and terminal escape
 * processing are out of scope.
 */
object TerminalEmulator {

    private val SUPPORTED_COMMANDS = setOf(
        "git", "ls", "cat", "head", "tail", "pwd", "echo", "grep", "find",
        "wc", "sort", "uniq", "diff", "stat", "file", "which", "date", "whoami"
    )

    private val READ_ONLY_GIT_SUBCOMMANDS = setOf(
        "status", "log", "diff", "show", "rev-parse", "ls-files", "grep", "blame"
    )

    private val FORBIDDEN_FIND_ARGUMENTS = setOf(
        "-exec", "-execdir", "-ok", "-okdir", "-delete", "-fls", "-fprint"
    )

    data class CommandResult(
        val output: String,
        val exitCode: Int,
        val timedOut: Boolean,
        val error: String? = null
    )

    /**
     * Execute a validated command and return its merged stdout/stderr.
     *
     * The output stream is drained concurrently with process execution. This
     * prevents the child process from blocking when the OS pipe buffer fills.
     */
    suspend fun executeCommand(
        command: String,
        workingDir: File? = null,
        timeoutMs: Long = 15_000L
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            require(timeoutMs in 1L..300_000L) { "timeoutMs must be between 1 and 300000" }
            if (workingDir != null) {
                require(workingDir.exists()) { "Working directory does not exist" }
                require(workingDir.isDirectory) { "Working directory is not a directory" }
            }

            val tokens = parseCommand(command)
            val process = ProcessBuilder(tokens)
                .redirectErrorStream(true)
                .apply { if (workingDir != null) directory(workingDir.canonicalFile) }
                .start()

            val output = StringBuffer()
            val readerThread = Thread({
                runCatching {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            if (output.isNotEmpty()) output.append('\n')
                            output.append(line)
                        }
                    }
                }
            }, "rafgittools-command-output")
            readerThread.isDaemon = true
            readerThread.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly()
                    process.waitFor(1, TimeUnit.SECONDS)
                }
                runCatching { process.inputStream.close() }
                readerThread.join(1_000L)
                return@withContext CommandResult(
                    output = output.toString(),
                    exitCode = -1,
                    timedOut = true,
                    error = "Command timed out after ${timeoutMs}ms"
                )
            }

            readerThread.join(1_000L)
            CommandResult(
                output = output.toString(),
                exitCode = process.exitValue(),
                timedOut = false,
                error = null
            )
        } catch (e: IllegalArgumentException) {
            CommandResult("", -1, false, e.message ?: "Invalid command")
        } catch (e: Exception) {
            CommandResult("", -1, false, "Error executing command: ${e.message}")
        }
    }

    /** Visible to unit tests; it never invokes a shell. */
    internal fun parseCommand(input: String): List<String> {
        require(input.indexOf('\u0000') < 0) { "NUL byte is not allowed" }
        require(!input.contains('\n') && !input.contains('\r')) { "Multiple commands are not allowed" }

        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false
        var escaped = false

        for (char in input.trim()) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' && !inSingleQuote -> escaped = true
                char == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                char == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                char.isWhitespace() && !inSingleQuote && !inDoubleQuote -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }

        require(!escaped) { "Command ends with an incomplete escape" }
        require(!inSingleQuote && !inDoubleQuote) { "Command contains an unclosed quote" }
        if (current.isNotEmpty()) tokens.add(current.toString())
        require(tokens.isNotEmpty()) { "No command provided" }

        val base = tokens.first().lowercase()
        require(base in SUPPORTED_COMMANDS) { "Command '$base' not allowed" }

        if (base == "git") {
            require(tokens.size >= 2) { "A read-only git subcommand is required" }
            require(tokens.drop(1).none { it == "-c" || it.startsWith("--config-env") || it.startsWith("--exec-path") }) {
                "Git runtime configuration overrides are not allowed"
            }
            val subcommand = tokens[1].lowercase()
            require(subcommand in READ_ONLY_GIT_SUBCOMMANDS) {
                "Git subcommand '$subcommand' is not allowed in the bounded executor"
            }
        }

        if (base == "find") {
            require(tokens.drop(1).none { it.lowercase() in FORBIDDEN_FIND_ARGUMENTS }) {
                "find actions that execute or modify files are not allowed"
            }
        }

        return tokens
    }
}
