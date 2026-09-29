#!/usr/bin/env python3
"""Validate RafGitTools private-processing seed V1.

This validator is dependency-free and intentionally checks semantic invariants
that JSON Schema alone should not be trusted to promote into runtime evidence.

It proves only structural/state coherence of the supplied seed. It does not
prove Drive authority, device execution, private GitHub publication, or hashes
that remain TOKEN_VAZIO.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SCHEMA = "rafgittools.private-processing-seed/v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")

REQUIRED_TOP = (
    "schema",
    "seed_id",
    "observed_at",
    "architecture",
    "source",
    "artifact",
    "execution",
    "evidence",
    "claim",
    "gaps",
    "next",
)


def _nonempty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _is_sha_or_token(value: Any) -> bool:
    return value == TOKEN_VAZIO or (
        isinstance(value, str) and SHA256_RE.fullmatch(value) is not None
    )


def _require_mapping(data: dict[str, Any], key: str, errors: list[str]) -> dict[str, Any]:
    value = data.get(key)
    if not isinstance(value, dict):
        errors.append(f"{key}: expected object")
        return {}
    return value


def _require_nonempty_string(
    obj: dict[str, Any], field: str, prefix: str, errors: list[str]
) -> None:
    if not _nonempty_string(obj.get(field)):
        errors.append(f"{prefix}.{field}: expected non-empty string")


def _validate_token_field(
    obj: dict[str, Any], field: str, prefix: str, errors: list[str]
) -> None:
    value = obj.get(field)
    if value in (None, "", 0, False):
        errors.append(
            f"{prefix}.{field}: expected explicit TOKEN_VAZIO or concrete value; "
            "null/empty/0/false are not TOKEN_VAZIO"
        )
    elif not _nonempty_string(value):
        errors.append(f"{prefix}.{field}: expected string")


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    for key in REQUIRED_TOP:
        if key not in data:
            errors.append(f"missing top-level field: {key}")

    if data.get("schema") != SCHEMA:
        errors.append("schema: mismatch")

    for field in ("seed_id", "observed_at"):
        if not _nonempty_string(data.get(field)):
            errors.append(f"{field}: expected non-empty string")

    architecture = _require_mapping(data, "architecture", errors)
    source = _require_mapping(data, "source", errors)
    artifact = _require_mapping(data, "artifact", errors)
    execution = _require_mapping(data, "execution", errors)
    evidence = _require_mapping(data, "evidence", errors)
    claim = _require_mapping(data, "claim", errors)
    nxt = _require_mapping(data, "next", errors)

    if architecture:
        if architecture.get("state") not in {"DETERMINED", "PARTIAL", TOKEN_VAZIO}:
            errors.append("architecture.state: invalid")
        _require_nonempty_string(architecture, "capability_ref", "architecture", errors)
        route = architecture.get("route")
        if not isinstance(route, list) or not route or not all(_nonempty_string(x) for x in route):
            errors.append("architecture.route: expected non-empty string list")

    if source:
        for field in ("id", "authority_alias"):
            _require_nonempty_string(source, field, "source", errors)
        if source.get("state") not in {"DETERMINED", "PARTIAL", TOKEN_VAZIO}:
            errors.append("source.state: invalid")
        if not _is_sha_or_token(source.get("sha256")):
            errors.append("source.sha256: expected lowercase SHA-256 or TOKEN_VAZIO")
        if source.get("raw_mutated") is not False:
            errors.append("source.raw_mutated: must be false")

    if artifact:
        _validate_token_field(artifact, "id", "artifact", errors)
        _require_nonempty_string(artifact, "destination_alias", "artifact", errors)
        if artifact.get("state") not in {
            TOKEN_VAZIO, "NOT_RUN", "PENDING", "IMPLEMENTED_UNTESTED", "PASS", "FAIL"
        }:
            errors.append("artifact.state: invalid")
        if not _is_sha_or_token(artifact.get("output_sha256")):
            errors.append("artifact.output_sha256: expected lowercase SHA-256 or TOKEN_VAZIO")

    if execution:
        for field in ("execution_id", "device_ref", "processor_ref"):
            _validate_token_field(execution, field, "execution", errors)
        if execution.get("state") not in {
            TOKEN_VAZIO, "NOT_RUN", "PENDING", "IMPLEMENTED_UNTESTED", "PASS", "FAIL", "AUDIT"
        }:
            errors.append("execution.state: invalid")
        if not _is_sha_or_token(execution.get("processor_sha256")):
            errors.append("execution.processor_sha256: expected lowercase SHA-256 or TOKEN_VAZIO")

    if evidence:
        _validate_token_field(evidence, "receipt_id", "evidence", errors)
        if evidence.get("state") not in {
            TOKEN_VAZIO, "NOT_RUN", "PENDING", "PASS", "FAIL", "AUDIT", "OBSERVED_UNPROMOTED"
        }:
            errors.append("evidence.state: invalid")
        if not _is_sha_or_token(evidence.get("receipt_sha256")):
            errors.append("evidence.receipt_sha256: expected lowercase SHA-256 or TOKEN_VAZIO")

    claim_allowed = claim.get("claim_allowed")
    if not isinstance(claim_allowed, bool):
        errors.append("claim.claim_allowed: expected boolean")
    if claim.get("state") not in {"BLOCKED", "PENDING", "ALLOWED"}:
        errors.append("claim.state: invalid")
    _require_nonempty_string(claim, "reason", "claim", errors)

    if claim_allowed is True:
        if architecture.get("state") != "DETERMINED":
            errors.append("claim gate: architecture.state must be DETERMINED")
        if execution.get("state") != "PASS":
            errors.append("claim gate: execution.state must be PASS")
        if evidence.get("state") != "PASS":
            errors.append("claim gate: evidence.state must be PASS")
        if artifact.get("state") != "PASS":
            errors.append("claim gate: artifact.state must be PASS")
        if claim.get("state") != "ALLOWED":
            errors.append("claim gate: claim.state must be ALLOWED")
        for prefix, obj, field in (
            ("source", source, "sha256"),
            ("artifact", artifact, "output_sha256"),
            ("execution", execution, "processor_sha256"),
            ("evidence", evidence, "receipt_sha256"),
        ):
            value = obj.get(field)
            if not (isinstance(value, str) and SHA256_RE.fullmatch(value)):
                errors.append(f"claim gate: {prefix}.{field} must be concrete SHA-256")
        for prefix, obj, field in (
            ("artifact", artifact, "id"),
            ("execution", execution, "execution_id"),
            ("execution", execution, "device_ref"),
            ("evidence", evidence, "receipt_id"),
        ):
            if obj.get(field) == TOKEN_VAZIO:
                errors.append(f"claim gate: {prefix}.{field} cannot be TOKEN_VAZIO")
    else:
        if claim.get("state") == "ALLOWED":
            errors.append("claim.state cannot be ALLOWED when claim_allowed=false")

    gaps = data.get("gaps")
    if not isinstance(gaps, list):
        errors.append("gaps: expected array")
    else:
        ids: set[str] = set()
        for i, gap in enumerate(gaps):
            if not isinstance(gap, dict):
                errors.append(f"gaps[{i}]: expected object")
                continue
            for field in ("id", "field", "reason", "promotion_condition"):
                if not _nonempty_string(gap.get(field)):
                    errors.append(f"gaps[{i}].{field}: expected non-empty string")
            if gap.get("state") not in {TOKEN_VAZIO, "PENDING", "AUDIT", "NOT_RUN", "FAIL"}:
                errors.append(f"gaps[{i}].state: invalid")
            gap_id = gap.get("id")
            if _nonempty_string(gap_id):
                if gap_id in ids:
                    errors.append(f"gaps[{i}].id: duplicate {gap_id}")
                ids.add(gap_id)

    if nxt:
        for field in ("action", "gate"):
            _require_nonempty_string(nxt, field, "next", errors)

    if execution.get("state") != "PASS" or evidence.get("state") != "PASS":
        if claim_allowed is True:
            errors.append(
                "promotion invariant: unresolved execution/evidence cannot allow claim"
            )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=Path)
    args = parser.parse_args()

    try:
        data = json.loads(args.path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"FAIL {exc}")
        return 2

    if not isinstance(data, dict):
        print("FAIL root must be object")
        return 1

    errors = validate(data)
    if errors:
        print("FAIL")
        for error in errors:
            print(f"- {error}")
        return 1

    print("PASS structural/state coherence only")
    print("claim promotion evidence: NOT_PROVEN_BY_THIS_VALIDATOR")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
