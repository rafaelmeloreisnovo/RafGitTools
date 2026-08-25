#!/usr/bin/env python3
"""Validate one bounded RAFAELIA federated work item.

The work item is an execution envelope, not evidence of success. The validator is
fail-closed on missing identity, authority, governance/data/privacy/security
classification, high-risk rollback, resolved-without-evidence, or claim promotion.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.federated-work-item.v1"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
URGENCY = {"P0", "P1", "P2", "P3"}
RISK = {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
STATES = {
    "TOKEN_VAZIO",
    "UNCERTAIN",
    "BLOCKED",
    "READY_TO_TEST",
    "TESTING",
    "EVIDENCED_SCOPED",
    "RESOLVED_NEGATIVE",
    "RESOLVED",
}
DATA_CLASSES = {"NONE", "PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "TOKEN_VAZIO"}
PRIVACY_CLASSES = {"NONE", "PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED", "TOKEN_VAZIO"}
SECURITY_CLASSES = {"LOW", "MEDIUM", "HIGH", "CRITICAL", "TOKEN_VAZIO"}
GOVERNANCE_CLASSES = {"LOCAL", "FEDERATED", "REVIEW_REQUIRED", "MUTATION_GATED", "TOKEN_VAZIO"}


def _nonempty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _list(value: Any) -> bool:
    return isinstance(value, list)


def _is_token(value: Any) -> bool:
    return isinstance(value, str) and value.startswith("TOKEN_VAZIO")


def _require_object(data: dict[str, Any], key: str, errors: list[str]) -> dict[str, Any]:
    value = data.get(key)
    if not isinstance(value, dict):
        errors.append(f"{key} must be an object")
        return {}
    return value


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != SCHEMA:
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if not _nonempty(data.get("work_item_id")):
        errors.append("work_item_id missing")
    if not _nonempty(data.get("observed_at")):
        errors.append("observed_at missing")
    if data.get("state") not in STATES:
        errors.append("invalid state")

    actor = _require_object(data, "actor", errors)
    for field in ("agent_role", "repository_role"):
        if not _nonempty(actor.get(field)):
            errors.append(f"actor.{field} missing")

    binding = _require_object(data, "object_binding", errors)
    for field in ("repository", "ref", "source_commit", "path_scope", "object_hash_or_TOKEN_VAZIO"):
        if not _nonempty(binding.get(field)):
            errors.append(f"object_binding.{field} missing")
    source_commit = binding.get("source_commit")
    if _nonempty(source_commit) and not (SHA40.fullmatch(source_commit) or _is_token(source_commit)):
        errors.append("object_binding.source_commit must be exact 40-hex SHA or typed TOKEN_VAZIO")

    authority = _require_object(data, "authority", errors)
    for field in ("local_authority", "federated_authority", "write_scope"):
        if not _nonempty(authority.get(field)):
            errors.append(f"authority.{field} missing")
    if authority.get("federated_authority") != "rafaelmeloreisnovo/Mapa":
        errors.append("authority.federated_authority must be rafaelmeloreisnovo/Mapa")

    boundary = _require_object(data, "boundary", errors)
    if boundary.get("claim_allowed") is not False:
        errors.append("boundary.claim_allowed must remain false")
    for field in ("allowed_claim_scope", "forbidden_promotions"):
        if not _list(boundary.get(field)) or not boundary.get(field):
            errors.append(f"boundary.{field} must be a non-empty list")

    indices = _require_object(data, "indices", errors)
    for field in ("minimum_indices", "index_reasons", "Mapa_route_or_TOKEN_VAZIO", "route_anchors", "route_stop_condition"):
        value = indices.get(field)
        if field in {"minimum_indices", "index_reasons", "route_anchors"}:
            if not _list(value) or not value:
                errors.append(f"indices.{field} must be a non-empty list")
        elif not _nonempty(value):
            errors.append(f"indices.{field} missing")

    gaps = _require_object(data, "gaps", errors)
    for field in ("gap_ids", "TOKEN_VAZIO_ids", "uncertainties", "dependencies"):
        if not _list(gaps.get(field)):
            errors.append(f"gaps.{field} must be a list")

    evidence = _require_object(data, "evidence", errors)
    for field in ("evidence_refs",):
        if not _list(evidence.get(field)):
            errors.append(f"evidence.{field} must be a list")
    for field in ("evidence_observed_at", "evidence_scope", "staleness_state"):
        if not _nonempty(evidence.get(field)):
            errors.append(f"evidence.{field} missing")

    service = _require_object(data, "service_classification", errors)
    required_service = {
        "governance": GOVERNANCE_CLASSES,
        "data": DATA_CLASSES,
        "privacy": PRIVACY_CLASSES,
        "security": SECURITY_CLASSES,
    }
    for axis, allowed in required_service.items():
        value = service.get(axis)
        if not isinstance(value, dict):
            errors.append(f"service_classification.{axis} must be an object")
            continue
        if value.get("class") not in allowed:
            errors.append(f"service_classification.{axis}.class invalid")
        if not _nonempty(value.get("basis")):
            errors.append(f"service_classification.{axis}.basis missing")

    execution = _require_object(data, "execution", errors)
    urgency = execution.get("urgency")
    risk = execution.get("risk")
    if urgency not in URGENCY:
        errors.append("execution.urgency invalid")
    if risk not in RISK:
        errors.append("execution.risk invalid")
    for field in ("gate", "falsifier", "exit_criterion", "stop_condition", "rollback_ref"):
        if not _nonempty(execution.get(field)):
            errors.append(f"execution.{field} missing")
    mutating = execution.get("mutating")
    if not isinstance(mutating, bool):
        errors.append("execution.mutating must be boolean")
    if mutating and _is_token(source_commit):
        errors.append("mutating work requires exact source_commit")
    if mutating and risk in {"HIGH", "CRITICAL"} and _is_token(execution.get("rollback_ref")):
        errors.append("HIGH/CRITICAL mutating work requires concrete rollback_ref")

    for critical_axis in ("governance", "privacy", "security"):
        value = service.get(critical_axis)
        if isinstance(value, dict) and value.get("class") == "TOKEN_VAZIO" and mutating:
            errors.append(f"mutating work cannot proceed with TOKEN_VAZIO {critical_axis} classification")

    delta = _require_object(data, "delta_targets", errors)
    for field in ("local_receipt_target", "Mapa_transition_target", "Drive_reconstruction_target_or_TOKEN_VAZIO"):
        if not _nonempty(delta.get(field)):
            errors.append(f"delta_targets.{field} missing")

    f3 = _require_object(data, "f3", errors)
    for field in ("F_ok", "F_gap", "F_next"):
        if not _nonempty(f3.get(field)):
            errors.append(f"f3.{field} missing")

    if data.get("state") in {"EVIDENCED_SCOPED", "RESOLVED_NEGATIVE", "RESOLVED"}:
        refs = evidence.get("evidence_refs")
        if not isinstance(refs, list) or not refs:
            errors.append("evidenced/resolved states require evidence_refs")

    return errors


def build_report(data: dict[str, Any], errors: list[str]) -> dict[str, Any]:
    execution = data.get("execution") if isinstance(data.get("execution"), dict) else {}
    return {
        "schema": "rafaelia.federated-work-item-validation.v1",
        "work_item_id": data.get("work_item_id", "TOKEN_VAZIO"),
        "status": "PASS" if not errors else "BLOCKED",
        "execution_allowed": not errors,
        "claim_allowed": False,
        "urgency": execution.get("urgency", "TOKEN_VAZIO"),
        "risk": execution.get("risk", "TOKEN_VAZIO"),
        "errors": errors,
        "boundary": "PASS authorizes only the bounded work envelope; it does not prove execution, evidence, privacy compliance, security, runtime, or the target claim.",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("work_item", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    try:
        data = json.loads(args.work_item.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            raise ValueError("work item root must be object")
        errors = validate(data)
    except Exception as exc:
        data = {"work_item_id": "TOKEN_VAZIO"}
        errors = [str(exc)]

    report = build_report(data, errors)
    text = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
