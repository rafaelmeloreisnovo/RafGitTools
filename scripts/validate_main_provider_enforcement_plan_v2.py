#!/usr/bin/env python3
"""Validate the RafGitTools main-provider enforcement plan v2.

The validator is deliberately stdlib-only and fail-closed.  It proves that the
prepared policy does not configure path-filtered or draft-conditional workflows
as unconditional provider-global required checks.  It does not apply GitHub
branch protection/rulesets and does not prove provider enforcement.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v2.json"

EXPECTED_GLOBAL_CONTEXTS = {
    "CI / RAFAELIA coherence / anti-regression",
    "CI / Test, lint and assemble devDebug",
    "Human Impact Cross-Repo Gate V1 / validate-human-impact-contract",
    "Source Gap Audit / source-gap-audit",
}
EXPECTED_SPECIALIZED_CONTEXTS = {
    "Auditor Closure Gate V1 / auditor-closure",
    "Documentation / Validate Documentation",
}


def _indent(line: str) -> int:
    return len(line) - len(line.lstrip(" "))


def workflow_pull_request_scope(path: Path) -> dict[str, Any]:
    """Return conservative pull_request trigger properties without a YAML dep."""
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    start = None
    base_indent = None
    for idx, line in enumerate(lines):
        stripped = line.strip()
        if stripped == "pull_request:" or stripped == '"pull_request":':
            start = idx
            base_indent = _indent(line)
            break
    if start is None or base_indent is None:
        return {
            "present": False,
            "path_filtered": False,
            "targets_main": False,
            "block": "",
        }

    block_lines: list[str] = [lines[start]]
    for line in lines[start + 1 :]:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and _indent(line) <= base_indent:
            break
        block_lines.append(line)

    block = "\n".join(block_lines)
    path_filtered = any(
        line.strip().startswith(("paths:", "paths-ignore:"))
        for line in block_lines[1:]
    )
    has_branches = any(
        line.strip().startswith(("branches:", "branches-ignore:"))
        for line in block_lines[1:]
    )
    targets_main = True
    if has_branches:
        targets_main = any(
            line.strip().lstrip("-").strip().strip("[],'\"").split(",")[0].strip() == "main"
            or "main" in line.strip().strip("[]")
            for line in block_lines[1:]
            if line.strip()
        )

    return {
        "present": True,
        "path_filtered": path_filtered,
        "targets_main": targets_main,
        "block": block,
    }


def _job_declared(path: Path, job_id: str) -> bool:
    text = path.read_text(encoding="utf-8")
    return f"\n  {job_id}:" in text


def load_plan() -> dict[str, Any]:
    data = json.loads(PLAN.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise ValueError("plan root must be an object")
    return data


def validate(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if data.get("schema") != "rafaelia.rafgittools-main-provider-enforcement-plan.v2":
        errors.append("schema mismatch")
    if data.get("repository") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("repository mismatch")
    if data.get("target_branch") != "main":
        errors.append("target_branch must remain main")
    if data.get("mode") != "PREPARED_NOT_APPLIED":
        errors.append("plan must remain PREPARED_NOT_APPLIED until provider readback closes")
    if data.get("provider_enforcement_active") is not False:
        errors.append("provider_enforcement_active must remain false in the prepared plan")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if data.get("merge_authorized_from_this_artifact") is not False:
        errors.append("prepared policy must not authorize merge")
    if not str(data.get("provider_enforcement_state", "")).startswith("TOKEN_VAZIO"):
        errors.append("provider enforcement state must remain typed TOKEN_VAZIO before application")

    supersedes = data.get("supersedes")
    if supersedes != "contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260830.v1.json":
        errors.append("v2 must explicitly supersede the historical v1 plan")
    elif not (ROOT / supersedes).exists():
        errors.append("historical v1 plan must remain present for append-only custody")

    policy = data.get("required_policy")
    if not isinstance(policy, dict):
        errors.append("required_policy must be an object")
    else:
        required_true = {
            "pull_request_required",
            "direct_main_write_forbidden",
            "force_push_forbidden",
            "branch_deletion_forbidden",
            "human_review_required",
            "dismiss_stale_approvals_on_new_commits",
            "require_conversation_resolution",
            "require_branch_up_to_date_before_merge",
        }
        for key in sorted(required_true):
            if policy.get(key) is not True:
                errors.append(f"required_policy.{key} must remain true")
        if policy.get("minimum_approving_reviews") != 1:
            errors.append("minimum_approving_reviews must remain 1")

    globals_ = data.get("required_status_checks_global")
    if not isinstance(globals_, list) or not globals_:
        errors.append("required_status_checks_global must be a non-empty list")
        globals_ = []

    contexts = [item.get("context") for item in globals_ if isinstance(item, dict)]
    if len(contexts) != len(set(contexts)):
        errors.append("global required status contexts must be unique")
    if set(contexts) != EXPECTED_GLOBAL_CONTEXTS:
        errors.append("global required status context set mismatch")

    for item in globals_:
        if not isinstance(item, dict):
            errors.append("each global required status check must be an object")
            continue
        context = item.get("context", "<unknown>")
        if item.get("provider_required_globally") is not True:
            errors.append(f"{context}: provider_required_globally must be true")
        if item.get("pull_request_scope") != "ALL_PULL_REQUESTS_TO_MAIN":
            errors.append(f"{context}: pull_request_scope must be ALL_PULL_REQUESTS_TO_MAIN")
        workflow_path = item.get("workflow_path")
        job_id = item.get("job_id")
        if not isinstance(workflow_path, str) or not workflow_path:
            errors.append(f"{context}: workflow_path missing")
            continue
        path = ROOT / workflow_path
        if not path.exists():
            errors.append(f"{context}: workflow missing: {workflow_path}")
            continue
        scope = workflow_pull_request_scope(path)
        if not scope["present"]:
            errors.append(f"{context}: workflow has no pull_request trigger")
        if scope["path_filtered"]:
            errors.append(f"{context}: path-filtered workflow cannot be provider-global required")
        if not scope["targets_main"]:
            errors.append(f"{context}: workflow pull_request trigger does not target main")
        if not isinstance(job_id, str) or not job_id or not _job_declared(path, job_id):
            errors.append(f"{context}: declared job_id not found in workflow")

    specialized = data.get("specialized_conditional_checks")
    if not isinstance(specialized, list):
        errors.append("specialized_conditional_checks must be a list")
        specialized = []
    specialized_contexts = [item.get("context") for item in specialized if isinstance(item, dict)]
    if set(specialized_contexts) != EXPECTED_SPECIALIZED_CONTEXTS:
        errors.append("specialized conditional context set mismatch")

    for item in specialized:
        if not isinstance(item, dict):
            errors.append("each specialized check must be an object")
            continue
        context = item.get("context", "<unknown>")
        if item.get("provider_required_globally") is not False:
            errors.append(f"{context}: specialized check must not be provider-global required")
        if item.get("pull_request_scope") != "PATH_FILTERED":
            errors.append(f"{context}: specialized check must remain PATH_FILTERED")
        workflow_path = item.get("workflow_path")
        if isinstance(workflow_path, str) and workflow_path:
            path = ROOT / workflow_path
            if not path.exists():
                errors.append(f"{context}: specialized workflow missing")
            else:
                scope = workflow_pull_request_scope(path)
                if not scope["present"]:
                    errors.append(f"{context}: specialized workflow has no pull_request trigger")
                if not scope["path_filtered"]:
                    errors.append(f"{context}: expected a path-filtered workflow")
        else:
            errors.append(f"{context}: specialized workflow_path missing")

    if EXPECTED_SPECIALIZED_CONTEXTS & set(contexts):
        errors.append("path-filtered specialized checks leaked into global required contexts")

    candidate = data.get("candidate_ready_checks")
    if not isinstance(candidate, list) or len(candidate) != 1:
        errors.append("candidate_ready_checks must contain the PR Validation gate exactly once")
    else:
        ready = candidate[0]
        if ready.get("context") != "PR Validation / Validate Pull Request":
            errors.append("candidate-ready context mismatch")
        if ready.get("provider_required_globally") is not False:
            errors.append("draft-conditional PR Validation must not be provider-global required")
        path = ROOT / str(ready.get("workflow_path", ""))
        if not path.exists():
            errors.append("candidate-ready PR Validation workflow missing")
        else:
            text = path.read_text(encoding="utf-8")
            if "pull_request.draft == false" not in text:
                errors.append("candidate-ready classification no longer matches PR Validation draft gate")

    boundary = data.get("application_boundary")
    if not isinstance(boundary, dict):
        errors.append("application_boundary must be an object")
    else:
        if boundary.get("administration_write_proven") is not False:
            errors.append("administration_write_proven must remain false until provider evidence exists")
        if boundary.get("current_connected_surface_exposes_branch_protection_write") is not False:
            errors.append("branch-protection write exposure must not be fabricated")
        if boundary.get("current_connected_surface_exposes_ruleset_write") is not False:
            errors.append("ruleset write exposure must not be fabricated")

    invariants = data.get("invariants", [])
    if "PATH_FILTERED_CHECK != GLOBAL_REQUIRED_CHECK" not in invariants:
        errors.append("path-filter/global-required invariant missing")
    if "TOKEN_VAZIO != PASS" not in invariants:
        errors.append("TOKEN_VAZIO invariant missing")

    return errors


def main() -> int:
    try:
        data = load_plan()
        errors = validate(data)
    except Exception as exc:
        print(json.dumps({
            "schema": "rafaelia.rafgittools-main-provider-enforcement-plan.report.v2",
            "status": "BLOCKED",
            "claim_allowed": False,
            "provider_enforcement_proven": False,
            "errors": [str(exc)],
        }, indent=2, ensure_ascii=False))
        return 2

    report = {
        "schema": "rafaelia.rafgittools-main-provider-enforcement-plan.report.v2",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "provider_enforcement_proven": False,
        "mode": data.get("mode"),
        "global_required_contexts": [
            item.get("context") for item in data.get("required_status_checks_global", [])
            if isinstance(item, dict)
        ],
        "specialized_conditional_contexts": [
            item.get("context") for item in data.get("specialized_conditional_checks", [])
            if isinstance(item, dict)
        ],
        "errors": errors,
        "boundary": "PASS proves only that the prepared provider policy is internally coherent and universally spawn-capable for its global required checks; it does not prove GitHub branch protection/ruleset application or merge rejection.",
    }
    print(json.dumps(report, indent=2, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
