#!/usr/bin/env python3
"""Validate the RafGitTools seven-guard knowledge/work adapter.

Structural readiness here never promotes a producer/domain claim. The adapter only
checks whether provenance, context, evidence, contradiction, uncertainty,
reproduction and rollback have explicit, reconstructible control-plane states.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs" / "knowledge-work-seven-guards.v1.json"

REQUIRED_TOP = {
    "id", "intent", "authority", "source_contract_ref", "provenance", "context",
    "evidence", "contradictions", "uncertainty", "reproduction", "rollback",
    "reconstruction_pointer", "mutation_performed", "claim_allowed", "next"
}
PROVENANCE_REQUIRED = {
    "source_provider", "repository", "ref", "path", "object_hash", "observed_at", "authority"
}
CONTEXT_REQUIRED = {"intent", "scope", "boundary", "observed_at", "dependencies"}
EVIDENCE_REQUIRED = {"ref", "type", "scope"}
CONTRADICTION_REQUIRED = {"id", "state", "comparison_scope", "ref"}
UNCERTAINTY_REQUIRED = {"id", "state", "evidence_needed", "falsifier", "next_probe"}
REPRODUCTION_REQUIRED = {"status", "procedure", "environment_ref", "input_ref", "output_ref"}
ROLLBACK_REQUIRED = {"state", "predecessor", "procedure", "verification"}

CONTRADICTION_STATES = {"OPEN", "RESOLVED", "SUPERSEDED"}
UNCERTAINTY_STATES = {"TOKEN_VAZIO", "BLOCKED", "PARTIAL", "NOT_APPLICABLE", "CLOSED"}
REPRODUCTION_STATES = {"TOKEN_VAZIO", "BLOCKED", "PASS", "FAIL", "NOT_APPLICABLE"}
ROLLBACK_STATES = {"TOKEN_VAZIO", "READY", "EXECUTED", "NOT_APPLICABLE"}
UNRESOLVED_UNCERTAINTY = {"TOKEN_VAZIO", "BLOCKED", "PARTIAL"}
BLOCKING_REPRODUCTION = {"TOKEN_VAZIO", "BLOCKED", "FAIL"}

def _nonempty(value: object) -> bool:
    return isinstance(value, str) and bool(value.strip())

def _missing(mapping: object, required: set[str]) -> list[str]:
    if not isinstance(mapping, dict):
        return sorted(required)
    return sorted(k for k in required if k not in mapping)

def validate_envelope(unit: dict) -> tuple[list[str], list[str], str]:
    errors: list[str] = []
    blockers: list[str] = []

    missing_top = sorted(REQUIRED_TOP - set(unit))
    errors.extend(f"missing:{key}" for key in missing_top)
    if missing_top:
        return errors, blockers, "BLOCKED"

    if unit.get("claim_allowed") is not False:
        errors.append("claim_gate:RafGitTools_claim_allowed_must_remain_false")

    if not _nonempty(unit.get("reconstruction_pointer")):
        errors.append("reconstruction_pointer:missing")

    provenance = unit.get("provenance")
    for key in _missing(provenance, PROVENANCE_REQUIRED):
        errors.append(f"provenance:missing_{key}")
    if isinstance(provenance, dict):
        for key in PROVENANCE_REQUIRED - {"object_hash"}:
            if not _nonempty(provenance.get(key)):
                errors.append(f"provenance:empty_{key}")
        if provenance.get("object_hash") in (None, "", "TOKEN_VAZIO"):
            blockers.append("provenance:object_hash_unresolved")

    context = unit.get("context")
    for key in _missing(context, CONTEXT_REQUIRED):
        errors.append(f"context:missing_{key}")
    if isinstance(context, dict):
        for key in {"intent", "scope", "boundary", "observed_at"}:
            if not _nonempty(context.get(key)):
                errors.append(f"context:empty_{key}")
        if not isinstance(context.get("dependencies"), list):
            errors.append("context:dependencies_must_be_list")

    evidence = unit.get("evidence")
    if not isinstance(evidence, list):
        errors.append("evidence:must_be_list")
    elif not evidence:
        blockers.append("evidence:empty")
    else:
        for i, item in enumerate(evidence):
            for key in _missing(item, EVIDENCE_REQUIRED):
                errors.append(f"evidence[{i}]:missing_{key}")
            if isinstance(item, dict) and any(not _nonempty(item.get(k)) for k in EVIDENCE_REQUIRED):
                errors.append(f"evidence[{i}]:empty_required_field")

    contradictions = unit.get("contradictions")
    if not isinstance(contradictions, list):
        errors.append("contradictions:must_be_list")
    else:
        for i, item in enumerate(contradictions):
            for key in _missing(item, CONTRADICTION_REQUIRED):
                errors.append(f"contradictions[{i}]:missing_{key}")
            if not isinstance(item, dict):
                continue
            state = item.get("state")
            if state not in CONTRADICTION_STATES:
                errors.append(f"contradictions[{i}]:invalid_state")
            if state == "OPEN":
                blockers.append(f"contradictions:{item.get('id', i)}:OPEN")

    uncertainty = unit.get("uncertainty")
    if not isinstance(uncertainty, list):
        errors.append("uncertainty:must_be_list")
    else:
        for i, item in enumerate(uncertainty):
            for key in _missing(item, UNCERTAINTY_REQUIRED):
                errors.append(f"uncertainty[{i}]:missing_{key}")
            if not isinstance(item, dict):
                continue
            state = item.get("state")
            if state not in UNCERTAINTY_STATES:
                errors.append(f"uncertainty[{i}]:invalid_state")
            if state in UNRESOLVED_UNCERTAINTY:
                for key in ("evidence_needed", "falsifier", "next_probe"):
                    if not _nonempty(item.get(key)):
                        errors.append(f"uncertainty[{i}]:{state}_requires_{key}")
                blockers.append(f"uncertainty:{item.get('id', i)}:{state}")

    reproduction = unit.get("reproduction")
    for key in _missing(reproduction, REPRODUCTION_REQUIRED):
        errors.append(f"reproduction:missing_{key}")
    if isinstance(reproduction, dict):
        state = reproduction.get("status")
        if state not in REPRODUCTION_STATES:
            errors.append("reproduction:invalid_status")
        if state in BLOCKING_REPRODUCTION:
            blockers.append(f"reproduction:{state}")
        if state == "PASS":
            for key in REPRODUCTION_REQUIRED - {"status"}:
                if not _nonempty(reproduction.get(key)):
                    errors.append(f"reproduction:PASS_requires_{key}")
        if state == "NOT_APPLICABLE" and not _nonempty(reproduction.get("procedure")):
            errors.append("reproduction:NOT_APPLICABLE_requires_reason_in_procedure")

    rollback = unit.get("rollback")
    for key in _missing(rollback, ROLLBACK_REQUIRED):
        errors.append(f"rollback:missing_{key}")
    if isinstance(rollback, dict):
        state = rollback.get("state")
        if state not in ROLLBACK_STATES:
            errors.append("rollback:invalid_state")
        if unit.get("mutation_performed") is True and state not in {"READY", "EXECUTED"}:
            blockers.append("rollback:mutation_without_ready_rollback")
        if state in {"READY", "EXECUTED"}:
            for key in ROLLBACK_REQUIRED - {"state"}:
                if not _nonempty(rollback.get(key)):
                    errors.append(f"rollback:{state}_requires_{key}")
        if state == "NOT_APPLICABLE" and unit.get("mutation_performed") is True:
            errors.append("rollback:NOT_APPLICABLE_invalid_for_mutation")

    decision = "READY_FOR_DOMAIN_REVIEW" if not errors and not blockers else "BLOCKED"
    return errors, blockers, decision

def load_config() -> dict:
    return json.loads(CONFIG.read_text(encoding="utf-8"))

def validate_config(cfg: dict) -> list[str]:
    errors: list[str] = []
    if cfg.get("schema") != "rafaelia.rafgittools.knowledge-work-seven-guards.v1":
        errors.append("config:schema_mismatch")
    if cfg.get("claim_allowed") is not False:
        errors.append("config:claim_allowed_must_be_false")
    guards = cfg.get("guards")
    expected = [
        "provenance", "context", "evidence", "contradictions",
        "uncertainty", "reproduction", "rollback"
    ]
    if not isinstance(guards, list) or [g.get("key") for g in guards if isinstance(g, dict)] != expected:
        errors.append("config:seven_guard_order_or_membership_mismatch")
    source = cfg.get("source_contract", {})
    if source.get("repository") != "rafaelmeloreisnovo/Mapa" or source.get("pull_request") != 619:
        errors.append("config:source_contract_must_bind_Mapa_PR619")
    return errors

def main() -> int:
    if len(sys.argv) not in {1, 2}:
        print("usage: validate_knowledge_work_seven_guards.py [ENVELOPE.json]", file=sys.stderr)
        return 2

    cfg_errors = validate_config(load_config())
    if cfg_errors:
        print(json.dumps({
            "schema": "rafaelia.rafgittools.knowledge-work-seven-guards.report.v1",
            "status": "FAIL",
            "claim_allowed": False,
            "errors": cfg_errors,
        }, indent=2, ensure_ascii=False))
        return 1

    if len(sys.argv) == 1:
        print(json.dumps({
            "schema": "rafaelia.rafgittools.knowledge-work-seven-guards.report.v1",
            "status": "PASS",
            "decision": "CONFIG_ONLY",
            "claim_allowed": False,
            "guards": 7,
            "boundary": "Structural config validation only; no producer/runtime/scientific claim is promoted.",
        }, indent=2, ensure_ascii=False))
        return 0

    path = Path(sys.argv[1])
    unit = json.loads(path.read_text(encoding="utf-8"))
    errors, blockers, decision = validate_envelope(unit)
    report = {
        "schema": "rafaelia.rafgittools.knowledge-work-seven-guards.report.v1",
        "status": "PASS" if not errors else "FAIL",
        "decision": decision,
        "claim_allowed": False,
        "errors": errors,
        "blockers": blockers,
        "boundary": "READY_FOR_DOMAIN_REVIEW is not claim promotion and does not prove producer runtime or scientific validity.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1

if __name__ == "__main__":
    raise SystemExit(main())
