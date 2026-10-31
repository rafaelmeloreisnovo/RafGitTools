#!/usr/bin/env python3
"""Offline, non-authorizing preflight. No secret, network, checkout or CI access.

A structurally valid packet is a review candidate, NEVER provider authorization.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SCHEMA = "rafgittools.private-ci-assurance-preflight.v1"
SHA = re.compile(r"[0-9a-f]{40}\Z")
ID = re.compile(r"[a-z][a-z0-9_-]{0,63}\Z")
GATES = (
    "actor_attestation", "policy_authority", "exact_head", "purpose_limit",
    "token_step_scope", "runtime_network_ipv4_ipv6_udp_dns", "filesystem_isolation",
    "process_tree_cleanup", "bounded_stdout_stderr", "receipt_noninterference",
    "idempotency_concurrency", "reproducible_independent_test",
)
STATES = {"TOKEN_VAZIO", "NOT_RUN", "FAIL", "PASS_WITH_RECEIPT"}
REQUIRED = {
    "schema", "intent_id", "control_repo", "control_commit", "target_id",
    "target_commit", "workflow_id", "purpose", "actor_type", "mutation",
    "publish", "uses_real_data", "claim_allowed", "gates", "evidence_refs",
}
# These fields are references/states, never credentials or private paths.
ALLOWED_PURPOSES = {"synthetic_preflight", "bounded_private_validation"}
ALLOWED_ACTOR_TYPES = {"human", "installation"}


def _symbolic_void(value: Any) -> str | None:
    """None, 0, False, empty, spaces and ASCII NUL are distinct unknowns."""
    if value is None:
        return "TOKEN_VAZIO_NULL"
    if value is False:
        return "TOKEN_VAZIO_FALSE"
    if type(value) is int and value == 0:
        return "TOKEN_VAZIO_ZERO"
    if type(value) is str:
        if value == "":
            return "TOKEN_VAZIO_EMPTY"
        if value.strip() == "":
            return "TOKEN_VAZIO_WHITESPACE"
        if "\x00" in value:
            return "TOKEN_VAZIO_NUL_BYTE"
    return None


def assess(packet: Any, registry: Any) -> dict[str, Any]:
    """Produce fixed-vocabulary findings; never echo user-controlled data."""
    failed: list[str] = []
    unknown: list[str] = []
    if type(packet) is not dict or type(registry) is not dict:
        return {"schema": SCHEMA, "state": "BLOCKED", "failed": ["INVALID_ROOT"],
                "unknown": [], "claim_allowed": False}
    if packet.keys() != REQUIRED:
        failed.append("PACKET_FIELDS_EXACT")
    if packet.get("schema") != SCHEMA:
        failed.append("SCHEMA_MISMATCH")
    if registry.get("schema") != "rafgittools.private-ci-execution-registry.v1":
        failed.append("REGISTRY_SCHEMA_MISMATCH")
    if registry.get("claim_allowed") is not False:
        failed.append("REGISTRY_NOT_FAIL_CLOSED")
    if registry.get("secret_value_persisted") is not False:
        failed.append("REGISTRY_SECRET_BOUNDARY_UNKNOWN")

    for name in ("intent_id", "control_repo", "target_id", "workflow_id", "purpose", "actor_type"):
        void = _symbolic_void(packet.get(name))
        if void:
            unknown.append(f"{name.upper()}_{void}")
    for name in ("control_commit", "target_commit"):
        value = packet.get(name)
        if type(value) is not str or not SHA.fullmatch(value):
            unknown.append(f"{name.upper()}_TOKEN_VAZIO_EXACT_SHA")
    if type(packet.get("intent_id")) is not str or not ID.fullmatch(packet["intent_id"]):
        failed.append("INTENT_ID_INVALID")
    if packet.get("control_repo") != "rafaelmeloreisnovo/RafGitTools":
        failed.append("CONTROL_AUTHORITY_MISMATCH")
    if type(packet.get("purpose")) is not str or packet["purpose"] not in ALLOWED_PURPOSES:
        failed.append("PURPOSE_OUT_OF_SCOPE")
    if type(packet.get("actor_type")) is not str or packet["actor_type"] not in ALLOWED_ACTOR_TYPES:
        failed.append("ACTOR_TYPE_UNATTESTED")
    for name in ("mutation", "publish", "uses_real_data", "claim_allowed"):
        if packet.get(name) is not False:
            failed.append(f"{name.upper()}_MUST_BE_FALSE")
    targets = registry.get("targets")
    if type(targets) is not list:
        failed.append("REGISTRY_TARGETS_INVALID")
        targets = []
    matches = [t for t in targets if type(t) is dict and t.get("target_id") == packet.get("target_id")]
    if len(matches) != 1:
        failed.append("TARGET_NOT_UNIQUELY_ALLOWLISTED")
    elif type(packet.get("workflow_id")) is not str or packet["workflow_id"] not in matches[0].get("allowed_workflow_ids", []):
        failed.append("WORKFLOW_NOT_ALLOWLISTED")
    gates = packet.get("gates")
    refs = packet.get("evidence_refs")
    if type(gates) is not dict or set(gates) != set(GATES):
        failed.append("GATE_SET_EXACT")
        gates = {}
    if type(refs) is not dict or set(refs) != set(GATES):
        failed.append("EVIDENCE_SET_EXACT")
        refs = {}
    for gate in GATES:
        state = gates.get(gate)
        ref = refs.get(gate)
        if type(state) is not str or state not in STATES:
            unknown.append(f"{gate.upper()}_TOKEN_VAZIO_STATE")
        elif state == "FAIL":
            failed.append(f"{gate.upper()}_FAILED")
        elif state != "PASS_WITH_RECEIPT":
            unknown.append(f"{gate.upper()}_{state}")
        # No source/receipt validation is performed offline. A syntactically valid
        # reference CANNOT prove a gate passed and CANNOT authorize execution.
        if state == "PASS_WITH_RECEIPT":
            if type(ref) is not str or not re.fullmatch(r"(?:github|drive):[A-Za-z0-9_./@:-]{6,200}", ref):
                unknown.append(f"{gate.upper()}_TOKEN_VAZIO_RECEIPT_REF")
        elif ref is not None:
            failed.append(f"{gate.upper()}_UNVERIFIED_RECEIPT_PRESENT")

    failed = sorted(set(failed))
    unknown = sorted(set(unknown))
    state = "BLOCKED" if failed else "HOLD" if unknown else "REVIEW_REQUIRED"
    return {"schema": SCHEMA, "state": state, "failed": failed,
            "unknown": unknown, "claim_allowed": False,
            "operation_permitted": False,
            "evidence_scope": "OFFLINE_STATIC_ONLY"}


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--packet", type=Path, required=True)
    p.add_argument("--registry", type=Path, required=True)
    args = p.parse_args()
    result = assess(json.loads(args.packet.read_text(encoding="utf-8")),
                    json.loads(args.registry.read_text(encoding="utf-8")))
    print(json.dumps(result, sort_keys=True, ensure_ascii=False))
    return 0 if result["state"] == "REVIEW_REQUIRED" else 2


if __name__ == "__main__":
    raise SystemExit(main())
