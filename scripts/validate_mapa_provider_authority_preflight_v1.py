#!/usr/bin/env python3
"""Fail-closed, no-secret read-only Mapa provider preflight.

A PAT's presence does not establish Administration:write. This file offers NO
provider mutation mode and cannot apply branch protection, rulesets or secrets.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

DEFAULT = Path(__file__).resolve().parents[1] / "contracts/MAPA_GITHUB_PROVIDER_AUTHORITY_PREFLIGHT_20261011.v1.json"
SCHEMA = "rafaelia.rafgittools-mapa-provider-authority-preflight/v1"
GATES = {
    "PAT_ENV_PROVIDER_ADMIN_WRITE_TARGET_ACCESS_VERIFIED",
    "BYPASS_ACTOR_IDENTITIES_AND_POLICIES_REVIEWED",
    "AT_LEAST_ONE_INDEPENDENT_ELIGIBLE_REVIEWER_VERIFIED",
    "CODE_SCAN_SECRET_BOUND_ANALYZER_AND_SARIF_PASS",
    "EXACT_REQUIRED_STATUS_CHECK_NAMES_PROVIDER_VERIFIED",
    "EXACT_MAIN_SHA_RECONFIRMED",
    "ROLLBACK_PRESTATE_CAPTURED_AND_REHEARSED",
}
HEX40 = re.compile(r"^[a-f0-9]{40}$")
OWNER_REPO = "rafaelmeloreisnovo/Mapa"


def validate_contract(data: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != SCHEMA or data.get("claim_allowed") is not False:
        errors.append("SCHEMA_OR_CLAIM_INVALID")
    if data.get("target_repository") != OWNER_REPO or data.get("target_branch") != "main":
        errors.append("UNAUTHORIZED_TARGET")
    if not HEX40.fullmatch(str(data.get("expected_target_main_sha", ""))):
        errors.append("EXPECTED_TARGET_MAIN_SHA_INVALID")
    if data.get("operation") != "READ_ONLY_PREFLIGHT" or data.get("mutation_allowed") is not False:
        errors.append("MUTATION_NOT_AUTHORIZED")
    if data.get("all_closure_gates_pass") is not False:
        errors.append("UNSUPPORTED_CLOSURE_PROMOTION")
    credential = data.get("credential")
    if not isinstance(credential, dict):
        errors.append("CREDENTIAL_NOT_OBJECT")
        credential = {}
    if (credential.get("secret_id"), credential.get("storage_scope"),
        credential.get("environment")) != ("PAT_ENV", "ENVIRONMENT_SECRET", "PAT_ENVIRONMENTS"):
        errors.append("WRONG_PAT_SCOPE")
    if credential.get("substitution_allowed") is not False or credential.get("value_persisted") is not False:
        errors.append("SECRET_SUBSTITUTION_OR_EXPOSURE")
    if not {"PAT_ACTIONS", "PAT_AGENTS", "PAT_ENVIRONMENTS"}.issubset(
        set(credential.get("forbidden_fallbacks", []))
    ):
        errors.append("PAT_FALLBACK_NOT_DENIED")
    prestate = data.get("provider_prestate")
    if not isinstance(prestate, dict):
        errors.append("PROVIDER_PRESTATE_MISSING")
        prestate = {}
    if prestate.get("ruleset_enforcement") != "disabled" or prestate.get("ruleset_approvals") != 0:
        errors.append("UNTRUSTED_PRIOR_PROVIDER_STATE")
    if prestate.get("bypass_actor_rationales") != "TOKEN_VAZIO":
        errors.append("BYPASS_JUSTIFICATION_FABRICATED")
    bypass = prestate.get("always_bypass_integrations", [])
    if sorted(bypass) != [20150, 29110, 73253, 1144995]:
        errors.append("BYPASS_INVENTORY_NOT_GROUNDED")
    if sorted(data.get("required_before_any_apply", [])) != sorted(GATES):
        errors.append("REQUIRED_GATES_CHANGED")
    if data.get("zero_approval_rejection_test") != "NOT_RUN_UNTIL_ACTIVE_ENFORCEMENT":
        errors.append("NEGATIVE_CONTROL_FABRICATED")
    if data.get("state") != "PREPARED_READ_ONLY_NOT_EXECUTED":
        errors.append("EXECUTION_FABRICATED")
    required_paths = {
        f"GET /repos/{OWNER_REPO}",
        f"GET /repos/{OWNER_REPO}/branches/main",
        f"GET /repos/{OWNER_REPO}/branches/main/protection",
        f"GET /repos/{OWNER_REPO}/rulesets",
        f"GET /repos/{OWNER_REPO}/rulesets/21909304",
        f"GET /repos/{OWNER_REPO}/actions/secrets",
    }
    if set(data.get("readback_endpoints", [])) != required_paths:
        errors.append("PROVIDER_ENDPOINT_ALLOWLIST_DRIFT")
    return errors


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *_args, **_kwargs):
        return None


def probe_read_only(data: dict[str, Any], token: str) -> dict[str, Any]:
    """GET only against a literal allowlist; return sanitized observations."""
    result: dict[str, Any] = {
        "schema": "rafaelia.rafgittools-mapa-pat-env-probe-receipt/v1",
        "scope": "READ_ONLY_NOT_ADMIN_WRITE_PROOF",
        "observed_at": datetime.now(timezone.utc).isoformat(),
        "repository": OWNER_REPO,
        "expected_main_sha": data["expected_target_main_sha"],
        "mutation_allowed": False,
        "secret_value_recorded": False,
        "claim_allowed": False,
        "endpoints": [],
        "state": "PARTIAL_PROVIDER_READBACK",
    }
    opener = urllib.request.build_opener(NoRedirect)
    statuses: dict[str, Any] = {}
    for label in data["readback_endpoints"]:
        path = label.removeprefix("GET ")
        if not label.startswith("GET /repos/" + OWNER_REPO):
            raise ValueError("ENDPOINT_DENIED")
        request = urllib.request.Request(
            "https://api.github.com" + path, method="GET",
            headers={"Authorization": "Bearer " + token,
                     "Accept": "application/vnd.github+json",
                     "X-GitHub-Api-Version": "2022-11-28",
                     "User-Agent": "rafgittools-readonly-mapa-pat-env-preflight"},
        )
        status = None
        raw = b""
        try:
            with opener.open(request, timeout=12) as response:
                status = response.status
                raw = response.read(131073)
        except urllib.error.HTTPError as exc:
            status = exc.code
        except (urllib.error.URLError, TimeoutError):
            status = "NETWORK_TOKEN_VAZIO"
        if len(raw) > 131072:
            status = "BOUND_EXCEEDED"
            raw = b""
        # Do not persist returned bodies, identities or provider secret inventory.
        endpoint = {"path": label, "status": status,
                    "sha256_observed_body": hashlib.sha256(raw).hexdigest() if raw else None}
        if status == 200:
            try:
                obj = json.loads(raw)
            except (ValueError, UnicodeDecodeError):
                obj = {}
                endpoint["state"] = "UNPARSEABLE_RESPONSE"
            if path.endswith("/branches/main"):
                sha = ((obj.get("commit") or {}).get("sha") if isinstance(obj, dict) else None)
                statuses["main_sha_matches"] = sha == data["expected_target_main_sha"]
                endpoint["main_sha_matches"] = statuses["main_sha_matches"]
                endpoint["protected"] = obj.get("protected") if isinstance(obj, dict) else None
            if path.endswith("/rulesets/21909304"):
                endpoint["ruleset_enforcement"] = obj.get("enforcement") if isinstance(obj, dict) else None
                endpoint["approvals_required"] = next(
                    (item.get("parameters", {}).get("required_approving_review_count")
                     for item in obj.get("rules", []) if item.get("type") == "pull_request"),
                    None,
                ) if isinstance(obj, dict) else None
            if path == f"/repos/{OWNER_REPO}":
                endpoint["identity_repo_admin_flag"] = (obj.get("permissions") or {}).get("admin") if isinstance(obj, dict) else None
            if path.endswith("/actions/secrets"):
                names = {x.get("name") for x in obj.get("secrets", [])} if isinstance(obj, dict) else set()
                endpoint["codescan_secret_names_present"] = {
                    n: (n in names) for n in
                    ("CODESCAN_AUTH_TOKEN", "CODESCAN_ORGANIZATION_KEY", "CODESCAN_PROJECT_KEY")
                }
        result["endpoints"].append(endpoint)
    if statuses.get("main_sha_matches") is not True:
        result["state"] = "BLOCKED_MAIN_SHA_UNVERIFIED_OR_DRIFTED"
    elif not all(x["status"] == 200 for x in result["endpoints"]):
        result["state"] = "PARTIAL_PROVIDER_READBACK"
    else:
        result["state"] = "READ_ONLY_API_REACHABLE_NO_WRITE_PERMISSION_PROOF"
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contract", type=Path, default=DEFAULT)
    parser.add_argument("--probe", action="store_true")
    parser.add_argument("--receipt", type=Path)
    args = parser.parse_args()
    try:
        data = json.loads(args.contract.read_text(encoding="utf-8"))
        errors = validate_contract(data)
    except (ValueError, OSError, TypeError) as exc:
        errors = [f"CONTRACT_UNREADABLE:{type(exc).__name__}"]
        data = {}
    if errors:
        print(json.dumps({"state": "FAIL", "errors": errors, "claim_allowed": False}))
        return 2
    if args.probe:
        token = os.environ.get("RAFAELIA_PROVIDER_TOKEN", "")
        if not token:
            print('{"state":"TOKEN_VAZIO_PAT_ENV_NOT_BOUND","claim_allowed":false}')
            return 3
        receipt = probe_read_only(data, token)
        if args.receipt:
            args.receipt.parent.mkdir(parents=True, exist_ok=True)
            args.receipt.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(receipt, ensure_ascii=False))
        return 0 if receipt["state"] == "READ_ONLY_API_REACHABLE_NO_WRITE_PERMISSION_PROOF" else 4
    print('{"state":"PASS_STATIC_PREFLIGHT_ONLY","claim_allowed":false,"provider_mutation":false}')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
