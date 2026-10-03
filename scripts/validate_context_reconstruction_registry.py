#!/usr/bin/env python3
"""Fail-closed structural validator for context reconstruction V1.

This intentionally uses only the Python standard library. It validates the
routing/seed control surface; it does not promote runtime or domain claims.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "configs/context-reconstruction-routes.v1.json"
SEED = ROOT / "examples/context-reconstruction-seed/minimal.example.json"
SCHEMA = ROOT / "contracts/context-reconstruction-seed-v1.schema.json"

REQUIRED_ROUTE_FIELDS = {
    "route_id",
    "purpose",
    "authority",
    "source_min",
    "service",
    "execution_target",
    "evidence_rule",
    "claim_boundary",
    "next_on_missing",
}

REQUIRED_SEED_FIELDS = {
    "schema",
    "seed_id",
    "created_at",
    "intent",
    "repo",
    "ref",
    "route_id",
    "current_state_ref",
    "source_refs",
    "authority",
    "execution_target",
    "evidence_refs",
    "gaps",
    "next",
    "claim_allowed",
}

FORBIDDEN_PAYLOAD_KEYS = {
    "content",
    "raw_text",
    "raw_content",
    "conversation_body",
    "corpus_body",
    "prompt_body",
    "response_body",
}


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def walk_keys(value: Any) -> set[str]:
    keys: set[str] = set()
    if isinstance(value, dict):
        for key, child in value.items():
            keys.add(str(key))
            keys.update(walk_keys(child))
    elif isinstance(value, list):
        for child in value:
            keys.update(walk_keys(child))
    return keys


def validate_local_path(locator: str, errors: list[str], context: str) -> None:
    if "://" in locator or locator.startswith("Drive:"):
        return
    candidate = ROOT / locator
    if not candidate.exists():
        errors.append(f"{context}: missing local path: {locator}")


def validate_registry(registry: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if registry.get("schema") != "rafgittools.context_reconstruction_routes.v1":
        errors.append("registry.schema mismatch")
    if registry.get("claim_allowed") is not False:
        errors.append("registry.claim_allowed must be false")

    routes = registry.get("routes")
    if not isinstance(routes, list) or not routes:
        return errors + ["registry.routes must be a non-empty array"]

    seen: set[str] = set()
    for index, route in enumerate(routes):
        ctx = f"route[{index}]"
        if not isinstance(route, dict):
            errors.append(f"{ctx}: must be an object")
            continue
        missing = REQUIRED_ROUTE_FIELDS - set(route)
        if missing:
            errors.append(f"{ctx}: missing fields {sorted(missing)}")
            continue
        route_id = route["route_id"]
        if route_id in seen:
            errors.append(f"{ctx}: duplicate route_id {route_id}")
        seen.add(route_id)
        sources = route["source_min"]
        if not isinstance(sources, list) or not 1 <= len(sources) <= 3:
            errors.append(f"{ctx}: source_min must contain 1..3 refs")
        else:
            for source in sources:
                if not isinstance(source, str) or not source:
                    errors.append(f"{ctx}: invalid source ref {source!r}")
                else:
                    validate_local_path(source, errors, ctx)
        for field in ("authority", "execution_target", "evidence_rule"):
            if not isinstance(route[field], str) or not route[field].strip():
                errors.append(f"{ctx}: {field} is required and non-empty")
        if not str(route["next_on_missing"]).startswith("TOKEN_VAZIO"):
            errors.append(f"{ctx}: next_on_missing must preserve TOKEN_VAZIO")
    return errors


def validate_seed(seed: dict[str, Any], route_ids: set[str]) -> list[str]:
    errors: list[str] = []
    missing = REQUIRED_SEED_FIELDS - set(seed)
    if missing:
        errors.append(f"seed: missing fields {sorted(missing)}")
    if seed.get("schema") != "rafgittools.context_reconstruction_seed.v1":
        errors.append("seed.schema mismatch")
    if seed.get("claim_allowed") is not False:
        errors.append("seed.claim_allowed must be false")
    if seed.get("route_id") not in route_ids:
        errors.append(f"seed.route_id unknown: {seed.get('route_id')}")

    source_refs = seed.get("source_refs")
    if not isinstance(source_refs, list) or not 1 <= len(source_refs) <= 3:
        errors.append("seed.source_refs must contain 1..3 refs")
    else:
        for index, ref in enumerate(source_refs):
            if not isinstance(ref, dict) or not isinstance(ref.get("locator"), str):
                errors.append(f"seed.source_refs[{index}] invalid")
            else:
                validate_local_path(ref["locator"], errors, f"seed.source_refs[{index}]")

    current_state = seed.get("current_state_ref")
    if isinstance(current_state, dict) and isinstance(current_state.get("locator"), str):
        validate_local_path(current_state["locator"], errors, "seed.current_state_ref")
    else:
        errors.append("seed.current_state_ref invalid")

    forbidden = walk_keys(seed) & FORBIDDEN_PAYLOAD_KEYS
    if forbidden:
        errors.append(f"seed contains forbidden raw-payload keys: {sorted(forbidden)}")
    return errors


def validate_all() -> list[str]:
    errors: list[str] = []
    for required in (REGISTRY, SEED, SCHEMA):
        if not required.exists():
            errors.append(f"missing required file: {required.relative_to(ROOT)}")
    if errors:
        return errors

    registry = load_json(REGISTRY)
    seed = load_json(SEED)
    load_json(SCHEMA)  # parse gate even without an external jsonschema dependency

    errors.extend(validate_registry(registry))
    route_ids = {
        route.get("route_id")
        for route in registry.get("routes", [])
        if isinstance(route, dict) and isinstance(route.get("route_id"), str)
    }
    errors.extend(validate_seed(seed, route_ids))
    return errors


def main() -> int:
    errors = validate_all()
    if errors:
        print("CONTEXT_RECONSTRUCTION_V1=FAIL")
        for error in errors:
            print(f"- {error}")
        return 1
    print("CONTEXT_RECONSTRUCTION_V1=PASS_STRUCTURAL")
    print("claim_allowed=false")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
