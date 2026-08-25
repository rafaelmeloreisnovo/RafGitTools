#!/usr/bin/env python3
"""Validate a fail-closed server merge-enforcement execution envelope.

This validator proves only that a bounded plan preserves its authority,
rollback and evidence boundaries. It never calls a provider mutation or merge
endpoint and can never promote ``claim_allowed``.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.server-merge-enforcement-execution.v1"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
EXPECTED_ACTOR = "rafaelmeloreisnovo/RafGitTools"
EXPECTED_TARGET = "rafaelmeloreisnovo/Mapa"
EXPECTED_BRANCH = "main"
EXPECTED_CONTEXT = "promotion-control / enforce"
REQUIRED_INVARIANTS = {
    "POLICY_DECISION_NE_SERVER_SIDE_MERGE_BARRIER",
    "CAN_DO_NE_MAY_DO",
    "PLAN_PASS_NE_PROVIDER_APPLY",
    "CONFIGURATION_NE_REJECTION_RECEIPT",
    "TOKEN_VAZIO_NE_PASS",
}
REQUIRED_PRECONDITIONS = {
    "MAIN_SHA_MATCH": "RECHECK_REQUIRED",
    "FULL_PROTECTION_PRESTATE_SNAPSHOT": "RECHECK_REQUIRED",
    "REQUIRED_CONTEXT_UNIQUE": "EVIDENCED_SCOPED",
    "ADMIN_WRITE_AUTHORITY": "TOKEN_VAZIO",
    "ROLLBACK_READY": "SPECIFIED_NOT_REHEARSED",
}
REQUIRED_FORBIDDEN_OPERATIONS = {
    "MERGE_WHILE_UNPROTECTED",
    "AUTO_MERGE",
    "DIRECT_PUSH_MAIN",
    "PUBLISH_SECRET",
}
REQUIRED_EVIDENCE_IDS = {
    "MAPA-PR393-PREMERGE-PROMOTION-DENIED",
    "MAPA-PR393-PREMERGE-SERVER-ENFORCEMENT-ABSENT",
    "MAPA-PR393-MERGED",
    "MAPA-PR394-PREMERGE-PROMOTION-DENIED",
    "MAPA-PR394-PREMERGE-SERVER-ENFORCEMENT-ABSENT",
    "MAPA-PR394-MERGED",
    "MAPA-PR395-PREMERGE-CI-FAILED",
    "MAPA-PR395-PREMERGE-PROMOTION-DENIED",
    "MAPA-PR395-PREMERGE-SERVER-ENFORCEMENT-ABSENT",
    "MAPA-PR395-MERGED",
    "MAPA-PR396-PREMERGE-PROMOTION-DENIED",
    "MAPA-PR396-PREMERGE-SERVER-ENFORCEMENT-ABSENT",
    "MAPA-PR396-MERGED",
}


def _object(data: dict[str, Any], key: str, errors: list[str]) -> dict[str, Any]:
    value = data.get(key)
    if not isinstance(value, dict):
        errors.append(f"{key} must be an object")
        return {}
    return value


def _nonempty(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != SCHEMA:
        errors.append("schema mismatch")
    if data.get("mode") != "PLAN_ONLY_FAIL_CLOSED":
        errors.append("mode must remain PLAN_ONLY_FAIL_CLOSED")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if data.get("automatic_merge") is not False:
        errors.append("automatic_merge must remain false")
    for field in ("execution_id", "observed_at"):
        if not _nonempty(data.get(field)):
            errors.append(f"{field} missing")

    actor = _object(data, "actor", errors)
    if actor.get("repository") != EXPECTED_ACTOR:
        errors.append("actor.repository must remain RafGitTools")
    if not SHA40.fullmatch(str(actor.get("source_commit", ""))):
        errors.append("actor.source_commit must be exact 40-hex SHA")
    for field in ("role", "authority_boundary"):
        if not _nonempty(actor.get(field)):
            errors.append(f"actor.{field} missing")

    target = _object(data, "target", errors)
    if target.get("repository") != EXPECTED_TARGET:
        errors.append("target.repository mismatch")
    if target.get("branch") != EXPECTED_BRANCH:
        errors.append("target.branch must be main")
    observed_commit = target.get("observed_commit")
    if not SHA40.fullmatch(str(observed_commit or "")):
        errors.append("target.observed_commit must be exact 40-hex SHA")

    observation = _object(target, "branch_observation", errors)
    if observation.get("protected") is not False:
        errors.append("point-in-time observation must retain protected=false")
    if observation.get("protection_enabled") is not False:
        errors.append("point-in-time observation must retain protection_enabled=false")
    checks = _object(observation, "required_status_checks", errors)
    if checks.get("enforcement_level") != "off":
        errors.append("observed status-check enforcement must remain off")
    if checks.get("contexts") != [] or checks.get("checks") != []:
        errors.append("observed required checks must remain empty")
    if observation.get("repository_rulesets") != []:
        errors.append("observed repository rulesets must remain empty")

    producer = _object(target, "producer", errors)
    if producer.get("repository") != EXPECTED_TARGET:
        errors.append("target.producer.repository mismatch")
    if producer.get("ref") != observed_commit:
        errors.append("target.producer.ref must equal target.observed_commit")
    if producer.get("path") != "scripts/apply_main_branch_protection.py":
        errors.append("target.producer.path mismatch")
    if not SHA40.fullmatch(str(producer.get("git_blob", ""))):
        errors.append("target.producer.git_blob must be exact 40-hex Git blob")

    desired = _object(data, "desired_state", errors)
    if desired.get("required_contexts") != [EXPECTED_CONTEXT]:
        errors.append("desired_state must bind the one exact promotion context")
    for field in (
        "strict_status_checks",
        "enforce_admins",
        "dismiss_stale_reviews",
        "require_last_push_approval",
    ):
        if desired.get(field) is not True:
            errors.append(f"desired_state.{field} must be true")
    if desired.get("required_approving_review_count") != 1:
        errors.append("desired_state requires exactly one independent approval")
    if desired.get("automatic_merge") is not False:
        errors.append("desired_state.automatic_merge must remain false")

    authority = _object(data, "authority_gate", errors)
    if authority.get("required_permission") != "Administration:write":
        errors.append("authority_gate.required_permission mismatch")
    credential_state = authority.get("credential_state")
    if not (isinstance(credential_state, str) and credential_state.startswith("TOKEN_VAZIO")):
        errors.append("public envelope must preserve credential_state as TOKEN_VAZIO")
    if authority.get("apply_allowed") is not False:
        errors.append("authority_gate.apply_allowed must remain false")
    if not _nonempty(authority.get("secret_handling")):
        errors.append("authority_gate.secret_handling missing")

    preconditions = data.get("preconditions")
    if not isinstance(preconditions, list):
        errors.append("preconditions must be a list")
        precondition_map: dict[str, Any] = {}
    else:
        precondition_map = {
            item.get("id"): item.get("state")
            for item in preconditions
            if isinstance(item, dict) and _nonempty(item.get("id"))
        }
    if precondition_map != REQUIRED_PRECONDITIONS:
        errors.append("preconditions must retain the exact fail-closed state vector")

    execution = _object(data, "execution", errors)
    if execution.get("urgency") != "P0" or execution.get("risk") != "HIGH":
        errors.append("execution must remain P0/HIGH")
    plan_command = str(execution.get("plan_command", ""))
    apply_command = str(execution.get("apply_command", ""))
    for command_name, command in (("plan_command", plan_command), ("apply_command", apply_command)):
        if EXPECTED_TARGET not in command or str(observed_commit) not in command or EXPECTED_CONTEXT not in command:
            errors.append(f"execution.{command_name} is not bound to target/SHA/context")
    if "--apply" in plan_command:
        errors.append("execution.plan_command must be non-mutating")
    if "--apply" not in apply_command or "<ADMIN_WRITE_TOKEN>" not in apply_command:
        errors.append("execution.apply_command must require an explicit non-secret placeholder and --apply")
    if "/merges" in plan_command or "/merges" in apply_command or " merge " in apply_command:
        errors.append("execution commands must not call a merge endpoint")
    forbidden = execution.get("forbidden_operations")
    if not isinstance(forbidden, list) or not REQUIRED_FORBIDDEN_OPERATIONS <= set(forbidden):
        errors.append("execution.forbidden_operations incomplete")
    for field in ("stop_condition", "exit_criterion"):
        if not _nonempty(execution.get(field)):
            errors.append(f"execution.{field} missing")

    rollback = _object(data, "rollback", errors)
    if rollback.get("observed_prestate") != "ABSENT_PROTECTION_AND_EMPTY_REPOSITORY_RULESETS":
        errors.append("rollback.observed_prestate mismatch")
    if rollback.get("state") != "SPECIFIED_NOT_REHEARSED":
        errors.append("rollback.state must remain SPECIFIED_NOT_REHEARSED")
    rollback_command = str(rollback.get("command", ""))
    if "--method DELETE" not in rollback_command or "rafaelmeloreisnovo/Mapa/branches/main/protection" not in rollback_command:
        errors.append("rollback.command must be concrete and target-scoped")
    for field in ("precondition", "postcondition"):
        if not _nonempty(rollback.get(field)):
            errors.append(f"rollback.{field} missing")

    evidence = data.get("evidence")
    if not isinstance(evidence, list):
        errors.append("evidence must be a list")
    else:
        evidence_ids = {
            item.get("id")
            for item in evidence
            if isinstance(item, dict) and _nonempty(item.get("id"))
        }
        if not REQUIRED_EVIDENCE_IDS <= evidence_ids:
            errors.append("four temporal discriminants require all evidence anchors")
        if any(
            not isinstance(item, dict)
            or not all(_nonempty(item.get(k)) for k in ("id", "ref", "result"))
            for item in evidence
        ):
            errors.append("every evidence anchor requires id/ref/result")

    invariants = data.get("invariants")
    if not isinstance(invariants, list) or not REQUIRED_INVARIANTS <= set(invariants):
        errors.append("required invariants missing")

    outputs = _object(data, "outputs", errors)
    if outputs.get("target_apply_receipt") != "TOKEN_VAZIO_PROVIDER_APPLY_NOT_EXECUTED":
        errors.append("target apply receipt must remain TOKEN_VAZIO")
    if outputs.get("zero_approval_rejection_receipt") != "TOKEN_VAZIO_NOT_EXECUTED":
        errors.append("zero-approval rejection receipt must remain TOKEN_VAZIO")
    if not _nonempty(outputs.get("local_report")):
        errors.append("outputs.local_report missing")
    return errors


def build_report(data: dict[str, Any], errors: list[str]) -> dict[str, Any]:
    target = data.get("target") if isinstance(data.get("target"), dict) else {}
    return {
        "schema": "rafaelia.server-merge-enforcement-execution-validation.v1",
        "execution_id": data.get("execution_id", "TOKEN_VAZIO"),
        "status": "PASS_PLAN_ONLY" if not errors else "BLOCKED",
        "structurally_ready": not errors,
        "provider_apply_allowed": False,
        "claim_allowed": False,
        "target": target.get("repository", "TOKEN_VAZIO"),
        "target_commit": target.get("observed_commit", "TOKEN_VAZIO"),
        "errors": errors,
        "next_gate": "AUTHORITATIVE_ADMIN_APPLY_REOBSERVE_AND_ZERO_APPROVAL_REJECTION_RECEIPT",
        "boundary": "PASS_PLAN_ONLY validates the envelope; it is not provider configuration, rejection evidence, runtime evidence or claim promotion.",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "plan",
        nargs="?",
        type=Path,
        default=Path("configs/server-merge-enforcement-execution.v1.json"),
    )
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        data = json.loads(args.plan.read_text(encoding="utf-8"))
        if not isinstance(data, dict):
            raise ValueError("plan root must be an object")
        errors = validate(data)
    except Exception as exc:
        data = {"execution_id": "TOKEN_VAZIO"}
        errors = [str(exc)]
    report = build_report(data, errors)
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
