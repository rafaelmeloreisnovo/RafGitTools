#!/usr/bin/env python3
"""Verify least-privilege read access to one exact private provider commit.

The gate never prints or persists the credential. It emits a machine-readable
receipt that distinguishes missing/insufficient authority from source absence and
never promotes CI access into runtime/device proof.
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
from typing import Any, Callable

SCHEMA = "rafaelia.private-provider-access-receipt.v1"
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
REPO_RE = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
TOKEN_VAZIO_ACCESS = "TOKEN_VAZIO_CROSS_REPO_PRIVATE_ACCESS"
TOKEN_VAZIO_NETWORK = "TOKEN_VAZIO_CROSS_REPO_PRIVATE_ACCESS_NETWORK"
TOKEN_VAZIO_COMMIT_IDENTITY = "TOKEN_VAZIO_CROSS_REPO_PRIVATE_COMMIT_IDENTITY"


def canonical_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def base_receipt(repo: str, commit: str, token_present: bool) -> dict[str, Any]:
    return {
        "schema": SCHEMA,
        "repository": repo,
        "requested_commit": commit,
        "token_present": token_present,
        "http_status": None,
        "observed_commit": None,
        "result": "BLOCKED",
        "access_proven": False,
        "claim_allowed": False,
        "runtime_proven": False,
        "device_proven": False,
        "f_gap": [],
        "f_next": "Provide a least-privilege read credential and re-run this exact commit-bound gate.",
    }


def validate_inputs(repo: str, commit: str) -> None:
    if not REPO_RE.fullmatch(repo):
        raise ValueError(f"invalid repository identifier: {repo!r}")
    if not SHA40_RE.fullmatch(commit):
        raise ValueError("commit must be an exact lowercase 40-hex SHA")


def evaluate(
    repo: str,
    commit: str,
    token: str,
    *,
    api_base: str = "https://api.github.com",
    opener: Callable[..., Any] = urllib.request.urlopen,
) -> dict[str, Any]:
    validate_inputs(repo, commit)
    receipt = base_receipt(repo, commit, bool(token))
    if not token:
        receipt["f_gap"] = [TOKEN_VAZIO_ACCESS]
        return receipt

    url = f"{api_base.rstrip('/')}/repos/{repo}/commits/{commit}"
    request = urllib.request.Request(
        url,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "RafGitTools-private-provider-access-gate/1",
        },
        method="GET",
    )

    try:
        with opener(request, timeout=20) as response:
            status = int(getattr(response, "status", response.getcode()))
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        receipt["http_status"] = int(exc.code)
        if exc.code in (401, 403, 404):
            receipt["f_gap"] = [TOKEN_VAZIO_ACCESS]
            return receipt
        receipt["f_gap"] = [TOKEN_VAZIO_NETWORK]
        receipt["f_next"] = "Re-run after the GitHub API/provider path is reachable; do not infer access from this failure."
        return receipt
    except (urllib.error.URLError, TimeoutError, OSError):
        receipt["f_gap"] = [TOKEN_VAZIO_NETWORK]
        receipt["f_next"] = "Re-run after the GitHub API/provider path is reachable; do not infer access from this failure."
        return receipt

    receipt["http_status"] = status
    if status != 200:
        receipt["f_gap"] = [TOKEN_VAZIO_NETWORK]
        return receipt

    try:
        payload = json.loads(body)
    except json.JSONDecodeError:
        receipt["f_gap"] = [TOKEN_VAZIO_COMMIT_IDENTITY]
        receipt["f_next"] = "Provider response was not valid JSON; re-run without promoting the source identity."
        return receipt

    observed = payload.get("sha") if isinstance(payload, dict) else None
    receipt["observed_commit"] = observed if isinstance(observed, str) else None
    if observed != commit:
        receipt["f_gap"] = [TOKEN_VAZIO_COMMIT_IDENTITY]
        receipt["f_next"] = "Resolve provider commit identity mismatch before checkout or build."
        return receipt

    receipt.update(
        {
            "result": "PASS",
            "access_proven": True,
            "f_gap": [],
            "f_next": "Proceed to checkout this exact commit; access proof alone is not build/runtime/device proof.",
        }
    )
    return receipt


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--token-env", default="RAF_CROSS_REPO_TOKEN")
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--api-base", default="https://api.github.com")
    args = parser.parse_args()

    try:
        token = os.environ.get(args.token_env, "")
        result = evaluate(args.repo, args.commit, token, api_base=args.api_base)
        args.receipt.parent.mkdir(parents=True, exist_ok=True)
        args.receipt.write_bytes(canonical_bytes(result))
        print(
            json.dumps(
                {
                    "result": result["result"],
                    "repository": result["repository"],
                    "requested_commit": result["requested_commit"],
                    "http_status": result["http_status"],
                    "access_proven": result["access_proven"],
                    "claim_allowed": False,
                    "f_gap": result["f_gap"],
                },
                sort_keys=True,
            )
        )
        return 0 if result["result"] == "PASS" else 3
    except (ValueError, OSError) as exc:
        print(f"[FALHA] private provider access gate: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
