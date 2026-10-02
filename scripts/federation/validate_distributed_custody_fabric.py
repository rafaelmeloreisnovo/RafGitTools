#!/usr/bin/env python3
"""Fail-closed structural validator for RAFAELIA distributed custody fabric v1.

This validates only the machine-readable control contract. It does not prove
cryptographic implementation, provider enforcement, Drive writes, CI execution,
or device runtime.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


EXPECTED_SCHEMA = "rafaelia.distributed-custody-fabric/v1"
REQUIRED_PLANES = {
    "source_control",
    "documentary_custody",
    "execution_evidence",
}
FORBIDDEN_ENVELOPE_FIELDS = {
    "secret",
    "secret_value",
    "private_key",
    "private_key_material",
    "plaintext_key",
    "recovery_phrase",
    "access_token",
    "refresh_token",
    "password",
}


class ContractError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def load_contract(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)
    require(isinstance(data, dict), "root must be a JSON object")
    return data


def validate(contract: dict[str, Any]) -> None:
    require(contract.get("schema") == EXPECTED_SCHEMA, "unexpected schema")
    require(contract.get("claim_allowed") is False, "claim_allowed must remain false")
    require(
        contract.get("status") in {"IMPLEMENTED_UNTESTED", "TESTED_STRUCTURAL"},
        "invalid contract status",
    )

    invariants = set(contract.get("invariants") or [])
    for invariant in {
        "EDGE_BUFFER!=AUTHORITATIVE_STORAGE",
        "NO_SINGLE_EXTERNAL_PLANE_RECONSTRUCTS_RESTRICTED_STATE",
        "SECRET_MATERIAL_NEVER_ENTERS_GIT_DRIVE_OR_RECEIPT_LOGS",
    }:
        require(invariant in invariants, f"missing invariant: {invariant}")

    edge = contract.get("edge") or {}
    require(edge.get("authoritative_persistence") is False, "edge must not be authoritative storage")

    planes = contract.get("persistent_planes") or {}
    require(set(planes) == REQUIRED_PLANES, "persistent planes must match the three-plane contract exactly")
    for name, plane in planes.items():
        require(bool(plane.get("authority")), f"{name}: authority missing")
        require(bool(plane.get("minimum_record")), f"{name}: minimum_record missing")

    restricted = contract.get("restricted_fragment") or {}
    for key in (
        "materialize_in_repository",
        "materialize_in_drive_documentation",
        "materialize_in_logs",
    ):
        require(restricted.get(key) is False, f"restricted_fragment.{key} must be false")

    projection = set(restricted.get("external_projection_only") or [])
    require("key_slot_ref" in projection, "opaque key_slot_ref projection required")
    require("commitment_digest" in projection, "commitment_digest projection required")
    require(
        not projection.intersection(FORBIDDEN_ENVELOPE_FIELDS),
        "restricted external projection contains secret-bearing fields",
    )

    for key in ("encryption_algorithm", "signature_algorithm", "key_rotation_policy"):
        value = str(restricted.get(key, ""))
        require(value.startswith("TOKEN_VAZIO"), f"{key} must remain explicit TOKEN_VAZIO until implemented/reviewed")

    envelope = contract.get("cross_plane_envelope") or {}
    fields = set(envelope.get("fields") or [])
    require(envelope.get("secret_fields_allowed") is False, "secret_fields_allowed must be false")
    require("key_slot_ref" in fields, "cross-plane envelope must bind key_slot_ref")
    require("commitment_digest" in fields, "cross-plane envelope must bind commitment_digest")
    require("rollback_ref" in fields and "successor_ref" in fields, "rollback/successor refs required")
    require(
        not fields.intersection(FORBIDDEN_ENVELOPE_FIELDS),
        "cross-plane envelope contains forbidden secret-bearing fields",
    )

    reconstruction = contract.get("reconstruction") or {}
    require(reconstruction.get("single_plane_reconstruction") is False, "single-plane reconstruction must be false")
    required = reconstruction.get("protected_state_requires") or []
    require(len(required) >= 4, "protected reconstruction must bind all external planes plus restricted fragment")
    require(reconstruction.get("mismatch_state") == "CONTRADICTION", "digest mismatch must be CONTRADICTION")

    privacy = contract.get("privacy_by_design") or {}
    for key in (
        "data_minimization",
        "least_authority",
        "purpose_binding_required",
        "secret_redaction_required",
        "public_receipt_must_be_sanitized",
    ):
        require(privacy.get(key) is True, f"privacy_by_design.{key} must be true")

    compliance = contract.get("compliance_boundary") or {}
    require(
        "not a legal compliance certification" in str(compliance.get("claim", "")),
        "compliance boundary must refuse certification inference",
    )


def canonical_digest(contract: dict[str, Any]) -> str:
    payload = json.dumps(contract, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "path",
        nargs="?",
        default="configs/distributed-custody-fabric.v1.json",
        help="path to the custody fabric contract",
    )
    args = parser.parse_args()

    path = Path(args.path)
    try:
        contract = load_contract(path)
        validate(contract)
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        print(f"FAIL distributed-custody-fabric/v1: {exc}")
        return 1

    print("PASS_STRUCTURAL distributed-custody-fabric/v1")
    print(f"canonical_sha256={canonical_digest(contract)}")
    print("claim_allowed=false")
    print("crypto_execution=TOKEN_VAZIO")
    print("drive_execution=TOKEN_VAZIO")
    print("cross_plane_canary=TOKEN_VAZIO")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
