#!/usr/bin/env python3
"""Fail-closed RafGitTools main branch-protection applicator.

Credential material is read only from --token-env and is never written to receipts.
Stage-1 apply supports automatic rollback to an observed ABSENT prestate.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

API_VERSION = "2026-03-10"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def build_payload(plan: dict[str, Any]) -> dict[str, Any]:
    contexts = [str(x["context"]) for x in plan["required_status_checks_global"]]
    policy = plan["required_policy"]
    return {
        "required_status_checks": {
            "strict": bool(policy["require_branch_up_to_date_before_merge"]),
            "contexts": contexts,
        },
        # Supplying this object enforces the pull-request path. Stage 1
        # deliberately uses zero approvals until an independent reviewer exists.
        "required_pull_request_reviews": {
            "dismiss_stale_reviews": bool(policy["dismiss_stale_approvals_on_new_commits"]),
            "require_code_owner_reviews": False,
            "required_approving_review_count": int(policy["minimum_approving_reviews"]),
            "require_last_push_approval": bool(policy["require_last_push_approval"]),
        },
        "enforce_admins": bool(policy["enforce_admins"]),
        "restrictions": None,
        "required_linear_history": False,
        "allow_force_pushes": not bool(policy["force_push_forbidden"]),
        "allow_deletions": not bool(policy["branch_deletion_forbidden"]),
        "block_creations": False,
        "required_conversation_resolution": bool(policy["require_conversation_resolution"]),
        "lock_branch": False,
        "allow_fork_syncing": True,
    }


def enabled(obj: Any) -> bool:
    if isinstance(obj, bool):
        return obj
    if isinstance(obj, dict):
        return obj.get("enabled") is True
    return False


def verify_readback(data: dict[str, Any], plan: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    policy = plan["required_policy"]
    expected_contexts = {str(x["context"]) for x in plan["required_status_checks_global"]}

    checks = data.get("required_status_checks") or {}
    actual_contexts = set(checks.get("contexts") or [])
    missing = sorted(expected_contexts - actual_contexts)
    if missing:
        errors.append("missing required contexts: " + ", ".join(missing))
    if checks.get("strict") is not bool(policy["require_branch_up_to_date_before_merge"]):
        errors.append("strict status-check mode mismatch")

    if enabled(data.get("enforce_admins")) is not bool(policy["enforce_admins"]):
        errors.append("enforce_admins mismatch")

    reviews = data.get("required_pull_request_reviews")
    if not isinstance(reviews, dict):
        errors.append("required_pull_request_reviews missing")
    else:
        expected_approvals = int(policy["minimum_approving_reviews"])
        if int(reviews.get("required_approving_review_count") or 0) != expected_approvals:
            errors.append("required approving review count mismatch")
        if reviews.get("dismiss_stale_reviews") is not bool(policy["dismiss_stale_approvals_on_new_commits"]):
            errors.append("dismiss_stale_reviews mismatch")
        if reviews.get("require_last_push_approval") is not bool(policy["require_last_push_approval"]):
            errors.append("require_last_push_approval mismatch")

    if enabled(data.get("required_conversation_resolution")) is not bool(policy["require_conversation_resolution"]):
        errors.append("required_conversation_resolution mismatch")
    if enabled(data.get("allow_force_pushes")):
        errors.append("force pushes allowed")
    if enabled(data.get("allow_deletions")):
        errors.append("branch deletions allowed")
    return errors


class Api:
    def __init__(self, token: str, base: str) -> None:
        self.token = token
        self.base = base.rstrip("/")

    def request(
        self,
        method: str,
        path: str,
        payload: dict[str, Any] | None = None,
        allow_404: bool = False,
    ) -> Any:
        body = None if payload is None else json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            self.base + path,
            data=body,
            method=method,
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": "Bearer " + self.token,
                "X-GitHub-Api-Version": API_VERSION,
                "Content-Type": "application/json",
                "User-Agent": "rafgittools-provider-enforcement-v2",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=30) as response:
                raw = response.read()
                return json.loads(raw.decode("utf-8")) if raw else {}
        except urllib.error.HTTPError as exc:
            if allow_404 and exc.code == 404:
                return None
            detail = exc.read().decode("utf-8", "replace")[:500]
            raise RuntimeError(f"GitHub API {method} {path} -> {exc.code}: {detail}") from exc


def write_receipt(path: Path, receipt: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def restore_absent_prestate(api: Api, repo: str, branch: str) -> tuple[bool, str]:
    try:
        api.request("DELETE", f"/repos/{repo}/branches/{branch}/protection")
        after = api.request(
            "GET",
            f"/repos/{repo}/branches/{branch}/protection",
            allow_404=True,
        )
        if after is None:
            return True, "PASS_ABSENT_RESTORED"
        return False, "FAIL_PROTECTION_STILL_PRESENT"
    except Exception as exc:  # receipt is sanitized to exception class only
        return False, f"FAIL_ROLLBACK_{type(exc).__name__}"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--branch", default="main")
    ap.add_argument("--expected-main-sha", required=True)
    ap.add_argument("--plan", type=Path, required=True)
    ap.add_argument("--receipt", type=Path, required=True)
    ap.add_argument("--token-env", default="PROVIDER_TOKEN")
    ap.add_argument("--api-base", default="https://api.github.com")
    mode = ap.add_mutually_exclusive_group()
    mode.add_argument("--apply", action="store_true")
    mode.add_argument("--rollback", action="store_true")
    args = ap.parse_args()

    op = "ROLLBACK" if args.rollback else ("APPLY" if args.apply else "PLAN")
    receipt: dict[str, Any] = {
        "schema": "rafaelia.rafgittools-main-provider-enforcement-receipt.v2",
        "repository": args.repo,
        "branch": args.branch,
        "expected_main_sha": args.expected_main_sha,
        "mode": op,
        "claim_allowed": False,
        "secret_value_recorded": False,
        "provider_mutation": False,
        "rollback_attempted": False,
    }

    token = os.environ.get(args.token_env, "")
    if not token:
        receipt["state"] = "TOKEN_VAZIO_SECRET_NOT_AVAILABLE"
        write_receipt(args.receipt, receipt)
        return 3

    plan = load_json(args.plan)
    if plan.get("repository") != args.repo or plan.get("target_branch") != args.branch:
        receipt["state"] = "BLOCKED_PLAN_TARGET_MISMATCH"
        write_receipt(args.receipt, receipt)
        return 4

    api = Api(token, args.api_base)
    repo_data = api.request("GET", f"/repos/{args.repo}")
    admin = (repo_data.get("permissions") or {}).get("admin") is True
    receipt["admin_permission_observed"] = admin
    if not admin:
        receipt["state"] = "BLOCKED_ADMIN_PERMISSION_NOT_OBSERVED"
        write_receipt(args.receipt, receipt)
        return 5

    branch_path = f"/repos/{args.repo}/branches/{args.branch}"
    protection_path = branch_path + "/protection"
    branch_data = api.request("GET", branch_path)
    before_sha = str((branch_data.get("commit") or {}).get("sha") or "")
    receipt["observed_main_sha_before"] = before_sha
    if before_sha != args.expected_main_sha:
        receipt["state"] = "BLOCKED_MAIN_SHA_DRIFT"
        write_receipt(args.receipt, receipt)
        return 6

    before = api.request("GET", protection_path, allow_404=True)
    prestate = "ABSENT" if before is None else "PRESENT"
    receipt["protection_prestate"] = prestate

    if args.rollback:
        if prestate == "ABSENT":
            receipt.update(state="PASS_ROLLBACK_ALREADY_ABSENT", provider_mutation=False)
            write_receipt(args.receipt, receipt)
            return 0
        receipt["rollback_attempted"] = True
        ok, result = restore_absent_prestate(api, args.repo, args.branch)
        receipt["rollback_result"] = result
        receipt["provider_mutation"] = True
        receipt["state"] = "PASS_ROLLBACK_ABSENT_RESTORED" if ok else "FAIL_ROLLBACK"
        write_receipt(args.receipt, receipt)
        return 0 if ok else 9

    required_prestate = str(plan.get("required_prestate") or "TOKEN_VAZIO")
    if required_prestate != "TOKEN_VAZIO" and prestate != required_prestate:
        receipt["state"] = "BLOCKED_PROTECTION_PRESTATE_DRIFT"
        write_receipt(args.receipt, receipt)
        return 7

    payload = build_payload(plan)
    receipt["required_contexts"] = list(payload["required_status_checks"]["contexts"])
    receipt["required_approving_review_count"] = int(
        payload["required_pull_request_reviews"]["required_approving_review_count"]
    )

    if not args.apply:
        receipt["state"] = "PLAN_ONLY_AUTHORITY_SHA_AND_PRESTATE_PASS"
        write_receipt(args.receipt, receipt)
        print(json.dumps(receipt, sort_keys=True))
        return 0

    mutation_started = False
    try:
        api.request("PUT", protection_path, payload)
        mutation_started = True
        receipt["provider_mutation"] = True

        after_branch = api.request("GET", branch_path)
        after_sha = str((after_branch.get("commit") or {}).get("sha") or "")
        receipt["observed_main_sha_after"] = after_sha
        if after_sha != args.expected_main_sha:
            receipt["state"] = "FAIL_MAIN_SHA_CHANGED_DURING_APPLY"
            raise RuntimeError("main_sha_drift_after_apply")

        after = api.request("GET", protection_path)
        errors = verify_readback(after, plan)
        receipt["readback_errors"] = errors
        if errors:
            receipt["state"] = "FAIL_PROVIDER_READBACK_MISMATCH"
            raise RuntimeError("provider_readback_mismatch")

    except Exception as exc:
        receipt["apply_error_class"] = type(exc).__name__
        if mutation_started and prestate == "ABSENT":
            receipt["rollback_attempted"] = True
            ok, result = restore_absent_prestate(api, args.repo, args.branch)
            receipt["rollback_result"] = result
            if not ok:
                receipt["state"] = "FAIL_APPLY_AND_ROLLBACK"
                write_receipt(args.receipt, receipt)
                return 9
            receipt["state"] = "FAIL_APPLY_ROLLED_BACK"
        write_receipt(args.receipt, receipt)
        return 8

    receipt["state"] = "PASS_PROVIDER_ENFORCEMENT_READBACK"
    write_receipt(args.receipt, receipt)
    print(json.dumps(receipt, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
