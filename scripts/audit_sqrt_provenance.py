#!/usr/bin/env python3
"""Audit textual √/sqrt occurrences against a provenance manifest.

Dependency-free by design: suitable for Termux, Linux and standard Python.
The scanner does not decide legal authorship; it prevents unclassified claims
from silently entering the canonical tree.
"""

from __future__ import annotations

import argparse
import fnmatch
import hashlib
import json
import os
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Sequence

SQRT_RE = re.compile(
    r"√|(?<![A-Za-z0-9_])(?:sqrt|sqrtf|sqrtl|isqrt)\s*\(|\b(?:SQRT|ISQRT)[A-Z0-9_]*\b",
    re.IGNORECASE,
)
AUTHOR_CLAIM_RE = re.compile(
    r"\bsole[_ -]?author\s*[:=]\s*true\b|\bautoria\s+exclusiva\b",
    re.IGNORECASE,
)
CLAIM_SENSITIVE = {"rafaelia_original", "derived_with_changes", "needs_review"}


@dataclass(frozen=True)
class Finding:
    path: str
    line: int
    column: int
    token: str
    classification: str
    origin: str
    severity: str
    message: str


class ManifestError(RuntimeError):
    pass


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Audit √/sqrt provenance and protected academic attribution."
    )
    parser.add_argument("--root", default=".", help="repository root")
    parser.add_argument(
        "--manifest",
        default="config/sqrt_provenance.json",
        help="manifest path relative to --root",
    )
    parser.add_argument(
        "--strict",
        action="store_true",
        help="fail on needs_review as well as unclassified/error findings",
    )
    parser.add_argument("--json-output", help="optional deterministic JSON report")
    return parser.parse_args(argv)


def load_manifest(path: Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ManifestError(f"cannot load manifest {path}: {exc}") from exc

    required = {"academic_reference_id", "allowed_classifications", "rules"}
    missing = sorted(required.difference(data))
    if missing:
        raise ManifestError(f"manifest missing keys: {', '.join(missing)}")

    allowed = set(data["allowed_classifications"])
    for index, rule in enumerate(data["rules"]):
        if "pattern" not in rule or "classification" not in rule:
            raise ManifestError(f"rule {index} requires pattern and classification")
        if rule["classification"] not in allowed:
            raise ManifestError(
                f"rule {index} has unknown classification {rule['classification']!r}"
            )
    return data


def classify(rel_path: str, rules: Iterable[dict]) -> dict | None:
    # First match wins; exact/specific rules must precede broad globs.
    for rule in rules:
        if fnmatch.fnmatchcase(rel_path, rule["pattern"]):
            return rule
    return None


def iter_text_files(root: Path, ignored_dirs: set[str]) -> Iterable[Path]:
    for current, dirs, files in os.walk(root):
        dirs[:] = sorted(d for d in dirs if d not in ignored_dirs)
        current_path = Path(current)
        for name in sorted(files):
            path = current_path / name
            try:
                if path.is_symlink() or path.stat().st_size > 8 * 1024 * 1024:
                    continue
                prefix = path.read_bytes()[:4096]
            except OSError:
                continue
            if b"\x00" not in prefix:
                yield path


def read_lines(path: Path) -> list[str] | None:
    try:
        return path.read_text(encoding="utf-8").splitlines()
    except (OSError, UnicodeDecodeError):
        return None


def load_private_identity_terms() -> list[str]:
    """Read sensitive names from an untracked, maintainer-controlled file."""

    configured = os.environ.get("RAF_PROTECTED_IDENTITY_TERMS_FILE")
    if not configured:
        return []
    try:
        return [
            line.strip()
            for line in Path(configured).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        ]
    except OSError as exc:
        raise ManifestError(f"cannot read protected identity terms: {exc}") from exc


def audit(root: Path, manifest: dict, strict: bool) -> tuple[list[Finding], dict]:
    findings: list[Finding] = []
    counts: dict[str, int] = {}
    forbidden_in_strict = set(manifest.get("strict_forbidden_classifications", []))
    reference_id = manifest["academic_reference_id"]
    identity_terms = load_private_identity_terms()

    for path in iter_text_files(root, set(manifest.get("ignore_directories", []))):
        rel = path.relative_to(root).as_posix()
        lines = read_lines(path)
        if lines is None:
            continue

        text = "\n".join(lines)
        rule = classify(rel, manifest["rules"])
        classification = rule["classification"] if rule else "unclassified"
        origin = rule.get("origin", "") if rule else ""

        for term in identity_terms:
            for match in re.finditer(re.escape(term), text, re.IGNORECASE):
                line = text.count("\n", 0, match.start()) + 1
                line_start = text.rfind("\n", 0, match.start()) + 1
                findings.append(
                    Finding(
                        rel,
                        line,
                        match.start() - line_start + 1,
                        "<protected-identity>",
                        classification,
                        origin,
                        "error",
                        "civil identifier of protected minor found",
                    )
                )

        for line_no, line_text in enumerate(lines, start=1):
            for match in SQRT_RE.finditer(line_text):
                counts[classification] = counts.get(classification, 0) + 1
                severity = "ok"
                message = "classified"

                if rule is None:
                    severity = "error"
                    message = "sqrt occurrence has no provenance rule"
                elif classification == "regression_julia_reference":
                    if rule.get("reference") != reference_id and reference_id not in text:
                        severity = "error"
                        message = f"specific academic use requires {reference_id}"
                elif strict and classification in forbidden_in_strict:
                    severity = "error"
                    message = "classification forbidden in strict/release mode"
                elif classification == "needs_review":
                    severity = "warning"
                    message = "origin remains TOKEN_VAZIO pending genealogy"

                findings.append(
                    Finding(
                        rel,
                        line_no,
                        match.start() + 1,
                        match.group(0),
                        classification,
                        origin,
                        severity,
                        message,
                    )
                )

        if classification in CLAIM_SENSITIVE and AUTHOR_CLAIM_RE.search(text):
            claim_allowed = bool(rule and rule.get("sole_author") is True)
            if not claim_allowed:
                findings.append(
                    Finding(
                        rel,
                        1,
                        1,
                        "sole-author-claim",
                        classification,
                        origin,
                        "error",
                        "exclusive authorship claim lacks a verified genealogy gate",
                    )
                )

    summary = {
        "schema": "rafaelia.sqrt.audit-report/v1",
        "root": str(root.resolve()),
        "manifest_sha256": hashlib.sha256(
            json.dumps(manifest, sort_keys=True, separators=(",", ":")).encode("utf-8")
        ).hexdigest(),
        "strict": strict,
        "occurrences_by_classification": dict(sorted(counts.items())),
        "errors": sum(f.severity == "error" for f in findings),
        "warnings": sum(f.severity == "warning" for f in findings),
        "classified": sum(f.severity == "ok" for f in findings),
    }
    return findings, summary


def emit_human(findings: Sequence[Finding], summary: dict) -> None:
    for finding in findings:
        if finding.severity == "ok":
            continue
        print(
            f"{finding.severity.upper():7} {finding.path}:{finding.line}:{finding.column} "
            f"[{finding.classification}] {finding.message}",
            file=sys.stderr if finding.severity == "error" else sys.stdout,
        )
    print(json.dumps(summary, ensure_ascii=False, sort_keys=True, indent=2))


def main(argv: Sequence[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    root = Path(args.root).resolve()
    try:
        manifest = load_manifest(root / args.manifest)
        findings, summary = audit(root, manifest, args.strict)
    except ManifestError as exc:
        print(f"MANIFEST ERROR: {exc}", file=sys.stderr)
        return 2

    emit_human(findings, summary)
    if args.json_output:
        output = root / args.json_output
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(
            json.dumps(
                {"summary": summary, "findings": [asdict(item) for item in findings]},
                ensure_ascii=False,
                sort_keys=True,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )
    return 1 if summary["errors"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
