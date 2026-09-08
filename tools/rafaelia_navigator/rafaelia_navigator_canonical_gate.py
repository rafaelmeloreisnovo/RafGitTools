#!/usr/bin/env python3
"""Fail-closed canonical corpus gate for RAFAELIA Navigator.

This wrapper does not replace the Navigator producer. It verifies an exact
conversation-shard range, runs the existing Build implementation, validates
its SQLite/source and MESSAGES projections, and emits a reconstructible
manifest. Missing/duplicate shards fail before build.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM
TOKEN_VAZIO != PASS
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import sys
from datetime import datetime, timezone
from pathlib import Path

from rafaelia_navigator import Build

SCHEMA = "RAFAELIA_NAVIGATOR_CANONICAL_GATE_V1"
CHUNK = 1 << 20


def now() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(CHUNK), b""):
            h.update(block)
    return h.hexdigest()


def canonical_json_hash(value: object) -> str:
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def fail(message: str, *, detail: object | None = None) -> "NoReturn":
    print(f"CANONICAL_GATE=FAIL\nreason={message}", file=sys.stderr)
    if detail is not None:
        print(json.dumps(detail, ensure_ascii=False, sort_keys=True), file=sys.stderr)
    raise SystemExit(2)


def expected_names(start: int, end: int) -> list[str]:
    if start < 0 or end < start:
        fail("invalid_range", detail={"start": start, "end": end})
    return [f"conversations-{i:03d}.json" for i in range(start, end + 1)]


def discover_exact(source: Path, names: list[str]) -> tuple[dict[str, Path], list[str]]:
    wanted = set(names)
    occurrences: dict[str, list[Path]] = {name: [] for name in names}
    extras: list[str] = []
    for p in sorted(source.rglob("conversations-*.json")):
        if p.name in wanted:
            occurrences[p.name].append(p)
        else:
            extras.append(p.relative_to(source).as_posix())

    missing = [name for name, paths in occurrences.items() if not paths]
    duplicates = {
        name: [p.relative_to(source).as_posix() for p in paths]
        for name, paths in occurrences.items()
        if len(paths) > 1
    }
    if missing or duplicates:
        fail(
            "canonical_source_set_not_exact",
            detail={"missing": missing, "duplicates": duplicates, "extras_not_in_range": extras},
        )
    return {name: paths[0] for name, paths in occurrences.items()}, extras


def source_manifest(source: Path, mapping: dict[str, Path]) -> tuple[list[dict], int]:
    rows: list[dict] = []
    total = 0
    for name in sorted(mapping):
        p = mapping[name]
        st = p.stat()
        total += st.st_size
        rows.append(
            {
                "name": name,
                "relative_path": p.relative_to(source).as_posix(),
                "bytes": st.st_size,
                "sha256": sha256_file(p),
            }
        )
    return rows, total


def verify_sqlite(output: Path, source_rows: list[dict]) -> dict:
    db = output / "RAFAELIA_NAVIGATOR.sqlite3"
    if not db.is_file():
        fail("navigator_database_missing", detail=str(db))

    conn = sqlite3.connect(db)
    try:
        source_db = {
            row[0]: {"sha256": row[1], "bytes": row[2], "status": row[3], "records": row[4]}
            for row in conn.execute(
                "select path,sha256,bytes,status,records from source_files where kind='conversation'"
            )
        }
        missing_db = []
        drift = []
        for src in source_rows:
            rel = src["relative_path"]
            got = source_db.get(rel)
            if got is None:
                missing_db.append(rel)
                continue
            if got["status"] != "COMPLETE" or got["sha256"] != src["sha256"] or got["bytes"] != src["bytes"]:
                drift.append({"source": src, "database": got})
        if missing_db or drift:
            fail("sqlite_source_custody_mismatch", detail={"missing": missing_db, "drift": drift})

        counts = {
            "source_files_complete_conversation": conn.execute(
                "select count(*) from source_files where kind='conversation' and status='COMPLETE'"
            ).fetchone()[0],
            "conversations": conn.execute("select count(*) from conversations").fetchone()[0],
            "messages": conn.execute("select count(*) from messages").fetchone()[0],
            "message_source_paths": conn.execute("select count(distinct source_path) from messages").fetchone()[0],
        }
        per_source = {
            row[0]: row[1]
            for row in conn.execute(
                "select source_path,count(*) from messages group by source_path order by source_path"
            )
        }
        return {"counts": counts, "messages_per_source_path": per_source}
    finally:
        conn.close()


def verify_message_segments(output: Path, expected_paths: set[str], segment_records: int) -> dict:
    directory = output / "DRIVE_SEARCH_INDEX"
    files = sorted(directory.glob("MESSAGES-*.jsonl.txt"))
    if not files:
        fail("messages_projection_missing")

    ordinals = []
    rows = []
    total_records = 0
    observed_paths: set[str] = set()
    for p in files:
        try:
            ordinal = int(p.stem.split("-")[-1].split(".")[0])
        except ValueError:
            fail("invalid_messages_segment_name", detail=p.name)
        ordinals.append(ordinal)

        n = 0
        with p.open("r", encoding="utf-8") as f:
            for line_no, line in enumerate(f, 1):
                if not line.strip():
                    continue
                try:
                    item = json.loads(line)
                except json.JSONDecodeError as exc:
                    fail("invalid_messages_jsonl", detail={"file": p.name, "line": line_no, "error": str(exc)})
                if item.get("kind") != "message":
                    fail("non_message_record_in_messages_segment", detail={"file": p.name, "line": line_no})
                source_path = item.get("source_path")
                if source_path not in expected_paths:
                    fail(
                        "messages_source_path_outside_canonical_set",
                        detail={"file": p.name, "line": line_no, "source_path": source_path},
                    )
                if item.get("claim_allowed") is not False:
                    fail("claim_allowed_regression", detail={"file": p.name, "line": line_no})
                if not item.get("source_pointer"):
                    fail("source_pointer_missing", detail={"file": p.name, "line": line_no})
                observed_paths.add(source_path)
                n += 1
        if n <= 0 or n > segment_records:
            fail("segment_record_count_invalid", detail={"file": p.name, "records": n, "limit": segment_records})
        total_records += n
        rows.append({"file": p.name, "records": n, "bytes": p.stat().st_size, "sha256": sha256_file(p)})

    expected_ordinals = list(range(1, len(files) + 1))
    if ordinals != expected_ordinals:
        fail("non_contiguous_messages_segments", detail={"observed": ordinals, "expected": expected_ordinals})

    return {
        "segments": rows,
        "segment_count": len(rows),
        "records": total_records,
        "observed_message_source_paths": sorted(observed_paths),
        "source_paths_without_messages": sorted(expected_paths - observed_paths),
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("source", type=Path)
    ap.add_argument("output", type=Path)
    ap.add_argument("--start", type=int, default=0)
    ap.add_argument("--end", type=int, default=50)
    ap.add_argument("--segment-records", type=int, default=5000)
    ap.add_argument("--expected-total-bytes", type=int)
    ap.add_argument("--preflight-only", action="store_true")
    args = ap.parse_args()

    source = args.source.expanduser().resolve()
    output = args.output.expanduser().resolve()
    if not source.is_dir():
        fail("source_directory_missing", detail=str(source))
    if args.segment_records < 1:
        fail("segment_records_must_be_positive")

    names = expected_names(args.start, args.end)
    mapping, extras = discover_exact(source, names)
    src_rows, total_bytes = source_manifest(source, mapping)
    if args.expected_total_bytes is not None and total_bytes != args.expected_total_bytes:
        fail(
            "canonical_total_bytes_mismatch",
            detail={"expected": args.expected_total_bytes, "observed": total_bytes},
        )

    preflight = {
        "schema": SCHEMA,
        "state": "PREFLIGHT_PASS",
        "claim_allowed": False,
        "range": {"start": args.start, "end": args.end, "count": len(names)},
        "expected_names": names,
        "sources": src_rows,
        "source_total_bytes": total_bytes,
        "extra_conversation_shards_outside_requested_range": extras,
    }
    preflight["content_hash"] = canonical_json_hash(preflight)
    print("CANONICAL_PREFLIGHT=PASS")
    print(f"canonical_shards={len(names)}")
    print(f"canonical_bytes={total_bytes}")

    if args.preflight_only:
        print(json.dumps(preflight, ensure_ascii=False, sort_keys=True))
        return 0

    output.mkdir(parents=True, exist_ok=True)
    rc = Build(source, output, None, args.segment_records).run()
    if rc != 0:
        fail("navigator_build_failed", detail={"exit": rc})

    sqlite_state = verify_sqlite(output, src_rows)
    expected_paths = {row["relative_path"] for row in src_rows}
    message_state = verify_message_segments(output, expected_paths, args.segment_records)

    if sqlite_state["counts"]["messages"] != message_state["records"]:
        fail(
            "sqlite_messages_vs_segments_mismatch",
            detail={"sqlite": sqlite_state["counts"]["messages"], "segments": message_state["records"]},
        )

    manifest = {
        "schema": SCHEMA,
        "generated_at_utc": now(),
        "state": "CANONICAL_SOURCE_TO_MESSAGES_PROJECTION_VERIFIED",
        "claim_allowed": False,
        "source_range": {"start": args.start, "end": args.end, "count": len(names)},
        "source_total_bytes": total_bytes,
        "sources": src_rows,
        "navigator": {
            "producer": "tools/rafaelia_navigator/rafaelia_navigator.py",
            "segment_records": args.segment_records,
            **sqlite_state,
        },
        "messages_projection": message_state,
        "invariants": [
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM",
            "TOKEN_VAZIO!=PASS",
            "DERIVED_SUFFIX!=SOURCE_SHARD_IDENTITY",
            "SOURCE_PATH_AND_POINTER_REQUIRED",
            "CLAIM_ALLOWED_FALSE",
        ],
    }
    content_basis = dict(manifest)
    content_basis.pop("generated_at_utc", None)
    manifest["content_hash_without_timestamp"] = canonical_json_hash(content_basis)

    target = output / "CANONICAL_NAVIGATOR_MANIFEST_V1.json"
    target.write_text(json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print("CANONICAL_GATE=PASS")
    print(f"manifest={target}")
    print(f"messages={message_state['records']}")
    print(f"segments={message_state['segment_count']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
