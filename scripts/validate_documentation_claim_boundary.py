#!/usr/bin/env python3
"""Fail-closed documentation check for unsupported attainment language."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Mapping

ROOT = Path(__file__).resolve().parents[1]

REQUIRED_MARKERS = {
    Path("docs/CLAIM_LANGUAGE_POLICY.md"): "claim_allowed=false",
    Path("docs/COMPLIANCE.md"): "claim_allowed=false",
    Path("docs/SECURITY.md"): "claim_allowed=false",
    Path("docs/PRIVACY.md"): "claim_allowed=false",
    Path("docs/ANDROID_STARTUP_HOTFIX_2026-08-14.md"): "TOKEN_VAZIO",
}

FORBIDDEN_PATTERNS = {
    "fully compliant": re.compile(r"\bfully\s+compliant\b", re.IGNORECASE),
    "substantially compliant": re.compile(r"\bsubstantially\s+compliant\b", re.IGNORECASE),
    "partially compliant": re.compile(r"\bpartially\s+compliant\b", re.IGNORECASE),
    "GDPR/CCPA attainment": re.compile(r"\bgdpr\s*/\s*ccpa\s+compliant\b", re.IGNORECASE),
    "GDPR attainment": re.compile(r"\bgdpr\s+compliant\b", re.IGNORECASE),
    "CCPA attainment": re.compile(r"\bccpa\s+compliant\b", re.IGNORECASE),
    "algorithm attainment": re.compile(r"\bcompliant\s+algorithms?\b", re.IGNORECASE),
    "standards attainment": re.compile(r"\bstandards\s+compliance\b", re.IGNORECASE),
    "license attainment": re.compile(r"\blicense\s+compliance\b", re.IGNORECASE),
    "regulatory attainment": re.compile(r"\bregulatory\s+compliance\b", re.IGNORECASE),
    "production readiness": re.compile(r"\bproduction[- ]ready\b", re.IGNORECASE),
    "unbounded production use": re.compile(
        r"\bready\s+for\s+production\s+(?:use|deployment|release)\b", re.IGNORECASE
    ),
    "positive certification": re.compile(
        r"\b(?:certified\s*=\s*true|certification_claim\s*[:=]\s*true|"
        r"conformance_claim\s*[:=]\s*true)\b",
        re.IGNORECASE,
    ),
}


def document_paths(root: Path) -> list[Path]:
    paths = [root / "README.md"]
    for directory in (root / "docs", root / ".github"):
        if directory.is_dir():
            paths.extend(path for path in directory.rglob("*.md") if path.is_file())
            paths.extend(path for path in directory.rglob("*.MD") if path.is_file())
    return sorted(set(paths))


def find_forbidden_occurrences(files: Mapping[str, str]) -> list[str]:
    errors: list[str] = []
    for relative, text in sorted(files.items()):
        for line_number, line in enumerate(text.splitlines(), start=1):
            for name, pattern in FORBIDDEN_PATTERNS.items():
                if pattern.search(line):
                    errors.append(f"{relative}:{line_number}: unsupported {name}: {line.strip()}")
    return errors


def validate(root: Path = ROOT) -> dict[str, object]:
    errors: list[str] = []
    files: dict[str, str] = {}

    for path in document_paths(root):
        try:
            files[str(path.relative_to(root))] = path.read_text(encoding="utf-8")
        except UnicodeDecodeError as error:
            errors.append(f"{path.relative_to(root)}: not UTF-8: {error}")

    for relative, marker in REQUIRED_MARKERS.items():
        path = root / relative
        if not path.is_file():
            errors.append(f"missing required evidence-boundary document: {relative}")
            continue
        if marker not in path.read_text(encoding="utf-8"):
            errors.append(f"{relative}: missing required marker {marker!r}")

    errors.extend(find_forbidden_occurrences(files))
    return {
        "schema": "rafgittools.documentation-claim-boundary.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "documents_scanned": len(files),
        "errors": errors,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    report = validate(args.root.resolve())
    print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    return 1 if args.strict and report["status"] != "PASS" else 0


if __name__ == "__main__":
    raise SystemExit(main())
