#!/usr/bin/env python3
"""Manual, GET-only PAT_ENV authority probe for the institutional RLL producer.

No workflow dispatch, no mutation, no secret printing, no fallback PAT.
"""
import argparse
import json
import os
import re
import urllib.error
import urllib.request
from pathlib import Path

REPO = "instituto-Rafael/relativity-living-light"
REPO_ID = 1046495816
OWNER = "rafaelmeloreisnovo"
RULESET_ID = 23116926
HEX40 = re.compile(r"[0-9a-f]{40}\Z")
BASE = "/repos/" + REPO
PATHS = {BASE, BASE + "/branches/main", BASE + "/branches/main/protection",
         BASE + "/rulesets", BASE + "/rulesets/" + str(RULESET_ID)}


class Blocked(Exception):
    pass


def admitted(expected, context):
    if not HEX40.fullmatch(expected):
        raise Blocked("INVALID_EXACT_SHA")
    required = {"GITHUB_ACTOR": OWNER, "GITHUB_EVENT_NAME": "workflow_dispatch",
                "GITHUB_REF": "refs/heads/main", "GITHUB_RUN_ATTEMPT": "1",
                "GITHUB_REPOSITORY": "rafaelmeloreisnovo/RafGitTools"}
    if any(context.get(k) != v for k, v in required.items()):
        raise Blocked("OWNER_MAIN_MANUAL_ONLY")
    if not HEX40.fullmatch(context.get("GITHUB_SHA", "")):
        raise Blocked("INVALID_WORKFLOW_SHA")


def provider_get(token, path):
    if path not in PATHS:
        raise Blocked("ENDPOINT_NOT_ALLOWLISTED")
    if not token or "\n" in token or "\r" in token:
        raise Blocked("PAT_ENV_UNAVAILABLE")
    request = urllib.request.Request("https://api.github.com" + path,
        headers={"Authorization": "Bearer " + token,
                 "Accept": "application/vnd.github+json",
                 "X-GitHub-Api-Version": "2026-03-10",
                 "User-Agent": "rafgittools-rll-pat-preflight-v1"}, method="GET")
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *_args, **_kwargs):
            return None
    try:
        with urllib.request.build_opener(NoRedirect).open(request, timeout=12) as response:
            data = response.read(65537)
            if len(data) > 65536:
                raise Blocked("PROVIDER_RESPONSE_TOO_LARGE")
            return response.status, json.loads(data.decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, None
    except (urllib.error.URLError, TimeoutError, ValueError, UnicodeError):
        raise Blocked("PROVIDER_READ_UNAVAILABLE") from None


def fetch(get, path):
    status, data = get(path)
    if status != 200 or not isinstance(data, (dict, list)):
        raise Blocked("READBACK_DENIED_" + path.rsplit("/", 1)[-1] + "_HTTP_" + str(status))
    return data


def probe(expected, context, get):
    admitted(expected, context)
    repo = fetch(get, BASE)
    if (repo.get("id") != REPO_ID or repo.get("full_name") != REPO or
            repo.get("private") is not False or repo.get("default_branch") != "main" or
            repo.get("permissions", {}).get("admin") is not True):
        raise Blocked("REPOSITORY_ID_OR_ADMIN_READ_NOT_VERIFIED")
    branch = fetch(get, BASE + "/branches/main")
    if branch.get("commit", {}).get("sha") != expected or type(branch.get("protected")) is not bool:
        raise Blocked("MAIN_SHA_OR_PROTECTION_DRIFT")
    rulesets = fetch(get, BASE + "/rulesets")
    if len(rulesets) != 1 or rulesets[0].get("id") != RULESET_ID:
        raise Blocked("RULESET_SET_DRIFT")
    ruleset = fetch(get, BASE + "/rulesets/" + str(RULESET_ID))
    if ruleset.get("id") != RULESET_ID or ruleset.get("target") != "branch":
        raise Blocked("RULESET_ID_DRIFT")
    status, protection = get(BASE + "/branches/main/protection")
    if status not in (200, 404) or (status == 200 and not isinstance(protection, dict)):
        raise Blocked("BRANCH_PROTECTION_UNREADABLE_HTTP_" + str(status))
    if branch["protected"] is False and status == 200:
        raise Blocked("BRANCH_PROTECTION_INCONSISTENT")
    types = [r.get("type") for r in ruleset.get("rules", [])]
    return {"schema": "rafgittools.rll-pat-env-preflight.v1", "target": REPO,
            "main_sha": expected, "operation": "GET_ONLY", "state": "EVIDENCED_SCOPED_READ_ONLY",
            "token_secret_name": "PAT_ENV", "token_value_recorded": False,
            "admin_read_observed": True, "administration_write": "TOKEN_VAZIO_NOT_TESTED",
            "branch_protected": branch["protected"], "legacy_protection_http": status,
            "ruleset_id": RULESET_ID, "ruleset_enforcement": ruleset.get("enforcement"),
            "ruleset_rule_types": sorted(str(t) for t in types),
            "bypass_actor_count": len(ruleset.get("bypass_actors", [])),
            "negative_merge_test": "NOT_RUN", "provider_mutation": "NONE",
            "rollback": "NO_MUTATION_TO_ROLL_BACK", "claim_allowed": False}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--expected-sha", required=True)
    ap.add_argument("--receipt", required=True)
    a = ap.parse_args()
    result = {"schema": "rafgittools.rll-pat-env-preflight.v1",
              "target": REPO, "state": "BLOCKED", "claim_allowed": False,
              "token_value_recorded": False, "provider_mutation": "NONE"}
    try:
        token = os.environ.get("PROVIDER_TOKEN", "")
        if not token:
            raise Blocked("PAT_ENV_UNAVAILABLE")
        result = probe(a.expected_sha, os.environ, lambda p: provider_get(token, p))
    except Blocked as exc:
        result["gap"] = str(exc)
    path = Path(a.receipt)
    path.parent.mkdir(parents=True, exist_ok=True)
    try:
        with path.open("x", encoding="utf-8") as f:
            json.dump(result, f, sort_keys=True, separators=(",", ":"))
            f.write("\n")
    except FileExistsError:
        print("RLL_PREFLIGHT=HOLD_RECEIPT_COLLISION")
        return 3
    print("RLL_PREFLIGHT=" + result["state"])
    return 0 if result["state"] == "EVIDENCED_SCOPED_READ_ONLY" else 2


if __name__ == "__main__":
    raise SystemExit(main())
