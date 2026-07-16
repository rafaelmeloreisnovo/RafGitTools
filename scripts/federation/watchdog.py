#!/usr/bin/env python3
"""Validate the RAFAELIA federated repository contract.

This tool validates the local manifest only. It does not claim to execute or
inspect remote repositories. Missing remote evidence must remain TOKEN_VAZIO.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import sys
from pathlib import Path
from typing import Any

REQUIRED_REPO_FIELDS = {
    "name",
    "role",
    "critical",
    "dependencies",
    "health_probe",
    "safe_state",
    "failover",
    "rollback",
    "blind_tests",
}
ALLOWED_BLIND_TESTS = {
    "order-permutation",
    "fixture-blindness",
    "implementation-blindness",
    "failure-injection",
    "temporal-blindness",
}


def canonical_repository_view(repositories: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Return an order-independent normalized repository view."""
    normalized: list[dict[str, Any]] = []
    for repo in repositories:
        item = dict(repo)
        item["dependencies"] = sorted(item["dependencies"])
        item["blind_tests"] = sorted(item["blind_tests"])
        normalized.append(item)
    return sorted(normalized, key=lambda value: value["name"])


def semantic_digest(manifest: dict[str, Any]) -> str:
    normalized = dict(manifest)
    normalized["repositories"] = canonical_repository_view(manifest["repositories"])
    payload = json.dumps(
        normalized,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def validate_manifest(manifest: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if manifest.get("schema_version") != "1.0.0":
        errors.append("schema_version must be 1.0.0")

    policies = manifest.get("policies", {})
    if policies.get("token_vazio_is_valid") is not True:
        errors.append("TOKEN_VAZIO policy must remain enabled")
    if policies.get("temporal_inference_is_forbidden") is not True:
        errors.append("temporal inference refusal must remain enabled")
    if policies.get("default_change_mode") != "draft_pull_request":
        errors.append("default change mode must be draft_pull_request")

    repositories = manifest.get("repositories")
    if not isinstance(repositories, list) or not repositories:
        return errors + ["repositories must be a non-empty list"]

    names: set[str] = set()
    for index, repo in enumerate(repositories):
        if not isinstance(repo, dict):
            errors.append(f"repositories[{index}] must be an object")
            continue

        missing = REQUIRED_REPO_FIELDS - repo.keys()
        if missing:
            errors.append(
                f"{repo.get('name', f'repositories[{index}]')} missing fields: "
                + ", ".join(sorted(missing))
            )
            continue

        name = repo["name"]
        if name in names:
            errors.append(f"duplicate repository: {name}")
        names.add(name)

        if not isinstance(repo["critical"], bool):
            errors.append(f"{name}: critical must be boolean")
        if not isinstance(repo["dependencies"], list):
            errors.append(f"{name}: dependencies must be a list")
        if not isinstance(repo["blind_tests"], list) or not repo["blind_tests"]:
            errors.append(f"{name}: at least one blind test is required")
        else:
            unknown = set(repo["blind_tests"]) - ALLOWED_BLIND_TESTS
            if unknown:
                errors.append(f"{name}: unknown blind tests: {sorted(unknown)}")
        for field in ("health_probe", "safe_state", "rollback"):
            if not isinstance(repo[field], str) or not repo[field].strip():
                errors.append(f"{name}: {field} must be non-empty")

    for repo in repositories:
        if not isinstance(repo, dict) or "name" not in repo:
            continue
        for dependency in repo.get("dependencies", []):
            if dependency not in names:
                errors.append(f"{repo['name']}: unknown dependency {dependency}")
        failover = repo.get("failover")
        if failover is not None and failover not in names:
            errors.append(f"{repo['name']}: unknown failover {failover}")
        if failover == repo["name"]:
            errors.append(f"{repo['name']}: failover cannot point to itself")

    control_plane = manifest.get("control_plane")
    if control_plane not in names:
        errors.append("control_plane must reference a repository in the manifest")

    return errors


def blind_order_test(manifest: dict[str, Any], seed: int) -> dict[str, Any]:
    baseline = semantic_digest(manifest)
    shuffled = dict(manifest)
    repositories = list(manifest["repositories"])
    random.Random(seed).shuffle(repositories)
    shuffled["repositories"] = repositories
    observed = semantic_digest(shuffled)
    return {
        "name": "order-permutation",
        "seed": seed,
        "baseline_digest": baseline,
        "observed_digest": observed,
        "pass": baseline == observed,
    }


def simulate_failure(manifest: dict[str, Any], failed_name: str) -> dict[str, Any]:
    by_name = {repo["name"]: repo for repo in manifest["repositories"]}
    if failed_name not in by_name:
        return {
            "failed_repository": failed_name,
            "status": "BLOCKED",
            "reason": "repository not found in manifest",
        }

    failed = by_name[failed_name]
    dependents = sorted(
        repo["name"]
        for repo in manifest["repositories"]
        if failed_name in repo["dependencies"]
    )
    failover = failed["failover"]

    if failover is not None:
        status = "FAILOVER_AVAILABLE"
    elif failed["safe_state"]:
        status = "FAIL_SAFE_ONLY"
    else:
        status = "UNSAFE_FAILURE"

    return {
        "failed_repository": failed_name,
        "critical": failed["critical"],
        "status": status,
        "safe_state": failed["safe_state"],
        "failover": failover,
        "dependents_isolated": dependents,
        "claim_policy": "failure cannot promote claims",
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--seed", type=int, default=144000)
    parser.add_argument("--simulate-failure")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"BLOCKED: cannot load manifest: {exc}", file=sys.stderr)
        return 2

    errors = validate_manifest(manifest)
    blind = blind_order_test(manifest, args.seed) if not errors else None
    simulation = (
        simulate_failure(manifest, args.simulate_failure)
        if args.simulate_failure and not errors
        else None
    )

    report = {
        "status": "PASS" if not errors and blind and blind["pass"] else "FAIL",
        "manifest": str(args.manifest),
        "semantic_digest": semantic_digest(manifest) if not errors else None,
        "validation_errors": errors,
        "blind_test": blind,
        "failure_simulation": simulation,
        "epistemic_note": (
            "This PASS validates manifest structure and determinism only; "
            "remote runtime evidence remains repository-local."
        ),
    }

    encoded = json.dumps(report, indent=2, ensure_ascii=False) + "\n"
    print(encoded, end="")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(encoded, encoding="utf-8")

    if errors or not blind or not blind["pass"]:
        return 1
    if simulation and simulation["status"] == "UNSAFE_FAILURE":
        return 3
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
