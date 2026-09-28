#!/usr/bin/env python3
"""Fail-closed GitHub commit check enumerator.

Separates the Checks API from legacy commit statuses and exhausts pagination.
Uses only the Python standard library.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from dataclasses import dataclass
from typing import Any, Callable

API_VERSION = "2022-11-28"
DEFAULT_API = "https://api.github.com"
FAILURE_CONCLUSIONS = {
    "failure", "timed_out", "action_required", "startup_failure",
}


@dataclass(frozen=True)
class Page:
    total_count: int
    check_runs: list[dict[str, Any]]


class GithubHttp:
    def __init__(self, token: str | None, api_base: str = DEFAULT_API) -> None:
        self.token = token
        self.api_base = api_base.rstrip("/")

    def get_json(self, path: str, query: dict[str, Any] | None = None) -> dict[str, Any]:
        url = self.api_base + path
        if query:
            url += "?" + urllib.parse.urlencode(query)
        headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": API_VERSION,
            "User-Agent": "RafGitTools-check-custody/1",
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        request = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.load(response)
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"GitHub HTTP {exc.code}: {body[:1000]}") from exc


def collect_check_runs(
    fetch_page: Callable[[int, int], Page],
    *,
    per_page: int = 100,
    max_pages: int = 100,
) -> dict[str, Any]:
    if not 1 <= per_page <= 100:
        raise ValueError("per_page must be in 1..100")
    by_id: dict[int, dict[str, Any]] = {}
    totals: list[int] = []
    page_sizes: list[int] = []

    for page_no in range(1, max_pages + 1):
        page = fetch_page(page_no, per_page)
        totals.append(int(page.total_count))
        page_sizes.append(len(page.check_runs))
        for item in page.check_runs:
            check_id = int(item["id"])
            by_id[check_id] = item

        target = max(totals)
        if len(by_id) >= target:
            break
        if not page.check_runs:
            break
    else:
        raise RuntimeError(f"check-run pagination exceeded max_pages={max_pages}")

    total_count = max(totals) if totals else 0
    observed_totals = sorted(set(totals))
    checks = list(by_id.values())
    complete = len(checks) == total_count
    snapshot_unstable = len(observed_totals) > 1

    status_counts = Counter(str(c.get("status") or "TOKEN_VAZIO") for c in checks)
    conclusion_counts = Counter(str(c.get("conclusion") or "TOKEN_VAZIO") for c in checks)
    nonterminal = [c for c in checks if c.get("status") != "completed"]
    failure_like = [c for c in checks if c.get("conclusion") in FAILURE_CONCLUSIONS]
    cancelled = [c for c in checks if c.get("conclusion") == "cancelled"]
    non_success_terminal = [
        c for c in checks
        if c.get("status") == "completed" and c.get("conclusion") != "success"
    ]

    if not complete:
        state = "INCOMPLETE_ENUMERATION"
    elif snapshot_unstable:
        state = "AUDIT_SNAPSHOT_UNSTABLE"
    elif nonterminal:
        state = "PENDING"
    elif failure_like:
        state = "COMPLETE_WITH_FAILURES"
    elif cancelled:
        state = "COMPLETE_WITH_CANCELLATIONS"
    elif non_success_terminal:
        state = "COMPLETE_WITH_NON_SUCCESS"
    else:
        state = "COMPLETE_ALL_SUCCESS"

    return {
        "total_count": total_count,
        "returned_unique_count": len(checks),
        "complete_enumeration": complete,
        "snapshot_unstable": snapshot_unstable,
        "observed_total_counts": observed_totals,
        "pages_fetched": len(page_sizes),
        "page_sizes": page_sizes,
        "per_page_requested": per_page,
        "status_counts": dict(sorted(status_counts.items())),
        "conclusion_counts": dict(sorted(conclusion_counts.items())),
        "failure_count": len(failure_like),
        "cancelled_count": len(cancelled),
        "pending_count": len(nonterminal),
        "state": state,
        "checks": [
            {
                "id": c.get("id"),
                "name": c.get("name"),
                "status": c.get("status"),
                "conclusion": c.get("conclusion"),
                "details_url": c.get("details_url"),
                "started_at": c.get("started_at"),
                "completed_at": c.get("completed_at"),
            }
            for c in checks
        ],
    }


def audit_commit(client: GithubHttp, owner: str, repo: str, sha: str) -> dict[str, Any]:
    def fetch(page_no: int, per_page: int) -> Page:
        data = client.get_json(
            f"/repos/{owner}/{repo}/commits/{sha}/check-runs",
            {"page": page_no, "per_page": per_page},
        )
        return Page(total_count=int(data["total_count"]), check_runs=list(data.get("check_runs", [])))

    checks = collect_check_runs(fetch)
    legacy = client.get_json(f"/repos/{owner}/{repo}/commits/{sha}/status")
    statuses = list(legacy.get("statuses", []))

    result = {
        "schema": "RAFGITTOOLS_COMMIT_CHECK_AUDIT_V1",
        "repository": f"{owner}/{repo}",
        "sha": sha,
        "checks_api": checks,
        "legacy_status_api": {
            "combined_state": legacy.get("state"),
            "status_count": len(statuses),
            "statuses": [
                {
                    "id": s.get("id"),
                    "context": s.get("context"),
                    "state": s.get("state"),
                    "target_url": s.get("target_url"),
                    "created_at": s.get("created_at"),
                    "updated_at": s.get("updated_at"),
                }
                for s in statuses
            ],
        },
        "invariants": [
            "FIRST_PAGE_CHECKS!=COMPLETE_CHECK_SET",
            "CHECKS_API!=LEGACY_STATUS_API",
            "TOKEN_VAZIO!=0",
        ],
        "claim_allowed": False,
    }
    return result


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--owner", required=True)
    p.add_argument("--repo", required=True)
    p.add_argument("--sha", required=True)
    p.add_argument("--output", required=True)
    p.add_argument("--api-base", default=DEFAULT_API)
    p.add_argument("--token-env", default="GITHUB_TOKEN")
    return p.parse_args()


def main() -> int:
    args = parse_args()
    client = GithubHttp(os.environ.get(args.token_env), args.api_base)
    try:
        result = audit_commit(client, args.owner, args.repo, args.sha)
    except Exception as exc:
        print(f"AUDIT_ERROR={exc}", file=sys.stderr)
        return 2

    with open(args.output, "w", encoding="utf-8") as fh:
        json.dump(result, fh, indent=2, sort_keys=True)
        fh.write("\n")

    checks = result["checks_api"]
    print(f"CHECK_TOTAL={checks['total_count']}")
    print(f"CHECK_RETURNED={checks['returned_unique_count']}")
    print(f"CHECK_PAGES={checks['pages_fetched']}")
    print(f"CHECK_STATE={checks['state']}")
    print(f"LEGACY_STATUS_COUNT={result['legacy_status_api']['status_count']}")

    if not checks["complete_enumeration"] or checks["snapshot_unstable"]:
        return 3
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
