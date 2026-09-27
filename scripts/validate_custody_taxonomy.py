#!/usr/bin/env python3
"""Fail-closed validator for the RAFAELIA custody taxonomy contract."""
from __future__ import annotations

import json
import re
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

ALLOWED_FEDERATED_PROFILES = {
    "GITHUB_SOURCE_CODE_CUSTODY",
    "GITHUB_REVIEW_PROMOTION_CUSTODY",
    "GITHUB_ACTIONS_EXECUTION_CUSTODY",
    "DRIVE_DOCUMENT_REVISION_CUSTODY",
    "DRIVE_CONTENT_MATERIALIZATION_CUSTODY",
    "DRIVE_MOVE_RENAME_CUSTODY",
    "TRANSFORMATION_LINEAGE_CUSTODY",
    "EVIDENCE_CUSTODY",
    "AGENT_ACTION_CUSTODY",
    "CROSS_SURFACE_BINDING_CUSTODY",
    "RECEIPT_CHAIN_CUSTODY",
}

def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))

def validate(contract: dict[str, Any], event_schema: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if contract.get("schema") != "rafgittools.custody-taxonomy.v1":
        errors.append("contract schema id mismatch")
    if contract.get("version") != "1.0.0":
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

    federation = contract.get("federated_authority", {})
    if federation.get("repository") != "rafaelmeloreisnovo/Mapa":
        errors.append("federated custody authority must remain Mapa")
    if federation.get("path") != "data/control-plane/CUSTODY_CHAIN_TYPE_REGISTRY.v1.json":
        errors.append("unexpected federated custody registry path")
    if federation.get("relation") != "LOCAL_EXECUTOR_PROJECTION":
        errors.append("RafGitTools custody taxonomy must remain a local executor projection")
    merge_commit = federation.get("observed_merge_commit", "")
    if not isinstance(merge_commit, str) or not re.fullmatch(r"[0-9a-f]{40}", merge_commit):
        errors.append("federated observed_merge_commit must be a full commit SHA")

    actors = contract.get("actors", {})
    assistant = actors.get("ASSISTANT_TOOL_OPERATOR", {})
    human = actors.get("HUMAN_AUTHOR", {})
    if assistant.get("may_authorize") is not False:
        errors.append("assistant must never become human authorization authority")
    if assistant.get("may_execute") is not False:
        errors.append("assistant orchestration must not be recorded as provider execution")
    if assistant.get("may_request_provider_mutation") is not True:
        errors.append("assistant must explicitly model provider mutation as a request")
    if assistant.get("may_execute_provider_mutation") is not False:
        errors.append("assistant cannot become connector/provider mutation executor")
    if human.get("may_authorize") is not True:
        errors.append("human authorization role must remain explicit")
    assistant_forbidden = set(assistant.get("cannot_self_promote", []))
    if "human authorization" not in assistant_forbidden:
        errors.append("assistant boundary must forbid self-promotion to human authorization")

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

    crosswalk = contract.get("federated_profile_crosswalk", {})
    if not isinstance(crosswalk, dict):
        errors.append("federated_profile_crosswalk must be an object")
    else:
        keys = set(crosswalk)
        if keys != EXPECTED_CLASSES:
            errors.append(f"federated crosswalk class set mismatch: {sorted(keys ^ EXPECTED_CLASSES)}")
        for class_id, profiles in crosswalk.items():
            if not isinstance(profiles, list) or not profiles:
                errors.append(f"{class_id}: federated profile mapping must be non-empty")
                continue
            unknown = sorted(set(profiles) - ALLOWED_FEDERATED_PROFILES)
            if unknown:
                errors.append(f"{class_id}: unknown federated profiles: {unknown}")

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
