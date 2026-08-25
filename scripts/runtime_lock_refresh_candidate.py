#!/usr/bin/env python3
"""Build a deterministic runtime-lock refresh candidate without mutating the canonical lock.

The input observation is evidence, not authorization. This tool validates a complete
cross-repository snapshot, emits a candidate lock plus a sidecar receipt, and never
promotes the candidate to runtime-lock.json by itself.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import importlib.util
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = ROOT / "scripts" / "runtime_lock_contract.py"
SPEC = importlib.util.spec_from_file_location("runtime_lock_contract", CONTRACT_PATH)
assert SPEC and SPEC.loader
contract = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = contract
SPEC.loader.exec_module(contract)

OBS_SCHEMA = "rafaelia.runtime-lock-observation.v1"
RECEIPT_SCHEMA = "rafaelia.runtime-lock-refresh-receipt.v1"
SHA40_RE = re.compile(r"^[0-9a-f]{40}$")
UTC_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


class RefreshError(ValueError):
    pass


def canonical_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise RefreshError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise RefreshError(f"invalid JSON in {path}: {exc}") from exc


def validate_observation(data: Any) -> dict[str, dict[str, str]]:
    if not isinstance(data, dict):
        raise RefreshError("observation root must be an object")
    if data.get("schema") != OBS_SCHEMA:
        raise RefreshError(f"observation.schema must be {OBS_SCHEMA!r}")
    observed_at = data.get("observed_at")
    if not isinstance(observed_at, str) or not UTC_RE.fullmatch(observed_at):
        raise RefreshError("observed_at must be explicit UTC YYYY-MM-DDTHH:MM:SSZ")
    rows = data.get("repositories")
    if not isinstance(rows, list):
        raise RefreshError("observation.repositories must be an array")

    mapped: dict[str, dict[str, str]] = {}
    for index, row in enumerate(rows):
        if not isinstance(row, dict):
            raise RefreshError(f"repositories[{index}] must be an object")
        name = row.get("name")
        branch = row.get("branch")
        commit = row.get("commit")
        if not isinstance(name, str) or not name:
            raise RefreshError(f"repositories[{index}].name must be non-empty")
        if name in mapped:
            raise RefreshError(f"duplicate observation repository: {name}")
        expected_branch = contract.REQUIRED_REPOSITORIES.get(name)
        if expected_branch is None:
            raise RefreshError(f"unexpected observation repository: {name}")
        if branch != expected_branch:
            raise RefreshError(f"{name}.branch must be {expected_branch!r}")
        if not isinstance(commit, str) or not SHA40_RE.fullmatch(commit):
            raise RefreshError(f"{name}.commit must be a concrete lowercase 40-hex SHA")
        mapped[name] = {"name": name, "branch": branch, "commit": commit}

    missing = sorted(set(contract.REQUIRED_REPOSITORIES) - set(mapped))
    if missing:
        raise RefreshError("missing observation repositories: " + ", ".join(missing))
    return mapped


def build_candidate(base: dict[str, Any], observation: dict[str, Any]) -> tuple[dict[str, Any], list[dict[str, str]]]:
    base_map = contract.validate(base, require_artifact_hashes=False)
    observed = validate_observation(observation)
    candidate = copy.deepcopy(base)
    candidate["generated_at"] = observation["observed_at"]
    candidate["release_state"] = "RAFCODEPHI_STACK_REFRESH_CANDIDATE_RUNTIME_PROOF_PENDING"

    changed: list[dict[str, str]] = []
    candidate_map = {item["name"]: item for item in candidate["repositories"]}
    for name in contract.REQUIRED_REPOSITORIES:
        old = base_map[name]["commit"]
        new = observed[name]["commit"]
        candidate_map[name]["commit"] = new
        if old != new:
            changed.append({"name": name, "from": old, "to": new})

    contract.validate(candidate, require_artifact_hashes=False)
    return candidate, changed


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("base_lock", type=Path)
    parser.add_argument("observation", type=Path)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    args = parser.parse_args()

    try:
        base_resolved = args.base_lock.resolve()
        if args.candidate.resolve() == base_resolved:
            raise RefreshError("refusing to overwrite canonical base lock")
        if args.receipt.resolve() == base_resolved:
            raise RefreshError("refusing to overwrite canonical base lock with receipt")
        if args.candidate.resolve() == args.receipt.resolve():
            raise RefreshError("candidate and receipt outputs must be distinct")

        base_input_bytes = args.base_lock.read_bytes()
        observation_input_bytes = args.observation.read_bytes()
        base = load_json(args.base_lock)
        observation = load_json(args.observation)
        candidate, changed = build_candidate(base, observation)

        candidate_bytes = canonical_bytes(candidate)
        receipt = {
            "schema": RECEIPT_SCHEMA,
            "observed_at": observation["observed_at"],
            "base_lock_sha256": sha256_bytes(base_input_bytes),
            "observation_sha256": sha256_bytes(observation_input_bytes),
            "candidate_sha256": sha256_bytes(candidate_bytes),
            "input_hash_semantics": "EXACT_FILE_BYTES",
            "changed_repositories": changed,
            "changed_count": len(changed),
            "artifact_hashes_preserved": True,
            "canonical_lock_mutated": False,
            "promoted": False,
            "claim_allowed": False,
            "f_gap": [
                "TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_EXECUTION",
                "TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION",
            ],
            "f_next": "Execute locked cross-repository integration gates against this exact candidate before any canonical promotion.",
        }

        args.candidate.parent.mkdir(parents=True, exist_ok=True)
        args.receipt.parent.mkdir(parents=True, exist_ok=True)
        args.candidate.write_bytes(candidate_bytes)
        args.receipt.write_bytes(canonical_bytes(receipt))
        print(json.dumps({
            "result": "PASS",
            "changed_count": len(changed),
            "candidate_sha256": receipt["candidate_sha256"],
            "promoted": False,
            "claim_allowed": False,
        }, sort_keys=True))
        return 0
    except (RefreshError, contract.ContractError, OSError) as exc:
        print(f"[FALHA] runtime-lock refresh candidate: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
