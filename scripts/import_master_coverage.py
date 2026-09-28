#!/usr/bin/env python3
"""Read-only importer/validator for RAFAELIA master coverage packets.

The importer consumes an already checked-out exact source tree. It performs no
network I/O, no repository mutation, and never reads credentials.

Expected source packet:
- program/master_coverage/2026-09-28/MASTER_WORKSTREAM_REGISTRY_V1.json
- program/master_coverage/2026-09-28/DOCUMENT_OBLIGATION_MATRIX_V1.json
- program/master_coverage/2026-09-28/DOCUMENT_COVERAGE_SNAPSHOT_V2.json
- program/master_coverage/2026-09-28/DOCUMENT_BACKLINK_GRAPH_V1.json
- program/master_coverage/2026-09-28/DOCUMENT_AUTHORITY_REGISTRY_V3.json
- data/governance/rafaelia_active_work_ledger.v2.json
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

SCHEMA = "RAFGITTOOLS_MASTER_COVERAGE_IMPORT_RECEIPT_V1"
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")

REQUIRED_FILES = {
    "registry": "program/master_coverage/2026-09-28/MASTER_WORKSTREAM_REGISTRY_V1.json",
    "obligations": "program/master_coverage/2026-09-28/DOCUMENT_OBLIGATION_MATRIX_V1.json",
    "coverage": "program/master_coverage/2026-09-28/DOCUMENT_COVERAGE_SNAPSHOT_V1.json",
    "backlinks": "program/master_coverage/2026-09-28/DOCUMENT_BACKLINK_GRAPH_V1.json",
    "authority": "program/master_coverage/2026-09-28/DOCUMENT_AUTHORITY_REGISTRY_V2.json",
    "ledger": "data/governance/rafaelia_active_work_ledger.v2.json",
}


def canonical_json_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_json(path: Path) -> tuple[dict[str, Any], str, int]:
    raw = path.read_bytes()
    payload = json.loads(raw.decode("utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"{path}: top-level JSON must be an object")
    return payload, sha256_bytes(raw), len(raw)


def fail(errors: list[str], message: str) -> None:
    errors.append(message)


def validate_packet(
    source_root: Path,
    *,
    source_repo: str,
    source_sha: str,
) -> dict[str, Any]:
    if not SHA40_RE.fullmatch(source_sha):
        raise ValueError("source_sha must be an exact lowercase 40-hex SHA")
    if "/" not in source_repo or source_repo.startswith("/") or source_repo.endswith("/"):
        raise ValueError("source_repo must be owner/name")

    errors: list[str] = []
    loaded: dict[str, dict[str, Any]] = {}
    file_receipts: list[dict[str, Any]] = []

    for key, rel in REQUIRED_FILES.items():
        path = source_root / rel
        if not path.is_file():
            fail(errors, f"missing required file: {rel}")
            continue
        try:
            payload, digest, size = load_json(path)
        except (OSError, UnicodeDecodeError, json.JSONDecodeError, ValueError) as exc:
            fail(errors, f"invalid required file {rel}: {exc}")
            continue
        loaded[key] = payload
        file_receipts.append(
            {
                "role": key,
                "path": rel,
                "sha256": digest,
                "bytes": size,
                "schema": payload.get("schema", "TOKEN_VAZIO_SCHEMA"),
            }
        )

    required_keys = set(REQUIRED_FILES)
    if set(loaded) == required_keys:
        registry = loaded["registry"]
        obligations = loaded["obligations"]
        coverage = loaded["coverage"]
        backlinks = loaded["backlinks"]
        authority = loaded["authority"]
        ledger = loaded["ledger"]

        registry_ws = registry.get("workstreams")
        if not isinstance(registry_ws, list) or not registry_ws:
            fail(errors, "registry.workstreams must be non-empty list")
            registry_ws = []
        registry_ids = [x.get("id") for x in registry_ws if isinstance(x, dict)]
        if any(not isinstance(x, str) or not x for x in registry_ids):
            fail(errors, "registry contains invalid workstream id")
        if len(registry_ids) != len(set(registry_ids)):
            fail(errors, "registry workstream ids are not unique")
        id_set = set(registry_ids)

        ledger_ws = ledger.get("workstreams")
        if not isinstance(ledger_ws, list):
            fail(errors, "ledger.workstreams must be a list")
            ledger_ws = []
        ledger_ids = {x.get("id") for x in ledger_ws if isinstance(x, dict)}
        if ledger_ids != id_set:
            fail(errors, "ledger workstream IDs do not match registry")

        profiles = obligations.get("workstream_profiles")
        if not isinstance(profiles, dict) or set(profiles) != id_set:
            fail(errors, "obligation profile IDs do not match registry")

        coverage_ws = coverage.get("workstreams")
        if not isinstance(coverage_ws, list):
            fail(errors, "coverage.workstreams must be a list")
            coverage_ws = []
        coverage_ids = {x.get("workstream_id") for x in coverage_ws if isinstance(x, dict)}
        if coverage_ids != id_set:
            fail(errors, "coverage workstream IDs do not match registry")

        backlink_nodes = backlinks.get("nodes")
        if not isinstance(backlink_nodes, list):
            fail(errors, "backlinks.nodes must be a list")
            backlink_nodes = []
        backlink_ws_ids = {
            x.get("id")
            for x in backlink_nodes
            if isinstance(x, dict) and x.get("type") == "WORKSTREAM"
        }
        if backlink_ws_ids != id_set:
            fail(errors, "backlink WORKSTREAM nodes do not match registry")

        authority_docs = authority.get("documents")
        if not isinstance(authority_docs, list):
            fail(errors, "authority.documents must be a list")
            authority_docs = []
        authority_paths = {
            x.get("path") for x in authority_docs if isinstance(x, dict)
        }
        for rel in REQUIRED_FILES.values():
            if rel == REQUIRED_FILES["authority"]:
                continue
            if rel not in authority_paths:
                fail(errors, f"authority registry does not classify: {rel}")

        waves = ledger.get("waves")
        if not isinstance(waves, list) or not waves:
            fail(errors, "ledger.waves must be a non-empty list")
            waves = []
        wave_ids: set[str] = set()
        for wave in waves:
            if isinstance(wave, dict):
                for ws_id in wave.get("workstreams", []):
                    if isinstance(ws_id, str):
                        wave_ids.add(ws_id)
        missing_wave = sorted(id_set - wave_ids)
        if missing_wave:
            fail(errors, f"workstreams absent from all waves: {missing_wave}")

        doc_counts = coverage.get("document_state_counts")
        if not isinstance(doc_counts, dict):
            fail(errors, "coverage.document_state_counts must be an object")
            doc_counts = {}

        summary = {
            "workstream_count": len(id_set),
            "coverage_state_counts": dict(sorted(doc_counts.items())),
            "backlink_node_count": len(backlink_nodes),
            "backlink_edge_count": len(backlinks.get("edges", []))
            if isinstance(backlinks.get("edges"), list)
            else 0,
            "authority_entry_count": len(authority_docs),
            "wave_count": len(waves),
        }
    else:
        summary = {
            "workstream_count": 0,
            "coverage_state_counts": {},
            "backlink_node_count": 0,
            "backlink_edge_count": 0,
            "authority_entry_count": 0,
            "wave_count": 0,
        }

    state = "PASS" if not errors else "FAIL"
    return {
        "schema": SCHEMA,
        "state": state,
        "source_repository": source_repo,
        "source_sha": source_sha,
        "source_root": str(source_root),
        "read_only": True,
        "network_io": "NONE",
        "mutation_performed": False,
        "credentials_read": False,
        "claim_allowed": False,
        "files": sorted(file_receipts, key=lambda x: x["path"]),
        "summary": summary,
        "errors": errors,
        "invariants": [
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM",
            "TOKEN_VAZIO!=0",
            "IMPORT_PASS!=DOMAIN_EXECUTION_PASS",
            "READ_ONLY_IMPORT!=AUTHORITY_TRANSFER",
            "EXACT_SHA_REQUIRED",
        ],
        "f_next": (
            "Use the imported IDs and backlinks as a read-only control plane; "
            "measure TOKEN_VAZIO classes with exact evidence before promotion."
            if state == "PASS"
            else "Repair packet consistency at the source exact SHA and re-import."
        ),
    }


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source-root", type=Path, required=True)
    p.add_argument("--source-repo", required=True)
    p.add_argument("--source-sha", required=True)
    p.add_argument("--output", type=Path, required=True)
    return p.parse_args()


def main() -> int:
    args = parse_args()
    try:
        receipt = validate_packet(
            args.source_root,
            source_repo=args.source_repo,
            source_sha=args.source_sha,
        )
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(canonical_json_bytes(receipt))
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"MASTER_COVERAGE_IMPORT_ERROR={exc}", file=sys.stderr)
        return 2

    print(json.dumps(
        {
            "schema": receipt["schema"],
            "state": receipt["state"],
            "source_repository": receipt["source_repository"],
            "source_sha": receipt["source_sha"],
            "workstream_count": receipt["summary"]["workstream_count"],
            "errors": receipt["errors"],
            "claim_allowed": False,
        },
        sort_keys=True,
    ))
    return 0 if receipt["state"] == "PASS" else 3


if __name__ == "__main__":
    raise SystemExit(main())
