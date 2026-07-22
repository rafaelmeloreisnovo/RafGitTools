package com.rafgittools.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnsiOutputProcessorTest {

    private val ESC = ''

    @Test
    fun `strip removes simple color code`() {
        val input = "${ESC}[31mhello${ESC}[0m world"
        assertEquals("hello world", AnsiOutputProcessor.strip(input))
    }

    @Test
    fun `strip leaves plain text unchanged`() {
        val plain = "git status --short"
        assertEquals(plain, AnsiOutputProcessor.strip(plain))
    }

    @Test
    fun `strip handles bold sequence`() {
        val input = "${ESC}[1mbold${ESC}[22m text"
        assertEquals("bold text", AnsiOutputProcessor.strip(input))
    }

    @Test
    fun `strip handles compound SGR codes`() {
        val input = "${ESC}[1;32mgreen bold${ESC}[0m"
        assertEquals("green bold", AnsiOutputProcessor.strip(input))
    }

    @Test
    fun `strip removes cursor movement sequences`() {
        val input = "line1${ESC}[2A${ESC}[Kline2"
        assertEquals("line1line2", AnsiOutputProcessor.strip(input))
    }

    @Test
    fun `parse returns single default span for plain text`() {
        val spans = AnsiOutputProcessor.parse("plain text")
        assertEquals(1, spans.size)
        assertEquals("plain text", spans[0].text)
        assertTrue(spans[0].style.isDefault)
    }

    @Test
    fun `parse splits text at color boundary`() {
        val input = "normal ${ESC}[31mred text${ESC}[0m back"
        val spans = AnsiOutputProcessor.parse(input)
        assertEquals("normal ", spans[0].text)
        assertTrue(spans[0].style.isDefault)
        assertEquals("red text", spans[1].text)
        assertEquals(AnsiOutputProcessor.AnsiColor.RED, spans[1].style.fg)
        assertEquals(" back", spans[2].text)
        assertTrue(spans[2].style.isDefault)
    }

    @Test
    fun `parse handles bold flag`() {
        val input = "${ESC}[1mbold${ESC}[0m"
        val spans = AnsiOutputProcessor.parse(input)
        assertEquals("bold", spans[0].text)
        assertTrue(spans[0].style.bold)
    }

    @Test
    fun `parse recognizes all standard foreground colors`() {
        val colorCodes = mapOf(
            30 to AnsiOutputProcessor.AnsiColor.BLACK,
            31 to AnsiOutputProcessor.AnsiColor.RED,
            32 to AnsiOutputProcessor.AnsiColor.GREEN,
            33 to AnsiOutputProcessor.AnsiColor.YELLOW,
            34 to AnsiOutputProcessor.AnsiColor.BLUE,
            35 to AnsiOutputProcessor.AnsiColor.MAGENTA,
            36 to AnsiOutputProcessor.AnsiColor.CYAN,
            37 to AnsiOutputProcessor.AnsiColor.WHITE,
        )
        for ((code, expected) in colorCodes) {
            val spans = AnsiOutputProcessor.parse("${ESC}[${code}mx${ESC}[0m")
            assertEquals(expected, spans[0].style.fg, "Expected color $expected for code $code")
        }
    }

    @Test
    fun `parse recognizes bright foreground colors`() {
        val spans = AnsiOutputProcessor.parse("${ESC}[91mbright red${ESC}[0m")
        assertEquals(AnsiOutputProcessor.AnsiColor.BRIGHT_RED, spans[0].style.fg)
    }

    @Test
    fun `parse resets style on ESC 0m`() {
        val input = "${ESC}[31mred${ESC}[0mnormal"
        val spans = AnsiOutputProcessor.parse(input)
        val lastSpan = spans.last()
        assertEquals("normal", lastSpan.text)
        assertTrue(lastSpan.style.isDefault)
    }

    @Test
    fun `parse handles git diff color output`() {
        // Typical git diff output: red for deletions, green for additions
        val deleted = "${ESC}[31m-old line${ESC}[0m"
        val added   = "${ESC}[32m+new line${ESC}[0m"

        val deletedSpans = AnsiOutputProcessor.parse(deleted)
        assertEquals(AnsiOutputProcessor.AnsiColor.RED, deletedSpans[0].style.fg)

        val addedSpans = AnsiOutputProcessor.parse(added)
        assertEquals(AnsiOutputProcessor.AnsiColor.GREEN, addedSpans[0].style.fg)
    }

    @Test
    fun `parse handles git log --oneline color`() {
        // git log --oneline --color: yellow commit hash, reset, then commit message
        val logLine = "${ESC}[33mabc1234${ESC}[0m Initial commit"
        val spans = AnsiOutputProcessor.parse(logLine)
        assertEquals(AnsiOutputProcessor.AnsiColor.YELLOW, spans[0].style.fg)
        assertEquals("abc1234", spans[0].text)
        assertEquals(" Initial commit", spans[1].text)
        assertTrue(spans[1].style.isDefault)
    }
}
