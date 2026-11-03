#!/usr/bin/env python3
"""Deterministic, source-only Omega typed-gap and cross-provider boundary gate.

Validates structure, not provider connectivity, credentials, scientific claims,
historical authenticity, or end-to-end execution. No network/dependencies.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

SENTINEL = re.compile(r"^TOKEN_VAZIO(?:[:_][A-Z0-9_]+)*$")
SHA40 = re.compile(r"^[0-9a-f]{40}$")
NODE_KINDS = {"MAPA", "EXECUTOR", "DATABASE", "MEMORY", "SCIENTIFIC_PRODUCER", "ATLAS", "SHARD", "LEADER"}
EDGE_STATES = {"PROPOSED", "BLOCKED", "VERIFIED_SCOPED"}
GAP_STATES = {"TOKEN_VAZIO", "BLOCKED", "READY_TO_TEST", "EVIDENCED_SCOPED", "RESOLVED_NEGATIVE"}
GAP_TYPES = {"PROVIDER_AUTHORITY", "RUNTIME", "PRIVACY", "EVIDENCE", "HISTORY", "CI", "ROUTE", "RESTORE", "INTEGRITY"}
EVIDENCE_TYPES = {"SOURCE", "ARTIFACT", "EXECUTION", "EVIDENCE", "CLAIM"}
RELATIONS = {"ROUTES_TO", "EXECUTES_FOR", "REQUESTS_MIGRATION", "RECORDS_RECEIPT", "CONSUMES_CONTRACT", "INDEXES", "CUSTODY_LINK"}


def literal_kind(value: object, *, present: bool = True) -> str:
    """No semantic inference: distinguish literal absence, zero and glyphs."""
    if not present:
        return "ABSENT"
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "BOOLEAN_FALSE" if value is False else "BOOLEAN_TRUE"
    if isinstance(value, (int, float)):
        return "NUMBER_ZERO" if value == 0 else "NUMBER"
    if isinstance(value, str):
        if value == "":
            return "EMPTY_STRING"
        if value.isspace():
            return "WHITESPACE_ONLY"
        if SENTINEL.fullmatch(value):
            return "TOKEN_VAZIO_TYPED"
        if value in {"∅", "空", "𓂀", "♾️"}:
            return "SYMBOLIC_LITERAL_UNVERIFIED"
        return "TEXT_UNVERIFIED"
    return "UNSUPPORTED_LITERAL"


def validate(doc: object) -> list[str]:
    errors: list[str] = []
    if not isinstance(doc, dict):
        return ["root: expected object"]
    if doc.get("schema") != "rafaelia.omega-cross-provider-gap.v1":
        errors.append("schema mismatch")
    if doc.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false in this non-runtime route")
    if doc.get("federated_authority") != "rafaelmeloreisnovo/Mapa":
        errors.append("Mapa federated authority mismatch")
    if doc.get("executor") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("RafGitTools executor mismatch")
    if doc.get("data_transfer") != "METADATA_POINTERS_ONLY":
        errors.append("cross-provider raw data transfer forbidden")
    nodes = doc.get("nodes")
    edges = doc.get("edges")
    gaps = doc.get("gaps")
    samples = doc.get("literal_cases")
    symbols = doc.get("symbolic_labels")
    if not isinstance(nodes, list) or not nodes:
        return errors + ["nodes must be nonempty array"]
    if not isinstance(edges, list) or not edges:
        return errors + ["edges must be nonempty array"]
    if not isinstance(gaps, list) or not gaps:
        return errors + ["gaps must be nonempty array"]
    if not isinstance(samples, list) or not samples:
        return errors + ["literal_cases must be nonempty array"]
    if not isinstance(symbols, list):
        return errors + ["symbolic_labels must be array"]
    node_ids: set[str] = set()
    for i, node in enumerate(nodes):
        if not isinstance(node, dict):
            errors.append(f"nodes[{i}] must be object")
            continue
        nid = node.get("id")
        if not isinstance(nid, str) or not nid.strip() or nid in node_ids:
            errors.append(f"nodes[{i}] id invalid or duplicated")
        else:
            node_ids.add(nid)
        if node.get("kind") not in NODE_KINDS:
            errors.append(f"nodes[{i}] kind invalid")
        if not isinstance(node.get("authority"), str) or not node["authority"].strip():
            errors.append(f"nodes[{i}] authority required")
        if not isinstance(node.get("source_ref"), str) or not node["source_ref"].strip():
            errors.append(f"nodes[{i}] source_ref required")
    edge_ids: set[str] = set()
    for i, edge in enumerate(edges):
        if not isinstance(edge, dict):
            errors.append(f"edges[{i}] must be object")
            continue
        eid = edge.get("id")
        if not isinstance(eid, str) or not eid.strip() or eid in edge_ids:
            errors.append(f"edges[{i}] id invalid or duplicated")
        else:
            edge_ids.add(eid)
        if edge.get("from") not in node_ids or edge.get("to") not in node_ids:
            errors.append(f"edges[{i}] missing graph node")
        if edge.get("relation") not in RELATIONS:
            errors.append(f"edges[{i}] relation invalid")
        state = edge.get("state")
        if state not in EDGE_STATES:
            errors.append(f"edges[{i}] state invalid")
        if edge.get("claim_allowed") is not False:
            errors.append(f"edges[{i}] claim must be false")
        if state == "VERIFIED_SCOPED":
            for key in ("producer_receipt", "consumer_receipt", "producer_sha", "consumer_sha"):
                value = edge.get(key)
                if key.endswith("_sha"):
                    if not isinstance(value, str) or not SHA40.fullmatch(value):
                        errors.append(f"edges[{i}].{key} exact SHA required")
                elif not isinstance(value, str) or not value.strip() or SENTINEL.fullmatch(value):
                    errors.append(f"edges[{i}].{key} concrete receipt required")
        else:
            for key in ("producer_receipt", "consumer_receipt"):
                value = edge.get(key)
                if not isinstance(value, str) or not value.strip():
                    errors.append(f"edges[{i}].{key} typed pointer required")
    gap_ids: set[str] = set()
    for i, gap in enumerate(gaps):
        if not isinstance(gap, dict):
            errors.append(f"gaps[{i}] must be object")
            continue
        gid = gap.get("id")
        if not isinstance(gid, str) or not gid.strip() or gid in gap_ids:
            errors.append(f"gaps[{i}] id invalid or duplicated")
        else:
            gap_ids.add(gid)
        if gap.get("state") not in GAP_STATES or gap.get("gap_type") not in GAP_TYPES:
            errors.append(f"gaps[{i}] invalid gap type/state")
        if gap.get("claim_allowed") is not False:
            errors.append(f"gaps[{i}] no promotion allowed")
        for field in ("authority", "reason", "next_gate", "stop_condition", "source_ref"):
            value = gap.get(field)
            if not isinstance(value, str) or not value.strip():
                errors.append(f"gaps[{i}].{field} required")
        if gap.get("state") in {"EVIDENCED_SCOPED", "RESOLVED_NEGATIVE"}:
            ev = gap.get("evidence_ref")
            if not isinstance(ev, str) or not ev.strip() or SENTINEL.fullmatch(ev):
                errors.append(f"gaps[{i}].evidence_ref concrete for evidenced state")
        elif "evidence_ref" not in gap:
            errors.append(f"gaps[{i}] typed evidence_ref missing")
    case_ids: set[str] = set()
    for i, sample in enumerate(samples):
        if not isinstance(sample, dict) or not isinstance(sample.get("id"), str):
            errors.append(f"literal_cases[{i}] invalid")
            continue
        if sample["id"] in case_ids:
            errors.append(f"literal_cases[{i}] duplicate id")
        case_ids.add(sample["id"])
        kind = literal_kind(sample.get("value"), present=sample.get("present", True))
        if kind != sample.get("expected_kind"):
            errors.append(f"literal_cases[{i}] expected {sample.get('expected_kind')!r}, got {kind}")
        if sample.get("evidence_kind") not in EVIDENCE_TYPES:
            errors.append(f"literal_cases[{i}] evidence-kind boundary missing")
        if sample.get("claim_allowed") is not False:
            errors.append(f"literal_cases[{i}] symbolic/literal cannot authorize claim")
    seen_symbols: set[str] = set()
    for i, symbol in enumerate(symbols):
        if not isinstance(symbol, dict):
            errors.append(f"symbolic_labels[{i}] invalid")
            continue
        sid = symbol.get("id")
        if sid in seen_symbols:
            errors.append(f"symbolic_labels[{i}] duplicate")
        seen_symbols.add(sid)
        if symbol.get("kind") != "USER_SYMBOLIC_TEST_VECTOR" or symbol.get("proof") != "NOT_EVIDENCE":
            errors.append(f"symbolic_labels[{i}] must not assert provenance or proof")
    if seen_symbols != {f"Ω{i:02d}" for i in range(16)}:
        errors.append("symbolic_labels must preserve exactly Ω00..Ω15")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    try:
        errors = validate(json.loads(args.manifest.read_text(encoding="utf-8")))
    except (ValueError, OSError) as exc:
        errors = [str(exc)]
    print(json.dumps({"gate": "SOURCE_ONLY_STRUCTURAL", "state": "PASS_SCOPED" if not errors else "FAIL",
                      "claim_promotion_performed": False, "errors": errors},
                     ensure_ascii=False, sort_keys=True))
    return 0 if not errors else 2


if __name__ == "__main__":
    raise SystemExit(main())
