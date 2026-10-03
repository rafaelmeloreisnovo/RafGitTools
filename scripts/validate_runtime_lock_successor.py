#!/usr/bin/env python3
"""Validate a runtime-lock successor without promoting the canonical lock.

This gate proves source-level reproducibility only:
- predecessor + exact provider-head observation deterministically reproduce the candidate;
- the refresh receipt binds exact input/output bytes and the observed delta;
- artifact hashes remain unchanged from the predecessor;
- even with synthetic in-memory integration/authorization fixtures, missing artifact
  hashes keep canonical promotion BLOCKED.

Synthetic fixtures are falsifiers only. They are never written as evidence and never
turn execution, integration, authorization or claim state into PASS.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]


def _load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


refresh = _load_module(
    "runtime_lock_refresh_candidate_for_successor_gate",
    ROOT / "scripts" / "runtime_lock_refresh_candidate.py",
)
promotion = _load_module(
    "runtime_lock_promotion_gate_for_successor_gate",
    ROOT / "scripts" / "runtime_lock_promotion_gate.py",
)


class SuccessorGateError(ValueError):
    pass


def _load(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise SuccessorGateError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise SuccessorGateError(f"invalid JSON in {path}: {exc}") from exc


def _expected_receipt(
    predecessor_bytes: bytes,
    observation_bytes: bytes,
    observation: dict[str, Any],
    candidate_bytes: bytes,
    changed: list[dict[str, str]],
) -> dict[str, Any]:
    return {
        "schema": refresh.RECEIPT_SCHEMA,
        "observed_at": observation["observed_at"],
        "base_lock_sha256": refresh.sha256_bytes(predecessor_bytes),
        "observation_sha256": refresh.sha256_bytes(observation_bytes),
        "candidate_sha256": refresh.sha256_bytes(candidate_bytes),
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


def _synthetic_integration(candidate_sha: str) -> dict[str, Any]:
    return {
        "schema": promotion.INTEGRATION_SCHEMA,
        "candidate_sha256": candidate_sha,
        "result": "PASS",
        "checks": [
            {
                "id": "synthetic-source-lock-falsifier-only",
                "status": "PASS",
                "evidence_sha256": "1" * 64,
            }
        ],
        "claim_allowed": False,
    }


def _synthetic_authorization(candidate_sha: str) -> dict[str, Any]:
    return {
        "schema": promotion.AUTH_SCHEMA,
        "candidate_sha256": candidate_sha,
        "authorized": True,
        "scope": "CANONICAL_RUNTIME_LOCK_PROMOTION_AFTER_ALL_TECHNICAL_GATES_PASS",
        "claim_allowed": False,
    }


def evaluate(
    predecessor_path: Path,
    observation_path: Path,
    candidate_path: Path,
    receipt_path: Path,
) -> dict[str, Any]:
    predecessor_bytes = predecessor_path.read_bytes()
    observation_bytes = observation_path.read_bytes()
    candidate_bytes = candidate_path.read_bytes()
    receipt_bytes = receipt_path.read_bytes()

    predecessor = _load(predecessor_path)
    observation = _load(observation_path)
    candidate = _load(candidate_path)
    receipt = _load(receipt_path)

    rebuilt_candidate, changed = refresh.build_candidate(predecessor, observation)
    rebuilt_candidate_bytes = refresh.canonical_bytes(rebuilt_candidate)
    if candidate_bytes != rebuilt_candidate_bytes:
        raise SuccessorGateError("candidate bytes do not reproduce from predecessor + observation")

    expected_receipt = _expected_receipt(
        predecessor_bytes,
        observation_bytes,
        observation,
        candidate_bytes,
        changed,
    )
    expected_receipt_bytes = refresh.canonical_bytes(expected_receipt)
    if receipt_bytes != expected_receipt_bytes:
        raise SuccessorGateError("refresh receipt bytes do not bind the exact successor transaction")

    predecessor_map = refresh.contract.validate(predecessor, require_artifact_hashes=False)
    candidate_map = refresh.contract.validate(candidate, require_artifact_hashes=False)
    hash_drift: list[str] = []
    for name in sorted(predecessor_map):
        if predecessor_map[name].get("expected_hashes") != candidate_map[name].get("expected_hashes"):
            hash_drift.append(name)
    if hash_drift:
        raise SuccessorGateError("artifact hash fields changed without artifact evidence: " + ", ".join(hash_drift))

    candidate_sha = refresh.sha256_bytes(candidate_bytes)
    assessment = promotion.assess(
        candidate_bytes,
        candidate,
        receipt,
        _synthetic_integration(candidate_sha),
        _synthetic_authorization(candidate_sha),
    )
    artifact_blockers = [
        item for item in assessment["f_gap"]
        if item.startswith("TOKEN_VAZIO_ARTIFACT_HASH:")
    ]
    non_artifact_blockers = [
        item for item in assessment["f_gap"]
        if not item.startswith("TOKEN_VAZIO_ARTIFACT_HASH:")
    ]

    if assessment["promotable"] is not False or assessment["evaluation"] != "BLOCKED":
        raise SuccessorGateError("candidate unexpectedly became promotable")
    if len(artifact_blockers) != 12:
        raise SuccessorGateError(f"expected 12 artifact-hash blockers, got {len(artifact_blockers)}")
    if non_artifact_blockers:
        raise SuccessorGateError(
            "synthetic falsifier isolation produced unexpected blockers: " + ", ".join(non_artifact_blockers)
        )

    return {
        "schema": "rafaelia.runtime-lock-successor-validation.v1",
        "state": "PASS",
        "predecessor_sha256": refresh.sha256_bytes(predecessor_bytes),
        "observation_sha256": refresh.sha256_bytes(observation_bytes),
        "candidate_sha256": candidate_sha,
        "receipt_sha256": refresh.sha256_bytes(receipt_bytes),
        "changed_count": len(changed),
        "changed_repositories": changed,
        "artifact_hashes_preserved": True,
        "artifact_hash_blocker_count": len(artifact_blockers),
        "promotion_state": "BLOCKED",
        "promotion_falsifier_mode": "SYNTHETIC_IN_MEMORY_ONLY_NOT_EVIDENCE",
        "integration_execution_evidence": "NOT_RUN",
        "promotion_authorization_evidence": "NOT_RUN",
        "canonical_lock_mutated_by_gate": False,
        "claim_allowed": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("predecessor", type=Path)
    parser.add_argument("observation", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("receipt", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    try:
        report = evaluate(args.predecessor, args.observation, args.candidate, args.receipt)
        rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(rendered, encoding="utf-8")
        print(rendered, end="")
        return 0
    except (SuccessorGateError, refresh.RefreshError, refresh.contract.ContractError, OSError) as exc:
        print(json.dumps({
            "schema": "rafaelia.runtime-lock-successor-validation.v1",
            "state": "FAIL",
            "reason": str(exc),
            "claim_allowed": False,
        }, ensure_ascii=False, sort_keys=True))
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
