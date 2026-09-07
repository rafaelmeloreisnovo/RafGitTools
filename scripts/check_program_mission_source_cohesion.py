#!/usr/bin/env python3
"""Validate separation between mission, dataset, model, LEARN and execution authority.

This is a structural governance gate. A PASS proves only that the repository contract
preserves the declared authority boundaries; it does not prove remote Mapa state,
model behavior, runtime/device execution, training, or scientific claims.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "configs" / "program-mission-source-cohesion.v1.json"

REQUIRED_PLANES = {"MISSION", "DATASET", "MODEL_PROPOSAL", "LEARN", "EXECUTION_GATE"}
REQUIRED_INVARIANTS = {
    "MISSION != DATASET",
    "MISSION != MODEL_PROPOSAL",
    "MISSION != LEARN",
    "DATASET != EXECUTION_AUTHORITY",
    "MODEL_PROPOSAL != EXECUTION_AUTHORITY",
    "LEARN != EXECUTION_AUTHORITY",
    "LEARN == APPEND_ONLY_MEMORY",
    "TRAINING != PROGRAM_EXECUTION",
    "WEIGHT_UPDATE != LEARNING_RECEIPT",
    "TOKEN_VAZIO != PASS",
}
REQUIRED_FORBIDDEN = {
    "DATASET -> MISSION_AUTHORITY",
    "MODEL_PROPOSAL -> MISSION_AUTHORITY",
    "MODEL_PROPOSAL -> SELF_AUTHORIZED_EXECUTION",
    "LEARN -> MISSION_AUTHORITY",
    "LEARN -> EXECUTION_AUTHORITY",
    "LEARN -> WEIGHT_UPDATE",
    "RECEIPT -> CLAIM_PROMOTION_WITHOUT_GATE",
    "TOKEN_VAZIO -> PASS",
}
REQUIRED_EXECUTION_INPUTS = {
    "EXPLICIT_MISSION_BINDING",
    "WRITE_SCOPE",
    "AUTHORITY",
    "EXIT_CRITERION",
    "ROLLBACK_WHEN_MUTATING",
    "GOVERNANCE_DATA_PRIVACY_SECURITY_GATES",
}
REQUIRED_FLOW_EDGES = {
    "MISSION -> ROUTING",
    "DATASET -> CONTEXT",
    "CONTEXT -> MODEL_PROPOSAL",
    "MODEL_PROPOSAL -> EXECUTION_GATE",
    "MISSION -> EXECUTION_GATE",
    "EXECUTION_GATE -> BOUNDED_EXECUTION",
    "BOUNDED_EXECUTION -> RECEIPT",
    "RECEIPT -> LEARN_APPEND_ONLY",
}


def load() -> dict:
    data = json.loads(CONTRACT.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("contract root must be an object")
    return data


def validate(data: dict) -> list[str]:
    errors: list[str] = []

    if data.get("schema") != "rafaelia.program-mission-source-cohesion.v1":
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")

    authority = data.get("authority")
    if not isinstance(authority, dict):
        errors.append("authority must be an object")
    else:
        if authority.get("federated_routing") != "rafaelmeloreisnovo/Mapa":
            errors.append("federated routing authority must remain Mapa")
        if authority.get("local_executor") != "rafaelmeloreisnovo/RafGitTools":
            errors.append("local executor must remain RafGitTools")
        if not isinstance(authority.get("program_mission"), str) or not authority["program_mission"].strip():
            errors.append("program mission authority source must be explicit")

    planes = data.get("planes")
    if not isinstance(planes, dict) or set(planes) != REQUIRED_PLANES:
        errors.append("planes must contain mission, dataset, model, LEARN and execution gate exactly")
        planes = {}

    mission = planes.get("MISSION", {})
    dataset = planes.get("DATASET", {})
    model = planes.get("MODEL_PROPOSAL", {})
    learn = planes.get("LEARN", {})
    gate = planes.get("EXECUTION_GATE", {})

    if mission.get("role") != "authoritative_goal_source":
        errors.append("MISSION must remain the authoritative goal source")
    if mission.get("may_grant_execution") is not False:
        errors.append("MISSION alone must not grant execution")
    if mission.get("may_update_weights") is not False:
        errors.append("MISSION must not authorize weight updates")

    if dataset.get("role") != "informational_context_only":
        errors.append("DATASET must remain informational context only")
    for key in ("may_define_or_rewrite_mission", "may_grant_execution", "may_update_weights"):
        if dataset.get(key) is not False:
            errors.append(f"DATASET {key} must remain false")

    if model.get("role") != "proposal_only":
        errors.append("MODEL_PROPOSAL must remain proposal only")
    for key in ("may_define_or_rewrite_mission", "may_self_authorize_execution", "may_update_weights"):
        if model.get(key) is not False:
            errors.append(f"MODEL_PROPOSAL {key} must remain false")

    if learn.get("role") != "append_only_memory_only" or learn.get("append_only") is not True:
        errors.append("LEARN must remain append-only memory only")
    for key in ("may_define_or_rewrite_mission", "may_grant_execution", "may_update_weights", "may_rewrite_history"):
        if learn.get(key) is not False:
            errors.append(f"LEARN {key} must remain false")

    if gate.get("role") != "bounded_authority_check":
        errors.append("EXECUTION_GATE must remain a bounded authority check")
    if set(gate.get("requires", [])) != REQUIRED_EXECUTION_INPUTS:
        errors.append("EXECUTION_GATE required inputs mismatch")
    for key in ("dataset_is_not_authority", "model_is_not_authority", "learn_is_not_authority"):
        if gate.get(key) is not True:
            errors.append(f"EXECUTION_GATE {key} must remain true")

    binding = data.get("mission_binding")
    if not isinstance(binding, dict):
        errors.append("mission_binding must be an object")
    else:
        if set(binding.get("accepted_sources", [])) != {"EXPLICIT_OPERATOR_INTENT", "MAPA_BOUND_MISSION"}:
            errors.append("mission_binding accepted sources mismatch")
        if binding.get("unknown_state") != "TOKEN_VAZIO_MISSION_BINDING":
            errors.append("unknown mission binding must remain typed TOKEN_VAZIO")
        if binding.get("unknown_behavior") != "BLOCK_MUTATION_ROUTE_ONLY":
            errors.append("unknown mission binding must fail closed for mutation")
        for key in ("dataset_copy_is_authoritative", "model_inference_is_authoritative", "learned_memory_is_authoritative"):
            if binding.get(key) is not False:
                errors.append(f"mission_binding {key} must remain false")

    training = data.get("training_boundary")
    if not isinstance(training, dict):
        errors.append("training_boundary must be an object")
    else:
        if training.get("training_or_weight_update_default") != "OUT_OF_SCOPE":
            errors.append("training/weight update must remain out of scope by default")
        if training.get("explicit_training_authorization_required") is not True:
            errors.append("training must require explicit authorization")
        if training.get("this_contract_authorizes_training") is not False:
            errors.append("this contract must not authorize training")
        if training.get("this_contract_authorizes_weight_updates") is not False:
            errors.append("this contract must not authorize weight updates")

    if set(data.get("invariants", [])) != REQUIRED_INVARIANTS:
        errors.append("mission cohesion invariants mismatch")
    if set(data.get("forbidden_promotions", [])) != REQUIRED_FORBIDDEN:
        errors.append("forbidden authority promotions mismatch")

    # The declared graph is part of the authority contract. Permission flags and
    # invariant text cannot compensate for a bypass edge or a missing gate.
    # Edges are relations, so their order in the JSON array is not significant.
    flow = data.get("flow")
    if not isinstance(flow, list) or not all(isinstance(edge, str) for edge in flow):
        errors.append("flow must be an array of string edges")
    elif len(flow) != len(REQUIRED_FLOW_EDGES) or set(flow) != REQUIRED_FLOW_EDGES:
        errors.append("flow must contain each canonical mission/gate/receipt edge exactly once")

    stops = data.get("stop_conditions")
    if not isinstance(stops, list) or len(stops) < 5:
        errors.append("stop_conditions must remain explicit")
    elif not any("TOKEN_VAZIO" in item for item in stops if isinstance(item, str)):
        errors.append("stop_conditions must preserve TOKEN_VAZIO fail-closed behavior")

    return errors


def main() -> int:
    try:
        data = load()
        errors = validate(data)
    except Exception as exc:
        report = {
            "schema": "rafaelia.program-mission-source-cohesion.report.v1",
            "status": "BLOCKED",
            "claim_allowed": False,
            "errors": [str(exc)],
        }
        print(json.dumps(report, indent=2, ensure_ascii=False))
        return 2

    report = {
        "schema": "rafaelia.program-mission-source-cohesion.report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "mission_authority": data.get("authority", {}).get("program_mission"),
        "planes": sorted(data.get("planes", {}).keys()),
        "training_authorized": False,
        "weight_update_authorized": False,
        "errors": errors,
        "boundary": "Structural PASS proves only mission/source authority separation in this repository contract; it does not prove remote mission state, model behavior, training, runtime/device execution, or claim validity.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
