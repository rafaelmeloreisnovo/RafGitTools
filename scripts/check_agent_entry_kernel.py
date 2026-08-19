#!/usr/bin/env python3
"""Validate the RAFAELIA federated agent-entry kernel.

This validator proves structural coherence of the entry contract only. It does not
prove remote repository state, physical runtime, or scientific claims.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "configs" / "agent-entry-kernel.v1.json"

REQUIRED_AXES = {
    "knowledge_state",
    "attention_state",
    "urgency",
    "operational_state",
    "claim_gate",
}
REQUIRED_ROLES = {"rafgittools", "termux_app", "termux_packages", "vectra", "llama"}
REQUIRED_INDICES = {"federation_topology", "gap_contract", "gap_ledger", "local_agent_router"}
REQUIRED_STEPS = {
    "E01_BIND",
    "E02_ROUTE",
    "E03_LOAD_GAPS",
    "E04_SELECT",
    "E05_BASELINE",
    "E06_EXECUTE",
    "E07_VERIFY_LOCAL",
    "E08_VERIFY_EDGES",
    "E09_RECEIPT",
    "E10_APPEND",
}
REQUIRED_RECEIPT_FIELDS = {
    "event_id",
    "parent_event_id",
    "observed_at",
    "agent_role",
    "repository",
    "source_commit",
    "attention_state_before",
    "attention_state_after",
    "knowledge_state_before",
    "knowledge_state_after",
    "urgency",
    "action",
    "invariants_checked",
    "evidence_refs",
    "f_ok",
    "f_gap",
    "f_next",
    "uncertainty_delta",
    "rollback_ref",
    "claim_allowed",
}


def load() -> dict:
    data = json.loads(CONTRACT.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("contract root must be an object")
    return data


def validate(data: dict) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != "rafaelia.agent-entry-kernel.v1":
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")

    principles = data.get("principles")
    if not isinstance(principles, list) or len(principles) < 8:
        errors.append("principles must contain at least 8 invariants")

    axes = data.get("orthogonal_axes")
    if not isinstance(axes, dict) or set(axes) != REQUIRED_AXES:
        errors.append("orthogonal_axes mismatch")
    else:
        for name, values in axes.items():
            if not isinstance(values, list) or not values or len(values) != len(set(values)):
                errors.append(f"axis {name} must be a non-empty unique list")
        if "TOKEN_VAZIO" not in axes.get("knowledge_state", []):
            errors.append("knowledge_state must preserve TOKEN_VAZIO")
        if "IGNORED_WITH_REASON" not in axes.get("attention_state", []):
            errors.append("attention_state must preserve ignored-with-reason work")
        if axes.get("urgency") != ["P0", "P1", "P2", "P3"]:
            errors.append("urgency ordering must be P0..P3")

    seq = data.get("entry_sequence")
    if not isinstance(seq, list):
        errors.append("entry_sequence must be a list")
    else:
        ids = [step.get("step") for step in seq if isinstance(step, dict)]
        if set(ids) != REQUIRED_STEPS or len(ids) != len(REQUIRED_STEPS):
            errors.append("entry_sequence must contain E01..E10 exactly once")
        if ids != sorted(ids):
            errors.append("entry_sequence must remain deterministically ordered")

    indices = data.get("canonical_indices")
    if not isinstance(indices, dict) or set(indices) != REQUIRED_INDICES:
        errors.append("canonical_indices mismatch")
    else:
        for label, relpath in indices.items():
            if not isinstance(relpath, str) or not relpath.strip():
                errors.append(f"canonical index {label} must be non-empty")
            elif label != "local_agent_router" and not (ROOT / relpath).exists():
                errors.append(f"canonical index {label} missing: {relpath}")
        if indices.get("local_agent_router") != "AGENTS.md":
            errors.append("local_agent_router must be AGENTS.md")

    roles = data.get("federation_roles")
    if not isinstance(roles, dict) or set(roles) != REQUIRED_ROLES:
        errors.append("federation_roles mismatch")
    elif any(not isinstance(v, str) or len(v.strip()) < 20 for v in roles.values()):
        errors.append("every federation role needs a meaningful local authority description")

    receipt = data.get("transition_receipt_required")
    if not isinstance(receipt, list) or set(receipt) != REQUIRED_RECEIPT_FIELDS:
        errors.append("transition receipt fields mismatch")

    forbidden = data.get("forbidden_shortcuts")
    if not isinstance(forbidden, list) or len(forbidden) < 5:
        errors.append("forbidden_shortcuts must remain explicit")

    return errors


def main() -> int:
    try:
        data = load()
        errors = validate(data)
    except Exception as exc:
        print(json.dumps({"schema": "rafaelia.agent-entry-kernel.report.v1", "status": "BLOCKED", "claim_allowed": False, "errors": [str(exc)]}, indent=2))
        return 2

    report = {
        "schema": "rafaelia.agent-entry-kernel.report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "entry_steps": len(data.get("entry_sequence", [])),
        "roles": sorted(data.get("federation_roles", {}).keys()),
        "axes": sorted(data.get("orthogonal_axes", {}).keys()),
        "errors": errors,
        "boundary": "Structural PASS does not prove remote repositories, device runtime, or claim validity.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
