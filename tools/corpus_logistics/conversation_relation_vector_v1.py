#!/usr/bin/env python3
"""Build governed interaction-relation vectors and adjacent micro-deltas.

Input: rafgittools.corpus-logistics-gymnasia.v1 plan.
Output contains no raw conversation body. It derives only deterministic
structural/lexical facts from chunk metadata, token refs and typed edges.

Important boundary:
LEXICAL_OVERLAP != SEMANTIC_EQUIVALENCE != CAUSALITY != TRUTH != CLAIM.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

INPUT_SCHEMA = "rafgittools.corpus-logistics-gymnasia.v1"
OUTPUT_SCHEMA = "rafgittools.conversation-relation-vector.v1"


def canonical(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode()


def sha(value: object) -> str:
    return hashlib.sha256(canonical(value)).hexdigest()


def token_ids(chunk: dict) -> set[str]:
    return {str(x.get("token_id")) for x in (chunk.get("token_refs") or []) if x.get("token_id")}


def jaccard(a: set[str], b: set[str]) -> float:
    u = a | b
    return 1.0 if not u else len(a & b) / len(u)


def build(plan: dict) -> dict:
    if plan.get("schema") != INPUT_SCHEMA:
        raise ValueError("input schema mismatch")
    if plan.get("raw_body_embedded") is not False:
        raise ValueError("raw bodies forbidden")
    if plan.get("claim_allowed") is not False:
        raise ValueError("claim_allowed must be false")

    chunks = list(plan.get("chunks") or [])
    edges = list(plan.get("edges") or [])
    by_id = {str(c["chunk_id"]): c for c in chunks}
    if len(by_id) != len(chunks):
        raise ValueError("duplicate chunk_id")

    outgoing: dict[str, list[dict]] = {cid: [] for cid in by_id}
    incoming: dict[str, list[dict]] = {cid: [] for cid in by_id}
    for edge in edges:
        source = str(edge.get("from") or "")
        target = str(edge.get("to") or "")
        if source not in by_id or target not in by_id:
            raise ValueError(f"edge references unknown chunk: {source}->{target}")
        outgoing[source].append(edge)
        incoming[target].append(edge)

    vectors = []
    for cid in sorted(by_id):
        c = by_id[cid]
        tids = sorted(token_ids(c))
        payload = {
            "chunk_id": cid,
            "source_family": c.get("source_family"),
            "book_id": c.get("book_id"),
            "session_id": c.get("session_id"),
            "text_sha256": c.get("text_sha256"),
            "tier": c.get("tier"),
            "materialization_state": c.get("materialization_state"),
            "bytes": int(c.get("bytes") or 0),
            "token_count": int(c.get("token_count") or 0),
            "token_signature_sha256": hashlib.sha256("\n".join(tids).encode("utf-8")).hexdigest(),
            "relations_out": len(outgoing[cid]),
            "relations_in": len(incoming[cid]),
            "next_count": sum(1 for e in outgoing[cid] if e.get("type") == "NEXT"),
            "previous_count": sum(1 for e in outgoing[cid] if e.get("type") == "PREVIOUS"),
            "semantic_embedding_state": "TOKEN_VAZIO",
            "causal_state": "TOKEN_VAZIO",
            "truth_state": "TOKEN_VAZIO",
            "claim_allowed": False,
        }
        payload["vector_id"] = "VEC-" + sha(payload)[:32]
        vectors.append(payload)

    microdeltas = []
    for edge in edges:
        if edge.get("type") != "NEXT":
            continue
        a = by_id[str(edge["from"])]
        b = by_id[str(edge["to"])]
        ta, tb = token_ids(a), token_ids(b)
        overlap = jaccard(ta, tb)
        delta = {
            "from_chunk_id": a["chunk_id"],
            "to_chunk_id": b["chunk_id"],
            "relation_type": "NEXT",
            "token_jaccard": round(overlap, 12),
            "lexical_divergence": round(1.0 - overlap, 12),
            "token_added": len(tb - ta),
            "token_removed": len(ta - tb),
            "token_count_delta": int(b.get("token_count") or 0) - int(a.get("token_count") or 0),
            "bytes_delta": int(b.get("bytes") or 0) - int(a.get("bytes") or 0),
            "convergence_state": "OBSERVED_LEXICAL_OVERLAP_ONLY",
            "novelty_state": "OBSERVED_TOKEN_SET_DELTA",
            "semantic_state": "TOKEN_VAZIO",
            "causal_state": "TOKEN_VAZIO",
            "evidence_state": "DERIVED_FROM_GOVERNED_CHUNK_METADATA",
            "claim_allowed": False,
        }
        delta["microdelta_id"] = "MDELTA-" + sha(delta)[:32]
        microdeltas.append(delta)

    body = {
        "schema": OUTPUT_SCHEMA,
        "source_schema": INPUT_SCHEMA,
        "source_manifest_sha256": plan.get("manifest_sha256"),
        "raw_body_embedded": False,
        "claim_allowed": False,
        "vectors": vectors,
        "microdeltas": microdeltas,
        "boundaries": [
            "LEXICAL_OVERLAP!=SEMANTIC_EQUIVALENCE",
            "CONVERGENCE!=TRUTH",
            "DIVERGENCE!=ERROR",
            "COOCCURRENCE!=CAUSALITY",
            "SOURCE!=ARTIFACT!=EXECUTION!=EVIDENCE!=CLAIM",
            "TOKEN_VAZIO!=0",
        ],
    }
    body["manifest_sha256"] = sha(body)
    return body


def write(source: Path, output: Path) -> dict:
    if output.exists() and output.stat().st_size:
        raise RuntimeError("output must be new or empty")
    plan = json.loads(source.read_text(encoding="utf-8"))
    result = build(plan)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return result


def selftest() -> int:
    base = {
        "schema": INPUT_SCHEMA,
        "raw_body_embedded": False,
        "claim_allowed": False,
        "manifest_sha256": "fixture",
        "chunks": [
            {
                "chunk_id": "CHK-A", "source_family": "CONVERSATIONS", "book_id": "B", "session_id": "S",
                "text_sha256": "a", "tier": "HOT", "materialization_state": "MATERIALIZED",
                "bytes": 10, "token_count": 2,
                "token_refs": [{"token_id": "TOK-X"}, {"token_id": "TOK-Y"}],
            },
            {
                "chunk_id": "CHK-B", "source_family": "CONVERSATIONS", "book_id": "B", "session_id": "S",
                "text_sha256": "b", "tier": "HOT", "materialization_state": "MATERIALIZED",
                "bytes": 12, "token_count": 2,
                "token_refs": [{"token_id": "TOK-Y"}, {"token_id": "TOK-Z"}],
            },
        ],
        "edges": [
            {"type": "NEXT", "from": "CHK-A", "to": "CHK-B"},
            {"type": "PREVIOUS", "from": "CHK-B", "to": "CHK-A"},
        ],
    }
    a = build(base)
    b = build(base)
    assert a == b
    assert a["raw_body_embedded"] is False
    assert a["claim_allowed"] is False
    assert len(a["vectors"]) == 2
    assert len(a["microdeltas"]) == 1
    md = a["microdeltas"][0]
    assert md["token_jaccard"] == round(1 / 3, 12)
    assert md["token_added"] == 1 and md["token_removed"] == 1
    assert md["semantic_state"] == "TOKEN_VAZIO"
    print("CONVERSATION_RELATION_VECTOR_V1_SELFTEST_PASS")
    return 0


def main() -> int:
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    b = sub.add_parser("build")
    b.add_argument("plan", type=Path)
    b.add_argument("output", type=Path)
    sub.add_parser("selftest")
    args = p.parse_args()
    if args.cmd == "selftest":
        return selftest()
    result = write(args.plan, args.output)
    print(json.dumps({
        "manifest_sha256": result["manifest_sha256"],
        "vectors": len(result["vectors"]),
        "microdeltas": len(result["microdeltas"]),
        "claim_allowed": result["claim_allowed"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
