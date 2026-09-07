#!/usr/bin/env python3
"""Evaluate a runtime-lock refresh candidate for canonical promotion.

This gate is intentionally fail-closed. A source observation, a refresh receipt, or
human authorization alone never proves runtime integration. Promotion eligibility
requires all evidence to bind to the exact candidate bytes and all artifact hashes
to be concrete. This script only emits an assessment; it never mutates
runtime-lock.json.
"""
from __future__ import annotations

import argparse
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

REFRESH_SCHEMA = "rafaelia.runtime-lock-refresh-receipt.v1"
INTEGRATION_SCHEMA = "rafaelia.runtime-lock-integration-gate-receipt.v1"
AUTH_SCHEMA = "rafaelia.runtime-lock-promotion-authorization.v1"
ASSESSMENT_SCHEMA = "rafaelia.runtime-lock-promotion-assessment.v1"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
TOKEN_VAZIO = "TOKEN_VAZIO"


class PromotionGateError(ValueError):
    pass


def canonical_bytes(obj: Any) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise PromotionGateError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise PromotionGateError(f"invalid JSON in {path}: {exc}") from exc


def require_sha256(value: Any, field: str) -> str:
    if not isinstance(value, str) or not SHA256_RE.fullmatch(value):
        raise PromotionGateError(f"{field} must be a concrete lowercase SHA-256")
    return value


def validate_refresh_receipt(data: Any, candidate_sha256: str) -> None:
    if not isinstance(data, dict) or data.get("schema") != REFRESH_SCHEMA:
        raise PromotionGateError(f"refresh receipt schema must be {REFRESH_SCHEMA}")
    if data.get("candidate_sha256") != candidate_sha256:
        raise PromotionGateError("refresh receipt candidate_sha256 does not match candidate bytes")
    if data.get("canonical_lock_mutated") is not False:
        raise PromotionGateError("refresh receipt must prove canonical_lock_mutated=false")
    if data.get("promoted") is not False:
        raise PromotionGateError("refresh receipt must describe an unpromoted candidate")


def validate_authorization(data: Any, candidate_sha256: str) -> list[str]:
    blockers: list[str] = []
    if not isinstance(data, dict) or data.get("schema") != AUTH_SCHEMA:
        return ["TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION_SCHEMA"]
    if data.get("candidate_sha256") != candidate_sha256:
        return ["TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION_CANDIDATE_MISMATCH"]
    if data.get("authorized") is not True:
        blockers.append("TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION")
    if data.get("scope") != "CANONICAL_RUNTIME_LOCK_PROMOTION_AFTER_ALL_TECHNICAL_GATES_PASS":
        blockers.append("TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION_SCOPE")
    return blockers


def validate_integration_receipt(data: Any, candidate_sha256: str) -> list[str]:
    blockers: list[str] = []
    if not isinstance(data, dict) or data.get("schema") != INTEGRATION_SCHEMA:
        return ["TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_RECEIPT"]
    if data.get("candidate_sha256") != candidate_sha256:
        return ["TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_CANDIDATE_MISMATCH"]
    if data.get("result") != "PASS":
        blockers.append("TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_EXECUTION")
    checks = data.get("checks")
    if not isinstance(checks, list) or not checks:
        blockers.append("TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_CHECKS")
        return blockers
    for index, check in enumerate(checks):
        if not isinstance(check, dict) or check.get("status") != "PASS":
            blockers.append(f"TOKEN_VAZIO_INTEGRATION_CHECK_{index}_PASS")
            continue
        evidence_sha = check.get("evidence_sha256")
        if not isinstance(evidence_sha, str) or not SHA256_RE.fullmatch(evidence_sha):
            blockers.append(f"TOKEN_VAZIO_INTEGRATION_CHECK_{index}_EVIDENCE_SHA256")
    return blockers


def artifact_hash_blockers(candidate: dict[str, Any]) -> list[str]:
    blockers: list[str] = []
    mapped = contract.validate(candidate, require_artifact_hashes=False)
    for name in sorted(mapped):
        hashes = mapped[name].get("expected_hashes", {})
        for field in ("manifest_sha256", "bundle_sha256"):
            value = hashes.get(field)
            if not isinstance(value, str) or not SHA256_RE.fullmatch(value):
                blockers.append(f"TOKEN_VAZIO_ARTIFACT_HASH:{name}:{field}")
    return blockers


def assess(candidate_bytes: bytes, candidate: dict[str, Any], refresh: Any, integration: Any, authorization: Any) -> dict[str, Any]:
    candidate_sha = sha256_bytes(candidate_bytes)
    validate_refresh_receipt(refresh, candidate_sha)
    blockers: list[str] = []
    blockers.extend(artifact_hash_blockers(candidate))
    blockers.extend(validate_integration_receipt(integration, candidate_sha))
    blockers.extend(validate_authorization(authorization, candidate_sha))
    blockers = sorted(set(blockers))
    promotable = not blockers
    return {
        "schema": ASSESSMENT_SCHEMA,
        "candidate_sha256": candidate_sha,
        "refresh_receipt_sha256": sha256_bytes(canonical_bytes(refresh)),
        "integration_receipt_sha256": sha256_bytes(canonical_bytes(integration)),
        "authorization_sha256": sha256_bytes(canonical_bytes(authorization)),
        "evaluation": "PASS" if promotable else "BLOCKED",
        "promotable": promotable,
        "canonical_lock_mutated": False,
        "claim_allowed": False,
        "f_gap": blockers,
        "f_next": (
            "Candidate is eligible for a separate canonical promotion transaction with race protection."
            if promotable
            else "Close every listed TOKEN_VAZIO against this exact candidate; do not mutate runtime-lock.json."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("refresh_receipt", type=Path)
    parser.add_argument("integration_receipt", type=Path)
    parser.add_argument("authorization", type=Path)
    parser.add_argument("--assessment", type=Path, required=True)
    args = parser.parse_args()

    try:
        candidate_bytes = args.candidate.read_bytes()
        candidate = load_json(args.candidate)
        refresh = load_json(args.refresh_receipt)
        integration = load_json(args.integration_receipt)
        authorization = load_json(args.authorization)
        result = assess(candidate_bytes, candidate, refresh, integration, authorization)
        args.assessment.parent.mkdir(parents=True, exist_ok=True)
        args.assessment.write_bytes(canonical_bytes(result))
        print(json.dumps({
            "result": result["evaluation"],
            "candidate_sha256": result["candidate_sha256"],
            "promotable": result["promotable"],
            "blocker_count": len(result["f_gap"]),
            "canonical_lock_mutated": False,
            "claim_allowed": False,
        }, sort_keys=True))
        return 0 if result["promotable"] else 3
    except (PromotionGateError, contract.ContractError, OSError, TypeError) as exc:
        print(f"[FALHA] runtime-lock promotion gate: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
