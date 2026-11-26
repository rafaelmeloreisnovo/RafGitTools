#!/usr/bin/env python3
"""Federation rulesets: public inventory, reviewed additive default-branch guard.

No target code execution, no secret persistence, no existing rule mutation.
Private targets need a private executor; public CI must not expose their data.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

OWNERS = {"rafaelmeloreisnovo", "instituto-Rafael"}
EXECUTOR = "rafaelmeloreisnovo/RafGitTools"
NAME = "RAFAELIA default branch safety v1"
REQUIRED = {"deletion", "non_fast_forward"}
SCHEMA = "rafaelia.federation-ruleset/v1"
OPS = {"federation_ruleset_inventory", "federation_ruleset_plan", "federation_ruleset_apply"}


class Hold(RuntimeError):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *_args, **_kwargs):
        raise Hold("REDIRECT_DENIED")


def canonical(x):
    return json.dumps(x, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode()


def digest(x):
    return hashlib.sha256(canonical(x)).hexdigest()


def guard(env, owner):
    if owner not in OWNERS:
        raise Hold("OWNER_DENIED")
    if (env.get("GITHUB_REPOSITORY") != EXECUTOR
            or env.get("GITHUB_ACTOR") != "rafaelmeloreisnovo"
            or env.get("GITHUB_EVENT_NAME") != "workflow_dispatch"
            or env.get("GITHUB_REF") != "refs/heads/main"
            or env.get("GITHUB_RUN_ATTEMPT") != "1"
            or not re.fullmatch(r"[0-9a-f]{40}", env.get("GITHUB_SHA", ""))):
        raise Hold("OWNER_MAIN_MANUAL_FIRST_ATTEMPT_ONLY")


def target(owner, name, rid, sha):
    if (owner not in OWNERS or not re.fullmatch(r"[A-Za-z0-9_.-]{1,100}", name)
            or name in (".", "..") or not re.fullmatch(r"[1-9][0-9]{0,19}", rid)
            or not re.fullmatch(r"[a-f0-9]{40}", sha)):
        raise Hold("EXACT_TARGET_ID_AND_HEAD_REQUIRED")
    return owner + "/" + name, int(rid)


class Provider:
    def __init__(self, owner, repo, token):
        if owner not in OWNERS or not token or "\n" in token or "\r" in token:
            raise Hold("OWNER_OR_PAT_DENIED")
        if repo and (not repo.startswith(owner + "/") or
                     not re.fullmatch(r"[A-Za-z0-9_.-]{1,100}", repo.split("/", 1)[1]) or
                     repo.split("/", 1)[1] in (".", "..")):
            raise Hold("TARGET_DENIED")
        self.owner, self.repo, self.token = owner, repo, token
        self.base = "/repos/" + repo if repo else ""
        self.branch = None
        self.write_attempted = False

    def request(self, method, path, data=None):
        if method == "GET":
            allowed = bool(re.fullmatch(r"/users/" + re.escape(self.owner) +
                           r"/repos\?type=owner&sort=full_name&direction=asc&per_page=100&page=[1-9][0-9]*", path))
            if self.base:
                allowed |= path in {self.base, self.base + "/rulesets?includes_parents=true&per_page=100"}
                allowed |= bool(re.fullmatch(re.escape(self.base) + r"/rulesets/[1-9][0-9]*", path))
                if self.branch:
                    b = self.base + "/branches/" + urllib.parse.quote(self.branch, safe="")
                    allowed |= path in {b, b + "/protection"}
            if not allowed or data is not None:
                raise Hold("READ_ENDPOINT_DENIED")
        elif method == "POST":
            if not self.base or path != self.base + "/rulesets" or not valid_payload(data):
                raise Hold("WRITE_ENDPOINT_OR_PAYLOAD_DENIED")
            self.write_attempted = True
        else:
            raise Hold("METHOD_DENIED")
        req = urllib.request.Request("https://api.github.com" + path,
            method=method, data=canonical(data) if data is not None else None,
            headers={"Authorization": "Bearer " + self.token, "Accept": "application/vnd.github+json",
                     "X-GitHub-Api-Version": "2022-11-28", "Content-Type": "application/json",
                     "User-Agent": "rafaelia-federation-ruleset-v1"})
        try:
            with urllib.request.build_opener(NoRedirect()).open(req, timeout=20) as res:
                raw = res.read(1048577)
                if len(raw) > 1048576:
                    raise Hold("OVERSIZE_RESPONSE")
                return res.status, json.loads(raw), bool(res.headers.get("Link") and 'rel="next"' in res.headers.get("Link"))
        except urllib.error.HTTPError as exc:
            return exc.code, None, False
        except (urllib.error.URLError, TimeoutError, ValueError, UnicodeError):
            raise Hold("PROVIDER_OUTCOME_UNKNOWN" if self.write_attempted else "PROVIDER_READ_FAILED") from None

    def get(self, path):
        return self.request("GET", path)


def require(provider, path, typ):
    status, obj, more = provider.get(path)
    if status != 200 or not isinstance(obj, typ) or more:
        raise Hold("PROVIDER_READBACK_OR_PAGINATION_REQUIRED")
    return obj


def inventory(provider, owner):
    rows = {}
    for page in range(1, 101):
        path = "/users/" + owner + "/repos?type=owner&sort=full_name&direction=asc&per_page=100&page=" + str(page)
        status, items, more = provider.get(path)
        if status != 200 or not isinstance(items, list):
            raise Hold("INVENTORY_READ_FAILED")
        for x in items:
            if not isinstance(x, dict):
                raise Hold("INVENTORY_SHAPE_DRIFT")
            if x.get("private") is not False:
                continue
            rid, full = x.get("id"), x.get("full_name")
            if type(rid) is not int or not isinstance(full, str) or not full.startswith(owner + "/") or rid in rows:
                raise Hold("INVENTORY_IDENTITY_DRIFT")
            rows[rid] = {"id": rid, "repository": full, "default_branch": x.get("default_branch"),
                         "archived": x.get("archived"), "fork": x.get("fork")}
        if not more:
            return {"state": "PUBLIC_INVENTORY_OBSERVED", "owner": owner,
                    "public_repositories": list(rows.values()), "count": len(rows),
                    "private_scope": "TOKEN_VAZIO_PRIVATE_EXECUTOR_REQUIRED",
                    "claim_allowed": False}
    raise Hold("INVENTORY_PAGE_CAP_REACHED")


def state(provider, repo, rid, sha):
    base = "/repos/" + repo
    meta = require(provider, base, dict)
    if meta.get("private") is not False:
        raise Hold("PRIVATE_TARGET_REQUIRES_PRIVATE_EXECUTOR")
    if (meta.get("id") != rid or meta.get("full_name") != repo or
            meta.get("archived") is not False or meta.get("disabled") is not False or
            meta.get("permissions", {}).get("admin") is not True):
        raise Hold("IDENTITY_OR_ADMIN_OR_REPOSITORY_STATE_DRIFT")
    branch = meta.get("default_branch")
    if not isinstance(branch, str) or not branch or len(branch) > 255 or any(ord(c) < 32 for c in branch):
        raise Hold("DEFAULT_BRANCH_UNKNOWN")
    provider.branch = branch
    b = base + "/branches/" + urllib.parse.quote(branch, safe="")
    if require(provider, b, dict).get("commit", {}).get("sha") != sha:
        raise Hold("TARGET_HEAD_MOVED")
    listed = require(provider, base + "/rulesets?includes_parents=true&per_page=100", list)
    ids = [x.get("id") for x in listed if isinstance(x, dict)]
    if (len(ids) != len(listed) or len(ids) >= 100 or
            any(type(i) is not int or i < 1 for i in ids) or len(set(ids)) != len(ids)):
        raise Hold("RULESET_INVENTORY_UNRECONCILED")
    details = []
    for i in sorted(ids):
        d = require(provider, base + "/rulesets/" + str(i), dict)
        if d.get("id") != i or not isinstance(d.get("rules"), list):
            raise Hold("RULESET_IDENTITY_DRIFT")
        details.append(d)
    code, classic, more = provider.get(b + "/protection")
    if code not in (200, 404) or more or (code == 200 and not isinstance(classic, dict)):
        raise Hold("CLASSIC_PROTECTION_UNVERIFIED")
    if require(provider, b, dict).get("commit", {}).get("sha") != sha:
        raise Hold("TARGET_HEAD_MOVED_DURING_READ")
    return {"repo": repo, "id": rid, "branch": branch, "head": sha,
            "rulesets": details, "classic_http": code, "classic": classic}


def valid_payload(p):
    if not isinstance(p, dict):
        return False
    return (set(p) == {"name", "target", "enforcement", "conditions", "bypass_actors", "rules"}
        and p["name"] == NAME and p["target"] == "branch" and p["enforcement"] == "active"
        and p["conditions"] == {"ref_name": {"include": ["~DEFAULT_BRANCH"], "exclude": []}}
        and p["bypass_actors"] == []
        and isinstance(p["rules"], list) and bool(p["rules"])
        and all(isinstance(r, dict) and set(r) == {"type"} and r["type"] in REQUIRED for r in p["rules"])
        and len({r["type"] for r in p["rules"]}) == len(p["rules"]))


def plan(s):
    proven = set()
    rows = []
    for d in s["rulesets"]:
        if d.get("target") not in ("branch", "tag", "push") or d.get("enforcement") not in ("active", "evaluate", "disabled"):
            raise Hold("UNKNOWN_RULESET_TARGET_OR_ENFORCEMENT")
        if d.get("target") != "branch":
            continue
        cond = d.get("conditions", {}).get("ref_name", {})
        inc, exc = cond.get("include"), cond.get("exclude")
        if not isinstance(inc, list) or not isinstance(exc, list) or not all(isinstance(x, str) for x in inc + exc):
            raise Hold("RULESET_SCOPE_UNKNOWN")
        known = {"~ALL", "~DEFAULT_BRANCH", "refs/heads/" + s["branch"]}
        if d["enforcement"] == "active" and any(
                (x.startswith("~") and x not in known) or any(c in x for c in "*?[]\\")
                for x in inc + exc):
            raise Hold("GLOB_SCOPE_REVIEW_REQUIRED")
        covered = d["enforcement"] == "active" and bool(known.intersection(inc)) and not known.intersection(exc)
        rules = [x.get("type") for x in d["rules"] if isinstance(x, dict)]
        if len(rules) != len(d["rules"]) or not all(isinstance(x, str) for x in rules):
            raise Hold("RULE_TYPE_UNKNOWN")
        bypass = d.get("bypass_actors")
        if covered and not isinstance(bypass, list):
            raise Hold("BYPASS_VISIBILITY_UNVERIFIED")
        if covered and bypass == []:
            proven.update(rules)
        rows.append({"id": d["id"], "types": rules, "default_covered": covered,
                     "bypass_count": len(bypass) if isinstance(bypass, list) else "TOKEN_VAZIO"})
    missing = sorted(REQUIRED - proven)
    payload = None
    if missing:
        if any(d.get("name") == NAME for d in s["rulesets"]):
            raise Hold("MANAGED_RULESET_COLLISION_REVIEW_REQUIRED")
        payload = {"name": NAME, "target": "branch", "enforcement": "active",
                   "conditions": {"ref_name": {"include": ["~DEFAULT_BRANCH"], "exclude": []}},
                   "bypass_actors": [], "rules": [{"type": x} for x in missing]}
        if not valid_payload(payload):
            raise Hold("PLAN_INVALID")
    # Plan binds all provider prestate, including existing rulesets and classic protection.
    # No existing rule is replaced. Existing bypass is not modified; the additive
    # rule may constrain an actor on the default branch, explicitly shown in plan.
    result = {"schema": SCHEMA, "repository": s["repo"], "repository_id": s["id"],
              "default_branch": s["branch"], "head_sha": s["head"],
              "prestate_sha256": digest(s), "existing_rule_inventory": rows,
              "classic_protection_http": s["classic_http"], "new_ruleset": payload,
              "effect": "ADDITIVE_DEFAULT_ONLY_NO_BYPASS_MAY_RESTRICT_EXISTING_ACTORS",
              "advanced_rules": "TOKEN_VAZIO_PER_PRODUCER_CONTRACT_REQUIRED",
              "rollback": "HUMAN_REVIEWED_CORRECTIVE_SUCCESSOR_ONLY_NO_AUTOMATIC_DELETE_OR_WEAKEN",
              "claim_allowed": False}
    result["plan_sha256"] = digest(result)
    return result


def verify_new(actual, expected):
    return (isinstance(actual, dict) and all(actual.get(k) == expected[k] for k in
            ("name", "target", "enforcement", "conditions", "bypass_actors"))
            and sorted((canonical(x) for x in actual.get("rules", []))) ==
                sorted((canonical(x) for x in expected["rules"])))


def apply(provider, repo, rid, sha, approved, receipt):
    if not re.fullmatch(r"[a-f0-9]{64}", approved):
        raise Hold("REVIEWED_PLAN_DIGEST_REQUIRED")
    before = state(provider, repo, rid, sha)
    p = plan(before)
    if p["plan_sha256"] != approved:
        raise Hold("PLAN_OR_PROVIDER_PRESTATE_DRIFT")
    if p["new_ruleset"] is None:
        return dict(p, state="NOOP_BASELINE_RULES_OBSERVED", apply_attempted=False)
    again = state(provider, repo, rid, sha)
    if digest(again) != p["prestate_sha256"]:
        raise Hold("CONCURRENT_PROVIDER_CHANGE")
    # Persist the approved plan and recovery pointer before the single POST.
    intent = dict(p, state="WRITE_INTENT_REMOTE_OUTCOME_UNKNOWN", apply_attempted=True)
    write_receipt(receipt.with_name("federation-ruleset-intent.json"), intent)
    status, created, more = provider.request("POST", "/repos/" + repo + "/rulesets", p["new_ruleset"])
    if status != 201 or more or not isinstance(created, dict) or type(created.get("id")) is not int:
        raise Hold("WRITE_OUTCOME_REQUIRES_READBACK_NO_RETRY")
    after = state(provider, repo, rid, sha)
    old = {x["id"]: x for x in before["rulesets"]}
    new = {x["id"]: x for x in after["rulesets"]}
    new_id = created["id"]
    if (after["branch"] != before["branch"]
            or new_id in old or set(new) != set(old) | {new_id}
            or any(new[k] != v for k, v in old.items())
            or after["classic_http"] != before["classic_http"] or after["classic"] != before["classic"]
            or not verify_new(new[new_id], p["new_ruleset"])):
        raise Hold("POSTSTATE_MISMATCH_MANUAL_RECOVERY_REQUIRED")
    return dict(p, state="CONFIG_READBACK_SCOPED_BASELINE_ONLY", apply_attempted=True,
                new_ruleset_id=new_id, poststate_sha256=digest(after),
                negative_merge_test="NOT_RUN", claim_allowed=False)


def write_receipt(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as f:
        f.write(json.dumps(obj, sort_keys=True, indent=2) + "\n")


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--operation", choices=sorted(OPS), required=True)
    a.add_argument("--owner", required=True)
    a.add_argument("--name", default="")
    a.add_argument("--repository-id", default="")
    a.add_argument("--expected-sha", default="")
    a.add_argument("--approved-plan", default="")
    a.add_argument("--receipt", type=Path, required=True)
    x = a.parse_args()
    result = {"schema": SCHEMA, "state": "HOLD", "claim_allowed": False}
    provider = None
    code = 2
    try:
        guard(os.environ, x.owner)
        token = os.environ.get("PROVIDER_TOKEN", "")
        if x.operation == "federation_ruleset_inventory":
            if any((x.name, x.repository_id, x.expected_sha, x.approved_plan)):
                raise Hold("INVENTORY_DOES_NOT_ACCEPT_TARGET_OR_APPROVAL")
            provider = Provider(x.owner, "", token)
            result = inventory(provider, x.owner)
        else:
            repo, rid = target(x.owner, x.name, x.repository_id, x.expected_sha)
            provider = Provider(x.owner, repo, token)
            if x.operation == "federation_ruleset_plan":
                if x.approved_plan:
                    raise Hold("PLAN_DOES_NOT_ACCEPT_APPROVAL")
                result = dict(plan(state(provider, repo, rid, x.expected_sha)),
                              state="PLAN_ONLY_NOT_APPLIED", apply_attempted=False)
            else:
                result = apply(provider, repo, rid, x.expected_sha, x.approved_plan, x.receipt)
        code = 0
    except Hold as exc:
        result["reason"] = str(exc)
    except (TypeError, ValueError, KeyError, AttributeError, OSError):
        result["reason"] = "MALFORMED_RESPONSE_OR_RECEIPT_FAILURE"
    if code:
        result["apply_attempted"] = bool(provider and provider.write_attempted)
        result["remote_outcome"] = "UNKNOWN_NO_RETRY" if result["apply_attempted"] else "NO_WRITE_ATTEMPTED"
    write_receipt(x.receipt, result)
    print("FEDERATION_RULESET=" + result["state"])
    return code


if __name__ == "__main__":
    sys.exit(main())
