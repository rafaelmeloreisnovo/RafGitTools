#!/usr/bin/env python3
"""Render RLL branch topology without changing branches or PRs."""
from __future__ import annotations

import argparse
import json
import os
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

TOPOLOGY = ["WORK", "rll/lab", "rll/integration", "rll/release", "main"]


def fetch_json(url: str) -> Any:
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "RafGitTools-RLL-Atlas/1.0"}
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.loads(response.read())


def classify(name: str) -> str:
    if name in {"rll/lab", "rll/integration", "rll/release", "main"}:
        return name
    return "WORK"


def render(repo: str) -> dict[str, Any]:
    branches = []
    page = 1
    while True:
        batch = fetch_json(f"https://api.github.com/repos/{repo}/branches?per_page=100&page={page}")
        if not batch:
            break
        branches.extend(batch)
        if len(batch) < 100:
            break
        page += 1
    groups = {key: [] for key in TOPOLOGY}
    for item in branches:
        name = item["name"]
        groups[classify(name)].append({"name": name, "sha": item["commit"]["sha"]})
    for group in groups.values():
        group.sort(key=lambda x: x["name"])
    return {
        "schema": "rafgittools.rll-branch-atlas/v1",
        "observed_at": datetime.now(timezone.utc).isoformat(),
        "repository": repo,
        "promotion_topology": "WORK -> rll/lab -> rll/integration -> rll/release -> main",
        "branch_count": len(branches),
        "groups": groups,
        "claim_allowed": False,
        "note": "Branch presence and topology are governance state, not scientific evidence.",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default="instituto-Rafael/relativity-living-light")
    parser.add_argument("--output", default="artifacts/rll-branch-atlas.json")
    args = parser.parse_args()
    payload = render(args.repo)
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"branch_count": payload["branch_count"], "output": str(out)}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
