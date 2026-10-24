#!/usr/bin/env python3
"""Private NOVOexport interaction genealogy and lexical tokenizer.

Source: original conversations-NNN.json; streamed with the existing full-recount
parser. Source files are read-only, output directories are exclusive-create.
Never publish generated JSONL: it contains raw private prompt/reply text.
No LLM token IDs, training, gradients or model-weight changes are claimed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys

from rafaelia_navigator_full_recount_v1 import iter_json_array, sha256_file

SCHEMA = "rafaelia.interaction-token-ledger/v1"
SHARD = re.compile(r"conversations-(\d{3})\.json\Z")
LEXICAL = re.compile(r"\w+|[^\w\s]", re.UNICODE)


class Blocked(ValueError):
    pass


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False,
                      separators=(",", ":"), allow_nan=False).encode("utf-8")


def hash_object(value):
    return hashlib.sha256(canonical(value)).hexdigest()


def payload(message):
    value = message.get("content")
    if isinstance(value, dict):
        kind = str(value.get("content_type") or value.get("type") or "unknown")
        if isinstance(value.get("parts"), list):
            parts = value["parts"]
        elif isinstance(value.get("text"), str):
            parts = [value["text"]]
        else:
            parts = [value]
    elif isinstance(value, str):
        kind, parts = "text", [value]
    else:
        kind, parts = "non_text", [value]
    texts = [p for p in parts if isinstance(p, str)]
    opaque = [{"index": i, "type": type(p).__name__,
               "canonical_sha256": hash_object(p),
               "source_ref": "ORIGINAL_SOURCE_POINTER"}
              for i, p in enumerate(parts) if not isinstance(p, str)]
    return kind, texts, opaque, hash_object(value)


def tokens_of(texts, emit_tokens=False):
    count, sha = 0, hashlib.sha256()
    emitted = [] if emit_tokens else None
    for part, text in enumerate(texts):
        for match in LEXICAL.finditer(text):
            surface = match.group(0)
            data = surface.encode("utf-8")
            sha.update(len(data).to_bytes(8, "big"))
            sha.update(data)
            if emit_tokens:
                emitted.append({"part": part, "ordinal": count,
                                "char_start": match.start(),
                                "char_end": match.end(), "surface": surface})
            count += 1
    result = {
        "definition": "PYTHON_RE_UNICODE_WORD_OR_PUNCT_V1",
        "regex": r"\w+|[^\w\s]",
        "lexical_count": count,
        "lexical_sequence_sha256": sha.hexdigest(),
        "utf8_bytes": sum(len(t.encode("utf-8")) for t in texts),
        "unicode_codepoints": sum(len(t) for t in texts),
        "model_token_count": "TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED",
        "model_token_ids": "TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED",
    }
    if emit_tokens:
        result["lexical_tokens"] = emitted
    return result


def user_origin(mapping, node_id, memo):
    """Find nearest user ancestor, not merely previous chronological message.

    Missing parents/cycles are typed. Traversal is iterative, avoids stack
    depth issues in long threads and preserves user/assistant branch forks.
    """
    if node_id in memo:
        return memo[node_id]
    current, trail, visited = node_id, [], set()
    resolved = (None, "PRE_USER_CONTEXT")
    while True:
        if current in memo:
            resolved = memo[current]
            break
        if current in visited:
            resolved = (None, "TOPOLOGY_CYCLE")
            break
        visited.add(current)
        node = mapping.get(current)
        if not isinstance(node, dict):
            resolved = (None, "PARENT_MISSING")
            break
        trail.append(current)
        msg = node.get("message")
        author = msg.get("author") if isinstance(msg, dict) else None
        if isinstance(author, dict) and author.get("role") == "user":
            resolved = (current, None)
            break
        if node.get("parent") is None:
            resolved = (None, "PRE_USER_CONTEXT")
            break
        current = str(node["parent"])
    for nid in reversed(trail):
        msg = mapping[nid].get("message")
        author = msg.get("author") if isinstance(msg, dict) else None
        if isinstance(author, dict) and author.get("role") == "user":
            resolved = (nid, None)
        memo[nid] = resolved
    return memo.get(node_id, resolved)


def row_for(shard, index, conversation_id, mapping, nid, node, memo, emit_tokens):
    identity = {"source_shard": shard.name, "conversation_ordinal": index,
                "conversation_id": conversation_id, "node_id": nid}
    user_nid, gap = user_origin(mapping, nid, memo)
    interaction_id = (hash_object({**identity, "node_id": user_nid})
                      if user_nid is not None else None)
    message = node.get("message")
    role, channel, author_name = ("TOKEN_VAZIO_NO_MESSAGE",
                                  "TOKEN_VAZIO_CHANNEL_UNSPECIFIED",
                                  "TOKEN_VAZIO_AUTHOR_UNSPECIFIED")
    content, metadata, mid = None, None, None
    if isinstance(message, dict):
        mid = message.get("id")
        author = message.get("author")
        if isinstance(author, dict):
            role = str(author.get("role") or "unknown")
            channel = str(author.get("channel") or channel)
            author_name = str(author.get("name") or author_name)
        kind, texts, opaque, obj_hash = payload(message)
        content = {"content_type": kind, "original_content_canonical_sha256": obj_hash,
                   "text_parts": texts, "opaque_part_descriptors": opaque,
                   "lexical_tokenization": tokens_of(texts, emit_tokens)}
        exported_metadata = message.get("metadata")
        exported_metadata = exported_metadata if isinstance(exported_metadata, dict) else {}
        model_hint = exported_metadata.get("model_slug")
        metadata = {"create_time": message.get("create_time"),
                    "end_turn": message.get("end_turn"),
                    "status": message.get("status"),
                    "recipient": message.get("recipient"),
                    "model_slug_from_export": (
                        model_hint if isinstance(model_hint, str) and model_hint
                        else "TOKEN_VAZIO_MODEL_SLUG_NOT_OBSERVED"),
                    "model_slug_is_tokenizer_proof": False}
    source_pointer = (f"{shard.name}#conversation[{index}]"
                      f".mapping[{json.dumps(nid, ensure_ascii=False)}]")
    return {"schema": SCHEMA, "scope": identity,
            "stable_node_key_sha256": hash_object(identity),
            "native_message_id": mid, "parent_node_id": node.get("parent"),
            "child_node_ids": node.get("children", []),
            "role": role, "channel": channel, "author_name": author_name,
            "message_metadata": metadata, "content": content,
            "interaction_id": interaction_id,
            "interaction_root_user_node_id": user_nid,
            "topology_gap": gap,
            "source_pointer": source_pointer,
            "privacy_class": "PRIVATE_DEFAULT_DENY",
            "epistemic_state": ("SOURCE_OBSERVED" if isinstance(message, dict)
                                else "STRUCTURAL_NODE"),
            "claim_allowed": False}


def emit_shard(source: Path, output: Path, *, emit_tokens=False):
    if not SHARD.fullmatch(source.name) or not source.is_file():
        raise Blocked("source must be a conversations-NNN.json file")
    if output.exists():
        raise Blocked("output directory already exists; no overwrites")
    # Force private output permissions on local filesystem; no corpus upload.
    output.mkdir(parents=True, mode=0o700)
    output.chmod(0o700)
    payload_path = output / "INTERACTIONS_PRIVATE.jsonl"
    turns_path = output / "INTERACTION_INDEX_PRIVATE.jsonl"
    receipt_path = output / "RECEIPT.json"
    before = sha256_file(source)
    counts = {"conversations": 0, "mapping_nodes": 0, "user_nodes": 0,
              "assistant_nodes": 0, "other_message_nodes": 0,
              "empty_nodes": 0, "interactions_anchored": 0,
              "topology_gaps": 0, "lexical_tokens": 0}
    user_roots = set()
    with payload_path.open("x", encoding="utf-8", newline="\n") as out, \
         turns_path.open("x", encoding="utf-8", newline="\n") as turns_out:
        payload_path.chmod(0o600)
        turns_path.chmod(0o600)
        for ci, convo in enumerate(iter_json_array(source)):
            if not isinstance(convo, dict):
                raise Blocked(f"conversation[{ci}] is not an object")
            mapping = convo.get("mapping")
            if not isinstance(mapping, dict):
                raise Blocked(f"conversation[{ci}] has no mapping")
            cid = str(convo.get("id") or convo.get("conversation_id")
                      or "TOKEN_VAZIO_CONVERSATION_ID")
            counts["conversations"] += 1
            memo = {}
            grouped = {}
            for nid, node in mapping.items():
                if not isinstance(node, dict):
                    raise Blocked(f"conversation[{ci}] invalid node {nid}")
                nid = str(nid)
                row = row_for(source, ci, cid, mapping, nid, node, memo, emit_tokens)
                counts["mapping_nodes"] += 1
                if row["content"] is None:
                    counts["empty_nodes"] += 1
                elif row["role"] == "user":
                    counts["user_nodes"] += 1
                elif row["role"] == "assistant":
                    counts["assistant_nodes"] += 1
                else:
                    counts["other_message_nodes"] += 1
                if row["interaction_id"] is not None:
                    user_roots.add((ci, row["interaction_root_user_node_id"]))
                if row["topology_gap"] not in (None, "PRE_USER_CONTEXT"):
                    counts["topology_gaps"] += 1
                if row["content"] is not None:
                    counts["lexical_tokens"] += row["content"]["lexical_tokenization"]["lexical_count"]
                out.write(json.dumps(row, ensure_ascii=False, sort_keys=True,
                                     separators=(",", ":")) + "\n")
                # A per-interaction view has pointers, not duplicated private text.
                iid = row["interaction_id"]
                if iid is not None:
                    entry = grouped.setdefault(iid, {
                        "schema": "rafaelia.interaction-turn-index/v1",
                        "interaction_id": iid, "conversation_id": cid,
                        "source_shard": source.name, "conversation_ordinal": ci,
                        "prompt_source_pointer": None,
                        "assistant_source_pointers": [],
                        "tool_or_other_source_pointers": [],
                        "role_counts": {}, "lexical_tokens_by_role": {},
                        "model_token_count": "TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED",
                        "exported_model_slugs": [],
                        "privacy_class": "PRIVATE_DEFAULT_DENY",
                        "claim_allowed": False,
                    })
                    entry["role_counts"][row["role"]] = entry["role_counts"].get(row["role"], 0) + 1
                    count_here = (row["content"]["lexical_tokenization"]["lexical_count"]
                                  if row["content"] is not None else 0)
                    entry["lexical_tokens_by_role"][row["role"]] = (
                        entry["lexical_tokens_by_role"].get(row["role"], 0) + count_here)
                    if row["role"] == "user":
                        entry["prompt_source_pointer"] = row["source_pointer"]
                    elif row["role"] == "assistant":
                        entry["assistant_source_pointers"].append(row["source_pointer"])
                        slug = row["message_metadata"]["model_slug_from_export"]
                        if slug not in entry["exported_model_slugs"]:
                            entry["exported_model_slugs"].append(slug)
                    else:
                        entry["tool_or_other_source_pointers"].append(row["source_pointer"])
            for entry in grouped.values():
                turns_out.write(json.dumps(entry, ensure_ascii=False, sort_keys=True,
                                           separators=(",", ":")) + "\n")
    after = sha256_file(source)
    if after != before:
        raise Blocked("source file changed during processing, receipt withheld")
    counts["interactions_anchored"] = len(user_roots)
    report = {
        "schema": SCHEMA, "status": (
            "PASS_SCOPED_STRUCTURAL_NOT_MODEL_TOKENS" if not counts["topology_gaps"]
            else "PARTIAL_TOPOLOGY_GAPS_NOT_PROMOTABLE"),
        "source_file": source.name, "source_sha256_before": before,
        "source_sha256_after": after, "source_size_bytes": source.stat().st_size,
        "output_file": payload_path.name, "output_sha256": sha256_file(payload_path),
        "interaction_index_file": turns_path.name,
        "interaction_index_sha256": sha256_file(turns_path),
        "counts": counts,
        "gaps": ["TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED",
                 "TOKEN_VAZIO_PHYSICAL_EXECUTION_NOT_OBSERVED"],
        "private_output": True, "training_executed": False,
        "model_token_count_verified": False, "claim_allowed": False,
    }
    if counts["topology_gaps"]:
        report["gaps"].append("TOKEN_VAZIO_PARENT_OR_CYCLE")
    with receipt_path.open("x", encoding="utf-8") as out:
        receipt_path.chmod(0o600)
        out.write(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_root", type=Path)
    parser.add_argument("output_root", type=Path)
    parser.add_argument("--first", type=int, default=0)
    parser.add_argument("--last", type=int, default=50)
    parser.add_argument("--max-shards", type=int)
    parser.add_argument("--emit-lexical-tokens", action="store_true")
    args = parser.parse_args()
    if args.first < 0 or args.last < args.first or args.last > 999:
        parser.error("invalid shard range")
    if args.max_shards is not None and args.max_shards < 1:
        parser.error("max-shards must be positive")
    if not args.source_root.is_dir():
        parser.error("source root missing")
    if args.source_root.resolve() == args.output_root.resolve():
        parser.error("source and output are identical")
    if args.output_root.resolve().is_relative_to(args.source_root.resolve()):
        parser.error("output cannot be beneath source root")
    work = list(range(args.first, args.last + 1))
    if args.max_shards is not None:
        work = work[:args.max_shards]
    try:
        for number in work:
            matches = sorted(args.source_root.rglob(f"conversations-{number:03d}.json"))
            if len(matches) != 1:
                raise Blocked(f"{number:03d}: expected exactly one source, found {len(matches)}")
            result = emit_shard(matches[0], args.output_root / f"SHARD_{number:03d}",
                                emit_tokens=args.emit_lexical_tokens)
            print(json.dumps({"shard": number, "status": result["status"],
                              "counts": result["counts"]}, sort_keys=True))
    except (Blocked, OSError, ValueError) as exc:
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
