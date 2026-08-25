#!/usr/bin/env python3
"""Validate the RAFAELIA longitudinal workflow index and session contract.

The validator is dependency-free by design. JSON Schema remains available for
editors, while this parser enforces cross-reference and promotion invariants
that JSON Schema alone cannot express.
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

INDEX_SCHEMA = "rafaelia.workflow-master-index.v1"
SESSION_SCHEMA = "rafaelia.workflow-session.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
EXPECTED_CYCLE = [
    "PSI_INTENT",
    "CHI_OBSERVE",
    "RHO_NOISE",
    "DELTA_TRANSFORM",
    "SIGMA_CUSTODY",
    "OMEGA_CLOSE",
    "PSI_REOPEN",
]
EXPECTED_LAYER_IDS = [f"S{number:02d}" for number in range(1, 31)]
EXPECTED_MODULE_IDS = [f"M{number:02d}" for number in range(1, 14)]
EXPECTED_CATEGORIES = {
    "semantic": 5,
    "formal": 5,
    "evidence": 5,
    "execution": 5,
    "governance": 5,
    "evolution": 5,
}
CLAIM_STATES = {
    "VERIFIED",
    "PARTIAL",
    "DECLARED",
    TOKEN_VAZIO,
    "CONTRADICTION",
    "BLOCKED",
}
SUPPORT_STATES = {"PASS", "PARTIAL", "FAIL", "NOT_APPLICABLE", TOKEN_VAZIO}


class ContractError(ValueError):
    """Raised when an index or session violates the canonical contract."""


def load_json(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError as exc:
        raise ContractError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ContractError(f"invalid JSON in {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise ContractError(f"{path}: root must be an object")
    return data


def _require_nonempty_string(value: Any, field: str, errors: list[str]) -> None:
    if not isinstance(value, str) or not value.strip():
        errors.append(f"{field} must be a non-empty string")


def _unique_map(items: Any, field: str, errors: list[str]) -> dict[str, dict[str, Any]]:
    if not isinstance(items, list):
        errors.append(f"{field} must be an array")
        return {}
    mapped: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            errors.append(f"{field}[{index}] must be an object")
            continue
        identifier = item.get("id")
        if not isinstance(identifier, str) or not identifier:
            errors.append(f"{field}[{index}].id must be a non-empty string")
            continue
        if identifier in mapped:
            errors.append(f"duplicate {field} id: {identifier}")
            continue
        mapped[identifier] = item
    return mapped


def _validate_refs(
    values: Any,
    allowed: set[str],
    field: str,
    errors: list[str],
) -> list[str]:
    if not isinstance(values, list):
        errors.append(f"{field} must be an array")
        return []
    refs: list[str] = []
    seen: set[str] = set()
    for value in values:
        if not isinstance(value, str) or not value:
            errors.append(f"{field} entries must be non-empty strings")
            continue
        if value in seen:
            errors.append(f"{field} contains duplicate reference: {value}")
            continue
        seen.add(value)
        refs.append(value)
        if value not in allowed:
            errors.append(f"{field} references unknown id: {value}")
    return refs


def validate_index(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    errors: list[str] = []
    if data.get("schema") != INDEX_SCHEMA:
        errors.append(f"schema must be {INDEX_SCHEMA!r}")
    if data.get("cycle") != EXPECTED_CYCLE:
        errors.append("cycle must use the canonical seven-stage order")

    authority = data.get("authority")
    if not isinstance(authority, dict):
        errors.append("authority must be an object")
    else:
        if authority.get("integration_repository") != "rafaelmeloreisnovo/RafGitTools":
            errors.append("authority.integration_repository must be rafaelmeloreisnovo/RafGitTools")
        if authority.get("source_lock") != "runtime-lock.json":
            errors.append("authority.source_lock must be runtime-lock.json")

    modules = _unique_map(data.get("modules"), "modules", errors)
    if sorted(modules) != EXPECTED_MODULE_IDS:
        errors.append("modules must contain exactly M01..M13")
    for module_id, module in modules.items():
        _require_nonempty_string(module.get("name"), f"modules.{module_id}.name", errors)
        _require_nonempty_string(module.get("role"), f"modules.{module_id}.role", errors)
        repository = module.get("canonical_repository")
        _require_nonempty_string(repository, f"modules.{module_id}.canonical_repository", errors)
        state = module.get("repository_state")
        if repository == TOKEN_VAZIO and state != TOKEN_VAZIO:
            errors.append(f"modules.{module_id}: TOKEN_VAZIO repository requires TOKEN_VAZIO state")
        if repository != TOKEN_VAZIO and state not in {"DECLARED", "VERIFIED"}:
            errors.append(f"modules.{module_id}: concrete repository requires DECLARED or VERIFIED state")

    layers = _unique_map(data.get("support_layers"), "support_layers", errors)
    if sorted(layers) != EXPECTED_LAYER_IDS:
        errors.append("support_layers must contain exactly S01..S30")
    counts: Counter[str] = Counter()
    for layer_id, layer in layers.items():
        for field in ("name", "purpose", "required_evidence", "failure_state", "promotion_gate"):
            _require_nonempty_string(layer.get(field), f"support_layers.{layer_id}.{field}", errors)
        category = layer.get("category")
        if category not in EXPECTED_CATEGORIES:
            errors.append(f"support_layers.{layer_id}.category is invalid: {category!r}")
        else:
            counts[category] += 1
    if dict(counts) != EXPECTED_CATEGORIES:
        errors.append(
            "support layer categories must contain exactly five entries each: "
            + ", ".join(f"{name}=5" for name in EXPECTED_CATEGORIES)
        )

    if errors:
        raise ContractError("\n".join(f"- {error}" for error in errors))
    return layers


def validate_session(
    index_data: dict[str, Any],
    session: dict[str, Any],
) -> dict[str, int]:
    layers = validate_index(index_data)
    errors: list[str] = []

    if session.get("schema") != SESSION_SCHEMA:
        errors.append(f"schema must be {SESSION_SCHEMA!r}")
    for field in ("session_id", "title", "observed_at"):
        _require_nonempty_string(session.get(field), field, errors)
    if session.get("cycle") != EXPECTED_CYCLE:
        errors.append("cycle must use the canonical seven-stage order")

    scope = session.get("scope")
    if not isinstance(scope, dict):
        errors.append("scope must be an object")
    else:
        _require_nonempty_string(scope.get("question"), "scope.question", errors)
        _require_nonempty_string(
            scope.get("completion_criterion"),
            "scope.completion_criterion",
            errors,
        )
        for field in ("repositories", "paths"):
            values = scope.get(field)
            if not isinstance(values, list):
                errors.append(f"scope.{field} must be an array")
            elif len(values) != len(set(values)):
                errors.append(f"scope.{field} must not contain duplicates")

    anchors = _unique_map(session.get("semantic_anchors"), "semantic_anchors", errors)
    if not anchors:
        errors.append("semantic_anchors must contain at least one item")

    evidence = _unique_map(session.get("evidence"), "evidence", errors)
    tests = _unique_map(session.get("tests"), "tests", errors)
    contradictions = _unique_map(session.get("contradictions"), "contradictions", errors)
    claims = _unique_map(session.get("claims"), "claims", errors)
    invariants = _unique_map(session.get("invariants"), "invariants", errors)
    token_records = _unique_map(session.get("token_vazio"), "token_vazio", errors)

    evidence_ids = set(evidence)
    test_ids = set(tests)
    contradiction_ids = set(contradictions)
    claim_ids = set(claims)

    for test_id, test in tests.items():
        result = test.get("result")
        if result not in {"PASS", "FAIL", TOKEN_VAZIO, "NOT_RUN"}:
            errors.append(f"tests.{test_id}.result is invalid")
        supported = _validate_refs(
            test.get("supports_claims"),
            claim_ids,
            f"tests.{test_id}.supports_claims",
            errors,
        )
        if result == "PASS" and not supported:
            errors.append(f"tests.{test_id}: PASS test must support at least one claim")
        evidence_ref = test.get("evidence_ref")
        if evidence_ref is not None and evidence_ref not in evidence_ids:
            errors.append(f"tests.{test_id}.evidence_ref references unknown id: {evidence_ref}")

    passing_tests = {test_id for test_id, test in tests.items() if test.get("result") == "PASS"}

    token_fields = {
        record.get("field")
        for record in token_records.values()
        if isinstance(record.get("field"), str)
    }

    for claim_id, claim in claims.items():
        state = claim.get("state")
        if state not in CLAIM_STATES:
            errors.append(f"claims.{claim_id}.state is invalid")
        _require_nonempty_string(claim.get("statement"), f"claims.{claim_id}.statement", errors)
        _require_nonempty_string(claim.get("domain"), f"claims.{claim_id}.domain", errors)
        evidence_refs = _validate_refs(
            claim.get("evidence_refs"),
            evidence_ids,
            f"claims.{claim_id}.evidence_refs",
            errors,
        )
        test_refs = _validate_refs(
            claim.get("test_refs"),
            test_ids,
            f"claims.{claim_id}.test_refs",
            errors,
        )
        contradiction_refs = _validate_refs(
            claim.get("contradiction_refs"),
            contradiction_ids,
            f"claims.{claim_id}.contradiction_refs",
            errors,
        )
        mode = claim.get("verification_mode")
        if state == "VERIFIED" and not evidence_refs:
            errors.append(f"claims.{claim_id}: VERIFIED requires evidence")
        if state == "VERIFIED" and mode == "TEST":
            if not test_refs or not set(test_refs).issubset(passing_tests):
                errors.append(f"claims.{claim_id}: TEST verification requires referenced PASS tests")
        if state == TOKEN_VAZIO:
            if evidence_refs or test_refs:
                errors.append(f"claims.{claim_id}: TOKEN_VAZIO cannot cite evidence or tests")
            _require_nonempty_string(claim.get("reason"), f"claims.{claim_id}.reason", errors)
            if f"claims.{claim_id}" not in token_fields:
                errors.append(f"claims.{claim_id}: TOKEN_VAZIO requires a token_vazio ledger entry")
        if state == "CONTRADICTION" and not contradiction_refs:
            errors.append(f"claims.{claim_id}: CONTRADICTION requires contradiction_refs")

    for invariant_id, invariant in invariants.items():
        _require_nonempty_string(
            invariant.get("statement"),
            f"invariants.{invariant_id}.statement",
            errors,
        )
        transformations = invariant.get("transformations")
        if not isinstance(transformations, list) or not transformations:
            errors.append(f"invariants.{invariant_id}.transformations must be a non-empty array")
        refs = _validate_refs(
            invariant.get("evidence_refs"),
            evidence_ids,
            f"invariants.{invariant_id}.evidence_refs",
            errors,
        )
        status = invariant.get("status")
        if status == "VERIFIED" and not refs:
            errors.append(f"invariants.{invariant_id}: VERIFIED requires evidence")
        if status == TOKEN_VAZIO:
            _require_nonempty_string(
                invariant.get("reason"),
                f"invariants.{invariant_id}.reason",
                errors,
            )

    support = _unique_map(session.get("support_layers"), "support_layers", errors)
    if sorted(support) != EXPECTED_LAYER_IDS:
        errors.append("session support_layers must contain exactly S01..S30")
    for layer_id, item in support.items():
        if layer_id not in layers:
            errors.append(f"support_layers references unknown master layer: {layer_id}")
        status = item.get("status")
        if status not in SUPPORT_STATES:
            errors.append(f"support_layers.{layer_id}.status is invalid")
        refs = _validate_refs(
            item.get("evidence_refs"),
            evidence_ids,
            f"support_layers.{layer_id}.evidence_refs",
            errors,
        )
        reason = item.get("reason")
        if status == "PASS" and not refs:
            errors.append(f"support_layers.{layer_id}: PASS requires evidence")
        if status in {"PARTIAL", "FAIL", "NOT_APPLICABLE", TOKEN_VAZIO}:
            _require_nonempty_string(reason, f"support_layers.{layer_id}.reason", errors)

    artifacts = session.get("artifacts")
    if not isinstance(artifacts, list):
        errors.append("artifacts must be an array")
    else:
        for index, artifact in enumerate(artifacts):
            if not isinstance(artifact, dict):
                errors.append(f"artifacts[{index}] must be an object")
                continue
            _require_nonempty_string(artifact.get("path"), f"artifacts[{index}].path", errors)
            sha256 = artifact.get("sha256")
            if sha256 == TOKEN_VAZIO:
                _require_nonempty_string(
                    artifact.get("reason"),
                    f"artifacts[{index}].reason",
                    errors,
                )
            elif not isinstance(sha256, str) or len(sha256) != 64:
                errors.append(f"artifacts[{index}].sha256 must be 64 chars or TOKEN_VAZIO")

    next_action = session.get("next_action")
    if not isinstance(next_action, dict):
        errors.append("next_action must be an object")
    else:
        _require_nonempty_string(next_action.get("action"), "next_action.action", errors)
        state = next_action.get("state")
        if state not in {"READY", "BLOCKED", TOKEN_VAZIO}:
            errors.append("next_action.state is invalid")
        if state in {"BLOCKED", TOKEN_VAZIO}:
            _require_nonempty_string(next_action.get("blocker"), "next_action.blocker", errors)

    if errors:
        raise ContractError("\n".join(f"- {error}" for error in errors))

    return {
        "anchors": len(anchors),
        "claims": len(claims),
        "invariants": len(invariants),
        "evidence": len(evidence),
        "tests": len(tests),
        "contradictions": len(contradictions),
        "token_vazio": len(token_records),
        "support_layers": len(support),
    }


def summarize(index_data: dict[str, Any], session: dict[str, Any]) -> str:
    counts = validate_session(index_data, session)
    claim_counts = Counter(item["state"] for item in session["claims"])
    layer_counts = Counter(item["status"] for item in session["support_layers"])
    lines = [
        f"session={session['session_id']}",
        "claims=" + ",".join(f"{state}:{claim_counts[state]}" for state in sorted(claim_counts)),
        "layers=" + ",".join(f"{state}:{layer_counts[state]}" for state in sorted(layer_counts)),
        "objects=" + ",".join(f"{name}:{value}" for name, value in counts.items()),
        f"next={session['next_action']['state']}:{session['next_action']['action']}",
    ]
    return "\n".join(lines)


def command_validate_index(args: argparse.Namespace) -> int:
    validate_index(load_json(args.index))
    print(f"[OK] {args.index}: 13 modules and 30 support layers valid")
    return 0


def command_validate_session(args: argparse.Namespace) -> int:
    counts = validate_session(load_json(args.index), load_json(args.session))
    print(
        f"[OK] {args.session}: workflow session valid "
        f"({counts['claims']} claims, {counts['support_layers']} layers)"
    )
    return 0


def command_summarize(args: argparse.Namespace) -> int:
    print(summarize(load_json(args.index), load_json(args.session)))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    index_parser = subparsers.add_parser("validate-index")
    index_parser.add_argument("index", type=Path)
    index_parser.set_defaults(handler=command_validate_index)

    session_parser = subparsers.add_parser("validate-session")
    session_parser.add_argument("index", type=Path)
    session_parser.add_argument("session", type=Path)
    session_parser.set_defaults(handler=command_validate_session)

    summary_parser = subparsers.add_parser("summarize")
    summary_parser.add_argument("index", type=Path)
    summary_parser.add_argument("session", type=Path)
    summary_parser.set_defaults(handler=command_summarize)
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return args.handler(args)
    except ContractError as exc:
        print(f"[FALHA] workflow contract:\n{exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
