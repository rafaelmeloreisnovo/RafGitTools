#!/usr/bin/env python3
"""Validate the modular RAFAELIA platform assurance control plane."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any

INDEX_SCHEMA = "rafaelia.platform-assurance-index.v1"
CONTROL_SCHEMA = "rafaelia.platform-assurance-control-plane.v1"
REPOSITORIES_SCHEMA = "rafaelia.platform-assurance-repositories.v1"
WORK_ITEMS_SCHEMA = "rafaelia.platform-assurance-work-items.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REPOSITORY_RE = re.compile(r"^[^/\s]+/[^/\s]+$")
WORK_ID_RE = re.compile(r"^WI-[A-Z0-9-]+$")
PRIORITIES = {"P0", "P1", "P2", "P3"}
WORK_STATES = {"MERGED_LIMITED", "PARTIAL", "BLOCKED", TOKEN_VAZIO, "CLOSED"}
DIMENSION_STATES = {
    "PASS", "PASS_LIMITED", "PARTIAL", "BLOCKED", TOKEN_VAZIO,
    "ZERO_STEP_NO_LOGS", "NOT_APPLICABLE",
}
REQUIRED_DIMENSIONS = {
    "code", "tests", "ci", "artifact", "runtime", "security", "rights",
    "documentation", "authority", "rollback", "provenance",
}
FALSE_POLICIES = {
    "claim_allowed", "unknown_is_success", "file_presence_is_execution",
    "zero_step_failure_is_code_failure", "active_adapter_is_complete_evidence",
    "documentation_can_override_security_blocker",
    "average_score_can_compensate_blocker",
    "automatic_cross_repository_write", "automatic_merge",
}


class ValidationError(ValueError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def string(value: Any, path: str) -> str:
    require(isinstance(value, str) and bool(value.strip()), f"{path}: non-empty string required")
    return value


def strings(value: Any, path: str, *, nonempty: bool = False) -> list[str]:
    require(isinstance(value, list), f"{path}: array required")
    if nonempty:
        require(bool(value), f"{path}: non-empty array required")
    for index, item in enumerate(value):
        string(item, f"{path}[{index}]")
    return value


def json_load(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValidationError(f"{path}: {exc}") from exc
    require(isinstance(data, dict), f"{path}: object required")
    return data


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def index_digest(index: dict[str, Any]) -> str:
    clone = json.loads(json.dumps(index))
    clone["aggregate_sha256"] = ""
    raw = json.dumps(
        clone, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def load_bundle(index_path: Path) -> dict[str, Any]:
    root = index_path.resolve().parents[2]
    index = json_load(index_path)
    require(index.get("schema") == INDEX_SCHEMA, "index.schema: invalid")
    require(index.get("claim_allowed") is False, "index.claim_allowed: must be false")
    require(
        index.get("authority_repository") == "rafaelmeloreisnovo/RafGitTools",
        "index.authority_repository: invalid",
    )
    files = index.get("files")
    require(isinstance(files, list) and files, "index.files: non-empty array required")
    paths: set[str] = set()
    documents: dict[str, dict[str, Any]] = {}
    for position, entry in enumerate(files):
        prefix = f"index.files[{position}]"
        require(isinstance(entry, dict), f"{prefix}: object required")
        relative = string(entry.get("path"), f"{prefix}.path")
        require(relative not in paths, f"{prefix}.path: duplicate")
        paths.add(relative)
        candidate = (root / relative).resolve()
        require(
            candidate == root or root in candidate.parents,
            f"{prefix}.path: traversal outside repository",
        )
        expected = string(entry.get("sha256"), f"{prefix}.sha256")
        require(bool(SHA256_RE.fullmatch(expected)), f"{prefix}.sha256: invalid")
        require(candidate.is_file(), f"{prefix}.path: missing file")
        require(sha256_file(candidate) == expected, f"{prefix}.sha256: file digest mismatch")
        document = json_load(candidate)
        records = len(document.get("repositories", document.get("work_items", [])))
        require(entry.get("records") == records, f"{prefix}.records: mismatch")
        documents[relative] = document
    require(
        index.get("aggregate_sha256") == index_digest(index),
        "index.aggregate_sha256: mismatch",
    )

    control = next((d for d in documents.values() if d.get("schema") == CONTROL_SCHEMA), None)
    repositories_doc = next(
        (d for d in documents.values() if d.get("schema") == REPOSITORIES_SCHEMA), None
    )
    work_docs = [d for d in documents.values() if d.get("schema") == WORK_ITEMS_SCHEMA]
    require(control is not None, "bundle: control-plane document missing")
    require(repositories_doc is not None, "bundle: repositories document missing")
    require(bool(work_docs), "bundle: work-item documents missing")
    work_items: list[dict[str, Any]] = []
    priorities: set[str] = set()
    for document in work_docs:
        priority = string(document.get("priority"), "work-doc.priority")
        require(priority in PRIORITIES, "work-doc.priority: invalid")
        require(priority not in priorities, "work-doc.priority: duplicate")
        priorities.add(priority)
        entries = document.get("work_items")
        require(isinstance(entries, list), "work-doc.work_items: array required")
        for item in entries:
            require(item.get("priority") == priority, "work-doc: item priority mismatch")
        work_items.extend(entries)

    assembled = dict(control)
    assembled["repositories"] = repositories_doc.get("repositories")
    assembled["work_items"] = work_items
    assembled["_index"] = index
    return assembled


def dependency_cycle(items: dict[str, dict[str, Any]]) -> bool:
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(item_id: str) -> bool:
        if item_id in visiting:
            return True
        if item_id in visited:
            return False
        visiting.add(item_id)
        for dependency in items[item_id].get("blocked_by", []):
            if visit(dependency):
                return True
        visiting.remove(item_id)
        visited.add(item_id)
        return False

    return any(visit(item_id) for item_id in items)


def validate(data: dict[str, Any]) -> dict[str, Any]:
    require(data.get("schema") == CONTROL_SCHEMA, "schema: invalid control plane")
    string(data.get("version"), "version")
    string(data.get("generated_at"), "generated_at")

    authority = data.get("authority")
    require(isinstance(authority, dict), "authority: object required")
    require(
        authority.get("repository") == "rafaelmeloreisnovo/RafGitTools",
        "authority.repository: invalid",
    )
    base_commit = string(authority.get("base_commit"), "authority.base_commit")
    require(bool(SHA40_RE.fullmatch(base_commit)), "authority.base_commit: SHA-40 required")
    string(authority.get("branch"), "authority.branch")

    policy = data.get("policy")
    require(isinstance(policy, dict), "policy: object required")
    for key in FALSE_POLICIES:
        require(policy.get(key) is False, f"policy.{key}: must be false")

    dimensions = data.get("assurance_dimensions")
    require(isinstance(dimensions, list), "assurance_dimensions: array required")
    require(len(dimensions) == len(set(dimensions)), "assurance_dimensions: duplicates")
    require(set(dimensions) == REQUIRED_DIMENSIONS, "assurance_dimensions: mismatch")
    blocking_dimensions = strings(data.get("blocking_dimensions"), "blocking_dimensions", nonempty=True)
    require(set(blocking_dimensions).issubset(REQUIRED_DIMENSIONS), "blocking_dimensions: unknown")

    repositories = data.get("repositories")
    require(isinstance(repositories, list) and repositories, "repositories: non-empty array required")
    repository_names: set[str] = set()
    roles: set[str] = set()
    for index, repository in enumerate(repositories):
        path = f"repositories[{index}]"
        require(isinstance(repository, dict), f"{path}: object required")
        name = string(repository.get("repository"), f"{path}.repository")
        require(bool(REPOSITORY_RE.fullmatch(name)), f"{path}.repository: owner/name required")
        require(name not in repository_names, f"{path}.repository: duplicate")
        repository_names.add(name)
        string(repository.get("default_branch"), f"{path}.default_branch")
        observed_ref = string(repository.get("observed_ref"), f"{path}.observed_ref")
        require(
            observed_ref == TOKEN_VAZIO or bool(SHA40_RE.fullmatch(observed_ref)),
            f"{path}.observed_ref: SHA-40 or TOKEN_VAZIO required",
        )
        role = string(repository.get("role"), f"{path}.role")
        require(role not in roles, f"{path}.role: duplicate")
        roles.add(role)

    work_items_raw = data.get("work_items")
    require(isinstance(work_items_raw, list) and work_items_raw, "work_items: non-empty array required")
    work_items: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(work_items_raw):
        path = f"work_items[{index}]"
        require(isinstance(item, dict), f"{path}: object required")
        item_id = string(item.get("id"), f"{path}.id")
        require(bool(WORK_ID_RE.fullmatch(item_id)), f"{path}.id: invalid")
        require(item_id not in work_items, f"{path}.id: duplicate")
        work_items[item_id] = item
        repository = string(item.get("repository"), f"{path}.repository")
        require(repository in repository_names, f"{path}.repository: unknown repository")
        string(item.get("title"), f"{path}.title")
        require(item.get("priority") in PRIORITIES, f"{path}.priority: invalid")
        state = item.get("state")
        require(state in WORK_STATES, f"{path}.state: invalid")
        string(item.get("source_kind"), f"{path}.source_kind")
        source_ref = string(item.get("source_ref"), f"{path}.source_ref")
        require(item.get("claim_allowed") is False, f"{path}.claim_allowed: must be false")
        require(isinstance(item.get("promotion_ready"), bool), f"{path}.promotion_ready: boolean required")
        evidence = strings(item.get("evidence"), f"{path}.evidence")
        gaps = strings(item.get("gaps"), f"{path}.gaps")
        exit_criteria = strings(item.get("exit_criteria"), f"{path}.exit_criteria")
        string(item.get("rollback"), f"{path}.rollback")
        strings(item.get("blocked_by"), f"{path}.blocked_by")
        item_dimensions = item.get("dimensions")
        require(isinstance(item_dimensions, dict), f"{path}.dimensions: object required")
        require(set(item_dimensions) == REQUIRED_DIMENSIONS, f"{path}.dimensions: mismatch")
        for dimension, dimension_state in item_dimensions.items():
            require(dimension_state in DIMENSION_STATES, f"{path}.dimensions.{dimension}: invalid")
        if state in {"BLOCKED", TOKEN_VAZIO}:
            require(bool(gaps), f"{path}.gaps: required for {state}")
            require(bool(exit_criteria), f"{path}.exit_criteria: required for {state}")
        if state == "MERGED_LIMITED":
            require(item.get("source_kind") == "MERGED_PR", f"{path}: MERGED_PR required")
            require(bool(SHA40_RE.search(source_ref)), f"{path}.source_ref: merge SHA required")
        if any(value in {"PASS", "PASS_LIMITED"} for value in item_dimensions.values()):
            require(bool(evidence), f"{path}.evidence: PASS requires evidence")
        if item.get("promotion_ready"):
            blocked = {
                dimension: item_dimensions[dimension]
                for dimension in blocking_dimensions
                if item_dimensions[dimension] in {"BLOCKED", TOKEN_VAZIO}
            }
            require(not blocked, f"{path}: promotion cannot compensate blockers {blocked}")
            require(
                item_dimensions["ci"] not in {"BLOCKED", TOKEN_VAZIO, "ZERO_STEP_NO_LOGS"},
                f"{path}: promotion requires observable CI",
            )
            require(state not in {"BLOCKED", TOKEN_VAZIO}, f"{path}: blocked item cannot promote")

    for item_id, item in work_items.items():
        blockers = item.get("blocked_by", [])
        require(item_id not in blockers, f"{item_id}.blocked_by: self dependency")
        unknown = sorted(set(blockers) - set(work_items))
        require(not unknown, f"{item_id}.blocked_by: unknown {unknown}")
    require(not dependency_cycle(work_items), "blocked_by graph: cycle detected")

    derived = data.get("derived")
    require(isinstance(derived, dict), "derived: object required")
    state_counts = dict(sorted(Counter(item["state"] for item in work_items_raw).items()))
    priority_counts = dict(sorted(Counter(item["priority"] for item in work_items_raw).items()))
    expected = {
        "repository_count": len(repositories),
        "work_item_count": len(work_items_raw),
        "state_counts": state_counts,
        "priority_counts": priority_counts,
        "open_blocking_count": sum(
            item["state"] in {"BLOCKED", TOKEN_VAZIO} for item in work_items_raw
        ),
        "promotion_ready_count": sum(bool(item["promotion_ready"]) for item in work_items_raw),
        "claim_allowed": False,
    }
    for key, value in expected.items():
        require(derived.get(key) == value, f"derived.{key}: expected {value!r}")

    return {
        "schema": CONTROL_SCHEMA,
        "status": "PASS",
        **expected,
        "blocking_dimensions": blocking_dimensions,
        "index_digest": data["_index"]["aggregate_sha256"],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "index",
        nargs="?",
        type=Path,
        default=Path("configs/platform-assurance/index.json"),
    )
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args(argv)
    try:
        result = validate(load_bundle(args.index))
    except ValidationError as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False))
        return 1
    text = json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
