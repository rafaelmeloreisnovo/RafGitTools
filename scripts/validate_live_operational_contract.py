#!/usr/bin/env python3
"""Fail-closed structural validator for the RAFAELIA live operational contract."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "contracts" / "live-operational-contract-v1.json"
STATE_SCHEMA = ROOT / "contracts" / "live-operational-state-v1.schema.json"

REQUIRED_INVARIANTS = {
    "SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM",
    "TOKEN_VAZIO != 0",
    "IMPLEMENTED_UNTESTED != PASS",
    "inference != fact",
    "context_growth != evolution",
    "authorization != execution_evidence",
    "coherence != claim_evidence",
    "missing_evidence blocks claim promotion",
}
REQUIRED_GATE_FIELDS = {
    "SOURCE", "AUTHORITY", "EXECUTION_TARGET", "EVIDENCE_RULE"
}
REQUIRED_STATE_FIELDS = {
    "SOURCE", "AUTHORITY", "ARTIFACT", "EXECUTION_TARGET", "EXECUTION",
    "EVIDENCE_RULE", "EVIDENCE", "CLAIM", "GAPS", "ROUTES",
    "ROUTE_STATE", "CLAIM_ALLOWED",
}
EXPECTED_SECTIONS = {
    "EXECUTIVE_SUMMARY", "ACTION_PLAN", "RISK_MATRIX",
    "MARKED_GAPS", "ATLAS_DELTA", "NEXT_CYCLE",
}

def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))

def validate(contract: dict[str, Any], schema: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if contract.get("schema") != "rafgittools.live-operational-contract.v1":
        errors.append("contract schema id mismatch")
    if contract.get("version") != "1.0.0":
        errors.append("contract version mismatch")
    if contract.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("contract authority mismatch")

    invariants = set(contract.get("invariants", []))
    missing_invariants = sorted(REQUIRED_INVARIANTS - invariants)
    if missing_invariants:
        errors.append(f"missing invariants: {missing_invariants}")

    sentinel = contract.get("semantic_sentinels", {}).get("TOKEN_VAZIO", {})
    distinct = set(sentinel.get("distinct_from", []))
    for required in {"0", "false", "FAIL", "NOT_RUN", "PENDING", "implicit_unknown"}:
        if required not in distinct:
            errors.append(f"TOKEN_VAZIO does not distinguish {required}")
    if sentinel.get("claim_effect") != "NO_PROMOTION":
        errors.append("TOKEN_VAZIO must not promote claims")

    pre_gate = contract.get("pre_execution_gate", {})
    if set(pre_gate.get("required", [])) != REQUIRED_GATE_FIELDS:
        errors.append("pre-execution gate field set mismatch")
    on_missing = pre_gate.get("on_missing", {})
    if on_missing.get("ROUTE_STATE") != "ROUTE_STATE_BLOCKED":
        errors.append("missing precondition must block route")
    if on_missing.get("EXECUTION") != "NOT_RUN":
        errors.append("missing precondition must keep execution NOT_RUN")
    if on_missing.get("CLAIM_ALLOWED") is not False:
        errors.append("missing precondition must keep claim false")

    response = contract.get("response_contract", {})
    if set(response.get("sections", [])) != EXPECTED_SECTIONS:
        errors.append("response section set mismatch")

    claim_gate = contract.get("claim_gate", {})
    if claim_gate.get("claim_allowed_default") is not False:
        errors.append("claim_allowed_default must remain false")

    if schema.get("$id") != "rafgittools.live-operational-state.v1":
        errors.append("state schema id mismatch")
    state_props = schema.get("properties", {}).get("state", {})
    if set(state_props.get("required", [])) != REQUIRED_STATE_FIELDS:
        errors.append("state required fields mismatch")

    response_props = schema.get("properties", {}).get("response", {})
    if set(response_props.get("required", [])) != EXPECTED_SECTIONS:
        errors.append("state response sections mismatch")

    execution_states = set(
        schema.get("$defs", {}).get("executionState", {}).get("enum", [])
    )
    for required in {
        "PASS", "FAIL", "NOT_RUN", "PENDING", "AUDIT", "TOKEN_VAZIO",
        "IMPLEMENTED_UNTESTED", "OBSERVED_UNPROMOTED", "ROUTE_STATE_BLOCKED",
    }:
        if required not in execution_states:
            errors.append(f"missing execution state: {required}")

    custody_ref = contract.get("custody_reference", {})
    if custody_ref.get("path") != "contracts/custody-taxonomy-v1.json":
        errors.append("contract must reference existing custody taxonomy")

    return errors

def main() -> int:
    contract = load(CONTRACT)
    schema = load(STATE_SCHEMA)
    errors = validate(contract, schema)
    if errors:
        for error in errors:
            print(f"FAIL: {error}")
        return 1
    print("PASS live-operational-contract-v1")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
