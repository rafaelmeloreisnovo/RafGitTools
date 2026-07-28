#!/usr/bin/env python3
"""Dependency-free semantic validator for RAFAELIA human/AI middleware v1."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

FORBIDDEN_SECRET_KEYS = {
    "access_token", "refresh_token", "password", "secret", "api_key",
    "private_key", "authorization_header", "cookie", "session_token",
}
SENSITIVE_CATEGORIES = {"VOICE", "LOCATION", "HEALTH", "BIOMETRIC", "CONTACTS", "CALL_LOG", "SMS", "SENSOR"}
WRITE_EFFECTS = {"LOCAL_WRITE", "GIT_WRITE", "NETWORK_WRITE", "PUBLIC_WRITE"}
HIGH_EFFECTS = {"SENSOR_READ", "GIT_WRITE", "NETWORK_WRITE", "PUBLIC_WRITE"}
REQUIRED_AI_DUTIES = {"IDENTIFY_RISK", "MINIMIZE_DATA", "EXPLAIN_LIMITS", "ABSTAIN_WHEN_UNCERTAIN"}
REQUIRED_TOP_LEVEL = {
    "schema", "request_id", "created_at", "intent", "people", "data_boundary", "risk",
    "ai_lane", "human_lane", "execution", "evidence", "friction",
}


@dataclass(frozen=True)
class Finding:
    code: str
    path: str
    message: str

    def as_dict(self) -> dict[str, str]:
        return {"code": self.code, "path": self.path, "message": self.message}


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":")).encode("utf-8")


def sha256_hex(value: Any) -> str:
    return hashlib.sha256(canonical_json(value)).hexdigest()


def walk(value: Any, path: str = "$") -> Iterable[tuple[str, str, Any]]:
    if isinstance(value, dict):
        for key, item in value.items():
            child = f"{path}.{key}"
            yield child, key, item
            yield from walk(item, child)
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from walk(item, f"{path}[{index}]")


def validate_adapter(adapter: dict[str, Any]) -> list[Finding]:
    findings: list[Finding] = []
    if adapter.get("schema") != "raf.human-ai.adapter.v1":
        findings.append(Finding("ADAPTER_SCHEMA", "$.schema", "schema must be raf.human-ai.adapter.v1"))
    required = {
        "repository", "default_branch", "role", "contract", "allowed_inputs",
        "allowed_outputs", "allowed_effects", "forbidden_effects", "privacy",
        "human_control", "token_vazio", "evidence",
    }
    missing = sorted(required - set(adapter))
    if missing:
        findings.append(Finding("ADAPTER_FIELDS", "$", f"missing fields: {missing}"))
        return findings
    allowed = set(adapter["allowed_effects"])
    forbidden = set(adapter["forbidden_effects"])
    overlap = sorted(allowed & forbidden)
    if overlap:
        findings.append(Finding("ADAPTER_EFFECT_OVERLAP", "$.allowed_effects", f"effects both allowed and forbidden: {overlap}"))
    if adapter["privacy"].get("raw_secrets_allowed") is not False:
        findings.append(Finding("ADAPTER_SECRET_POLICY", "$.privacy.raw_secrets_allowed", "raw secrets must be forbidden"))
    if adapter["privacy"].get("public_private_data_allowed") is not False:
        findings.append(Finding("ADAPTER_PUBLIC_PRIVATE", "$.privacy.public_private_data_allowed", "private data may not be public"))
    if adapter["evidence"].get("claim_allowed") is not False:
        findings.append(Finding("ADAPTER_CLAIM", "$.evidence.claim_allowed", "adapter may not promote claims"))
    contract = adapter["contract"]
    if contract.get("repository") != "rafaelmeloreisnovo/RafGitTools" or contract.get("path") != "contracts/human-ai-middleware-v1.schema.json":
        findings.append(Finding("ADAPTER_CONTRACT", "$.contract", "adapter must pin the canonical RafGitTools contract"))
    if not re.fullmatch(r"[a-f0-9]{40}", str(contract.get("commit_pin", ""))):
        findings.append(Finding("ADAPTER_PIN", "$.contract.commit_pin", "commit pin must be a 40-character lowercase SHA"))
    return findings


def validate_request(doc: dict[str, Any], adapters: dict[str, dict[str, Any]] | None = None) -> list[Finding]:
    findings: list[Finding] = []
    missing = sorted(REQUIRED_TOP_LEVEL - set(doc))
    if missing:
        findings.append(Finding("TOP_LEVEL_FIELDS", "$", f"missing fields: {missing}"))
        return findings
    if doc.get("schema") != "raf.human-ai.middleware.v1":
        findings.append(Finding("SCHEMA", "$.schema", "schema must be raf.human-ai.middleware.v1"))

    for path, key, value in walk(doc):
        if key.lower() in FORBIDDEN_SECRET_KEYS and value not in (None, "", False):
            findings.append(Finding("SECRET_MATERIAL", path, f"secret-bearing key {key!r} must not carry a value"))

    intent = doc["intent"]
    target = intent.get("expected_usefulness_target")
    measured = intent.get("measured_usefulness")
    if not isinstance(target, (int, float)) or not 0 <= target <= 1:
        findings.append(Finding("USEFULNESS_TARGET", "$.intent.expected_usefulness_target", "target must be within [0,1]"))
    if measured is not None:
        if not isinstance(measured, (int, float)) or not 0 <= measured <= 1:
            findings.append(Finding("USEFULNESS_MEASURE", "$.intent.measured_usefulness", "measured usefulness must be null or within [0,1]"))
        elif doc["evidence"].get("state") != "VERIFIED" or not doc["evidence"].get("refs"):
            findings.append(Finding("USEFULNESS_EVIDENCE", "$.intent.measured_usefulness", "measured usefulness requires VERIFIED evidence and refs"))

    ai = doc["ai_lane"]
    if ai.get("may_finalize") or ai.get("may_expand_scope") or ai.get("may_execute"):
        findings.append(Finding("AI_AUTHORITY", "$.ai_lane", "AI cannot finalize, expand scope, or execute"))
    missing_duties = sorted(REQUIRED_AI_DUTIES - set(ai.get("duties", [])))
    if missing_duties:
        findings.append(Finding("AI_DUTIES", "$.ai_lane.duties", f"missing duties: {missing_duties}"))

    people = doc["people"]
    if people.get("human_final_decision") is not True:
        findings.append(Finding("HUMAN_FINAL", "$.people.human_final_decision", "human final decision must remain true"))

    data = doc["data_boundary"]
    classification = data.get("classification")
    destination = data.get("destination_visibility")
    categories = set(data.get("categories", []))
    if classification in {"PRIVATE", "SENSITIVE", "SECRET"} and destination == "PUBLIC":
        findings.append(Finding("PUBLIC_PRIVATE_DATA", "$.data_boundary.destination_visibility", "private/sensitive/secret data cannot be public"))
    if classification in {"SENSITIVE", "SECRET"} and data.get("raw_data_export"):
        findings.append(Finding("RAW_SENSITIVE_EXPORT", "$.data_boundary.raw_data_export", "raw sensitive export is blocked"))
    if not data.get("minimized_fields"):
        findings.append(Finding("MINIMIZATION", "$.data_boundary.minimized_fields", "at least one minimized field must be declared"))
    if not FORBIDDEN_SECRET_KEYS.issubset(set(data.get("excluded_fields", []))):
        missing_secrets = sorted(FORBIDDEN_SECRET_KEYS - set(data.get("excluded_fields", [])))
        findings.append(Finding("SECRET_EXCLUSIONS", "$.data_boundary.excluded_fields", f"missing secret exclusions: {missing_secrets}"))

    risk = doc["risk"]
    human = doc["human_lane"]
    execution = doc["execution"]
    effect = execution.get("effect_class")
    high = risk.get("level") in {"HIGH", "CRITICAL"} or effect in HIGH_EFFECTS or bool(categories & SENSITIVE_CATEGORIES)
    if risk.get("level") == "CRITICAL":
        findings.append(Finding("CRITICAL_BLOCK", "$.risk.level", "critical risk is blocked in-band"))
    if high:
        if human.get("consent_state") != "APPROVED":
            findings.append(Finding("CONSENT", "$.human_lane.consent_state", "high/sensitive action requires approved consent"))
        reviewers = set(human.get("reviewers", []))
        if "PRIVACY_REVIEWER" not in reviewers:
            findings.append(Finding("PRIVACY_REVIEW", "$.human_lane.reviewers", "privacy reviewer required"))
    if categories & {"HEALTH", "BIOMETRIC", "LOCATION", "VOICE"}:
        reviewers = set(human.get("reviewers", []))
        if "RIGHTS_REVIEWER" not in reviewers:
            findings.append(Finding("RIGHTS_REVIEW", "$.human_lane.reviewers", "rights reviewer required for health/biometric/location/voice"))
    if any(person.get("minor") and person.get("count", 0) > 0 for person in people.get("affected_people", [])):
        reviewers = set(human.get("reviewers", []))
        if destination == "PUBLIC":
            findings.append(Finding("MINOR_PUBLIC", "$.data_boundary.destination_visibility", "minor-related data cannot be public"))
        if not {"PRIVACY_REVIEWER", "RIGHTS_REVIEWER"}.issubset(reviewers):
            findings.append(Finding("MINOR_REVIEW", "$.human_lane.reviewers", "minor-related action requires privacy and rights reviewers"))

    if effect in WRITE_EFFECTS:
        if human.get("decision") not in {"APPROVE_BOUNDED", "APPROVE_TWO_STEP"}:
            findings.append(Finding("WRITE_APPROVAL", "$.human_lane.decision", "write action requires bounded or two-step approval"))
        if not execution.get("dry_run_first"):
            findings.append(Finding("DRY_RUN", "$.execution.dry_run_first", "write action must dry-run first"))
        rollback = execution.get("rollback", {})
        if not rollback.get("available") or not rollback.get("strategy"):
            findings.append(Finding("ROLLBACK", "$.execution.rollback", "write action requires an explicit rollback"))
    if execution.get("irreversible"):
        if human.get("decision") != "APPROVE_TWO_STEP" or len(set(human.get("reviewers", []))) < 2:
            findings.append(Finding("IRREVERSIBLE", "$.execution.irreversible", "irreversible action requires two-step approval and two reviewers"))

    evidence = doc["evidence"]
    if evidence.get("claim_allowed"):
        if evidence.get("state") != "VERIFIED" or not evidence.get("refs"):
            findings.append(Finding("CLAIM_GATE", "$.evidence.claim_allowed", "claim requires VERIFIED state and evidence refs"))
    if evidence.get("state") == "TOKEN_VAZIO":
        if not evidence.get("F_gap") or not evidence.get("F_next") or not doc["friction"].get("token_vazio_reason"):
            findings.append(Finding("TOKEN_VAZIO_CONTEXT", "$.evidence", "TOKEN_VAZIO requires gap, next step, and reason"))

    friction = doc["friction"]
    if friction.get("current_loop", 0) > friction.get("loop_budget", 0):
        findings.append(Finding("LOOP_BUDGET", "$.friction.current_loop", "loop budget exceeded"))
    if friction.get("current_loop") == friction.get("loop_budget") and evidence.get("state") == "TOKEN_VAZIO" and not friction.get("stop_on_no_new_evidence"):
        findings.append(Finding("LOOP_STOP", "$.friction.stop_on_no_new_evidence", "must stop when no evidence and loop budget reached"))

    if adapters is not None:
        repo = execution.get("target_repository")
        adapter = adapters.get(repo)
        if adapter is None:
            findings.append(Finding("ADAPTER_MISSING", "$.execution.target_repository", f"no adapter for {repo}"))
        else:
            effect_allowed = effect in set(adapter.get("allowed_effects", []))
            effect_forbidden = effect in set(adapter.get("forbidden_effects", []))
            if not effect_allowed or effect_forbidden:
                findings.append(Finding("ADAPTER_EFFECT", "$.execution.effect_class", f"effect {effect} is not allowed by {repo}"))
    return findings


def load_adapters(path: Path | None) -> tuple[dict[str, dict[str, Any]], list[Finding]]:
    if path is None:
        return {}, []
    adapters: dict[str, dict[str, Any]] = {}
    findings: list[Finding] = []
    if path.is_dir():
        sources = [(str(file), json.loads(file.read_text(encoding="utf-8"))) for file in sorted(path.glob("*.json"))]
    else:
        registry = json.loads(path.read_text(encoding="utf-8"))
        if registry.get("schema") != "raf.human-ai.adapter-registry.v1" or not isinstance(registry.get("adapters"), list):
            return {}, [Finding("ADAPTER_REGISTRY", str(path), "invalid adapter registry")]
        sources = [(f"{path}#{index}", item) for index, item in enumerate(registry["adapters"])]
    for source, data in sources:
        findings.extend(validate_adapter(data))
        repo = data.get("repository")
        if repo in adapters:
            findings.append(Finding("ADAPTER_DUPLICATE", source, f"duplicate adapter for {repo}"))
        else:
            adapters[repo] = data
    return adapters, findings


def report_for(request: dict[str, Any], request_findings: list[Finding], adapter_findings: list[Finding], adapter_count: int) -> dict[str, Any]:
    ok = not request_findings and not adapter_findings
    canonical = copy.deepcopy(request)
    return {
        "schema": "raf.human-ai.validation-receipt.v1",
        "validator_version": "1.0.0",
        "request_id": request.get("request_id"),
        "request_sha256": sha256_hex(canonical),
        "adapter_count": adapter_count,
        "request_findings": [finding.as_dict() for finding in request_findings],
        "adapter_findings": [finding.as_dict() for finding in adapter_findings],
        "result": "PASS" if ok else "FAIL",
        "claim_allowed": False,
        "runtime_execution": "TOKEN_VAZIO",
        "F_ok": ["canonical request parsed", "semantic policy evaluated"] if ok else [],
        "F_gap": ["target runtime operation not executed"],
        "F_next": ["execute only after human approval and preserve an immutable runtime receipt"],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("request", type=Path)
    parser.add_argument("--adapters", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    request = json.loads(args.request.read_text(encoding="utf-8"))
    adapters, adapter_findings = load_adapters(args.adapters)
    request_findings = validate_request(request, adapters if args.adapters else None)
    report = report_for(request, request_findings, adapter_findings, len(adapters))
    output = json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(output, encoding="utf-8")
    print(output, end="")
    return 0 if report["result"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
