#!/usr/bin/env python3
"""Validate and classify GitHub Actions execution evidence without inference inflation."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

TOKEN_VAZIO = "TOKEN_VAZIO"
ALLOWED_LOG_STATES = {"AVAILABLE", "ABSENT", TOKEN_VAZIO}
ALLOWED_STATUS = {"queued", "in_progress", "completed", TOKEN_VAZIO}
ALLOWED_CONCLUSIONS = {
    "success", "failure", "cancelled", "skipped", "timed_out",
    "action_required", "neutral", TOKEN_VAZIO,
}


def load_json(path: str | Path) -> dict[str, Any]:
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"{path}: root must be an object")
    return data


def classify_incident(incident: dict[str, Any], contract: dict[str, Any]) -> str:
    steps = incident["steps_observed"]
    conclusion = incident["conclusion"]
    logs_state = incident["logs_state"]
    evidence_code = incident["evidence_code"]

    if evidence_code in set(contract["billing_evidence_codes"]):
        return "BILLING_BLOCKED"
    if evidence_code in set(contract["policy_evidence_codes"]):
        return "POLICY_BLOCKED"
    if conclusion == "success" and isinstance(steps, int) and not isinstance(steps, bool) and steps > 0:
        return "WORKFLOW_PASS"
    if conclusion == "failure" and isinstance(steps, int) and not isinstance(steps, bool) and steps > 0:
        return "WORKFLOW_EXECUTED_FAILURE"
    if conclusion == "failure" and steps == 0 and logs_state == "ABSENT":
        return "ZERO_STEP_NO_LOGS"
    if conclusion == "cancelled" and steps == 0:
        return "CANCELLED_BEFORE_EXECUTION"
    return TOKEN_VAZIO


def validate_contract(contract: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    required = {
        "schema", "schema_version", "token_vazio", "classifications", "rules",
        "billing_evidence_codes", "policy_evidence_codes", "required_incident_fields",
    }
    missing = sorted(required - set(contract))
    if missing:
        return [f"contract missing fields: {missing}"]
    if contract["schema"] != "actions_execution_evidence_contract_v1":
        errors.append("invalid contract schema")
    if contract["schema_version"] != "1.0.0":
        errors.append("invalid contract schema_version")
    if contract["token_vazio"] != TOKEN_VAZIO:
        errors.append("token_vazio must be TOKEN_VAZIO")
    if len(contract["classifications"]) != len(set(contract["classifications"])):
        errors.append("classifications must be unique")
    required_classes = {
        "WORKFLOW_PASS", "WORKFLOW_EXECUTED_FAILURE", "ZERO_STEP_NO_LOGS",
        "BILLING_BLOCKED", "POLICY_BLOCKED", "CANCELLED_BEFORE_EXECUTION", TOKEN_VAZIO,
    }
    if set(contract["classifications"]) != required_classes:
        errors.append("classifications mismatch")
    for key in (
        "billing_requires_explicit_evidence_code",
        "zero_steps_never_implies_billing",
        "pass_requires_positive_step_count",
        "executed_failure_requires_positive_step_count",
        "zero_step_no_logs_requires_failed_conclusion",
        "declared_scope_is_not_remote_proof",
        "legacy_startup_failure_label_is_deprecated",
    ):
        if contract["rules"].get(key) is not True:
            errors.append(f"rule {key} must be true")
    overlap = set(contract["billing_evidence_codes"]) & set(contract["policy_evidence_codes"])
    if overlap:
        errors.append(f"evidence code overlap: {sorted(overlap)}")
    return errors


def _valid_positive_id(value: Any) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value > 0


def validate_manifest(contract: dict[str, Any], manifest: dict[str, Any]) -> list[str]:
    errors = validate_contract(contract)
    required_top = {"schema", "schema_version", "observed_at", "incidents", "scope_assertions"}
    missing_top = sorted(required_top - set(manifest))
    if missing_top:
        return errors + [f"manifest missing fields: {missing_top}"]
    if manifest["schema"] != "actions_execution_evidence_manifest_v1":
        errors.append("invalid manifest schema")
    if manifest["schema_version"] != "1.0.0":
        errors.append("invalid manifest schema_version")
    if not isinstance(manifest["observed_at"], str) or not manifest["observed_at"].endswith("Z"):
        errors.append("manifest observed_at must be UTC timestamp ending in Z")

    incidents = manifest["incidents"]
    if not isinstance(incidents, list):
        return errors + ["incidents must be a list"]
    seen_incident_ids: set[str] = set()
    seen_job_keys: set[tuple[str, int]] = set()
    required_fields = set(contract["required_incident_fields"])

    for index, incident in enumerate(incidents):
        prefix = f"incidents[{index}]"
        if not isinstance(incident, dict):
            errors.append(f"{prefix} must be an object")
            continue
        missing = sorted(required_fields - set(incident))
        if missing:
            errors.append(f"{prefix} missing fields: {missing}")
            continue
        incident_id = incident["incident_id"]
        if not isinstance(incident_id, str) or not incident_id.strip():
            errors.append(f"{prefix}.incident_id must be non-empty string")
        elif incident_id in seen_incident_ids:
            errors.append(f"duplicate incident_id: {incident_id}")
        seen_incident_ids.add(incident_id)

        repo = incident["repository_full_name"]
        if not isinstance(repo, str) or repo.count("/") != 1:
            errors.append(f"{prefix}.repository_full_name must be owner/repo")
        for field in ("run_id", "job_id"):
            if not _valid_positive_id(incident[field]):
                errors.append(f"{prefix}.{field} must be positive integer")
        if isinstance(repo, str) and _valid_positive_id(incident["job_id"]):
            key = (repo, incident["job_id"])
            if key in seen_job_keys:
                errors.append(f"duplicate repository/job pair: {key}")
            seen_job_keys.add(key)

        if incident["status"] not in ALLOWED_STATUS:
            errors.append(f"{prefix}.status invalid")
        if incident["conclusion"] not in ALLOWED_CONCLUSIONS:
            errors.append(f"{prefix}.conclusion invalid")
        steps = incident["steps_observed"]
        if not ((isinstance(steps, int) and not isinstance(steps, bool) and steps >= 0) or steps == TOKEN_VAZIO):
            errors.append(f"{prefix}.steps_observed must be non-negative integer or TOKEN_VAZIO")
        if incident["logs_state"] not in ALLOWED_LOG_STATES:
            errors.append(f"{prefix}.logs_state invalid")
        if not isinstance(incident["evidence_code"], str):
            errors.append(f"{prefix}.evidence_code must be string")
        if not isinstance(incident["evidence_source"], str) or not incident["evidence_source"].strip():
            errors.append(f"{prefix}.evidence_source must be non-empty string")
        if not isinstance(incident["observed_at"], str) or not incident["observed_at"].endswith("Z"):
            errors.append(f"{prefix}.observed_at must end in Z")

        derived = classify_incident(incident, contract)
        if incident["declared_classification"] != derived:
            errors.append(
                f"{prefix}.declared_classification mismatch: "
                f"declared={incident['declared_classification']} derived={derived}"
            )
        if steps == 0 and derived == "BILLING_BLOCKED" and incident["evidence_code"] not in contract["billing_evidence_codes"]:
            errors.append(f"{prefix}: zero steps cannot infer billing")
        if incident["conclusion"] == "success" and steps == 0:
            errors.append(f"{prefix}: success requires positive observed step count")

    assertions = manifest["scope_assertions"]
    if not isinstance(assertions, list):
        return errors + ["scope_assertions must be a list"]
    for index, assertion in enumerate(assertions):
        prefix = f"scope_assertions[{index}]"
        if not isinstance(assertion, dict):
            errors.append(f"{prefix} must be an object")
            continue
        required = {"assertion_id", "statement", "source_type", "state", "claim_allowed", "exit_criteria"}
        missing = sorted(required - set(assertion))
        if missing:
            errors.append(f"{prefix} missing fields: {missing}")
            continue
        if assertion["state"] not in {"DECLARED", "VERIFIED", TOKEN_VAZIO}:
            errors.append(f"{prefix}.state invalid")
        if assertion["state"] != "VERIFIED" and assertion["claim_allowed"] is not False:
            errors.append(f"{prefix}: non-VERIFIED assertion cannot allow claim")
        if not isinstance(assertion["exit_criteria"], list) or not assertion["exit_criteria"]:
            errors.append(f"{prefix}.exit_criteria must be non-empty list")
    return errors


def summarize(contract: dict[str, Any], manifest: dict[str, Any]) -> dict[str, Any]:
    errors = validate_manifest(contract, manifest)
    classifications = [classify_incident(item, contract) for item in manifest.get("incidents", []) if isinstance(item, dict)]
    return {
        "schema": "actions_execution_evidence_summary_v1",
        "status": "PASS" if not errors else "FAIL",
        "incident_count": len(classifications),
        "classification_counts": dict(sorted(Counter(classifications).items())),
        "billing_inferred_from_zero_steps": False,
        "legacy_startup_failure_label_deprecated": True,
        "errors": errors,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    for name in ("validate-contract", "validate-manifest", "summarize"):
        command = sub.add_parser(name)
        command.add_argument("contract")
        if name != "validate-contract":
            command.add_argument("manifest")
    args = parser.parse_args(argv)
    try:
        contract = load_json(args.contract)
        if args.command == "validate-contract":
            errors = validate_contract(contract)
            result = {"status": "PASS" if not errors else "FAIL", "errors": errors}
        else:
            manifest = load_json(args.manifest)
            if args.command == "validate-manifest":
                errors = validate_manifest(contract, manifest)
                result = {"status": "PASS" if not errors else "FAIL", "errors": errors}
            else:
                result = summarize(contract, manifest)
        print(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2))
        return 0 if result["status"] == "PASS" else 1
    except Exception as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False))
        return 1


if __name__ == "__main__":
    sys.exit(main())
