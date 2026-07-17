#!/usr/bin/env python3
"""Validate the RAFAELIA content/token validity contract and manifests.

The validator is dependency-free. A numeric weight is a support annotation, not a
truth probability. TOKEN_VAZIO remains a first-class valid epistemic record and
is never converted to a number.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

CONTRACT_SCHEMA = "rafaelia.content-validity-contract.v1"
MANIFEST_SCHEMA = "rafaelia.content-validity-manifest.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
EXPECTED_DIMENSIONS = [f"D{number:02d}" for number in range(1, 9)]
VALID_PROMOTED_STATES = {"VALID", "VALID_LIMITED"}
SEMANTIC_FEATURE_KEYS = {
    "negation",
    "modality",
    "conditions",
    "exceptions",
    "numbers",
    "units",
}


class ContractError(ValueError):
    """Raised when a contract or manifest violates an invariant."""


def load_json(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ContractError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ContractError(f"invalid JSON in {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise ContractError(f"{path}: root must be an object")
    return data


def _nonempty(value: Any, field: str, errors: list[str]) -> None:
    if not isinstance(value, str) or not value.strip():
        errors.append(f"{field} must be a non-empty string")


def _unique(items: Any, field: str, errors: list[str]) -> dict[str, dict[str, Any]]:
    if not isinstance(items, list):
        errors.append(f"{field} must be an array")
        return {}
    result: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            errors.append(f"{field}[{index}] must be an object")
            continue
        identifier = item.get("id")
        if not isinstance(identifier, str) or not identifier:
            errors.append(f"{field}[{index}].id must be a non-empty string")
            continue
        if identifier in result:
            errors.append(f"duplicate {field} id: {identifier}")
            continue
        result[identifier] = item
    return result


def _refs(values: Any, allowed: set[str], field: str, errors: list[str]) -> list[str]:
    if not isinstance(values, list):
        errors.append(f"{field} must be an array")
        return []
    result: list[str] = []
    seen: set[str] = set()
    for value in values:
        if not isinstance(value, str) or not value:
            errors.append(f"{field} entries must be non-empty strings")
            continue
        if value in seen:
            errors.append(f"{field} contains duplicate reference: {value}")
            continue
        seen.add(value)
        result.append(value)
        if value not in allowed:
            errors.append(f"{field} references unknown id: {value}")
    return result


def validate_contract(contract: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []
    if contract.get("schema") != CONTRACT_SCHEMA:
        errors.append(f"schema must be {CONTRACT_SCHEMA!r}")
    authority = contract.get("authority")
    if not isinstance(authority, dict):
        errors.append("authority must be an object")
    elif authority.get("repository") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("authority.repository must be rafaelmeloreisnovo/RafGitTools")

    states = contract.get("token_states")
    if not isinstance(states, list) or TOKEN_VAZIO not in states or "VALID" not in states:
        errors.append("token_states must include TOKEN_VAZIO and VALID")
    kinds = contract.get("token_kinds")
    if not isinstance(kinds, list) or not kinds:
        errors.append("token_kinds must be a non-empty array")

    dimensions = _unique(contract.get("dimensions"), "dimensions", errors)
    if sorted(dimensions) != EXPECTED_DIMENSIONS:
        errors.append("dimensions must contain exactly D01..D08")
    for dimension_id, dimension in dimensions.items():
        _nonempty(dimension.get("name"), f"dimensions.{dimension_id}.name", errors)
        _nonempty(dimension.get("meaning"), f"dimensions.{dimension_id}.meaning", errors)

    required = contract.get("required_dimensions_by_kind")
    if not isinstance(required, dict):
        errors.append("required_dimensions_by_kind must be an object")
        required = {}
    elif isinstance(kinds, list) and set(required) != set(kinds):
        errors.append("required_dimensions_by_kind must cover every token kind exactly")
    for kind, refs in required.items():
        _refs(refs, set(dimensions), f"required_dimensions_by_kind.{kind}", errors)

    weight_domain = contract.get("weight_domain")
    if not isinstance(weight_domain, dict):
        errors.append("weight_domain must be an object")
    else:
        if weight_domain.get("missing_value") != TOKEN_VAZIO:
            errors.append("weight_domain.missing_value must be TOKEN_VAZIO")
        if weight_domain.get("token_vazio_is_numeric") is not False:
            errors.append("TOKEN_VAZIO must not be numeric")
        low = weight_domain.get("numeric_min")
        high = weight_domain.get("numeric_max")
        if not isinstance(low, (int, float)) or not isinstance(high, (int, float)) or low >= high:
            errors.append("weight_domain numeric bounds are invalid")

    aggregation = contract.get("aggregation")
    if not isinstance(aggregation, dict):
        errors.append("aggregation must be an object")
    else:
        if aggregation.get("tensor_axes") != ["token", "window", "dimension"]:
            errors.append("aggregation.tensor_axes must be token/window/dimension")
        if aggregation.get("non_compensatory") is not True:
            errors.append("aggregation must be non-compensatory")
        for field in ("valid_threshold", "valid_limited_threshold"):
            value = aggregation.get(field)
            if not isinstance(value, (int, float)) or not 0 <= value <= 1:
                errors.append(f"aggregation.{field} must be between 0 and 1")
        if (
            isinstance(aggregation.get("valid_threshold"), (int, float))
            and isinstance(aggregation.get("valid_limited_threshold"), (int, float))
            and aggregation["valid_limited_threshold"] > aggregation["valid_threshold"]
        ):
            errors.append("valid_limited_threshold cannot exceed valid_threshold")

    if errors:
        raise ContractError("\n".join(f"- {error}" for error in errors))
    return {
        "states": set(states),
        "kinds": set(kinds),
        "dimensions": dimensions,
        "required": required,
        "weight_domain": weight_domain,
        "aggregation": aggregation,
    }


def validate_manifest(contract: dict[str, Any], manifest: dict[str, Any]) -> dict[str, Any]:
    compiled = validate_contract(contract)
    errors: list[str] = []
    if manifest.get("schema") != MANIFEST_SCHEMA:
        errors.append(f"schema must be {MANIFEST_SCHEMA!r}")
    for field in ("manifest_id", "title", "observed_at", "declared_use"):
        _nonempty(manifest.get(field), field, errors)

    sources = _unique(manifest.get("sources"), "sources", errors)
    evidence = _unique(manifest.get("evidence"), "evidence", errors)
    tokens = _unique(manifest.get("tokens"), "tokens", errors)
    windows = _unique(manifest.get("windows"), "windows", errors)
    contradictions = _unique(manifest.get("contradictions"), "contradictions", errors)
    bridges = _unique(manifest.get("bridges"), "bridges", errors)
    vazio_records = _unique(manifest.get("token_vazio_ledger"), "token_vazio_ledger", errors)

    source_ids = set(sources)
    evidence_ids = set(evidence)
    token_ids = set(tokens)
    window_ids = set(windows)
    contradiction_ids = set(contradictions)
    bridge_ids = set(bridges)

    for source_id, source in sources.items():
        for field in ("repository", "ref", "path", "source_state"):
            _nonempty(source.get(field), f"sources.{source_id}.{field}", errors)

    for evidence_id, item in evidence.items():
        source_ref = item.get("source_ref")
        if source_ref not in source_ids:
            errors.append(f"evidence.{evidence_id}.source_ref references unknown id: {source_ref}")
        for field in ("locator", "kind", "observed_at"):
            _nonempty(item.get(field), f"evidence.{evidence_id}.{field}", errors)

    vazio_fields = {
        item.get("field") for item in vazio_records.values() if isinstance(item.get("field"), str)
    }
    for record_id, record in vazio_records.items():
        for field in ("field", "reason", "owner", "next_action", "exit_criteria"):
            _nonempty(record.get(field), f"token_vazio_ledger.{record_id}.{field}", errors)

    for token_id, token in tokens.items():
        kind = token.get("kind")
        state = token.get("state")
        if kind not in compiled["kinds"]:
            errors.append(f"tokens.{token_id}.kind is invalid: {kind!r}")
        if state not in compiled["states"]:
            errors.append(f"tokens.{token_id}.state is invalid: {state!r}")
        if token.get("source_ref") not in source_ids:
            errors.append(f"tokens.{token_id}.source_ref references unknown source")
        for field in ("raw_text", "normalized_text", "declared_domain", "declared_use"):
            _nonempty(token.get(field), f"tokens.{token_id}.{field}", errors)
        refs = _refs(token.get("evidence_refs"), evidence_ids, f"tokens.{token_id}.evidence_refs", errors)
        features = token.get("semantic_features")
        if not isinstance(features, dict) or set(features) != SEMANTIC_FEATURE_KEYS:
            errors.append(
                f"tokens.{token_id}.semantic_features must contain exactly "
                + ", ".join(sorted(SEMANTIC_FEATURE_KEYS))
            )
        else:
            for feature, values in features.items():
                if not isinstance(values, list):
                    errors.append(f"tokens.{token_id}.semantic_features.{feature} must be an array")
        if state in VALID_PROMOTED_STATES and not refs:
            errors.append(f"tokens.{token_id}: {state} requires evidence")
        if state == TOKEN_VAZIO:
            if refs:
                errors.append(f"tokens.{token_id}: TOKEN_VAZIO cannot cite positive evidence")
            for field in ("reason", "owner", "next_action", "exit_criteria"):
                _nonempty(token.get(field), f"tokens.{token_id}.{field}", errors)
            if f"tokens.{token_id}" not in vazio_fields:
                errors.append(f"tokens.{token_id}: TOKEN_VAZIO requires a ledger entry")

    token_windows: dict[str, set[str]] = {token_id: set() for token_id in token_ids}
    for window_id, window in windows.items():
        source_refs = _refs(
            window.get("source_refs"), source_ids, f"windows.{window_id}.source_refs", errors
        )
        token_refs = _refs(
            window.get("token_refs"), token_ids, f"windows.{window_id}.token_refs", errors
        )
        for token_ref in token_refs:
            token_windows[token_ref].add(window_id)
        maximum = window.get("max_tokens")
        overlap = window.get("overlap_tokens")
        observed = window.get("observed_token_count")
        if not isinstance(maximum, int) or maximum <= 0:
            errors.append(f"windows.{window_id}.max_tokens must be a positive integer")
        if not isinstance(overlap, int) or overlap < 0:
            errors.append(f"windows.{window_id}.overlap_tokens must be a non-negative integer")
        if isinstance(maximum, int) and isinstance(overlap, int) and overlap >= maximum:
            errors.append(f"windows.{window_id}: overlap must be less than max_tokens")
        if not isinstance(observed, int) or observed < 0:
            errors.append(f"windows.{window_id}.observed_token_count must be non-negative")
        elif isinstance(maximum, int) and observed > maximum:
            errors.append(f"windows.{window_id}: observed_token_count exceeds max_tokens")
        _nonempty(window.get("truncation"), f"windows.{window_id}.truncation", errors)
        bridge_ref = window.get("bridge_ref")
        if len(source_refs) > 1:
            if not isinstance(bridge_ref, str) or bridge_ref not in bridge_ids:
                errors.append(f"windows.{window_id}: cross-source window requires an explicit bridge")
        elif bridge_ref is not None and bridge_ref not in bridge_ids:
            errors.append(f"windows.{window_id}.bridge_ref references unknown bridge")

    for token_id, memberships in token_windows.items():
        if not memberships:
            errors.append(f"tokens.{token_id} is not assigned to any window")

    tensor_cells = manifest.get("tensor_cells")
    if not isinstance(tensor_cells, list):
        errors.append("tensor_cells must be an array")
        tensor_cells = []
    cell_map: dict[tuple[str, str, str], dict[str, Any]] = {}
    for index, cell in enumerate(tensor_cells):
        if not isinstance(cell, dict):
            errors.append(f"tensor_cells[{index}] must be an object")
            continue
        token_ref = cell.get("token_ref")
        window_ref = cell.get("window_ref")
        dimension_ref = cell.get("dimension_ref")
        key = (str(token_ref), str(window_ref), str(dimension_ref))
        if key in cell_map:
            errors.append(f"duplicate tensor cell: {'.'.join(key)}")
            continue
        cell_map[key] = cell
        if token_ref not in token_ids:
            errors.append(f"tensor_cells[{index}].token_ref references unknown token")
        if window_ref not in window_ids:
            errors.append(f"tensor_cells[{index}].window_ref references unknown window")
        if dimension_ref not in compiled["dimensions"]:
            errors.append(f"tensor_cells[{index}].dimension_ref references unknown dimension")
        refs = _refs(cell.get("evidence_refs"), evidence_ids, f"tensor_cells[{index}].evidence_refs", errors)
        weight = cell.get("weight")
        if weight == TOKEN_VAZIO:
            _nonempty(cell.get("reason"), f"tensor_cells[{index}].reason", errors)
            field = f"tensor_cells.{token_ref}.{window_ref}.{dimension_ref}"
            if field not in vazio_fields:
                errors.append(f"{field}: TOKEN_VAZIO requires a ledger entry")
            if refs:
                errors.append(f"{field}: TOKEN_VAZIO weight cannot cite positive evidence")
        elif isinstance(weight, bool) or not isinstance(weight, (int, float)):
            errors.append(f"tensor_cells[{index}].weight must be numeric or TOKEN_VAZIO")
        else:
            low = compiled["weight_domain"]["numeric_min"]
            high = compiled["weight_domain"]["numeric_max"]
            if not low <= float(weight) <= high:
                errors.append(f"tensor_cells[{index}].weight is outside [{low}, {high}]")
            if not refs:
                errors.append(f"tensor_cells[{index}]: numeric weight requires evidence")

    unresolved_by_token: set[str] = set()
    for contradiction_id, contradiction in contradictions.items():
        token_refs = _refs(
            contradiction.get("token_refs"), token_ids, f"contradictions.{contradiction_id}.token_refs", errors
        )
        status = contradiction.get("status")
        if status not in {"OPEN", "RESOLVED", TOKEN_VAZIO}:
            errors.append(f"contradictions.{contradiction_id}.status is invalid")
        if status != "RESOLVED":
            unresolved_by_token.update(token_refs)

    qualities: dict[str, dict[str, Any]] = {}
    for token_id, token in tokens.items():
        kind = token.get("kind")
        required = compiled["required"].get(kind, [])
        values: list[float] = []
        missing: list[str] = []
        for dimension_id in required:
            dimension_values: list[float] = []
            dimension_missing = False
            for window_id in sorted(token_windows.get(token_id, set())):
                cell = cell_map.get((token_id, window_id, dimension_id))
                if cell is None or cell.get("weight") == TOKEN_VAZIO:
                    dimension_missing = True
                    continue
                weight = cell.get("weight")
                if isinstance(weight, (int, float)) and not isinstance(weight, bool):
                    dimension_values.append(float(weight))
            if dimension_missing or not dimension_values:
                missing.append(dimension_id)
            else:
                values.append(min(dimension_values))
        coverage = 0.0 if not required else (len(required) - len(missing)) / len(required)
        quality = min(values) if values and not missing else TOKEN_VAZIO
        qualities[token_id] = {"coverage": coverage, "quality_floor": quality, "missing": missing}

        state = token.get("state")
        if state in VALID_PROMOTED_STATES:
            if token_id in unresolved_by_token:
                errors.append(f"tokens.{token_id}: promoted token has unresolved contradiction")
            if missing:
                errors.append(f"tokens.{token_id}: {state} requires all dimensions; missing {missing}")
            threshold = (
                compiled["aggregation"]["valid_threshold"]
                if state == "VALID"
                else compiled["aggregation"]["valid_limited_threshold"]
            )
            if quality != TOKEN_VAZIO and float(quality) < threshold:
                errors.append(
                    f"tokens.{token_id}: quality floor {quality:.3f} is below {state} threshold {threshold:.3f}"
                )
        if state == TOKEN_VAZIO and quality != TOKEN_VAZIO:
            errors.append(f"tokens.{token_id}: TOKEN_VAZIO must retain at least one missing required dimension")

    if errors:
        raise ContractError("\n".join(f"- {error}" for error in errors))
    return {
        "sources": len(sources),
        "evidence": len(evidence),
        "tokens": len(tokens),
        "windows": len(windows),
        "tensor_cells": len(cell_map),
        "token_vazio": len(vazio_records),
        "qualities": qualities,
    }


def summarize(contract: dict[str, Any], manifest: dict[str, Any]) -> str:
    result = validate_manifest(contract, manifest)
    state_counts: dict[str, int] = {}
    for token in manifest["tokens"]:
        state = token["state"]
        state_counts[state] = state_counts.get(state, 0) + 1
    states = ",".join(f"{key}:{state_counts[key]}" for key in sorted(state_counts))
    return (
        f"manifest={manifest['manifest_id']} tokens={result['tokens']} windows={result['windows']} "
        f"cells={result['tensor_cells']} states={states} token_vazio={result['token_vazio']}"
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    contract_parser = sub.add_parser("validate-contract")
    contract_parser.add_argument("contract", type=Path)
    manifest_parser = sub.add_parser("validate-manifest")
    manifest_parser.add_argument("contract", type=Path)
    manifest_parser.add_argument("manifest", type=Path)
    summary_parser = sub.add_parser("summarize")
    summary_parser.add_argument("contract", type=Path)
    summary_parser.add_argument("manifest", type=Path)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        contract = load_json(args.contract)
        if args.command == "validate-contract":
            validate_contract(contract)
            print("PASS: content validity contract")
        else:
            manifest = load_json(args.manifest)
            if args.command == "validate-manifest":
                result = validate_manifest(contract, manifest)
                print(
                    "PASS: content validity manifest "
                    f"({result['tokens']} tokens, {result['windows']} windows, "
                    f"{result['tensor_cells']} tensor cells)"
                )
            else:
                print(summarize(contract, manifest))
    except ContractError as exc:
        print(f"FAIL:\n{exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
