#!/usr/bin/env python3
"""Validate the RAFAELIA knowledge antiderivative profile.

This module is deliberately stdlib-only and bounded.  It validates typed empty
states, rejects copied secrets, and executes finite mathematical checks for a
hexagonal torus, global flow conservation, circular-shift energy, canonical
permutation digests and single-vertex connectivity.

Finite checks are evidence for the declared domain only.  They are not proof of
a physical universe, universal reconstruction, quantum behavior or scientific
truth.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import deque
from pathlib import Path
from typing import Any, Iterable

PROFILE_ID = "RAFAELIA-KNOWLEDGE-ANTIDERIVATIVE-1"
SCHEMA_VERSION = "1.0.0"
EMPTY_STATES = {
    "NOT_EXAMINED",
    "TOKEN_VAZIO",
    "OPEN_GAP",
    "BLOCKED",
    "CONTRADICTION",
    "CLOSED",
    "NOT_APPLICABLE",
}
UNRESOLVED_STATES = {
    "NOT_EXAMINED",
    "TOKEN_VAZIO",
    "OPEN_GAP",
    "BLOCKED",
    "CONTRADICTION",
}
REQUIRED_UNRESOLVED = {"reason", "owner", "next_action", "exit_criteria"}
SECRET_PATTERNS = (
    re.compile(r"github_pat_[A-Za-z0-9_]{20,}"),
    re.compile(r"gh[pousr]_[A-Za-z0-9_]{20,}"),
    re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
)
HEX_DIRECTIONS = ((1, 0), (-1, 0), (0, 1), (0, -1), (1, -1), (-1, 1))
POSITIVE_HEX_DIRECTIONS = ((1, 0), (0, 1), (1, -1))


def load_json(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("profile root must be an object")
    return data


def walk_strings(value: Any) -> Iterable[str]:
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for key, item in value.items():
            yield str(key)
            yield from walk_strings(item)
    elif isinstance(value, list):
        for item in value:
            yield from walk_strings(item)


def contains_secret(value: Any) -> bool:
    return any(pattern.search(text) for text in walk_strings(value) for pattern in SECRET_PATTERNS)


def semantic_view(profile: dict[str, Any]) -> dict[str, Any]:
    view = json.loads(json.dumps(profile, ensure_ascii=False))
    view["source_pointers"] = sorted(view.get("source_pointers", []), key=lambda x: x.get("id", ""))
    boundaries = view.get("term_boundaries", {})
    boundaries["observed_terms"] = sorted(boundaries.get("observed_terms", []))
    boundaries["unresolved_terms"] = sorted(boundaries.get("unresolved_terms", []), key=lambda x: x.get("term", ""))
    view["operators"] = sorted(view.get("operators", []), key=lambda x: x.get("id", ""))
    view["repository_routes"] = sorted(view.get("repository_routes", []), key=lambda x: x.get("repository", ""))
    return view


def semantic_digest(profile: dict[str, Any]) -> str:
    payload = json.dumps(
        semantic_view(profile),
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def canonical_records_digest(records: list[dict[str, Any]]) -> str:
    ordered = sorted(records, key=lambda item: item["id"])
    payload = json.dumps(ordered, sort_keys=True, separators=(",", ":")).encode("utf-8") + b"\n"
    return hashlib.sha256(payload).hexdigest()


def hex_torus(width: int, height: int) -> dict[tuple[int, int], set[tuple[int, int]]]:
    if width < 3 or height < 3:
        raise ValueError("hex torus requires width and height >= 3")
    graph: dict[tuple[int, int], set[tuple[int, int]]] = {}
    for q in range(width):
        for r in range(height):
            node = (q, r)
            graph[node] = {
                ((q + dq) % width, (r + dr) % height)
                for dq, dr in HEX_DIRECTIONS
            }
    return graph


def deterministic_flux_divergence(width: int, height: int) -> dict[tuple[int, int], int]:
    divergence = {(q, r): 0 for q in range(width) for r in range(height)}
    for q in range(width):
        for r in range(height):
            source = (q, r)
            for direction_index, (dq, dr) in enumerate(POSITIVE_HEX_DIRECTIONS, start=1):
                target = ((q + dq) % width, (r + dr) % height)
                flow = 1 + ((q * 17 + r * 31 + direction_index * 13) % 97)
                divergence[source] -= flow
                divergence[target] += flow
    return divergence


def circular_shift(values: list[int], amount: int) -> list[int]:
    if not values:
        return []
    offset = amount % len(values)
    return values[-offset:] + values[:-offset] if offset else list(values)


def energy(values: Iterable[int]) -> int:
    return sum(value * value for value in values)


def is_connected(
    graph: dict[tuple[int, int], set[tuple[int, int]]],
    removed: set[tuple[int, int]] | None = None,
) -> bool:
    excluded = removed or set()
    remaining = [node for node in graph if node not in excluded]
    if not remaining:
        return True
    seen = {remaining[0]}
    queue: deque[tuple[int, int]] = deque([remaining[0]])
    while queue:
        node = queue.popleft()
        for neighbor in graph[node]:
            if neighbor not in excluded and neighbor not in seen:
                seen.add(neighbor)
                queue.append(neighbor)
    return len(seen) == len(remaining)


def typed_empty_roundtrip() -> bool:
    token = {
        "state": "TOKEN_VAZIO",
        "reason": "canonical source not found",
        "owner": "rafaelmeloreisnovo",
        "next_action": "locate source",
        "exit_criteria": "source and authority identified",
    }
    encoded = json.dumps(token, sort_keys=True, separators=(",", ":"))
    decoded = json.loads(encoded)
    return decoded == token and not isinstance(decoded["state"], (int, float, bool))


def finite_checks() -> dict[str, Any]:
    width, height = 5, 4
    graph = hex_torus(width, height)
    degree_set = sorted({len(neighbors) for neighbors in graph.values()})
    divergence = deterministic_flux_divergence(width, height)
    global_divergence = sum(divergence.values())

    signal = [3, -1, 4, 1, 5, -9, 2, 6]
    base_energy = energy(signal)
    shifted_energy = energy(circular_shift(signal, 3))

    records = [
        {"id": "b", "state": "TOKEN_VAZIO"},
        {"id": "a", "state": "VERIFIED_LIMITED"},
        {"id": "c", "state": "BLOCKED"},
    ]
    digest_forward = canonical_records_digest(records)
    digest_reverse = canonical_records_digest(list(reversed(records)))

    single_vertex_failures = [
        node for node in graph if not is_connected(graph, {node})
    ]

    checks = {
        "hex_torus": {
            "width": width,
            "height": height,
            "nodes": len(graph),
            "degree_set": degree_set,
            "status": "PASS" if degree_set == [6] else "FAIL",
        },
        "global_flux_conservation": {
            "sum_divergence": global_divergence,
            "status": "PASS" if global_divergence == 0 else "FAIL",
        },
        "circular_shift_energy": {
            "before": base_energy,
            "after": shifted_energy,
            "status": "PASS" if base_energy == shifted_energy else "FAIL",
        },
        "canonical_permutation_digest": {
            "forward_sha256": digest_forward,
            "reverse_sha256": digest_reverse,
            "status": "PASS" if digest_forward == digest_reverse else "FAIL",
        },
        "typed_empty_roundtrip": {
            "status": "PASS" if typed_empty_roundtrip() else "FAIL",
        },
        "single_vertex_removal_connectivity": {
            "tested_removals": len(graph),
            "failures": [list(node) for node in single_vertex_failures],
            "status": "PASS" if not single_vertex_failures else "FAIL",
            "boundary": "finite single-vertex test; not a universal k-removal theorem",
        },
    }
    checks["status"] = "PASS" if all(
        item.get("status") == "PASS"
        for key, item in checks.items()
        if key != "status" and isinstance(item, dict)
    ) else "FAIL"
    return checks


def validate(profile: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if profile.get("schema_version") != SCHEMA_VERSION:
        errors.append(f"schema_version must be {SCHEMA_VERSION}")
    if profile.get("profile_id") != PROFILE_ID:
        errors.append(f"profile_id must be {PROFILE_ID}")
    for field in (
        "claim_allowed",
        "automatic_merge",
        "automatic_cross_repository_write",
        "private_payload_copied",
    ):
        if profile.get(field) is not False:
            errors.append(f"{field} must remain false")

    if contains_secret(profile):
        errors.append("profile contains a credential/private-key pattern")

    security = profile.get("security", {})
    if security.get("secret_value_copied") is not False:
        errors.append("secret_value_copied must remain false")
    if not security.get("required_remediation"):
        errors.append("security.required_remediation must be non-empty")

    outputs = profile.get("drive_outputs", {})
    if outputs.get("storage_mode") != "POINTER_ONLY":
        errors.append("Drive output storage_mode must be POINTER_ONLY")
    for field in ("folder_id", "document_id", "spreadsheet_id"):
        if not isinstance(outputs.get(field), str) or len(outputs[field]) < 10:
            errors.append(f"drive_outputs.{field} is invalid")

    pointers = profile.get("source_pointers", [])
    if not pointers:
        errors.append("source_pointers must be non-empty")
    for pointer in pointers:
        if pointer.get("payload_copied") is not False:
            errors.append(f"source pointer {pointer.get('id')} copied payload")

    empty_model = profile.get("empty_state_model", {})
    if empty_model.get("token_vazio_is_numeric") is not False:
        errors.append("TOKEN_VAZIO cannot be numeric")
    if empty_model.get("token_vazio_is_pass") is not False:
        errors.append("TOKEN_VAZIO cannot be PASS")
    states = set(empty_model.get("states", []))
    if states != EMPTY_STATES:
        errors.append("empty-state taxonomy mismatch")
    required = set(empty_model.get("required_fields_for_unresolved", []))
    if required != REQUIRED_UNRESOLVED:
        errors.append("required unresolved fields mismatch")

    operators = profile.get("operators", [])
    operator_ids = {operator.get("id") for operator in operators if isinstance(operator, dict)}
    expected_operators = {
        "DIRECT_DERIVATIVE",
        "REVERSE_CAUSAL_TRAVERSAL",
        "HISTORICAL_ANTIDERIVATIVE",
        "LOG_MISSINGNESS_ANTIDERIVATIVE",
        "RECURSIVE_MULTISCALE",
        "COUNTERFACTUAL_REMOVAL",
    }
    if operator_ids != expected_operators:
        errors.append("operator set mismatch")

    torus = profile.get("toroidal_model", {})
    if torus.get("global_flux_invariant") != "sum_v div(J)(v)=0":
        errors.append("toroidal global flux invariant mismatch")
    if torus.get("physical_universe_claim") != "TOKEN_VAZIO":
        errors.append("physical universe claim must remain TOKEN_VAZIO")

    hex_model = profile.get("hexagonal_longitudinal_model", {})
    if hex_model.get("three_edges_is_proof") is not False:
        errors.append("three remaining edges cannot be promoted as reconstruction proof")
    if hex_model.get("universal_reconstruction_claim") != "TOKEN_VAZIO":
        errors.append("universal reconstruction claim must remain TOKEN_VAZIO")

    permutation = profile.get("permutation_contract", {})
    if permutation.get("admissible_reordering_preserves_digest") is not True:
        errors.append("permutation digest invariant must remain enabled")

    boundaries = profile.get("term_boundaries", {})
    observed = boundaries.get("observed_terms", [])
    if len(observed) != len(set(observed)):
        errors.append("observed_terms contains duplicates")
    unresolved = boundaries.get("unresolved_terms", [])
    unresolved_names: set[str] = set()
    for term in unresolved:
        name = term.get("term")
        if name in unresolved_names:
            errors.append(f"duplicate unresolved term {name}")
        unresolved_names.add(name)
        if term.get("state") not in UNRESOLVED_STATES:
            errors.append(f"{name}: invalid unresolved state")
        for field in REQUIRED_UNRESOLVED:
            if not isinstance(term.get(field), str) or not term[field].strip():
                errors.append(f"{name}: missing {field}")
    if set(observed) & unresolved_names:
        errors.append("a term cannot be both observed and unresolved")

    routes = profile.get("repository_routes", [])
    repositories = [route.get("repository") for route in routes if isinstance(route, dict)]
    if len(repositories) != len(set(repositories)):
        errors.append("repository routes contain duplicates")
    for route in routes:
        if "/" not in route.get("repository", "") or not route.get("authority"):
            errors.append("invalid repository route")

    return errors


def build_report(profile: dict[str, Any], errors: list[str]) -> dict[str, Any]:
    checks = finite_checks()
    status = "PASS" if not errors and checks["status"] == "PASS" else "FAIL"
    return {
        "schema": "rafaelia.knowledge-antiderivative.report.v1",
        "status": status,
        "claim_allowed": False,
        "semantic_digest": semantic_digest(profile) if not errors else None,
        "profile_errors": errors,
        "finite_checks": checks,
        "drive_pointer_only": profile.get("drive_outputs", {}).get("storage_mode") == "POINTER_ONLY",
        "secret_value_copied": False,
        "unresolved_terms": [
            {"term": item["term"], "state": item["state"]}
            for item in profile.get("term_boundaries", {}).get("unresolved_terms", [])
        ],
        "boundary": (
            "Finite structural checks do not prove a physical universe, quantum effect, "
            "universal reconstruction theorem, authorship priority or external academic validation."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    try:
        profile = load_json(args.profile)
    except Exception as exc:  # pragma: no cover - CLI boundary
        print(f"BLOCKED: {exc}", file=sys.stderr)
        return 2

    errors = validate(profile)
    report = build_report(profile, errors)
    encoded = json.dumps(report, indent=2, ensure_ascii=False, sort_keys=True) + "\n"
    print(encoded, end="")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded, encoding="utf-8")
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
