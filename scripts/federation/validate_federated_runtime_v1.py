#!/usr/bin/env python3
"""Validate the RAFAELIA four-body federated runtime contract using stdlib only."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

EXPECTED_SCHEMA = "raf.federated-runtime-contract.v1"
EXPECTED_ROUTE = {
    "control": "rafaelmeloreisnovo/RafGitTools",
    "executor": "rafaelmeloreisnovo/termux-app-rafacodephi",
    "evidence": "rafaelmeloreisnovo/RafPolimata",
    "vm": "rafaelmeloreisnovo/Vectras-VM-Android",
}
EXPECTED_PERMISSION = "com.termux.rafacodephi.permission.RUN_COMMAND"


class ContractError(ValueError):
    pass


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def validate_contract(data: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []

    if data.get("schema_version") != EXPECTED_SCHEMA:
        errors.append("schema_version")

    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed_must_be_false")

    participants = data.get("participants")
    if not isinstance(participants, dict):
        errors.append("participants")
        participants = {}

    for key, repository in EXPECTED_ROUTE.items():
        node = participants.get(key)
        if not isinstance(node, dict) or node.get("repository") != repository:
            errors.append(f"participant:{key}")

    ipc = data.get("android_ipc")
    if not isinstance(ipc, dict):
        errors.append("android_ipc")
        ipc = {}

    if ipc.get("protocol_version") != 2:
        errors.append("protocol_version")
    if ipc.get("run_command_permission") != EXPECTED_PERMISSION:
        errors.append("run_command_permission")
    if ipc.get("private_paths_exposed") is not False:
        errors.append("private_paths_exposed")
    if ipc.get("execution_mode") != "run_command_service":
        errors.append("execution_mode")

    binaries = ipc.get("allowed_qemu_binaries")
    if not isinstance(binaries, list) or not binaries or len(binaries) != len(set(binaries)):
        errors.append("allowed_qemu_binaries")
    elif any(not isinstance(item, str) or not item.startswith("qemu-system-") for item in binaries):
        errors.append("allowed_qemu_binary_name")

    gates = data.get("promotion_gates")
    if not isinstance(gates, dict):
        errors.append("promotion_gates")
    else:
        for required_true in (
            "dispatch_is_not_execution",
            "path_discovery_is_not_execution",
            "device_runtime_requires_receipt",
            "vm_start_requires_vm_required_true",
        ):
            if gates.get(required_true) is not True:
                errors.append(f"gate:{required_true}")
        if gates.get("missing_evidence_state") != "TOKEN_VAZIO":
            errors.append("gate:missing_evidence_state")

    if errors:
        raise ContractError(",".join(errors))

    digest = hashlib.sha256(canonical_bytes(data)).hexdigest()
    return {
        "schema": "raf.federated-runtime-contract-validation.v1",
        "status": "PASS",
        "claim_allowed": False,
        "canonical_sha256": digest,
        "participants": len(participants),
        "qemu_binaries": len(binaries),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("contract", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()

    try:
        data = json.loads(args.contract.read_text(encoding="utf-8"))
        report = validate_contract(data)
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        report = {
            "schema": "raf.federated-runtime-contract-validation.v1",
            "status": "FAIL",
            "claim_allowed": False,
            "reason": str(exc),
        }

    rendered = json.dumps(report, sort_keys=True, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
