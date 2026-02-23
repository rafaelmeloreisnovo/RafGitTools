package com.rafgittools.terminal

/**
 * Basic terminal emulator stub.
 *
 * This implementation provides a minimal command execution facility
 * using the underlying Android process API. It is intended as a
 * placeholder for a full VT100/PTY-based emulator.
 */
object TerminalEmulator {
    /**
     * Execute a shell command and return its output.
     *
     * @param command the command to run, e.g. "ls -la"
     * @return the combined standard output and error of the command
     */
    fun executeCommand(command: String): String {
        return try {
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output
        } catch (e: Exception) {
            "Error executing command: ${'$'}{e.message}"
        }
    }
}