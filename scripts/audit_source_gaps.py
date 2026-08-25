#!/usr/bin/env python3
"""Deterministic source-gap scanner for RafGitTools.

Scope is intentionally limited to compiled/runtime source under ``app/src/main``.
The scanner never copies complete source lines into receipts. It records only a
marker class, relative path, line number and SHA-256 of the normalized line, so
credentials or private source text are not duplicated into audit artifacts.

Exit status:
  0 -> no unallowlisted executable implementation blockers
  1 -> one or more executable implementation blockers
  2 -> scanner/configuration error

Warnings (TODO/FIXME comments, textual 'stub'/'placeholder', and TOKEN_VAZIO
state references) are inventory/attention only. TOKEN_VAZIO is a valid
fail-closed epistemic state in RAFAELIA and is not, by lexical occurrence alone,
evidence that an implementation path is missing.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE_ROOT = ROOT / "app" / "src" / "main"
DEFAULT_ALLOWLIST = ROOT / "contracts" / "source-gap-allowlist.v1.json"
DEFAULT_RECEIPT_DIR = ROOT / ".rafgittools" / "receipts"
SCHEMA = "RAFGITTOOLS_SOURCE_GAP_RECEIPT_V1"

SOURCE_SUFFIXES = {
    ".kt", ".java", ".c", ".cc", ".cpp", ".h", ".hpp",
    ".xml", ".gradle", ".kts", ".properties", ".pro",
}

EXECUTABLE_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("KOTLIN_TODO_CALL", re.compile(r"\bTODO\s*\(")),
    ("NOT_IMPLEMENTED_ERROR", re.compile(r"\bNotImplemented(?:Error|Exception)\b")),
    (
        "UNSUPPORTED_NOT_IMPLEMENTED",
        re.compile(
            r"\bthrow\s+UnsupportedOperationException\s*\(\s*[\"']"
            r"[^\"']*(?:not\s+implemented|todo|stub)[^\"']*[\"']",
            re.IGNORECASE,
        ),
    ),
    (
        "ERROR_NOT_IMPLEMENTED",
        re.compile(
            r"\b(?:error|check|require)\s*\(\s*[\"']"
            r"[^\"']*(?:not\s+implemented|todo|stub)[^\"']*[\"']",
            re.IGNORECASE,
        ),
    ),
)

COMMENT_WARNING_PATTERNS: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("TODO_COMMENT", re.compile(r"\bTODO\b", re.IGNORECASE)),
    ("FIXME_COMMENT", re.compile(r"\bFIXME\b", re.IGNORECASE)),
    ("STUB_TEXT", re.compile(r"\bstub\b", re.IGNORECASE)),
    ("PLACEHOLDER_TEXT", re.compile(r"\bplaceholder\b", re.IGNORECASE)),
)

TOKEN_VAZIO_RE = re.compile(r"\bTOKEN_VAZIO\b")


@dataclass(frozen=True)
class Finding:
    severity: str
    marker: str
    path: str
    line: int
    line_sha256: str
    allowlisted: bool
    allow_reason: str | None = None


def normalized_line_hash(line: str) -> str:
    normalized = " ".join(line.strip().split())
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def is_comment_only(line: str) -> bool:
    stripped = line.lstrip()
    return (
        stripped.startswith("//")
        or stripped.startswith("/*")
        or stripped.startswith("*")
        or stripped.startswith("#")
        or stripped.startswith("<!--")
    )


def iter_source_files(source_root: Path) -> Iterable[Path]:
    for path in sorted(source_root.rglob("*")):
        if path.is_file() and path.suffix.lower() in SOURCE_SUFFIXES:
            yield path


def load_allowlist(path: Path) -> dict[tuple[str, str, str], str]:
    allowlist = {}
    if not path.exists():
        return allowlist
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"invalid allowlist: {exc}") from exc

    if data.get("schema") != "raf.source-gap-allowlist.v1":
        raise ValueError("invalid allowlist schema")
    entries = data.get("entries")
    if not isinstance(entries, list):
        raise ValueError("allowlist entries must be a list")

    result: dict[tuple[str, str, str], str] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError("allowlist entry must be an object")
        marker = entry.get("marker")
        rel_path = entry.get("path")
        line_sha = entry.get("line_sha256")
        reason = entry.get("reason")
        if not all(isinstance(value, str) and value for value in (marker, rel_path, line_sha, reason)):
            raise ValueError("allowlist entry requires marker/path/line_sha256/reason")
        if not re.fullmatch(r"[0-9a-f]{64}", line_sha):
            raise ValueError(f"invalid allowlist line_sha256 for {rel_path}")
        key = (marker, rel_path, line_sha)
        if key in result:
            raise ValueError(f"duplicate allowlist entry: {key}")
        result[key] = reason
    return result


def classify_line(rel_path: str, line_number: int, line: str, allowlist: dict[tuple[str, str, str], str]) -> list[Finding]:
    findings: list[Finding] = []
    line_sha = normalized_line_hash(line)
    comment_only = is_comment_only(line)

    def append(marker: str, severity: str) -> None:
        key = (marker, rel_path, line_sha)
        reason = allowlist.get(key)
        findings.append(
            Finding(
                severity=severity,
                marker=marker,
                path=rel_path,
                line=line_number,
                line_sha256=line_sha,
                allowlisted=reason is not None,
                allow_reason=reason,
            )
        )

    for marker, pattern in EXECUTABLE_PATTERNS:
        if pattern.search(line):
            append(marker, "WARNING" if comment_only else "BLOCKER")

    # TOKEN_VAZIO is a first-class epistemic/runtime state. A lexical occurrence
    # may be an enum member, comparison, fail-closed return, or explicit missing-
    # evidence state. None of those proves source implementation is absent.
    # Keep every occurrence observable as attention inventory, but never promote
    # it to an implementation blocker without an independent executable marker.
    if TOKEN_VAZIO_RE.search(line):
        append("TOKEN_VAZIO_SOURCE", "WARNING")

    # Avoid duplicating TODO/FIXME as warnings when the same line already has an
    # executable blocker of the same conceptual class.
    existing_markers = {finding.marker for finding in findings}
    for marker, pattern in COMMENT_WARNING_PATTERNS:
        if pattern.search(line):
            if marker == "TODO_COMMENT" and "KOTLIN_TODO_CALL" in existing_markers:
                continue
            append(marker, "WARNING")

    return findings


def scan(source_root: Path, allowlist_path: Path) -> tuple[list[Finding], dict[str, str]]:
    allowlist = load_allowlist(allowlist_path)
    findings: list[Finding] = []
    file_hashes: dict[str, str] = {}

    if not source_root.is_dir():
        raise ValueError(f"source root does not exist: {source_root}")

    for path in iter_source_files(source_root):
        rel_path = path.relative_to(ROOT).as_posix()
        try:
            raw = path.read_bytes()
        except OSError as exc:
            raise ValueError(f"cannot read {rel_path}: {exc}") from exc
        file_hashes[rel_path] = hashlib.sha256(raw).hexdigest()

        # Compiled text source should be valid UTF-8. Treat undecodable text as a
        # scanner configuration error rather than silently skipping bytes.
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise ValueError(f"non-UTF8 source file {rel_path}: {exc}") from exc

        for number, line in enumerate(text.splitlines(), start=1):
            findings.extend(classify_line(rel_path, number, line, allowlist))

    findings.sort(key=lambda item: (item.path, item.line, item.marker, item.severity))
    return findings, file_hashes


def source_tree_digest(file_hashes: dict[str, str]) -> str:
    material = "".join(f"{path}\0{digest}\n" for path, digest in sorted(file_hashes.items()))
    return hashlib.sha256(material.encode("utf-8")).hexdigest()


def build_receipt(findings: list[Finding], file_hashes: dict[str, str], source_root: Path) -> dict:
    blockers = [finding for finding in findings if finding.severity == "BLOCKER" and not finding.allowlisted]
    warnings = [finding for finding in findings if finding.severity == "WARNING" and not finding.allowlisted]
    allowlisted = [finding for finding in findings if finding.allowlisted]
    return {
        "schema": SCHEMA,
        "observed_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "source_root": source_root.relative_to(ROOT).as_posix(),
        "source_files": len(file_hashes),
        "source_tree_sha256": source_tree_digest(file_hashes),
        "blockers": len(blockers),
        "warnings": len(warnings),
        "allowlisted": len(allowlisted),
        "claim_allowed": False,
        "findings": [asdict(finding) for finding in findings],
    }


def write_receipt(receipt: dict, receipt_dir: Path) -> tuple[Path, str]:
    receipt_dir.mkdir(parents=True, exist_ok=True)
    stamp = receipt["observed_at"].replace("-", "").replace(":", "")
    stamp = stamp.replace("Z", "Z")
    path = receipt_dir / f"source-gap-{stamp}.json"
    payload = (json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    path.write_bytes(payload)
    digest = hashlib.sha256(payload).hexdigest()
    path.with_suffix(path.suffix + ".sha256").write_text(f"{digest}  {path.name}\n", encoding="utf-8")
    return path, digest


def self_test() -> None:
    empty_allow: dict[tuple[str, str, str], str] = {}
    blocker = classify_line("x.kt", 1, "fun f() = TODO(\"later\")", empty_allow)
    assert any(item.marker == "KOTLIN_TODO_CALL" and item.severity == "BLOCKER" for item in blocker)

    comment = classify_line("x.kt", 2, "// TODO document this", empty_allow)
    assert any(item.marker == "TODO_COMMENT" and item.severity == "WARNING" for item in comment)
    assert not any(item.severity == "BLOCKER" for item in comment)

    tv = classify_line("x.kt", 3, "val state = TOKEN_VAZIO", empty_allow)
    assert any(item.marker == "TOKEN_VAZIO_SOURCE" and item.severity == "WARNING" for item in tv)
    assert not any(item.severity == "BLOCKER" for item in tv)

    mixed = classify_line("x.kt", 4, "fun f() = TODO(\"TOKEN_VAZIO\")", empty_allow)
    assert any(item.marker == "KOTLIN_TODO_CALL" and item.severity == "BLOCKER" for item in mixed)
    assert any(item.marker == "TOKEN_VAZIO_SOURCE" and item.severity == "WARNING" for item in mixed)

    line = "fun f() = TODO(\"later\")"
    sha = normalized_line_hash(line)
    allow = {("KOTLIN_TODO_CALL", "x.kt", sha): "fixture"}
    allowed = classify_line("x.kt", 1, line, allow)
    assert any(item.allowlisted for item in allowed if item.marker == "KOTLIN_TODO_CALL")
    print("PASS source-gap-self-test")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, default=DEFAULT_SOURCE_ROOT)
    parser.add_argument("--allowlist", type=Path, default=DEFAULT_ALLOWLIST)
    parser.add_argument("--receipt-dir", type=Path, default=DEFAULT_RECEIPT_DIR)
    parser.add_argument("--no-receipt", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.self_test:
        self_test()
        return 0

    try:
        findings, file_hashes = scan(args.source_root.resolve(), args.allowlist.resolve())
        receipt = build_receipt(findings, file_hashes, args.source_root.resolve())
    except (ValueError, OSError) as exc:
        print(f"FAIL source-gap-audit: {exc}", file=sys.stderr)
        return 2

    if not args.no_receipt:
        try:
            path, digest = write_receipt(receipt, args.receipt_dir.resolve())
            print(f"receipt={path}")
            print(f"receipt_sha256={digest}")
        except OSError as exc:
            print(f"FAIL source-gap-audit receipt: {exc}", file=sys.stderr)
            return 2

    print(
        "SOURCE_GAP "
        f"files={receipt['source_files']} blockers={receipt['blockers']} "
        f"warnings={receipt['warnings']} allowlisted={receipt['allowlisted']} "
        f"tree_sha256={receipt['source_tree_sha256']}"
    )

    for finding in receipt["findings"]:
        if finding["allowlisted"]:
            continue
        print(
            f"{finding['severity']} {finding['marker']} "
            f"{finding['path']}:{finding['line']} line_sha256={finding['line_sha256']}"
        )

    return 1 if receipt["blockers"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
