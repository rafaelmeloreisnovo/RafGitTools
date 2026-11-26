#!/usr/bin/env python3
"""Owner-dispatched GET-only Termux provider inventory. No write or promotion."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.request

EXECUTOR = "rafaelmeloreisnovo/RafGitTools"
OWNER = "rafaelmeloreisnovo"
TARGETS = {
    "rafaelmeloreisnovo/termux-app-rafacodephi": (1082194563, "master", 21908888),
    "rafaelmeloreisnovo/termux-packages": (1324848257, "main", 26461859),
}
APP_REQUIRED = {"deletion", "non_fast_forward", "required_linear_history",
                "required_signatures", "code_quality", "pull_request",
                "required_status_checks"}
APP_CONTRACT = "github:rafaelmeloreisnovo/termux-app-rafacodephi@78fb037f60a0d9e013d066eba3b60b92fe1423aa:governance/provider/PROVIDER_RULESET_TARGET.v3.json"
SCHEMA = "rafaelia.termux-ruleset-preflight/v1"


class Hold(RuntimeError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *_args, **_kwargs):
        raise Hold("REDIRECT_DENIED")


def guard(repo, sha, env):
    if repo not in TARGETS or not re.fullmatch(r"[0-9a-f]{40}", sha):
        raise Hold("TARGET_OR_SHA_DENIED")
    if (env.get("GITHUB_REPOSITORY") != EXECUTOR
            or env.get("GITHUB_ACTOR") != OWNER
            or env.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
            or env.get("GITHUB_REF") != "refs/heads/main"
            or env.get("GITHUB_RUN_ATTEMPT") != "1"
            or not re.fullmatch(r"[0-9a-f]{40}", env.get("GITHUB_SHA", ""))):
        raise Hold("OWNER_MAIN_MANUAL_FIRST_ATTEMPT_ONLY")


class Provider:
    def __init__(self, repo, token):
        if repo not in TARGETS or not token or "\n" in token or "\r" in token:
            raise Hold("TARGET_OR_PAT_UNAVAILABLE")
        _, branch, _ = TARGETS[repo]
        self.base = "/repos/" + repo
        self.token = token
        self.paths = {"", "/branches/" + branch, "/branches/" + branch + "/protection",
                      "/rulesets?includes_parents=true&per_page=100"}
        self.detail = re.compile(r"/rulesets/[1-9][0-9]*\Z")

    def get(self, suffix):
        if suffix not in self.paths and not self.detail.fullmatch(suffix):
            raise Hold("ENDPOINT_DENIED")
        req = urllib.request.Request("https://api.github.com" + self.base + suffix,
            method="GET", headers={"Authorization": "Bearer " + self.token,
            "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "rafaelia-termux-ruleset-preflight-v1"})
        try:
            with urllib.request.build_opener(NoRedirect()).open(req, timeout=15) as res:
                raw = res.read(1048577)
                if len(raw) > 1048576 or res.headers.get("Link"):
                    raise Hold("RESPONSE_SIZE_OR_PAGINATION_UNRECONCILED")
                return res.status, json.loads(raw)
        except urllib.error.HTTPError as exc:
            return exc.code, None
        except (urllib.error.URLError, TimeoutError, ValueError, UnicodeError):
            raise Hold("PROVIDER_READBACK_UNAVAILABLE") from None


def require(provider, suffix, typ):
    status, data = provider.get(suffix)
    if status != 200 or not isinstance(data, typ):
        raise Hold("PROVIDER_READBACK_REQUIRED")
    return data


def inventory(provider, repo, sha):
    rid, branch, known_id = TARGETS[repo]
    meta = require(provider, "", dict)
    if (meta.get("id") != rid or meta.get("full_name") != repo
            or meta.get("default_branch") != branch or meta.get("private") is not False
            or meta.get("permissions", {}).get("admin") is not True):
        raise Hold("REPOSITORY_IDENTITY_OR_ADMIN_READBACK_DRIFT")
    head = require(provider, "/branches/" + branch, dict)
    if head.get("commit", {}).get("sha") != sha:
        raise Hold("TARGET_HEAD_MOVED")
    listed = require(provider, "/rulesets?includes_parents=true&per_page=100", list)
    ids = [r.get("id") for r in listed if isinstance(r, dict)]
    if (len(ids) != len(listed) or len(ids) >= 100 or
            any(type(i) is not int or i < 1 for i in ids) or len(set(ids)) != len(ids)
            or known_id not in ids):
        raise Hold("RULESET_INVENTORY_UNRECONCILED")
    details = []
    for i in sorted(ids):
        d = require(provider, "/rulesets/" + str(i), dict)
        if d.get("id") != i or not isinstance(d.get("rules"), list):
            raise Hold("RULESET_IDENTITY_OR_SHAPE_DRIFT")
        details.append(d)
    classic_status, classic = provider.get("/branches/" + branch + "/protection")
    if classic_status not in (200, 404) or (classic_status == 200 and not isinstance(classic, dict)):
        raise Hold("CLASSIC_PROTECTION_READBACK_DENIED")
    # A 404 does not establish that all protection is absent.
    # Bind raw provider prestate by digest; expose only bounded metadata.
    snapshot = {"repo_id": rid, "branch": branch, "head": sha,
                "rulesets": details, "classic_http": classic_status, "classic": classic}
    digest = hashlib.sha256(json.dumps(snapshot, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    rows = []
    types = set()
    uncertain = False
    for d in details:
        cond = d.get("conditions", {}).get("ref_name", {})
        inc, exc = cond.get("include"), cond.get("exclude")
        if not isinstance(inc, list) or not isinstance(exc, list) or not all(isinstance(x, str) for x in inc + exc):
            raise Hold("RULESET_CONDITION_UNKNOWN")
        known = {"~ALL", "~DEFAULT_BRANCH", "refs/heads/" + branch}
        relevant = d.get("target") == "branch" and d.get("enforcement") == "active"
        covered = relevant and bool(known.intersection(inc)) and not known.intersection(exc)
        # Unsupported glob semantics are not permission to mutate.
        if relevant and any(x.startswith("~") and x not in known or any(c in x for c in "*?[]\\") for x in inc + exc):
            uncertain = True
            covered = False
        kinds = [r.get("type") for r in d["rules"] if isinstance(r, dict)]
        if len(kinds) != len(d["rules"]) or not all(isinstance(k, str) for k in kinds):
            raise Hold("RULE_TYPE_UNKNOWN")
        if covered:
            types.update(kinds)
        bypass = d.get("bypass_actors")
        rows.append({"id": d["id"], "updated_at": d.get("updated_at"),
                     "target": d.get("target"), "enforcement": d.get("enforcement"),
                     "include": inc, "exclude": exc, "covers_default_proven": covered,
                     "rule_types": kinds, "bypass_count": len(bypass) if isinstance(bypass, list) else "TOKEN_VAZIO",
                     "bypass_visibility": "OBSERVED" if isinstance(bypass, list) else "TOKEN_VAZIO"})
    if require(provider, "/branches/" + branch, dict).get("commit", {}).get("sha") != sha:
        raise Hold("TARGET_HEAD_MOVED_DURING_READ")
    return {"schema": SCHEMA, "state": "PREFLIGHT_OBSERVED_READ_ONLY",
            "repository": repo, "repository_id": rid, "default_branch": branch, "head_sha": sha,
            "rule_inventory": rows, "scope_uncertainty": uncertain,
            "classic_protection_http": classic_status, "prestate_sha256": digest,
            "expected_policy": APP_CONTRACT if branch == "master" else "TOKEN_VAZIO_PRODUCER_POLICY_REQUIRED",
            "app_missing_rule_types_only": sorted(APP_REQUIRED - types) if branch == "master" and not uncertain else "TOKEN_VAZIO",
            "admin_metadata_observed": True, "administration_write_permission": "TOKEN_VAZIO_NOT_TESTED",
            "apply_attempted": False, "claim_allowed": False, "negative_merge_test": "NOT_RUN",
            "next": "REVIEW_EXACT_POLICY_BYPASS_SCOPE_INDEPENDENT_REVIEWER_AND_ROLLBACK_BEFORE_APPLY"}


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--repo", required=True)
    p.add_argument("--expected-sha", required=True)
    p.add_argument("--receipt", required=True)
    a = p.parse_args()
    out = {"schema": SCHEMA, "state": "HOLD", "claim_allowed": False, "apply_attempted": False}
    code = 2
    try:
        guard(a.repo, a.expected_sha, os.environ)
        out = inventory(Provider(a.repo, os.environ.get("PROVIDER_TOKEN", "")), a.repo, a.expected_sha)
        code = 0
    except Hold as exc:
        out["reason"] = str(exc)
    except (TypeError, KeyError, AttributeError, ValueError):
        out["reason"] = "MALFORMED_PROVIDER_RESPONSE"
    target = Path(a.receipt)
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("x", encoding="utf-8") as f:
        json.dump(out, f, sort_keys=True, indent=2)
        f.write("\n")
    print("TERMUX_RULESET_PREFLIGHT=" + out["state"])
    return code


if __name__ == "__main__":
    sys.exit(main())
