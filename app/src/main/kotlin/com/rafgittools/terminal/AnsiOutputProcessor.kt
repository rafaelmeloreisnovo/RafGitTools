package com.rafgittools.terminal

/**
 * Minimal VT100/ANSI escape sequence processor for bounded-executor output.
 *
 * Handles the SGR (Select Graphic Rendition) subset that git, grep, and
 * diff emit. Full cursor-movement sequences are not supported — this is a
 * line-based executor, not a character-cell terminal.
 *
 * Closes RG3: bounded executor output now parses ANSI color codes so that
 * `git diff --color` and `git log --color` render correctly instead of
 * leaking raw ESC bytes into the text view.
 */
object AnsiOutputProcessor {

    private const val ESC = ''

    /** Pattern to match an SGR sequence: ESC [ <params> m */
    private val SGR_PATTERN = Regex("\\[(\\d*(?:;\\d*)*)m")

    /** Strip any ESC sequence from [line], returning plain text. */
    private val ANSI_STRIP_PATTERN = Regex("\\[[^A-Za-z]*[A-Za-z]|.")

    /** A styled text segment produced by parsing one output line. */
    data class Span(val text: String, val style: AnsiStyle)

    /** Accumulated SGR state for one text run. */
    data class AnsiStyle(
        val bold: Boolean = false,
        val fg: AnsiColor? = null
    ) {
        val isDefault: Boolean get() = !bold && fg == null
    }

    enum class AnsiColor {
        BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE,
        BRIGHT_BLACK, BRIGHT_RED, BRIGHT_GREEN, BRIGHT_YELLOW,
        BRIGHT_BLUE, BRIGHT_MAGENTA, BRIGHT_CYAN, BRIGHT_WHITE
    }

    /**
     * Parse [line] into a list of styled spans, consuming all SGR escape sequences.
     * Non-SGR escape sequences are silently stripped.
     */
    fun parse(line: String): List<Span> {
        val spans = mutableListOf<Span>()
        var style = AnsiStyle()
        var pos = 0

        while (pos < line.length) {
            val esc = line.indexOf(ESC, pos)
            if (esc < 0) {
                val tail = line.substring(pos)
                if (tail.isNotEmpty()) spans.add(Span(tail, style))
                break
            }

            // Emit any literal text before the escape
            if (esc > pos) {
                val literal = line.substring(pos, esc)
                if (literal.isNotEmpty()) spans.add(Span(literal, style))
            }

            val match = SGR_PATTERN.find(line, esc)
            if (match != null && match.range.first == esc) {
                style = applySgr(style, match.groupValues[1])
                pos = match.range.last + 1
            } else {
                // Non-SGR escape sequence — advance past the ESC byte
                pos = esc + 1
            }
        }

        return spans.ifEmpty { listOf(Span(line, AnsiStyle())) }
    }

    /** Strip all ANSI escape sequences from [line], returning plain text. */
    fun strip(line: String): String = ANSI_STRIP_PATTERN.replace(line, "")

    // ── Internal helpers ──────────────────────────────────────────────────

    private fun applySgr(base: AnsiStyle, codes: String): AnsiStyle {
        if (codes.isEmpty() || codes == "0") return AnsiStyle()

        var style = base
        val parts = codes.split(';').mapNotNull { it.trim().toIntOrNull() }

        for (code in parts) {
            style = when (code) {
                0 -> AnsiStyle()
                1 -> style.copy(bold = true)
                22 -> style.copy(bold = false)
                // Standard foreground colors (30–37)
                30 -> style.copy(fg = AnsiColor.BLACK)
                31 -> style.copy(fg = AnsiColor.RED)
                32 -> style.copy(fg = AnsiColor.GREEN)
                33 -> style.copy(fg = AnsiColor.YELLOW)
                34 -> style.copy(fg = AnsiColor.BLUE)
                35 -> style.copy(fg = AnsiColor.MAGENTA)
                36 -> style.copy(fg = AnsiColor.CYAN)
                37 -> style.copy(fg = AnsiColor.WHITE)
                39 -> style.copy(fg = null) // default fg
                // Bright foreground colors (90–97)
                90 -> style.copy(fg = AnsiColor.BRIGHT_BLACK)
                91 -> style.copy(fg = AnsiColor.BRIGHT_RED)
                92 -> style.copy(fg = AnsiColor.BRIGHT_GREEN)
                93 -> style.copy(fg = AnsiColor.BRIGHT_YELLOW)
                94 -> style.copy(fg = AnsiColor.BRIGHT_BLUE)
                95 -> style.copy(fg = AnsiColor.BRIGHT_MAGENTA)
                96 -> style.copy(fg = AnsiColor.BRIGHT_CYAN)
                97 -> style.copy(fg = AnsiColor.BRIGHT_WHITE)
                // Background colors (40–47, 100–107) — ignored, terminal bg is fixed
                in 40..47, in 100..107 -> style
                else -> style
            }
        }
        return style
    }
}
