#!/usr/bin/env python3
"""Fail-closed RafGitTools main branch-protection applicator.

The credential is read only from --token-env and is never written to receipts.
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
        "required_status_checks": {"strict": True, "contexts": contexts},
        "enforce_admins": True,
        "required_pull_request_reviews": {
            "dismiss_stale_reviews": bool(policy["dismiss_stale_approvals_on_new_commits"]),
            "require_code_owner_reviews": False,
            "required_approving_review_count": int(policy["minimum_approving_reviews"]),
            "require_last_push_approval": True,
        },
        "restrictions": None,
        "required_linear_history": False,
        "allow_force_pushes": False,
        "allow_deletions": False,
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

def verify_readback(data: dict[str, Any], expected_contexts: list[str]) -> list[str]:
    errors: list[str] = []
    checks = data.get("required_status_checks") or {}
    actual_contexts = set(checks.get("contexts") or [])
    missing = sorted(set(expected_contexts) - actual_contexts)
    if missing:
        errors.append("missing required contexts: " + ", ".join(missing))
    if not enabled(data.get("enforce_admins")):
        errors.append("enforce_admins not enabled")
    reviews = data.get("required_pull_request_reviews") or {}
    if int(reviews.get("required_approving_review_count") or 0) < 1:
        errors.append("required approving review count < 1")
    if reviews.get("dismiss_stale_reviews") is not True:
        errors.append("dismiss_stale_reviews not true")
    if reviews.get("require_last_push_approval") is not True:
        errors.append("require_last_push_approval not true")
    if not enabled(data.get("required_conversation_resolution")):
        errors.append("required_conversation_resolution not enabled")
    if enabled(data.get("allow_force_pushes")):
        errors.append("force pushes allowed")
    if enabled(data.get("allow_deletions")):
        errors.append("branch deletions allowed")
    return errors

class Api:
    def __init__(self, token: str, base: str) -> None:
        self.token = token
        self.base = base.rstrip("/")

    def request(self, method: str, path: str, payload: dict[str, Any] | None = None, allow_404: bool = False) -> Any:
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
                "User-Agent": "rafgittools-provider-enforcement-v1",
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

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--branch", default="main")
    ap.add_argument("--expected-main-sha", required=True)
    ap.add_argument("--plan", type=Path, required=True)
    ap.add_argument("--receipt", type=Path, required=True)
    ap.add_argument("--token-env", default="PROVIDER_TOKEN")
    ap.add_argument("--api-base", default="https://api.github.com")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    receipt: dict[str, Any] = {
        "schema": "rafaelia.rafgittools-main-provider-enforcement-receipt.v1",
        "repository": args.repo,
        "branch": args.branch,
        "expected_main_sha": args.expected_main_sha,
        "mode": "APPLY" if args.apply else "PLAN",
        "claim_allowed": False,
        "secret_value_recorded": False,
    }

    token = os.environ.get(args.token_env, "")
    if not token:
        receipt.update(state="TOKEN_VAZIO_SECRET_NOT_AVAILABLE", provider_mutation=False)
        write_receipt(args.receipt, receipt)
        print("FAIL provider token unavailable", file=sys.stderr)
        return 3

    plan = load_json(args.plan)
    if plan.get("repository") != args.repo or plan.get("target_branch") != args.branch:
        receipt.update(state="BLOCKED_PLAN_TARGET_MISMATCH", provider_mutation=False)
        write_receipt(args.receipt, receipt)
        return 4

    api = Api(token, args.api_base)
    repo_data = api.request("GET", f"/repos/{args.repo}")
    permissions = repo_data.get("permissions") or {}
    admin = permissions.get("admin") is True
    receipt["admin_permission_observed"] = admin
    if not admin:
        receipt.update(state="BLOCKED_ADMIN_PERMISSION_NOT_OBSERVED", provider_mutation=False)
        write_receipt(args.receipt, receipt)
        return 5

    branch_data = api.request("GET", f"/repos/{args.repo}/branches/{args.branch}")
    before_sha = str((branch_data.get("commit") or {}).get("sha") or "")
    receipt["observed_main_sha_before"] = before_sha
    if before_sha != args.expected_main_sha:
        receipt.update(state="BLOCKED_MAIN_SHA_DRIFT", provider_mutation=False)
        write_receipt(args.receipt, receipt)
        return 6

    before = api.request("GET", f"/repos/{args.repo}/branches/{args.branch}/protection", allow_404=True)
    receipt["protection_prestate"] = "ABSENT" if before is None else "PRESENT"

    payload = build_payload(plan)
    expected_contexts = list(payload["required_status_checks"]["contexts"])
    receipt["required_contexts"] = expected_contexts

    if not args.apply:
        receipt.update(state="PLAN_ONLY_AUTHORITY_AND_SHA_PRECONDITIONS_PASS", provider_mutation=False)
        write_receipt(args.receipt, receipt)
        print(json.dumps(receipt, sort_keys=True))
        return 0

    api.request("PUT", f"/repos/{args.repo}/branches/{args.branch}/protection", payload)
    after_branch = api.request("GET", f"/repos/{args.repo}/branches/{args.branch}")
    after_sha = str((after_branch.get("commit") or {}).get("sha") or "")
    receipt["observed_main_sha_after"] = after_sha
    if after_sha != args.expected_main_sha:
        receipt.update(state="FAIL_MAIN_SHA_CHANGED_DURING_APPLY", provider_mutation=True)
        write_receipt(args.receipt, receipt)
        return 7

    after = api.request("GET", f"/repos/{args.repo}/branches/{args.branch}/protection")
    errors = verify_readback(after, expected_contexts)
    receipt["readback_errors"] = errors
    receipt["provider_mutation"] = True
    if errors:
        receipt["state"] = "FAIL_PROVIDER_READBACK_MISMATCH"
        write_receipt(args.receipt, receipt)
        return 8

    receipt["state"] = "PASS_PROVIDER_ENFORCEMENT_READBACK"
    write_receipt(args.receipt, receipt)
    print(json.dumps(receipt, sort_keys=True))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
