package com.rafgittools.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * SyntaxHighlighter — P33-12
 *
 * Pure-Kotlin, zero-dependency syntax highlighter for Compose.
 * Supports: Kotlin, Java, Python, JavaScript/TypeScript, XML, JSON,
 *           Shell/Bash, YAML, Markdown (basic), Gradle (Groovy/Kotlin DSL).
 *
 * Usage:
 *   val annotated = SyntaxHighlighter.highlight(code, "kt")
 *   Text(annotated, fontFamily = FontFamily.Monospace)
 */
object SyntaxHighlighter {

    fun highlight(code: String, extension: String): AnnotatedString {
        val lang = detectLanguage(extension)
        return when (lang) {
            Language.KOTLIN, Language.JAVA -> highlightJvmLike(code, lang)
            Language.PYTHON -> highlightPython(code)
            Language.JAVASCRIPT, Language.TYPESCRIPT -> highlightJs(code)
            Language.XML, Language.HTML -> highlightXml(code)
            Language.JSON -> highlightJson(code)
            Language.YAML -> highlightYaml(code)
            Language.SHELL -> highlightShell(code)
            Language.GRADLE -> highlightJvmLike(code, Language.KOTLIN)
            else -> AnnotatedString(code)
        }
    }

    private fun detectLanguage(ext: String): Language = when (ext.lowercase().trimStart('.')) {
        "kt", "kts" -> Language.KOTLIN
        "java" -> Language.JAVA
        "py" -> Language.PYTHON
        "js", "jsx", "mjs" -> Language.JAVASCRIPT
        "ts", "tsx" -> Language.TYPESCRIPT
        "xml", "svg" -> Language.XML
        "html", "htm" -> Language.HTML
        "json" -> Language.JSON
        "yaml", "yml" -> Language.YAML
        "sh", "bash", "zsh" -> Language.SHELL
        "gradle", "groovy" -> Language.GRADLE
        else -> Language.PLAIN
    }

    // ─── Color palette (dark-friendly defaults) ─────────────────────────
    private val COLOR_KEYWORD     = Color(0xFF569CD6)  // blue
    private val COLOR_STRING      = Color(0xFFCE9178)  // orange
    private val COLOR_COMMENT     = Color(0xFF6A9955)  // green
    private val COLOR_NUMBER      = Color(0xFFB5CEA8)  // light green
    private val COLOR_TYPE        = Color(0xFF4EC9B0)  // teal
    private val COLOR_ANNOTATION  = Color(0xFFDCDCAA)  // yellow
    private val COLOR_ATTRIBUTE   = Color(0xFF9CDCFE)  // light blue
    private val COLOR_TAG         = Color(0xFF569CD6)  // blue
    private val COLOR_OPERATOR    = Color(0xFFD4D4D4)  // white-ish

    // ─── JVM (Kotlin/Java) ───────────────────────────────────────────────
    private val KOTLIN_KEYWORDS = setOf(
        "val", "var", "fun", "class", "object", "interface", "enum", "sealed",
        "data", "abstract", "open", "override", "private", "protected", "public",
        "internal", "companion", "suspend", "inline", "reified", "when", "is",
        "as", "in", "out", "return", "if", "else", "for", "while", "do", "try",
        "catch", "finally", "throw", "import", "package", "typealias", "by",
        "this", "super", "null", "true", "false", "init", "constructor", "lateinit"
    )
    private val JAVA_KEYWORDS = setOf(
        "public", "private", "protected", "static", "final", "abstract", "class",
        "interface", "extends", "implements", "new", "return", "if", "else",
        "for", "while", "do", "switch", "case", "break", "continue", "throw",
        "throws", "try", "catch", "finally", "import", "package", "void",
        "int", "long", "double", "float", "boolean", "char", "byte", "short",
        "null", "true", "false", "this", "super", "instanceof"
    )

    private fun highlightJvmLike(code: String, lang: Language): AnnotatedString {
        val keywords = if (lang == Language.KOTLIN) KOTLIN_KEYWORDS else JAVA_KEYWORDS
        return buildAnnotatedString {
            var i = 0
            while (i < code.length) {
                when {
                    // Single-line comment
                    code.startsWith("//", i) -> {
                        val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                        pushStyle(SpanStyle(color = COLOR_COMMENT))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    // Block comment
                    code.startsWith("/*", i) -> {
                        val end = code.indexOf("*/", i + 2).let { if (it == -1) code.length else it + 2 }
                        pushStyle(SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    // String literals (handle triple-quoted for Kotlin)
                    code.startsWith("\"\"\"", i) -> {
                        val end = code.indexOf("\"\"\"", i + 3).let { if (it == -1) code.length else it + 3 }
                        pushStyle(SpanStyle(color = COLOR_STRING))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    code[i] == '"' -> {
                        val end = findStringEnd(code, i + 1, '"')
                        pushStyle(SpanStyle(color = COLOR_STRING))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    code[i] == '\'' -> {
                        val end = findStringEnd(code, i + 1, '\'')
                        pushStyle(SpanStyle(color = COLOR_STRING))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    // Annotation
                    code[i] == '@' -> {
                        val end = i + 1 + code.substring(i + 1).takeWhile { it.isLetterOrDigit() || it == '_' }.length
                        pushStyle(SpanStyle(color = COLOR_ANNOTATION))
                        append(code.substring(i, end))
                        pop()
                        i = end
                    }
                    // Number
                    code[i].isDigit() || (code[i] == '-' && i + 1 < code.length && code[i + 1].isDigit()) -> {
                        val end = i + code.substring(i).takeWhile { it.isDigit() || it == '.' || it == 'L' || it == 'f' }.length
                        pushStyle(SpanStyle(color = COLOR_NUMBER))
                        append(code.substring(i, end))
                        pop()
                        i = end.coerceAtLeast(i + 1)
                    }
                    // Keyword or identifier
                    code[i].isLetter() || code[i] == '_' -> {
                        val end = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == '_' }.length
                        val word = code.substring(i, end)
                        when {
                            word in keywords -> {
                                pushStyle(SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.SemiBold))
                                append(word)
                                pop()
                            }
                            word.firstOrNull()?.isUpperCase() == true -> {
                                pushStyle(SpanStyle(color = COLOR_TYPE))
                                append(word)
                                pop()
                            }
                            else -> append(word)
                        }
                        i = end
                    }
                    else -> {
                        append(code[i])
                        i++
                    }
                }
            }
        }
    }

    // ─── Python ──────────────────────────────────────────────────────────
    private val PYTHON_KEYWORDS = setOf(
        "def", "class", "import", "from", "as", "if", "elif", "else", "for",
        "while", "return", "yield", "pass", "break", "continue", "try",
        "except", "finally", "raise", "with", "lambda", "and", "or", "not",
        "in", "is", "True", "False", "None", "self", "async", "await", "global"
    )

    private fun highlightPython(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code[i] == '#' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    pushStyle(SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code.startsWith("\"\"\"", i) || code.startsWith("'''", i) -> {
                    val delim = code.substring(i, i + 3)
                    val end = code.indexOf(delim, i + 3).let { if (it == -1) code.length else it + 3 }
                    pushStyle(SpanStyle(color = COLOR_STRING))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i] == '"' || code[i] == '\'' -> {
                    val end = findStringEnd(code, i + 1, code[i])
                    pushStyle(SpanStyle(color = COLOR_STRING))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i] == '@' -> {
                    val end = i + 1 + code.substring(i + 1).takeWhile { it.isLetterOrDigit() || it == '_' }.length
                    pushStyle(SpanStyle(color = COLOR_ANNOTATION))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i].isDigit() -> {
                    val end = i + code.substring(i).takeWhile { it.isDigit() || it == '.' }.length
                    pushStyle(SpanStyle(color = COLOR_NUMBER))
                    append(code.substring(i, end))
                    pop()
                    i = end.coerceAtLeast(i + 1)
                }
                code[i].isLetter() || code[i] == '_' -> {
                    val end = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == '_' }.length
                    val word = code.substring(i, end)
                    if (word in PYTHON_KEYWORDS) {
                        pushStyle(SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.SemiBold))
                        append(word)
                        pop()
                    } else {
                        append(word)
                    }
                    i = end
                }
                else -> { append(code[i]); i++ }
            }
        }
    }

    // ─── JSON ────────────────────────────────────────────────────────────
    private fun highlightJson(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        var isKey = true
        while (i < code.length) {
            when {
                code[i] == '"' -> {
                    val end = findStringEnd(code, i + 1, '"')
                    pushStyle(SpanStyle(color = if (isKey) COLOR_ATTRIBUTE else COLOR_STRING))
                    append(code.substring(i, end))
                    pop()
                    i = end
                    if (isKey) {
                        // After a key, the next non-whitespace should be ':'
                        val after = code.substring(i).trimStart()
                        isKey = !after.startsWith(":")
                    } else {
                        isKey = true
                    }
                }
                code[i] == ':' -> { append(':'); isKey = false; i++ }
                code[i] == ',' || code[i] == '{' || code[i] == '}' || code[i] == '[' || code[i] == ']' -> {
                    append(code[i])
                    if (code[i] == ',' || code[i] == '{' || code[i] == '[') isKey = true
                    i++
                }
                code[i].isDigit() || code[i] == '-' -> {
                    val end = i + code.substring(i).takeWhile { it.isDigit() || it == '.' || it == '-' || it == 'e' || it == 'E' }.length
                    pushStyle(SpanStyle(color = COLOR_NUMBER))
                    append(code.substring(i, end.coerceAtLeast(i + 1)))
                    pop()
                    i = end.coerceAtLeast(i + 1)
                }
                code.startsWith("true", i) || code.startsWith("false", i) || code.startsWith("null", i) -> {
                    val end = i + code.substring(i).takeWhile { it.isLetter() }.length
                    pushStyle(SpanStyle(color = COLOR_KEYWORD))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                else -> { append(code[i]); i++ }
            }
        }
    }

    // ─── XML/HTML ────────────────────────────────────────────────────────
    private fun highlightXml(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("<!--", i) -> {
                    val end = code.indexOf("-->", i + 4).let { if (it == -1) code.length else it + 3 }
                    pushStyle(SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i] == '<' -> {
                    pushStyle(SpanStyle(color = COLOR_TAG))
                    append('<')
                    pop()
                    i++
                    if (i < code.length && code[i] == '/') { append('/'); i++ }
                    val tagEnd = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == ':' || it == '-' }.length
                    pushStyle(SpanStyle(color = COLOR_TAG, fontWeight = FontWeight.SemiBold))
                    append(code.substring(i, tagEnd))
                    pop()
                    i = tagEnd
                }
                code[i] == '"' -> {
                    val end = findStringEnd(code, i + 1, '"')
                    pushStyle(SpanStyle(color = COLOR_STRING))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i].isLetter() && i > 0 && code[i - 1] == ' ' -> {
                    // Likely attribute
                    val end = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == ':' || it == '-' || it == '_' }.length
                    pushStyle(SpanStyle(color = COLOR_ATTRIBUTE))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                else -> { append(code[i]); i++ }
            }
        }
    }

    // ─── YAML ────────────────────────────────────────────────────────────
    private fun highlightYaml(code: String): AnnotatedString = buildAnnotatedString {
        code.lines().forEach { line ->
            when {
                line.trimStart().startsWith("#") -> {
                    pushStyle(SpanStyle(color = COLOR_COMMENT))
                    append(line)
                    pop()
                }
                line.contains(":") -> {
                    val colonIdx = line.indexOf(':')
                    pushStyle(SpanStyle(color = COLOR_ATTRIBUTE))
                    append(line.substring(0, colonIdx))
                    pop()
                    pushStyle(SpanStyle(color = COLOR_STRING))
                    append(line.substring(colonIdx))
                    pop()
                }
                else -> append(line)
            }
            append('\n')
        }
    }

    // ─── Shell ───────────────────────────────────────────────────────────
    private val SHELL_KEYWORDS = setOf("if", "then", "else", "elif", "fi", "for",
        "while", "do", "done", "case", "esac", "function", "return", "export",
        "echo", "exit", "cd", "ls", "rm", "mkdir", "grep", "sed", "awk", "curl")

    private fun highlightShell(code: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code[i] == '#' -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    pushStyle(SpanStyle(color = COLOR_COMMENT))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i] == '"' || code[i] == '\'' -> {
                    val end = findStringEnd(code, i + 1, code[i])
                    pushStyle(SpanStyle(color = COLOR_STRING))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i] == '$' -> {
                    val end = i + 1 + code.substring(i + 1).takeWhile { it.isLetterOrDigit() || it == '_' || it == '{' || it == '}' }.length
                    pushStyle(SpanStyle(color = COLOR_ANNOTATION))
                    append(code.substring(i, end))
                    pop()
                    i = end
                }
                code[i].isLetter() || code[i] == '_' -> {
                    val end = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == '_' || it == '-' }.length
                    val word = code.substring(i, end)
                    if (word in SHELL_KEYWORDS) {
                        pushStyle(SpanStyle(color = COLOR_KEYWORD))
                        append(word)
                        pop()
                    } else append(word)
                    i = end
                }
                else -> { append(code[i]); i++ }
            }
        }
    }

    // ─── JavaScript/TypeScript ───────────────────────────────────────────
    private val JS_KEYWORDS = setOf(
        "const", "let", "var", "function", "class", "extends", "import", "export",
        "default", "return", "if", "else", "for", "while", "do", "switch", "case",
        "break", "continue", "try", "catch", "finally", "throw", "new", "this",
        "super", "null", "undefined", "true", "false", "typeof", "instanceof",
        "async", "await", "yield", "of", "in", "from", "interface", "type",
        "enum", "abstract", "implements", "readonly"
    )

    private fun highlightJs(code: String): AnnotatedString = highlightJsLike(code, JS_KEYWORDS)

    private fun highlightJsLike(code: String, keywords: Set<String>): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("//", i) -> {
                    val end = code.indexOf('\n', i).let { if (it == -1) code.length else it }
                    pushStyle(SpanStyle(color = COLOR_COMMENT)); append(code.substring(i, end)); pop()
                    i = end
                }
                code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i + 2).let { if (it == -1) code.length else it + 2 }
                    pushStyle(SpanStyle(color = COLOR_COMMENT, fontStyle = FontStyle.Italic))
                    append(code.substring(i, end)); pop(); i = end
                }
                code[i] == '`' -> {
                    val end = code.indexOf('`', i + 1).let { if (it == -1) code.length else it + 1 }
                    pushStyle(SpanStyle(color = COLOR_STRING)); append(code.substring(i, end)); pop(); i = end
                }
                code[i] == '"' || code[i] == '\'' -> {
                    val end = findStringEnd(code, i + 1, code[i])
                    pushStyle(SpanStyle(color = COLOR_STRING)); append(code.substring(i, end)); pop(); i = end
                }
                code[i].isDigit() -> {
                    val end = i + code.substring(i).takeWhile { it.isDigit() || it == '.' }.length
                    pushStyle(SpanStyle(color = COLOR_NUMBER)); append(code.substring(i, end.coerceAtLeast(i+1))); pop()
                    i = end.coerceAtLeast(i + 1)
                }
                code[i].isLetter() || code[i] == '_' || code[i] == '$' -> {
                    val end = i + code.substring(i).takeWhile { it.isLetterOrDigit() || it == '_' || it == '$' }.length
                    val word = code.substring(i, end)
                    when {
                        word in keywords -> { pushStyle(SpanStyle(color = COLOR_KEYWORD)); append(word); pop() }
                        word.firstOrNull()?.isUpperCase() == true -> { pushStyle(SpanStyle(color = COLOR_TYPE)); append(word); pop() }
                        else -> append(word)
                    }
                    i = end
                }
                else -> { append(code[i]); i++ }
            }
        }
    }

    // ─── Util ────────────────────────────────────────────────────────────
    private fun findStringEnd(code: String, start: Int, delimiter: Char): Int {
        var i = start
        while (i < code.length) {
            if (code[i] == '\\') { i += 2; continue }
            if (code[i] == delimiter) return i + 1
            i++
        }
        return code.length
    }

    private enum class Language {
        KOTLIN, JAVA, PYTHON, JAVASCRIPT, TYPESCRIPT,
        XML, HTML, JSON, YAML, SHELL, GRADLE, PLAIN
    }
}
