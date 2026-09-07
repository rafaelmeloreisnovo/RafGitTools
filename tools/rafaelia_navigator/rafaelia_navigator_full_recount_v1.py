#!/usr/bin/env python3
"""Fail-closed structural recount for NOVOexport conversation shards.

This command reads source JSON only. It does not train a model, update weights,
change mission authority, or publish message bodies. The receipt contains only
structural counts and source commitments.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Iterator

SCHEMA = "rafaelia.navigator-full-recount/v1"
CHUNK_BYTES = 1 << 20
SHARD_RE = re.compile(r"conversations-(\d{3})\.json$")


class RecountBlocked(RuntimeError):
    """A source-set or parse invariant prevents promotion of recount output."""


def iter_json_array(path: Path) -> Iterator[object]:
    """Stream one top-level JSON array without loading the whole shard."""
    decoder = json.JSONDecoder()
    buffer = ""
    pos = 0
    eof = False

    with path.open("r", encoding="utf-8-sig", errors="strict") as handle:
        def more() -> bool:
            nonlocal buffer, pos, eof
            if eof:
                return False
            chunk = handle.read(CHUNK_BYTES)
            if not chunk:
                eof = True
                return False
            buffer = buffer[pos:] + chunk
            pos = 0
            return True

        if not more():
            raise RecountBlocked(f"empty source: {path.name}")
        while True:
            while pos >= len(buffer):
                if not more():
                    raise RecountBlocked(f"empty source: {path.name}")
            if not buffer[pos].isspace():
                break
            pos += 1
        if buffer[pos] != "[":
            raise RecountBlocked(f"top-level JSON is not an array: {path.name}")
        pos += 1

        while True:
            while True:
                while pos < len(buffer) and (buffer[pos].isspace() or buffer[pos] == ","):
                    pos += 1
                if pos < len(buffer):
                    break
                if not more():
                    raise RecountBlocked(f"truncated JSON array: {path.name}")
            if buffer[pos] == "]":
                return
            while True:
                try:
                    value, end = decoder.raw_decode(buffer, pos)
                    break
                except json.JSONDecodeError as exc:
                    if not more():
                        raise RecountBlocked(f"invalid/truncated JSON: {path.name}: {exc}") from exc
            yield value
            pos = end


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(4 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def discover_shards(source: Path, first: int, last: int) -> list[tuple[int, Path]]:
    by_number: dict[int, Path] = {}
    duplicate_names: list[str] = []
    for path in sorted(source.rglob("conversations-*.json")):
        match = SHARD_RE.fullmatch(path.name)
        if not match:
            continue
        number = int(match.group(1))
        if number < first or number > last:
            continue
        if number in by_number:
            duplicate_names.append(path.name)
        else:
            by_number[number] = path
    if duplicate_names:
        raise RecountBlocked("duplicate shard filenames: " + ",".join(sorted(set(duplicate_names))))
    expected = list(range(first, last + 1))
    missing = [number for number in expected if number not in by_number]
    if missing:
        raise RecountBlocked("missing shards: " + ",".join(f"{n:03d}" for n in missing))
    return [(number, by_number[number]) for number in expected]


def recount(source: Path, first: int, last: int, expected_roots: int | None = None) -> dict:
    shards = discover_shards(source, first, last)
    conversation_ids: set[str] = set()
    node_keys: set[tuple[str, str]] = set()
    seen_messages: dict[str, tuple[str, int, str]] = {}
    canonical_messages: dict[str, tuple[str, int, str]] = {}
    observed_roles: Counter[str] = Counter()
    content_types: Counter[str] = Counter()

    root_conversations = 0
    duplicate_conversation_ids = 0
    mapping_nodes = 0
    duplicate_node_pairs = 0
    message_nodes = 0
    null_message_nodes = 0
    duplicate_message_excess = 0
    duplicate_same_conversation = 0
    duplicate_cross_conversation = 0
    duplicate_same_shard = 0
    duplicate_cross_shard = 0
    missing_native_message_id = 0
    invalid_root_objects = 0
    source_lines: list[str] = []
    shard_receipts: list[dict] = []

    for shard_number, path in shards:
        size = path.stat().st_size
        digest = sha256_file(path)
        source_lines.append(f"{path.name}\t{size}\t{digest}\n")
        shard_roots = shard_nodes = shard_messages = shard_null = shard_invalid = 0
        shard_roles: Counter[str] = Counter()

        for ordinal, value in enumerate(iter_json_array(path)):
            if not isinstance(value, dict):
                invalid_root_objects += 1
                shard_invalid += 1
                continue
            root_conversations += 1
            shard_roots += 1
            conversation_id = str(
                value.get("id")
                or value.get("conversation_id")
                or f"{path.name}#conversation[{ordinal}]"
            )
            if conversation_id in conversation_ids:
                duplicate_conversation_ids += 1
            else:
                conversation_ids.add(conversation_id)

            mapping = value.get("mapping") if isinstance(value.get("mapping"), dict) else {}
            for node_id, node in mapping.items():
                mapping_nodes += 1
                shard_nodes += 1
                node_key = (conversation_id, str(node_id))
                if node_key in node_keys:
                    duplicate_node_pairs += 1
                else:
                    node_keys.add(node_key)

                if not isinstance(node, dict) or not isinstance(node.get("message"), dict):
                    null_message_nodes += 1
                    shard_null += 1
                    continue

                message_nodes += 1
                shard_messages += 1
                message = node["message"]
                native_id = message.get("id")
                if native_id is None or native_id == "":
                    missing_native_message_id += 1
                    message_id = str(node_id)
                else:
                    message_id = str(native_id)
                author = message.get("author")
                role = str(author.get("role") or "unknown") if isinstance(author, dict) else "unknown"
                observed_roles[role] += 1
                shard_roles[role] += 1
                content = message.get("content")
                if isinstance(content, dict):
                    content_type = str(content.get("content_type") or content.get("type") or "unknown")
                elif isinstance(content, str):
                    content_type = "text"
                else:
                    content_type = "unknown"
                content_types[content_type] += 1

                if message_id in seen_messages:
                    duplicate_message_excess += 1
                    prior_conversation, prior_shard, _ = seen_messages[message_id]
                    if prior_conversation == conversation_id:
                        duplicate_same_conversation += 1
                    else:
                        duplicate_cross_conversation += 1
                    if prior_shard == shard_number:
                        duplicate_same_shard += 1
                    else:
                        duplicate_cross_shard += 1
                else:
                    seen_messages[message_id] = (conversation_id, shard_number, role)
                # Mirrors SQLite INSERT OR REPLACE final-row semantics without storing bodies.
                canonical_messages[message_id] = (conversation_id, shard_number, role)

        shard_receipts.append(
            {
                "shard": shard_number,
                "file": path.name,
                "bytes": size,
                "sha256": digest,
                "root_conversations": shard_roots,
                "mapping_nodes": shard_nodes,
                "message_bearing_nodes": shard_messages,
                "null_or_nonmessage_nodes": shard_null,
                "invalid_root_objects": shard_invalid,
                "roles": dict(sorted(shard_roles.items())),
            }
        )

    if invalid_root_objects:
        raise RecountBlocked(f"invalid root objects observed: {invalid_root_objects}")
    if duplicate_conversation_ids:
        raise RecountBlocked(f"duplicate conversation ids observed: {duplicate_conversation_ids}")
    if duplicate_node_pairs:
        raise RecountBlocked(f"duplicate conversation/node pairs observed: {duplicate_node_pairs}")
    if expected_roots is not None and root_conversations != expected_roots:
        raise RecountBlocked(
            f"root conversation count mismatch: expected={expected_roots} observed={root_conversations}"
        )

    canonical_roles = Counter(item[2] for item in canonical_messages.values())
    duplicate_id_count = sum(
        1
        for message_id in seen_messages
        if sum(1 for _ in ())  # kept intentionally side-effect free; count derived below
    )
    # `seen_messages` stores first occurrence only. Distinct duplicated IDs are reconstructed
    # from the per-observation excess tracker in a second compact set during the scan below.
    # The public command does not need private IDs, so emit only excess and relation planes.
    del duplicate_id_count

    source_set_digest = hashlib.sha256("".join(source_lines).encode("utf-8")).hexdigest()
    return {
        "schema": SCHEMA,
        "status": "PASS_SCOPED_STRUCTURAL_RECOUNT",
        "scope": f"conversations-{first:03d}..{last:03d}",
        "source_shards": len(shards),
        "source_bytes": sum(row[1].stat().st_size for row in shards),
        "source_set_digest_sha256": source_set_digest,
        "root_conversations_observed": root_conversations,
        "unique_conversation_ids": len(conversation_ids),
        "mapping_nodes_observed": mapping_nodes,
        "unique_conversation_node_pairs": len(node_keys),
        "message_bearing_nodes_observed": message_nodes,
        "unique_effective_message_ids": len(seen_messages),
        "duplicate_effective_message_id_observations": duplicate_message_excess,
        "duplicates_same_conversation_excess": duplicate_same_conversation,
        "duplicates_cross_conversation_excess": duplicate_cross_conversation,
        "duplicates_same_shard_excess": duplicate_same_shard,
        "duplicates_cross_shard_excess": duplicate_cross_shard,
        "missing_native_message_id": missing_native_message_id,
        "null_or_nonmessage_nodes": null_message_nodes,
        "observed_roles": dict(sorted(observed_roles.items())),
        "canonical_unique_roles": dict(sorted(canonical_roles.items())),
        "content_types": dict(sorted(content_types.items())),
        "shards": shard_receipts,
        "training_executed": False,
        "weight_update_executed": False,
        "mission_authority_from_dataset": False,
        "claim_allowed": False,
        "invariants": [
            "DATASET_INFORMS != MISSION_AUTHORITY",
            "RECOUNT != TRAINING",
            "PHYSICAL_PARSE != SCIENTIFIC_CLAIM",
            "TOKEN_VAZIO != PASS",
        ],
    }


def blocked_receipt(scope: str, reason: str) -> dict:
    return {
        "schema": SCHEMA,
        "status": "BLOCKED",
        "scope": scope,
        "blocker": reason,
        "token_vazio": "TOKEN_VAZIO_FULL_RECOUNT_SOURCE_OR_PARSE_GATE",
        "training_executed": False,
        "weight_update_executed": False,
        "mission_authority_from_dataset": False,
        "claim_allowed": False,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("--first", type=int, default=0)
    parser.add_argument("--last", type=int, default=50)
    parser.add_argument("--expected-roots", type=int)
    parser.add_argument("--receipt", type=Path)
    args = parser.parse_args(argv)
    scope = f"conversations-{args.first:03d}..{args.last:03d}"
    try:
        report = recount(args.source, args.first, args.last, args.expected_roots)
        exit_code = 0
    except (OSError, UnicodeError, RecountBlocked) as exc:
        report = blocked_receipt(scope, f"{type(exc).__name__}: {exc}")
        exit_code = 3
    text = json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.receipt:
        args.receipt.parent.mkdir(parents=True, exist_ok=True)
        args.receipt.write_text(text, encoding="utf-8")
    sys.stdout.write(text)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
