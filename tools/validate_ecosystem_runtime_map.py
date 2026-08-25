#!/usr/bin/env python3
"""Validate RAFAELIA ecosystem runtime-map invariants.

This validator intentionally uses only the Python standard library so it can run
in constrained Termux, CI and offline audit environments.  It validates the
operational invariants that matter for the phase-1 map; JSON Schema remains the
machine-readable structural contract.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any

SCHEMA_ID = "raf.ecosystem-runtime-map.v1"
STATES = (
    "PASS",
    "PARTIAL",
    "DESIGN",
    "TOKEN_VAZIO",
    "BLOCKED",
    "QUARANTINE",
)
_EXTERNAL_STATES = set(STATES) | {"REFERENCE"}
_SHA40 = re.compile(r"^[a-f0-9]{40}$")
_REPOSITORY = re.compile(r"^[^/]+/[^/]+$")


class ValidationError(ValueError):
    """Raised when an ecosystem runtime map violates a canonical invariant."""


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def _require_string(value: Any, path: str) -> str:
    _require(isinstance(value, str) and bool(value.strip()), f"{path}: expected non-empty string")
    return value


def _require_string_list(value: Any, path: str, *, allow_empty: bool = True) -> list[str]:
    _require(isinstance(value, list), f"{path}: expected array")
    if not allow_empty:
        _require(bool(value), f"{path}: expected non-empty array")
    for index, item in enumerate(value):
        _require_string(item, f"{path}[{index}]")
    return value


def _state_counts(items: list[dict[str, Any]]) -> dict[str, int]:
    counts = Counter(item["state"] for item in items)
    return {
        "pass": counts["PASS"],
        "partial": counts["PARTIAL"],
        "design": counts["DESIGN"],
        "token_vazio": counts["TOKEN_VAZIO"],
        "blocked": counts["BLOCKED"],
        "quarantine": counts["QUARANTINE"],
    }


def validate_map(data: dict[str, Any]) -> dict[str, Any]:
    """Validate *data* and return a compact deterministic audit summary."""

    _require(isinstance(data, dict), "root: expected object")
    _require(data.get("schema") == SCHEMA_ID, f"schema: expected {SCHEMA_ID}")
    _require_string(data.get("generated_at"), "generated_at")

    coordinator = data.get("coordinator")
    _require(isinstance(coordinator, dict), "coordinator: expected object")
    coordinator_repo = _require_string(coordinator.get("repository"), "coordinator.repository")
    _require(bool(_REPOSITORY.fullmatch(coordinator_repo)), "coordinator.repository: expected owner/name")
    _require_string(coordinator.get("branch"), "coordinator.branch")

    policy = data.get("policy")
    _require(isinstance(policy, dict), "policy: expected object")
    for key in (
        "claim_allowed",
        "unknown_is_success",
        "file_presence_is_runtime",
        "certified_without_external_evidence",
    ):
        _require(policy.get(key) is False, f"policy.{key}: must be false")

    repositories = data.get("repositories")
    _require(isinstance(repositories, list) and repositories, "repositories: expected non-empty array")
    repository_ids: set[str] = set()
    for index, repository in enumerate(repositories):
        path = f"repositories[{index}]"
        _require(isinstance(repository, dict), f"{path}: expected object")
        repo_id = _require_string(repository.get("repository"), f"{path}.repository")
        _require(bool(_REPOSITORY.fullmatch(repo_id)), f"{path}.repository: expected owner/name")
        _require(repo_id not in repository_ids, f"{path}.repository: duplicate {repo_id}")
        repository_ids.add(repo_id)
        _require_string(repository.get("default_branch"), f"{path}.default_branch")
        observed_ref = _require_string(repository.get("observed_ref"), f"{path}.observed_ref")
        _require(bool(_SHA40.fullmatch(observed_ref)), f"{path}.observed_ref: expected 40 lowercase hex chars")
        size = repository.get("repository_size_kib")
        _require(isinstance(size, int) and not isinstance(size, bool) and size >= 0, f"{path}.repository_size_kib: expected non-negative integer")
        _require_string(repository.get("role"), f"{path}.role")
        _require(repository.get("state") in STATES, f"{path}.state: unknown state")
        _require_string_list(repository.get("evidence"), f"{path}.evidence")
        _require_string_list(repository.get("gaps"), f"{path}.gaps")
        _require_string_list(repository.get("next_actions"), f"{path}.next_actions")

    _require(coordinator_repo in repository_ids, "coordinator.repository: must be present in repositories")

    external_dependencies = data.get("external_dependencies")
    _require(isinstance(external_dependencies, list), "external_dependencies: expected array")
    external_ids: set[str] = set()
    for index, dependency in enumerate(external_dependencies):
        path = f"external_dependencies[{index}]"
        _require(isinstance(dependency, dict), f"{path}: expected object")
        dependency_id = _require_string(dependency.get("id"), f"{path}.id")
        _require(dependency_id not in external_ids, f"{path}.id: duplicate {dependency_id}")
        _require(dependency_id not in repository_ids, f"{path}.id: collides with repository")
        external_ids.add(dependency_id)
        _require_string(dependency.get("role"), f"{path}.role")
        _require(dependency.get("state") in _EXTERNAL_STATES, f"{path}.state: unknown state")
        _require_string(dependency.get("reason"), f"{path}.reason")

    known_nodes = repository_ids | external_ids

    capabilities = data.get("capabilities")
    _require(isinstance(capabilities, list) and capabilities, "capabilities: expected non-empty array")
    capability_ids: set[str] = set()
    for index, capability in enumerate(capabilities):
        path = f"capabilities[{index}]"
        _require(isinstance(capability, dict), f"{path}: expected object")
        capability_id = _require_string(capability.get("id"), f"{path}.id")
        _require(capability_id not in capability_ids, f"{path}.id: duplicate {capability_id}")
        capability_ids.add(capability_id)
        _require(capability.get("state") in STATES, f"{path}.state: unknown state")
        nodes = _require_string_list(capability.get("repositories"), f"{path}.repositories", allow_empty=False)
        _require(len(nodes) == len(set(nodes)), f"{path}.repositories: duplicates are not allowed")
        unknown_nodes = sorted(set(nodes) - known_nodes)
        _require(not unknown_nodes, f"{path}.repositories: unknown nodes {unknown_nodes}")
        evidence = _require_string_list(capability.get("evidence"), f"{path}.evidence")
        _require_string(capability.get("gap"), f"{path}.gap")
        _require_string(capability.get("gate"), f"{path}.gate")
        if capability.get("state") == "PASS":
            _require(bool(evidence), f"{path}.evidence: PASS requires evidence")

    edges = data.get("edges")
    _require(isinstance(edges, list), "edges: expected array")
    edge_keys: set[tuple[str, str, str]] = set()
    for index, edge in enumerate(edges):
        path = f"edges[{index}]"
        _require(isinstance(edge, dict), f"{path}: expected object")
        source = _require_string(edge.get("from"), f"{path}.from")
        target = _require_string(edge.get("to"), f"{path}.to")
        _require(source in known_nodes, f"{path}.from: unknown node {source}")
        _require(target in known_nodes, f"{path}.to: unknown node {target}")
        _require(source != target, f"{path}: self-edge is not allowed")
        contract = _require_string(edge.get("contract"), f"{path}.contract")
        edge_key = (source, target, contract)
        _require(edge_key not in edge_keys, f"{path}: duplicate edge")
        edge_keys.add(edge_key)
        _require(edge.get("state") in STATES, f"{path}.state: unknown state")
        _require_string_list(edge.get("evidence"), f"{path}.evidence")
        _require_string(edge.get("gap"), f"{path}.gap")

    summary = data.get("summary")
    _require(isinstance(summary, dict), "summary: expected object")
    _require(summary.get("repositories_total") == len(repositories), "summary.repositories_total: mismatch")
    _require(summary.get("capabilities_total") == len(capabilities), "summary.capabilities_total: mismatch")
    expected_repository_states = _state_counts(repositories)
    expected_capability_states = _state_counts(capabilities)
    _require(summary.get("repository_states") == expected_repository_states, "summary.repository_states: mismatch")
    _require(summary.get("capability_states") == expected_capability_states, "summary.capability_states: mismatch")

    return {
        "schema": SCHEMA_ID,
        "status": "PASS",
        "repositories": len(repositories),
        "external_dependencies": len(external_dependencies),
        "capabilities": len(capabilities),
        "edges": len(edges),
        "repository_states": expected_repository_states,
        "capability_states": expected_capability_states,
        "claim_allowed": False,
    }


def load_and_validate(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    return validate_map(data)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate a RAFAELIA ecosystem runtime map")
    parser.add_argument(
        "map",
        nargs="?",
        type=Path,
        default=Path("configs/ecosystem-runtime-map.phase1.json"),
        help="path to ecosystem runtime map JSON",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        result = load_and_validate(args.map)
    except (OSError, json.JSONDecodeError, ValidationError) as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False))
        return 1
    print(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
