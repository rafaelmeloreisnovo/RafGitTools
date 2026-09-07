#!/usr/bin/env python3
"""Fail-closed validator for the START single-root provider plan v3.

This proves only source/governance topology. It does not apply or prove GitHub
branch protection/rulesets, human review, physical runtime, scientific claims,
model training, provider authorization, or release signing.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v3.json"
START = ROOT / ".github" / "workflows" / "START.yml"
LEGACY = ROOT / ".github" / "workflows-legacy-20260907"

EXPECTED_GLOBAL_JOBS = {"plan", "topology", "coherence", "receipt"}
EXPECTED_LANES = {"federation", "docs", "android", "security", "release"}


def _indent(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def _block(text: str, key: str, indent: int) -> str:
    lines = text.splitlines()
    start = None
    for idx, line in enumerate(lines):
        if _indent(line) == indent and line.strip() == key:
            start = idx
            break
    if start is None:
        return ""
    out = [lines[start]]
    for line in lines[start + 1 :]:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and _indent(line) <= indent:
            break
        out.append(line)
    return "\n".join(out)


def _active_yaml_paths() -> list[Path]:
    workflow_dir = ROOT / ".github" / "workflows"
    return sorted(
        [*workflow_dir.glob("*.yml"), *workflow_dir.glob("*.yaml")],
        key=lambda p: p.as_posix(),
    )


def _job_declared(text: str, job_id: str) -> bool:
    jobs = _block(text, "jobs:", 0)
    return f"\n  {job_id}:" in jobs


def _pull_request_properties(text: str) -> dict[str, bool]:
    block = _block(text, "pull_request:", 2)
    if not block:
        return {"present": False, "path_filtered": False, "targets_main": False}
    lines = block.splitlines()[1:]
    path_filtered = any(
        line.strip().startswith(("paths:", "paths-ignore:")) for line in lines
    )
    targets_main = any("main" in line for line in lines if "branches" in line or line.strip().startswith("-"))
    # START uses inline branches: [main, develop], so inspect full block too.
    targets_main = targets_main or "main" in block
    return {"present": True, "path_filtered": path_filtered, "targets_main": targets_main}


def load_plan() -> dict[str, Any]:
    data = json.loads(PLAN.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("plan root must be an object")
    return data


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if data.get("schema") != "rafaelia.rafgittools-main-provider-enforcement-plan.v3":
        errors.append("schema mismatch")
    if data.get("repository") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("repository mismatch")
    if data.get("target_branch") != "main":
        errors.append("target_branch must remain main")
    if data.get("mode") != "PREPARED_NOT_APPLIED":
        errors.append("plan must remain PREPARED_NOT_APPLIED until provider readback")
    if data.get("provider_enforcement_active") is not False:
        errors.append("provider_enforcement_active must remain false")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if data.get("merge_authorized_from_this_artifact") is not False:
        errors.append("prepared policy must not authorize merge")
    if not str(data.get("provider_enforcement_state", "")).startswith("TOKEN_VAZIO"):
        errors.append("provider state must remain TOKEN_VAZIO before external application")

    supersedes = data.get("supersedes")
    if supersedes != "contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v2.json":
        errors.append("v3 must supersede the v2 snapshot")
    elif not (ROOT / supersedes).is_file():
        errors.append("v2 predecessor must remain preserved")
    if not (ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260830.v1.json").is_file():
        errors.append("v1 predecessor must remain preserved")

    active = _active_yaml_paths()
    if active != [START]:
        errors.append(
            "active workflow set must contain exactly .github/workflows/START.yml; got "
            + ", ".join(path.relative_to(ROOT).as_posix() for path in active)
        )
    if not START.is_file():
        errors.append("START workflow missing")
        start_text = ""
    else:
        start_text = START.read_text(encoding="utf-8")

    contract = data.get("active_workflow_contract")
    if not isinstance(contract, dict):
        errors.append("active_workflow_contract must be an object")
    else:
        if contract.get("single_root_required") is not True:
            errors.append("single_root_required must be true")
        if contract.get("workflow_path") != ".github/workflows/START.yml":
            errors.append("active workflow path mismatch")
        if contract.get("path_filtered") is not False:
            errors.append("START provider root cannot be path-filtered")
        if contract.get("legacy_archive") != ".github/workflows-legacy-20260907":
            errors.append("legacy archive path mismatch")

    if not LEGACY.is_dir():
        errors.append("legacy workflow archive missing")
    else:
        legacy_yml = sorted([*LEGACY.glob("*.yml"), *LEGACY.glob("*.yaml")])
        if len(legacy_yml) < 2:
            errors.append("legacy archive does not preserve the former multi-workflow tree")
        for historical in ("ci.yml", "docs.yml", "pr-validation.yml", "federation-audit.yml"):
            if not (LEGACY / historical).is_file():
                errors.append(f"legacy archive missing historical workflow: {historical}")

    scope = _pull_request_properties(start_text) if start_text else {
        "present": False,
        "path_filtered": False,
        "targets_main": False,
    }
    if not scope["present"]:
        errors.append("START must have pull_request trigger")
    if scope["path_filtered"]:
        errors.append("START pull_request trigger must not be path-filtered")
    if not scope["targets_main"]:
        errors.append("START pull_request trigger must target main")

    globals_ = data.get("required_status_checks_global")
    if not isinstance(globals_, list) or not globals_:
        errors.append("required_status_checks_global must be a non-empty list")
        globals_ = []
    jobs: set[str] = set()
    contexts: set[str] = set()
    for item in globals_:
        if not isinstance(item, dict):
            errors.append("each required global check must be an object")
            continue
        context = item.get("context")
        job_id = item.get("job_id")
        if not isinstance(context, str) or not context:
            errors.append("global check context missing")
        elif context in contexts:
            errors.append(f"duplicate global context: {context}")
        else:
            contexts.add(context)
        if item.get("workflow_path") != ".github/workflows/START.yml":
            errors.append(f"{context}: global provider check must bind to START.yml")
        if item.get("provider_required_globally") is not True:
            errors.append(f"{context}: provider_required_globally must be true")
        if item.get("pull_request_scope") != "ALL_PULL_REQUESTS_TO_MAIN":
            errors.append(f"{context}: pull_request_scope mismatch")
        if not isinstance(job_id, str) or not job_id:
            errors.append(f"{context}: job_id missing")
        else:
            jobs.add(job_id)
            if start_text and not _job_declared(start_text, job_id):
                errors.append(f"{context}: job_id not declared in START")

    if jobs != EXPECTED_GLOBAL_JOBS:
        errors.append(f"global START core job set mismatch: {sorted(jobs)}")

    if start_text:
        receipt = _block(start_text, "receipt:", 2)
        if "if: always()" not in receipt:
            errors.append("receipt core job must remain if: always()")

    lanes = data.get("specialized_conditional_lanes")
    if not isinstance(lanes, list):
        errors.append("specialized_conditional_lanes must be a list")
        lanes = []
    lane_names: set[str] = set()
    for item in lanes:
        if not isinstance(item, dict):
            errors.append("each specialized lane must be an object")
            continue
        lane = item.get("lane")
        job_id = item.get("job_id")
        if isinstance(lane, str):
            lane_names.add(lane)
        if item.get("provider_required_globally") is not False:
            errors.append(f"{lane}: conditional lane cannot be provider-global")
        if item.get("skip_state") != "TOKEN_VAZIO_NOT_SELECTED":
            errors.append(f"{lane}: conditional skip must remain typed TOKEN_VAZIO_NOT_SELECTED")
        if not isinstance(job_id, str) or not _job_declared(start_text, job_id):
            errors.append(f"{lane}: conditional job_id not declared in START")
    if lane_names != EXPECTED_LANES:
        errors.append(f"conditional lane set mismatch: {sorted(lane_names)}")

    historical = data.get("historical_workflow_contexts")
    if not isinstance(historical, dict):
        errors.append("historical_workflow_contexts must be an object")
    elif historical.get("state") != "SUPERSEDED_ARCHIVED_NOT_PROVIDER_ACTIVE":
        errors.append("legacy workflow contexts must remain explicitly superseded")

    boundary = data.get("application_boundary")
    if not isinstance(boundary, dict):
        errors.append("application_boundary must be an object")
    else:
        for key in (
            "administration_write_proven",
            "current_connected_surface_exposes_branch_protection_write",
            "current_connected_surface_exposes_ruleset_write",
            "current_agent_may_self_approve",
            "current_agent_may_fabricate_provider_state",
        ):
            if boundary.get(key) is not False:
                errors.append(f"application_boundary.{key} must remain false")

    invariants = set(data.get("invariants", []))
    for required in (
        "CONDITIONAL_LANE != GLOBAL_REQUIRED_CHECK",
        "ARCHIVED_WORKFLOW != ACTIVE_TRIGGER_ROOT",
        "TOKEN_VAZIO != PASS",
    ):
        if required not in invariants:
            errors.append(f"missing invariant: {required}")

    return errors


def main() -> int:
    try:
        data = load_plan()
        errors = validate(data)
    except Exception as exc:
        print(json.dumps({
            "schema": "rafaelia.rafgittools-main-provider-enforcement-plan.report.v3",
            "status": "BLOCKED",
            "claim_allowed": False,
            "provider_enforcement_proven": False,
            "errors": [str(exc)],
        }, indent=2, ensure_ascii=False))
        return 2

    report = {
        "schema": "rafaelia.rafgittools-main-provider-enforcement-plan.report.v3",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "provider_enforcement_proven": False,
        "mode": data.get("mode"),
        "active_workflow": ".github/workflows/START.yml",
        "global_required_job_ids": sorted(
            item.get("job_id") for item in data.get("required_status_checks_global", [])
            if isinstance(item, dict) and isinstance(item.get("job_id"), str)
        ),
        "conditional_lanes": sorted(
            item.get("lane") for item in data.get("specialized_conditional_lanes", [])
            if isinstance(item, dict) and isinstance(item.get("lane"), str)
        ),
        "errors": errors,
        "boundary": "PASS proves only single-root source/governance policy coherence. Provider enforcement remains TOKEN_VAZIO until an authorized administration surface applies and reads back the rule.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
