package com.rafgittools.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TerminalEmulatorTest {

    @Test
    fun `parses quoted arguments without invoking a shell`() {
        assertEquals(
            listOf("grep", "hello world", "file.txt"),
            TerminalEmulator.parseCommand("grep 'hello world' file.txt")
        )
    }

    @Test
    fun `rejects unclosed quote`() {
        assertFailsWith<IllegalArgumentException> {
            TerminalEmulator.parseCommand("grep 'unfinished")
        }
    }

    @Test
    fun `rejects writable git subcommand`() {
        assertFailsWith<IllegalArgumentException> {
            TerminalEmulator.parseCommand("git push origin main")
        }
    }

    @Test
    fun `rejects find exec action`() {
        assertFailsWith<IllegalArgumentException> {
            TerminalEmulator.parseCommand("find . -exec cat {} ;")
        }
    }

    @Test
    fun `allows bounded read only git status`() {
        assertEquals(
            listOf("git", "status", "--short"),
            TerminalEmulator.parseCommand("git status --short")
        )
    }
}
