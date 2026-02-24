package com.rafgittools.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Basic terminal emulator stub.
 *
 * This implementation provides a minimal command execution facility
 * using the underlying Android process API. It is intended as a
 * placeholder for a full VT100/PTY-based emulator.
 */
object TerminalEmulator {

    private val SUPPORTED_COMMANDS = setOf(
        "git", "ls", "cat", "head", "tail", "pwd", "echo", "grep", "find",
        "wc", "sort", "uniq", "diff", "stat", "file", "which", "date", "whoami"
    )

    data class CommandResult(
        val output: String,
        val exitCode: Int,
        val timedOut: Boolean,
        val error: String? = null
    )

    /**
     * Execute a supported command and return its output.
     *
     * @param command the command to run, e.g. "ls -la"
     * @param workingDir optional working directory for the process
     * @param timeoutMs execution timeout in milliseconds
     */
    suspend fun executeCommand(
        command: String,
        workingDir: File? = null,
        timeoutMs: Long = 15_000L
    ): CommandResult = withContext(Dispatchers.IO) {
        try {
            val tokens = tokenize(command)
            if (tokens.isEmpty()) {
                return@withContext CommandResult(
                    output = "",
                    exitCode = -1,
                    timedOut = false,
                    error = "No command provided"
                )
            }

            val base = tokens.first().lowercase()
            if (base !in SUPPORTED_COMMANDS) {
                return@withContext CommandResult(
                    output = "",
                    exitCode = -1,
                    timedOut = false,
                    error = "Command '$base' not allowed"
                )
            }

            val process = ProcessBuilder(tokens)
                .redirectErrorStream(true)
                .apply { if (workingDir != null) directory(workingDir) }
                .start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                if (process.isAlive) process.destroyForcibly()
                val timeoutOutput = process.inputStream.bufferedReader().use { it.readText() }.trimEnd()
                return@withContext CommandResult(
                    output = timeoutOutput,
                    exitCode = -1,
                    timedOut = true,
                    error = "Command timed out after ${timeoutMs}ms"
                )
            }

            val output = process.inputStream.bufferedReader().use { it.readText() }.trimEnd()
            CommandResult(
                output = output,
                exitCode = process.exitValue(),
                timedOut = false,
                error = null
            )
        } catch (e: Exception) {
            CommandResult(
                output = "",
                exitCode = -1,
                timedOut = false,
                error = "Error executing command: ${e.message}"
            )
        }
    }

    private fun tokenize(input: String): List<String> {
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

        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
