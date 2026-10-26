#!/usr/bin/env python3
"""Bounded, manual GitHub branch-protection control plane (NOT a bulk executor).

Only selected public user-owned repositories; one exact target per dispatch.
PAT_ENV is read from a CI protected environment at runtime, never persisted.
If provider prestate cannot be fully read, apply is not possible.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

SCHEMA = "rafgittools.federated-admin-protection.v1"
EXECUTOR = "rafaelmeloreisnovo/RafGitTools"
OWNER = "rafaelmeloreisnovo"
TARGETS = {
    "rafaelmeloreisnovo/Mapa": 1267885874,
    "rafaelmeloreisnovo/actions": 1083210044,
}
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
REPO = re.compile(r"rafaelmeloreisnovo/[A-Za-z0-9_.-]{1,100}\Z")
PROTECTION = {
    "required_status_checks": None,
    "enforce_admins": True,
    "required_pull_request_reviews": None,
    "restrictions": None,
    "allow_force_pushes": False,
    "allow_deletions": False,
    "required_conversation_resolution": False,
}
# This baseline is force-push/delete prevention, NOT verified review/check enforcement.


class Denied(Exception):
    """Typed blocked operation; no sensitive provider bodies exposed."""


class Provider:
    def __init__(self, token: str):
        if not token or "\n" in token or "\r" in token:
            raise Denied("TOKEN_VAZIO_PROVIDER_AUTHORITY")
        self.token = token

    def request(self, method: str, path: str, payload: dict | None = None):
        if method not in {"GET", "PUT"}:
            raise Denied("METHOD_DENIED")
        if not re.fullmatch(r"/repos/rafaelmeloreisnovo/(?:Mapa|actions)(?:/branches/main(?:/protection)?|/rulesets)?", path):
            raise Denied("ENDPOINT_DENIED")
        if method == "PUT" and (not path.endswith("/branches/main/protection") or payload != PROTECTION):
            raise Denied("UNAPPROVED_MUTATION")
        class NoRedirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, *_args, **_kwargs):
                return None
        req = urllib.request.Request(
            "https://api.github.com" + path,
            method=method,
            data=(json.dumps(payload, separators=(",", ":")).encode("utf-8") if payload is not None else None),
            headers={
                "Authorization": "Bearer " + self.token,
                "Accept": "application/vnd.github+json",
                "Content-Type": "application/json",
                "X-GitHub-Api-Version": "2022-11-28",
                "User-Agent": "rafgittools-scoped-federation-p0",
            },
        )
        try:
            with urllib.request.build_opener(NoRedirect).open(req, timeout=12) as response:
                raw = response.read(131073)
                if len(raw) > 131072:
                    raise Denied("PROVIDER_RESPONSE_TOO_LARGE")
                return response.status, json.loads(raw.decode("utf-8"))
        except urllib.error.HTTPError as exc:
            # Never expose provider response body: it may contain customer data.
            return exc.code, None
        except (urllib.error.URLError, TimeoutError, ValueError, UnicodeError) as exc:
            raise Denied("PROVIDER_READBACK_UNAVAILABLE") from None


def validate(repo: str, expected_sha: str, operation: str, actor: str, event: str,
             ref: str, workflow_sha: str, run_attempt: str) -> None:
    if not REPO.fullmatch(repo) or repo not in TARGETS:
        raise Denied("TARGET_NOT_ALLOWLISTED")
    if not HEX40.fullmatch(expected_sha):
        raise Denied("SHA_INVALID")
    if operation not in ("preflight", "protect"):
        raise Denied("OPERATION_DENIED")
    if (actor != OWNER or event != "workflow_dispatch" or ref != "refs/heads/main"
            or not HEX40.fullmatch(workflow_sha) or run_attempt != "1"):
        raise Denied("OWNER_MAIN_MANUAL_ONLY")


def prestate(provider: Provider, repo: str, expected_sha: str) -> dict:
    base = "/repos/" + repo
    status, metadata = provider.request("GET", base)
    if status != 200 or not isinstance(metadata, dict):
        raise Denied("REPOSITORY_PROVIDER_READBACK_FAILED")
    if (metadata.get("id") != TARGETS[repo]
            or metadata.get("full_name") != repo
            or metadata.get("default_branch") != "main"
            or metadata.get("private") is not False
            or metadata.get("permissions", {}).get("admin") is not True):
        raise Denied("REPOSITORY_ID_OR_AUTHORITY_DRIFT")
    status, branch = provider.request("GET", base + "/branches/main")
    if status != 200 or not isinstance(branch, dict):
        raise Denied("BRANCH_PROVIDER_READBACK_FAILED")
    if branch.get("commit", {}).get("sha") != expected_sha:
        raise Denied("EXACT_SHA_DRIFT")
    status_protection, existing = provider.request("GET", base + "/branches/main/protection")
    if status_protection not in (200, 404):
        raise Denied("PROTECTION_PROVIDER_READBACK_DENIED")
    protected = branch.get("protected")
    if type(protected) is not bool:
        raise Denied("BRANCH_PROTECTED_STATE_UNKNOWN")
    if (protected and status_protection != 200) or (not protected and status_protection != 404):
        raise Denied("PROTECTION_PRESTATE_INCONSISTENT")
    status_rules, rulesets = provider.request("GET", base + "/rulesets")
    if status_rules != 200 or not isinstance(rulesets, list):
        raise Denied("RULESETS_PROVIDER_READBACK_DENIED")
    if any(not isinstance(item, dict) or item.get("enforcement") != "disabled" for item in rulesets):
        raise Denied("RULESET_CONFLICT_OR_UNKNOWN")
    return {"protected": protected, "protection_http": status_protection,
            "ruleset_count": len(rulesets)}


def run(provider, repo: str, expected_sha: str, operation: str) -> dict:
    state = prestate(provider, repo, expected_sha)
    receipt = {
        "schema": SCHEMA, "executor": EXECUTOR, "repository": repo,
        "target_repository_id": TARGETS[repo], "expected_main_sha": expected_sha,
        "operation": operation, "prestate": state, "claim_allowed": False,
        "automatic_promotion": False, "secret_value_recorded": False,
        "rollback_prestate_captured": True, "apply_attempted": False,
        "provider_write_verified": False,
        "status": "PREFLIGHT_SCOPED_NOT_APPLIED",
    }
    if operation == "preflight":
        return receipt
    if state["protected"]:
        raise Denied("EXISTING_PROTECTION_NEVER_OVERWRITTEN")
    # Re-read SHA, protection and rulesets immediately prior to applying.
    if prestate(provider, repo, expected_sha) != state:
        raise Denied("PROVIDER_PRESTATE_DRIFT")
    receipt["apply_attempted"] = True
    status, response = provider.request("PUT", "/repos/" + repo + "/branches/main/protection", PROTECTION)
    if status != 200:
        raise Denied("PROVIDER_APPLY_NOT_CONFIRMED")
    # Confirm live branch SHA, protection state, protection semantics and rulesets.
    base = "/repos/" + repo
    status, branch = provider.request("GET", base + "/branches/main")
    if status != 200 or branch.get("commit", {}).get("sha") != expected_sha or branch.get("protected") is not True:
        raise Denied("PROVIDER_POSTSTATE_BRANCH_NOT_VERIFIED")
    status, details = provider.request("GET", base + "/branches/main/protection")
    if status != 200 or not isinstance(details, dict):
        raise Denied("PROVIDER_POSTSTATE_PROTECTION_NOT_VERIFIED")
    for field in ("allow_force_pushes", "allow_deletions", "enforce_admins"):
        obj = details.get(field)
        if not isinstance(obj, dict) or obj.get("enabled") is not (field == "enforce_admins"):
            raise Denied("PROVIDER_POSTSTATE_POLICY_MISMATCH")
    status, rulesets = provider.request("GET", base + "/rulesets")
    if status != 200 or not isinstance(rulesets, list):
        raise Denied("PROVIDER_POSTSTATE_RULESETS_UNVERIFIED")
    receipt["provider_write_verified"] = True
    receipt["status"] = "PASS_SCOPED_FORCE_PUSH_DELETE_GUARD_ONLY"
    return receipt


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", required=True)
    parser.add_argument("--expected-sha", required=True)
    parser.add_argument("--operation", choices=("preflight", "protect"), required=True)
    parser.add_argument("--receipt", required=True)
    args = parser.parse_args(argv)
    result = {"schema": SCHEMA, "status": "HOLD", "claim_allowed": False,
              "secret_value_recorded": False, "operation": args.operation,
              "repository": args.repository if args.repository in TARGETS else "TARGET_DENIED"}
    ok = False
    try:
        validate(args.repository, args.expected_sha, args.operation,
                 os.environ.get("GITHUB_ACTOR", ""), os.environ.get("GITHUB_EVENT_NAME", ""),
                 os.environ.get("GITHUB_REF", ""), os.environ.get("GITHUB_SHA", ""),
                 os.environ.get("GITHUB_RUN_ATTEMPT", ""))
        provider = Provider(os.environ.get("PROVIDER_TOKEN", ""))
        result = run(provider, args.repository, args.expected_sha, args.operation)
        ok = True
    except Denied as exc:
        result["status"] = "HOLD_" + str(exc)
    path = Path(args.receipt)
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        with path.open("x", encoding="utf-8") as f:
            json.dump(result, f, sort_keys=True, separators=(",", ":"))
            f.write("\n")
    except FileExistsError:
        print("HOLD_RECEIPT_ALREADY_EXISTS")
        return 3
    print("FEDERATED_ADMIN=" + result["status"])
    return 0 if ok else 2


if __name__ == "__main__":
    sys.exit(main())
