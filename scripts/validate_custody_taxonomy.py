#!/usr/bin/env python3
"""Fail-closed validator for the RAFAELIA custody taxonomy contract."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONTRACT = ROOT / "contracts" / "custody-taxonomy-v1.json"
DEFAULT_EVENT_SCHEMA = ROOT / "contracts" / "custody-event-v1.schema.json"

EXPECTED_CLASSES = {f"C{i:02d}_{name}" for i, name in [
    (1, "SOURCE_IDENTITY"),
    (2, "DOCUMENTARY_CUSTODY"),
    (3, "CODE_CUSTODY"),
    (4, "ARTIFACT_BYTE_CUSTODY"),
    (5, "EXECUTION_CUSTODY"),
    (6, "RECEIPT_CUSTODY"),
    (7, "AUTHORIZATION_CUSTODY"),
    (8, "TRANSFER_BRIDGE_CUSTODY"),
    (9, "HUMAN_OBSERVATION_CUSTODY"),
    (10, "ASSISTANT_TOOL_ACTION_CUSTODY"),
]}

EXPECTED_STATES = {
    "PASS", "FAIL", "NOT_RUN", "PENDING", "AUDIT", "TOKEN_VAZIO",
    "IMPLEMENTED_UNTESTED", "OBSERVED_UNPROMOTED", "ROUTE_STATE_BLOCKED",
}

def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))

def validate(contract: dict[str, Any], event_schema: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if contract.get("schema") != "rafgittools.custody-taxonomy.v1":
        errors.append("contract schema id mismatch")
    if contract.get("version") != "1.0.1":
        errors.append("contract version mismatch")
    if contract.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("local authority must remain RafGitTools")

    invariants = set(contract.get("invariants", []))
    required_invariants = {
        "SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM",
        "TOKEN_VAZIO != 0",
        "IMPLEMENTED_UNTESTED != PASS",
        "assistant_observation != provider_authority",
        "human_authorization != execution_evidence",
        "assistant_orchestration != connector_provider_execution",
    }
    missing = sorted(required_invariants - invariants)
    if missing:
        errors.append(f"missing invariants: {missing}")

    actors = contract.get("actors", {})
    assistant = actors.get("ASSISTANT_TOOL_OPERATOR", {})
    human = actors.get("HUMAN_AUTHOR", {})
    if assistant.get("may_authorize") is not False:
        errors.append("assistant must never become human authorization authority")
    if assistant.get("may_execute") is not False:
        errors.append("assistant orchestrator must not become connector/provider executor")
    if human.get("may_authorize") is not True:
        errors.append("human authorization role must remain explicit")
    assistant_forbidden = set(assistant.get("cannot_self_promote", []))
    if "human authorization" not in assistant_forbidden:
        errors.append("assistant boundary must forbid self-promotion to human authorization")

    federation = contract.get("federation_authority", {})
    if federation.get("control_plane") != "rafaelmeloreisnovo/Mapa":
        errors.append("federation control plane must remain Mapa")
    if federation.get("registry_path") != "data/control-plane/CUSTODY_CHAIN_TYPE_REGISTRY.v1.json":
        errors.append("federation registry path mismatch")
    mapping = contract.get("federation_actor_mapping", {}).get("local_to_global", {})
    expected_mapping = {
        "HUMAN_AUTHOR": "HUMAN_AUTHORITY",
        "ASSISTANT_TOOL_OPERATOR": "ASSISTANT_ORCHESTRATOR",
        "PROVIDER": "CONNECTOR_PROVIDER",
        "PHYSICAL_DEVICE": "RUNTIME_EXECUTOR",
    }
    if mapping != expected_mapping:
        errors.append("federation actor mapping mismatch")

    classes = contract.get("custody_classes", [])
    class_ids = [item.get("id") for item in classes]
    if len(class_ids) != len(set(class_ids)):
        errors.append("duplicate custody class id")
    if set(class_ids) != EXPECTED_CLASSES:
        errors.append(f"custody class set mismatch: {sorted(set(class_ids) ^ EXPECTED_CLASSES)}")
    for item in classes:
        if not item.get("minimum_anchors"):
            errors.append(f"{item.get('id')}: minimum_anchors must not be empty")
        if not item.get("authority_domain"):
            errors.append(f"{item.get('id')}: authority_domain missing")
        if not item.get("evidence_effect"):
            errors.append(f"{item.get('id')}: evidence_effect missing")

    states = set(contract.get("states", []))
    if states != EXPECTED_STATES:
        errors.append(f"state set mismatch: {sorted(states ^ EXPECTED_STATES)}")

    claim_gate = contract.get("claim_gate", {})
    if claim_gate.get("automatic_promotion") is not False:
        errors.append("automatic claim promotion must remain false")
    if claim_gate.get("claim_allowed_default") is not False:
        errors.append("claim_allowed_default must remain false")

    bridges = {item.get("id"): item for item in contract.get("bridge_rules", [])}
    if set(bridges) != {"BR01", "BR02", "BR03", "BR04"}:
        errors.append("bridge rule set must be BR01..BR04")
    if bridges.get("BR03", {}).get("relation") != "TOOL_ACTION_WITH_READBACK":
        errors.append("assistant/provider bridge must require tool action with readback")
    if bridges.get("BR04", {}).get("relation") != "AUTHORIZED_INTENT":
        errors.append("human/assistant bridge must remain authorized intent, not PASS evidence")

    props = event_schema.get("properties", {})
    schema_classes = set(props.get("custodyClass", {}).get("enum", []))
    schema_states = set(props.get("state", {}).get("enum", []))
    if schema_classes != EXPECTED_CLASSES:
        errors.append("event schema custodyClass enum differs from taxonomy")
    if schema_states != EXPECTED_STATES:
        errors.append("event schema state enum differs from taxonomy")
    required_event_fields = {
        "schemaVersion", "receiptId", "custodyClass", "actorClass", "provider",
        "authorityDomain", "sourceRef", "targetRef", "state", "claimAllowed",
        "evidenceRefs", "gap", "next", "observedAt",
    }
    if not required_event_fields.issubset(set(event_schema.get("required", []))):
        errors.append("event schema missing fail-closed required fields")

    return errors

def main(argv: list[str]) -> int:
    contract_path = Path(argv[1]) if len(argv) > 1 else DEFAULT_CONTRACT
    event_schema_path = Path(argv[2]) if len(argv) > 2 else DEFAULT_EVENT_SCHEMA
    contract = _load(contract_path)
    event_schema = _load(event_schema_path)
    errors = validate(contract, event_schema)
    if errors:
        for error in errors:
            print(f"FAIL: {error}")
        return 1
    print("PASS custody-taxonomy-v1")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
