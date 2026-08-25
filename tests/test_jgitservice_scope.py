import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt"
CLASS_RE = re.compile(r"\bclass\s+JGitService\b")
RESET_MARKER = "enum class ResetMode"


def structural_braces(text: str):
    """Yield (line, char) braces outside Kotlin strings/comments.

    This is intentionally a structural lexer, not a Kotlin parser. It handles
    line/block comments, chars, normal strings and triple-quoted strings, which
    is sufficient to keep braces in comments/string interpolation text from
    being mistaken for class-scope delimiters.
    """
    i = 0
    line = 1
    state = "code"
    block_depth = 0
    n = len(text)
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        tri = text[i : i + 3]

        if ch == "\n":
            line += 1
            if state == "line_comment":
                state = "code"
            i += 1
            continue

        if state == "line_comment":
            i += 1
            continue
        if state == "block_comment":
            if ch == "/" and nxt == "*":
                block_depth += 1
                i += 2
            elif ch == "*" and nxt == "/":
                block_depth -= 1
                i += 2
                if block_depth == 0:
                    state = "code"
            else:
                i += 1
            continue
        if state == "char":
            if ch == "\\":
                i += 2
            elif ch == "'":
                state = "code"
                i += 1
            else:
                i += 1
            continue
        if state == "string":
            if ch == "\\":
                i += 2
            elif ch == '"':
                state = "code"
                i += 1
            else:
                i += 1
            continue
        if state == "triple":
            if tri == '"""':
                state = "code"
                i += 3
            else:
                i += 1
            continue

        if ch == "/" and nxt == "/":
            state = "line_comment"
            i += 2
        elif ch == "/" and nxt == "*":
            state = "block_comment"
            block_depth = 1
            i += 2
        elif tri == '"""':
            state = "triple"
            i += 3
        elif ch == '"':
            state = "string"
            i += 1
        elif ch == "'":
            state = "char"
            i += 1
        elif ch in "{}":
            yield line, ch
            i += 1
        else:
            i += 1


def first_class_close_line(text: str) -> int:
    match = CLASS_RE.search(text)
    if not match:
        raise AssertionError("JGitService class declaration missing")

    start_line = text.count("\n", 0, match.start()) + 1
    depth = 0
    seen_open = False
    for line, brace in structural_braces(text):
        if line < start_line:
            continue
        if brace == "{":
            depth += 1
            seen_open = True
        else:
            depth -= 1
            if seen_open and depth == 0:
                return line
            if depth < 0:
                raise AssertionError(f"negative structural depth at line {line}")
    raise AssertionError("JGitService class never closes")


class JGitServiceScopeTests(unittest.TestCase):
    def test_class_closes_immediately_before_reset_mode(self):
        text = SOURCE.read_text(encoding="utf-8")
        close_line = first_class_close_line(text)
        lines = text.splitlines()
        reset_line = next(
            (i for i, value in enumerate(lines, 1) if RESET_MARKER in value),
            None,
        )
        self.assertIsNotNone(reset_line, "ResetMode marker missing")
        expected_close = reset_line - 5  # class close, blank, /**, comment, */
        self.assertEqual(
            expected_close,
            close_line,
            f"JGitService closes prematurely at line {close_line}; expected {expected_close}",
        )


if __name__ == "__main__":
    unittest.main()
