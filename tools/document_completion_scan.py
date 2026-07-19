#!/usr/bin/env python3
"""Build a non-destructive RAFAELIA document-completion manifest.

The scanner never edits a canonical document or any source file. It hashes,
classifies and records candidates so a later reviewed patch can decide what to
include. The output follows schemas/document-completion.schema.json.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
from typing import Any, Iterable

SCHEMA_ID = "raf.document-completion.v1"

_TEXT_EXTENSIONS = {
    ".md",
    ".mdx",
    ".txt",
    ".rst",
    ".adoc",
    ".org",
}
_CODE_EXTENSIONS = {
    ".c",
    ".h",
    ".cc",
    ".cpp",
    ".cxx",
    ".hpp",
    ".s",
    ".S",
    ".asm",
    ".kt",
    ".kts",
    ".java",
    ".py",
    ".rs",
    ".go",
    ".lua",
    ".sh",
    ".bash",
    ".zsh",
}
_SPEC_EXTENSIONS = {".json", ".yaml", ".yml", ".toml", ".xml", ".xsd"}
_EVIDENCE_NAMES = {
    "proof",
    "proofs",
    "evidence",
    "results",
    "validation",
    "validations",
    "reports",
    "logs",
}
_HISTORICAL_NAMES = {
    "archive",
    "archives",
    "legacy",
    "old",
    "historical",
    "fazer",
    "livro",
    "_upcoming",
}
_GENERATED_NAMES = {"build", "dist", "out", "generated", ".gradle", ".idea"}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _path_parts_lower(path: Path) -> set[str]:
    return {part.lower() for part in path.parts}


def classify(path: Path) -> str:
    parts = _path_parts_lower(path)
    suffix = path.suffix
    suffix_lower = suffix.lower()

    if parts & _GENERATED_NAMES:
        return "GENERATED"
    if parts & _HISTORICAL_NAMES:
        return "HISTORICAL"
    if parts & _EVIDENCE_NAMES:
        return "EVIDENCE"
    if suffix in {".S"} or suffix_lower in _CODE_EXTENSIONS:
        return "CODE"
    if suffix_lower in _TEXT_EXTENSIONS:
        return "DOCUMENTATION"
    if suffix_lower in _SPEC_EXTENSIONS:
        return "SPECIFICATION"
    return "UNKNOWN"


def epistemic_status(classification: str) -> str:
    if classification == "EVIDENCE":
        return "REFERENCE"
    if classification in {"GENERATED", "HISTORICAL", "UNKNOWN"}:
        return "QUARANTINE"
    return "TOKEN_VAZIO"


def initial_decision(classification: str) -> str:
    if classification in {"GENERATED", "HISTORICAL", "UNKNOWN"}:
        return "QUARANTINE"
    return "REFERENCE_ONLY"


def iter_source_files(roots: Iterable[Path]) -> Iterable[Path]:
    seen_paths: set[Path] = set()
    for root in sorted((path.resolve() for path in roots), key=str):
        if not root.exists():
            continue
        paths = [root] if root.is_file() else root.rglob("*")
        for path in paths:
            if not path.is_file() or path.is_symlink():
                continue
            resolved = path.resolve()
            if resolved in seen_paths:
                continue
            seen_paths.add(resolved)
            yield resolved


def _display_path(path: Path, repository_root: Path) -> str:
    try:
        return path.relative_to(repository_root).as_posix()
    except ValueError:
        return path.as_posix()


def build_manifest(
    *,
    canonical: Path,
    source_roots: list[Path],
    repository: str,
    ref: str,
    job_id: str | None = None,
    repository_root: Path | None = None,
) -> dict[str, Any]:
    canonical = canonical.resolve(strict=True)
    if not canonical.is_file():
        raise ValueError(f"canonical document is not a file: {canonical}")

    repository_root = (repository_root or Path.cwd()).resolve()
    canonical_hash = sha256_file(canonical)
    effective_job_id = job_id or f"docscan-{canonical_hash[:12]}"

    candidates: list[dict[str, Any]] = []
    conflicts: list[dict[str, str]] = []
    hashes: dict[str, str] = {canonical_hash: _display_path(canonical, repository_root)}

    canonical_resolved = canonical.resolve()
    for path in iter_source_files(source_roots):
        if path == canonical_resolved:
            continue

        digest = sha256_file(path)
        classification = classify(path)
        decision = initial_decision(classification)
        status = epistemic_status(classification)
        reason = "candidate requires reviewed section mapping and provenance"

        if digest in hashes:
            classification = "DUPLICATE"
            decision = "REJECT_DUPLICATE"
            status = "REFERENCE"
            reason = f"byte-identical to {hashes[digest]}"
            conflicts.append(
                {
                    "candidate_path": _display_path(path, repository_root),
                    "canonical_fragment": hashes[digest],
                    "kind": "DUPLICATE",
                    "resolution": "CANONICAL_WINS",
                }
            )
        else:
            hashes[digest] = _display_path(path, repository_root)

        candidates.append(
            {
                "source_path": _display_path(path, repository_root),
                "sha256": digest,
                "classification": classification,
                "target_section": None,
                "epistemic_status": status,
                "provenance": {
                    "repository": repository,
                    "ref": ref,
                    "commit": None,
                    "author": None,
                    "license": None,
                },
                "decision": decision,
                "reason": reason,
            }
        )

    candidates.sort(key=lambda item: item["source_path"])
    conflicts.sort(key=lambda item: item["candidate_path"])

    quarantined = any(item["decision"] == "QUARANTINE" for item in candidates)
    manifest_status = "BLOCKED" if quarantined else "PARTIAL"
    if not candidates:
        manifest_status = "TOKEN_VAZIO"

    return {
        "schema": SCHEMA_ID,
        "job_id": effective_job_id,
        "canonical_document": {
            "path": _display_path(canonical, repository_root),
            "sha256": canonical_hash,
            "repository": repository,
            "ref": ref,
        },
        "source_roots": [
            _display_path(path.resolve(), repository_root) for path in source_roots
        ],
        "policy": {
            "mode": "DRY_RUN",
            "preserve_sources": True,
            "require_provenance": True,
            "on_conflict": "RECORD",
            "allow_unverified_claims": False,
        },
        "candidates": candidates,
        "conflicts": conflicts,
        "result": {
            "status": manifest_status,
            "claim_allowed": False,
            "patch_path": None,
            "evidence_paths": [],
        },
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scan loose files and emit a review-only document completion manifest."
    )
    parser.add_argument("--canonical", required=True, type=Path)
    parser.add_argument("--source-root", action="append", required=True, type=Path)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--job-id")
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--repository-root", type=Path, default=Path.cwd())
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    manifest = build_manifest(
        canonical=args.canonical,
        source_roots=args.source_root,
        repository=args.repository,
        ref=args.ref,
        job_id=args.job_id,
        repository_root=args.repository_root,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    temp = args.output.with_suffix(args.output.suffix + ".tmp")
    temp.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    os.replace(temp, args.output)
    print(
        json.dumps(
            {
                "output": str(args.output),
                "candidates": len(manifest["candidates"]),
                "conflicts": len(manifest["conflicts"]),
                "status": manifest["result"]["status"],
                "claim_allowed": False,
            },
            ensure_ascii=False,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
