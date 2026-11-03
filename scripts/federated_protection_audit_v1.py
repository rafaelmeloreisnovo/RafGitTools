#!/usr/bin/env python3
"""Bounded, GET-only GitHub protection census. Never export provider identities or PATs."""
import argparse
import datetime
import json
import os
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.github.com"
MAX_PAGES = 10
MAX_REPOS = 500


class Blocked(Exception):
    pass


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        raise Blocked("REDIRECT_BLOCKED")


class Github:
    def __init__(self, token):
        if not token:
            raise Blocked("TOKEN_VAZIO_PROVIDER_TOKEN")
        self.token = token
        self.opener = urllib.request.build_opener(NoRedirect())

    def get(self, path):
        if not path.startswith("/") or "//" in path:
            raise Blocked("INVALID_ENDPOINT")
        request = urllib.request.Request(API + path, headers={
            "Accept": "application/vnd.github+json",
            "Authorization": "Bearer " + self.token,
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "RAFAELIA-readonly-census-v1",
        }, method="GET")
        try:
            with self.opener.open(request, timeout=12) as response:
                return response.status, json.loads(response.read(2_000_000))
        except urllib.error.HTTPError as error:
            if error.code in (403, 404, 429):
                return error.code, None
            raise Blocked("PROVIDER_HTTP_" + str(error.code)) from None
        except (urllib.error.URLError, TimeoutError, ValueError):
            raise Blocked("PROVIDER_TRANSPORT_OR_FORMAT") from None


def census(client, max_pages=MAX_PAGES, max_repos=MAX_REPOS):
    if not (1 <= max_pages <= MAX_PAGES and 1 <= max_repos <= MAX_REPOS):
        raise Blocked("LIMIT_INVALID")
    counts = {field: 0 for field in (
        "observed", "duplicates", "default_branch_unknown",
        "branch_protection_200", "branch_protection_404_unknown",
        "branch_protection_403_unknown", "branch_protection_other_unknown",
        "ruleset_list_nonempty", "ruleset_list_empty", "ruleset_unknown"
    )}
    seen = set()
    terminal = False
    reason = "TOKEN_VAZIO"
    page = 1
    while page <= max_pages:
        status, data = client.get(
            "/user/repos?per_page=100&page=" + str(page)
            + "&affiliation=owner,collaborator,organization_member&sort=full_name"
        )
        if status != 200 or not isinstance(data, list):
            reason = "REPO_LIST_PROVIDER_UNKNOWN_" + str(status)
            break
        if not data:
            reason, terminal = "EMPTY_TERMINAL_PAGE", True
            break
        for item in data:
            if not isinstance(item, dict):
                raise Blocked("INVALID_PROVIDER_ITEM")
            name = item.get("full_name")
            branch = item.get("default_branch")
            if not isinstance(name, str) or name.count("/") != 1:
                raise Blocked("INVALID_REPOSITORY_IDENTITY")
            owner, repo = name.split("/", 1)
            if not owner or not repo:
                raise Blocked("INVALID_REPOSITORY_IDENTITY")
            if name in seen:
                counts["duplicates"] += 1
                continue
            seen.add(name)
            counts["observed"] += 1
            if isinstance(branch, str) and branch:
                enc = lambda value: urllib.parse.quote(value, safe="")
                base = "/repos/" + enc(owner) + "/" + enc(repo)
                status_prot, _ = client.get(
                    base + "/branches/" + enc(branch) + "/protection"
                )
                if status_prot == 200:
                    counts["branch_protection_200"] += 1
                elif status_prot == 404:
                    counts["branch_protection_404_unknown"] += 1
                elif status_prot == 403:
                    counts["branch_protection_403_unknown"] += 1
                else:
                    counts["branch_protection_other_unknown"] += 1
                status_rules, rules = client.get(
                    base + "/rulesets?includes_parents=true"
                )
                if status_rules == 200 and isinstance(rules, list):
                    key = "ruleset_list_nonempty" if rules else "ruleset_list_empty"
                    counts[key] += 1
                else:
                    counts["ruleset_unknown"] += 1
            else:
                counts["default_branch_unknown"] += 1
            if counts["observed"] >= max_repos:
                reason = "MAX_REPOSITORIES_REACHED"
                break
        if reason == "MAX_REPOSITORIES_REACHED":
            break
        if len(data) < 100:
            reason, terminal = "SHORT_TERMINAL_PAGE", True
            break
        page += 1
    if not terminal and reason == "TOKEN_VAZIO":
        reason = "MAX_PAGES_REACHED"
    return {
        "schema": "rafaelia.github.protection-census.v1",
        "credential_id": "PAT_ENV",
        "operation": "READ_ONLY_GET",
        "provider_mutation_performed": False,
        "scope": "REPOSITORIES_VISIBLE_TO_PAT_ENV_ONLY",
        "page_terminal_observed": terminal,
        "snapshot_atomic": "TOKEN_VAZIO",
        "coverage_state": "TERMINAL_OBSERVED_NOT_GLOBAL" if terminal
                          else "PARTIAL_OR_BLOCKED",
        "exit_reason": reason,
        "protection_policy_adequacy": "TOKEN_VAZIO_NOT_VERIFIED",
        "claim_allowed": False,
        "counts": counts,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--receipt", required=True)
    args = parser.parse_args()
    result = {
        "schema": "rafaelia.github.protection-census.v1",
        "credential_id": "PAT_ENV",
        "claim_allowed": False,
        "provider_mutation_performed": False,
        "coverage_state": "BLOCKED_TOKEN_VAZIO",
    }
    status = 2
    try:
        if (os.getenv("GITHUB_EVENT_NAME") != "workflow_dispatch" or
                os.getenv("GITHUB_REF") != "refs/heads/main" or
                os.getenv("GITHUB_ACTOR") != "rafaelmeloreisnovo"):
            raise Blocked("NOT_OWNER_MANUAL_MAIN")
        client = Github(os.environ.get("PROVIDER_TOKEN", ""))
        result = census(client)
        status = 0 if result["page_terminal_observed"] else 2
    except Blocked as error:
        result["exit_reason"] = str(error)
    result["observed_at"] = datetime.datetime.now(
        datetime.timezone.utc
    ).isoformat()
    result["source_commit"] = os.environ.get("GITHUB_SHA", "TOKEN_VAZIO")
    result["run_id"] = os.environ.get("GITHUB_RUN_ID", "TOKEN_VAZIO")
    os.makedirs(os.path.dirname(os.path.abspath(args.receipt)), exist_ok=True)
    with open(args.receipt, "w", encoding="utf-8") as output:
        json.dump(result, output, sort_keys=True, indent=2)
        output.write("\n")
    print("CENSUS_STATE=" + result["coverage_state"])
    print("EXIT_REASON=" + result.get("exit_reason", "TOKEN_VAZIO"))
    print("CLAIM_ALLOWED=false")
    return status


if __name__ == "__main__":
    raise SystemExit(main())
