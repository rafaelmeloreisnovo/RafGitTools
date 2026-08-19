#!/usr/bin/env python3
"""Validate the RAFAELIA federated agent-entry/service kernel.

This validator proves structural coherence of the entry contract only. It does not
prove remote Mapa state, producer repositories, physical runtime, privacy compliance
of an entire dataset, or scientific claims.
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
REQUIRED_DIMENSIONS = {
    "epistemic",
    "operational",
    "provenance",
    "governance",
    "data",
    "privacy",
    "security",
    "reconstruction",
}
REQUIRED_ROLES = {"mapa", "rafgittools", "termux_app", "termux_packages", "vectra", "llama", "drive"}
REQUIRED_INDICES = {
    "federation_topology",
    "gap_contract",
    "gap_ledger",
    "local_agent_router",
    "mapa_work_service_contract",
    "mapa_fgap_fnext_transit_index",
}
LOCAL_INDEX_KEYS = {"federation_topology", "gap_contract", "gap_ledger", "local_agent_router"}
MAPA_INDEX_KEYS = {"mapa_work_service_contract", "mapa_fgap_fnext_transit_index"}
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
REQUIRED_QUESTIONS = {
    "Q01_IDENTITY",
    "Q02_OBJECT_BINDING",
    "Q03_AUTHORITY",
    "Q04_BOUNDARY",
    "Q05_INDICES",
    "Q06_MAP_ROUTE",
    "Q07_GAPS",
    "Q08_CURRENT_EVIDENCE",
    "Q09_GATE",
    "Q10_STOP",
    "Q11_DELTA",
    "Q12_GOV_DATA_PRIV_SEC",
}
REQUIRED_RECEIPT_FIELDS = {
    "event_id",
    "parent_event_id",
    "observed_at",
    "agent_role",
    "repository",
    "ref",
    "source_commit",
    "path_scope",
    "authority",
    "gap_or_goal_ids",
    "attention_state_before",
    "attention_state_after",
    "knowledge_state_before",
    "knowledge_state_after",
    "urgency",
    "risk",
    "governance_class",
    "data_class",
    "privacy_class",
    "security_class",
    "action",
    "falsifier",
    "exit_criterion",
    "stop_reason",
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


def _unique_nonempty_list(value: object) -> bool:
    return isinstance(value, list) and bool(value) and len(value) == len(set(value))


def validate(data: dict) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != "rafaelia.agent-entry-kernel.v1":
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if data.get("federated_authority") != "rafaelmeloreisnovo/Mapa":
        errors.append("federated_authority must remain Mapa")

    principles = data.get("principles")
    if not isinstance(principles, list) or len(principles) < 12:
        errors.append("principles must contain at least 12 invariants")
    elif not any("privacy" in p.lower() and "security" in p.lower() for p in principles if isinstance(p, str)):
        errors.append("principles must preserve explicit privacy/security boundary")

    axes = data.get("orthogonal_axes")
    if not isinstance(axes, dict) or set(axes) != REQUIRED_AXES:
        errors.append("orthogonal_axes mismatch")
    else:
        for name, values in axes.items():
            if not _unique_nonempty_list(values):
                errors.append(f"axis {name} must be a non-empty unique list")
        if "TOKEN_VAZIO" not in axes.get("knowledge_state", []):
            errors.append("knowledge_state must preserve TOKEN_VAZIO")
        if "IGNORED_WITH_REASON" not in axes.get("attention_state", []):
            errors.append("attention_state must preserve ignored-with-reason work")
        if axes.get("urgency") != ["P0", "P1", "P2", "P3"]:
            errors.append("urgency ordering must be P0..P3")

    dimensions = data.get("mandatory_service_dimensions")
    if not isinstance(dimensions, dict) or set(dimensions) != REQUIRED_DIMENSIONS:
        errors.append("mandatory_service_dimensions mismatch")
    else:
        for name, fields in dimensions.items():
            if not _unique_nonempty_list(fields):
                errors.append(f"service dimension {name} must contain unique required fields")
        for critical in ("governance", "data", "privacy", "security"):
            if len(dimensions.get(critical, [])) < 5:
                errors.append(f"service dimension {critical} is under-specified")

    questions = data.get("entry_questions")
    if not isinstance(questions, list):
        errors.append("entry_questions must be a list")
    else:
        ids = [q.get("id") for q in questions if isinstance(q, dict)]
        if set(ids) != REQUIRED_QUESTIONS or len(ids) != len(REQUIRED_QUESTIONS):
            errors.append("entry_questions must contain Q01..Q12 exactly once")
        if ids != sorted(ids):
            errors.append("entry_questions must remain deterministically ordered")
        for q in questions:
            if not isinstance(q, dict):
                continue
            if not isinstance(q.get("question"), str) or not q["question"].strip():
                errors.append(f"question {q.get('id')} text missing")
            required = q.get("required")
            if not _unique_nonempty_list(required):
                errors.append(f"question {q.get('id')} required outputs missing")

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
        for label, pointer in indices.items():
            if not isinstance(pointer, str) or not pointer.strip():
                errors.append(f"canonical index {label} must be non-empty")
                continue
            if label in LOCAL_INDEX_KEYS:
                if label == "local_agent_router":
                    if pointer != "AGENTS.md":
                        errors.append("local_agent_router must be AGENTS.md")
                elif not (ROOT / pointer).exists():
                    errors.append(f"canonical index {label} missing: {pointer}")
            elif label in MAPA_INDEX_KEYS and not pointer.startswith("github:rafaelmeloreisnovo/Mapa/"):
                errors.append(f"Mapa index {label} must be an explicit Mapa pointer")

    roles = data.get("federation_roles")
    if not isinstance(roles, dict) or set(roles) != REQUIRED_ROLES:
        errors.append("federation_roles mismatch")
    elif any(not isinstance(v, str) or len(v.strip()) < 20 for v in roles.values()):
        errors.append("every federation role needs a meaningful authority description")

    receipt = data.get("transition_receipt_required")
    if not isinstance(receipt, list) or set(receipt) != REQUIRED_RECEIPT_FIELDS:
        errors.append("transition receipt fields mismatch")

    stops = data.get("stop_conditions")
    if not isinstance(stops, list) or len(stops) < 5:
        errors.append("stop_conditions must remain explicit and non-empty")
    elif not any("privacy" in s.lower() or "security" in s.lower() for s in stops if isinstance(s, str)):
        errors.append("stop_conditions must fail closed on privacy/security")

    forbidden = data.get("forbidden_shortcuts")
    if not isinstance(forbidden, list) or len(forbidden) < 8:
        errors.append("forbidden_shortcuts must remain explicit")
    elif not any("hardcode" in item.lower() and "security" in item.lower() for item in forbidden if isinstance(item, str)):
        errors.append("forbidden_shortcuts must reject hardcoded security success")

    return errors


def main() -> int:
    try:
        data = load()
        errors = validate(data)
    except Exception as exc:
        print(json.dumps({
            "schema": "rafaelia.agent-entry-kernel.report.v1",
            "status": "BLOCKED",
            "claim_allowed": False,
            "errors": [str(exc)],
        }, indent=2))
        return 2

    report = {
        "schema": "rafaelia.agent-entry-kernel.report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "entry_steps": len(data.get("entry_sequence", [])),
        "entry_questions": len(data.get("entry_questions", [])),
        "roles": sorted(data.get("federation_roles", {}).keys()),
        "axes": sorted(data.get("orthogonal_axes", {}).keys()),
        "service_dimensions": sorted(data.get("mandatory_service_dimensions", {}).keys()),
        "errors": errors,
        "boundary": "Structural PASS does not prove Mapa current state, producer runtime, dataset-wide privacy/security, physical devices, or claim validity.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
