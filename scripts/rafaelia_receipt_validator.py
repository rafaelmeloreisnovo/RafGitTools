#!/usr/bin/env python3
"""RAFAELIA cross-repository receipt validator.

Fail-closed, stdlib-only validator for FNEXT receipts.
A VALID result proves only that the receipt satisfies this structural contract.
It does not prove runtime, device, scientific, mathematical, or semantic claims.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any, Dict, List

SCHEMA_VERSION = "rafaelia.cross-repo-receipt.v1"

REQUIRED_FIELDS = (
    "receipt_id",
    "schema_version",
    "repo",
    "ref",
    "commit_sha",
    "event_type",
    "source_pointer",
    "artifact_hashes",
    "predecessor_receipt",
    "evidence_level_before",
    "evidence_level_after",
    "claim_allowed",
    "token_vazio",
    "next_verifiable_step",
    "timestamp",
)

OUTPUT_STATES = (
    "VALID",
    "VALID_WITH_TOKEN_VAZIO",
    "INVALID_SCHEMA",
    "BROKEN_LINEAGE",
    "UNSUPPORTED_PROMOTION",
)

EVIDENCE_LEVELS = {
    "TOKEN_VAZIO": -1,
    "UNOBSERVED": 0,
    "SOURCE_OBSERVED": 1,
    "WIRED": 2,
    "BUILD_PROVEN": 3,
    "RUNTIME_PROVEN": 4,
    "DEVICE_PROVEN": 5,
    "PHYSICAL_MEASURED": 6,
    "INDEPENDENTLY_REPRODUCED": 7,
}

EVENT_MAX_LEVEL = {
    "CONTRACT_COMMIT": "SOURCE_OBSERVED",
    "DOCUMENT_COMMIT": "SOURCE_OBSERVED",
    "SOURCE_COMMIT": "SOURCE_OBSERVED",
    "BUILD": "BUILD_PROVEN",
    "RUNTIME": "RUNTIME_PROVEN",
    "DEVICE_RUNTIME": "DEVICE_PROVEN",
    "PHYSICAL_MEASUREMENT": "PHYSICAL_MEASURED",
    "INDEPENDENT_REPRODUCTION": "INDEPENDENTLY_REPRODUCED",
}

SHA40 = re.compile(r"^[0-9a-f]{40}$")
REPO = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")


def _nonempty_string(value: Any) -> bool:
    return isinstance(value, str) and bool(value.strip())


def _load_json(path: Path) -> List[Dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        receipts = data
    elif isinstance(data, dict) and isinstance(data.get("receipts"), list):
        receipts = data["receipts"]
    elif isinstance(data, dict):
        receipts = [data]
    else:
        raise ValueError("top-level JSON must be a receipt object, list, or {'receipts': [...]}")
    if not all(isinstance(item, dict) for item in receipts):
        raise ValueError("every receipt must be a JSON object")
    return receipts


def _schema_errors(receipt: Dict[str, Any]) -> List[str]:
    errors: List[str] = []
    missing = [field for field in REQUIRED_FIELDS if field not in receipt]
    if missing:
        errors.append("missing fields: " + ", ".join(missing))
        return errors

    if receipt["schema_version"] != SCHEMA_VERSION:
        errors.append(f"schema_version must equal {SCHEMA_VERSION}")
    if not _nonempty_string(receipt["receipt_id"]):
        errors.append("receipt_id must be a non-empty string")
    if not isinstance(receipt["repo"], str) or not REPO.fullmatch(receipt["repo"]):
        errors.append("repo must be owner/name")
    if not _nonempty_string(receipt["ref"]):
        errors.append("ref must be a non-empty string")
    if not isinstance(receipt["commit_sha"], str) or not SHA40.fullmatch(receipt["commit_sha"]):
        errors.append("commit_sha must be 40 lowercase hex characters")
    if receipt["event_type"] not in EVENT_MAX_LEVEL:
        errors.append("unsupported event_type")
    if not _nonempty_string(receipt["source_pointer"]):
        errors.append("source_pointer must be a non-empty string")
    if not isinstance(receipt["artifact_hashes"], list):
        errors.append("artifact_hashes must be a list")
    elif not all(_nonempty_string(v) for v in receipt["artifact_hashes"]):
        errors.append("artifact_hashes entries must be non-empty strings")
    if not _nonempty_string(receipt["predecessor_receipt"]):
        errors.append("predecessor_receipt must be a receipt id or TOKEN_VAZIO")
    if receipt["evidence_level_before"] not in EVIDENCE_LEVELS:
        errors.append("invalid evidence_level_before")
    if receipt["evidence_level_after"] not in EVIDENCE_LEVELS:
        errors.append("invalid evidence_level_after")
    if not isinstance(receipt["claim_allowed"], bool):
        errors.append("claim_allowed must be boolean")
    if not isinstance(receipt["token_vazio"], list):
        errors.append("token_vazio must be a list")
    elif not all(_nonempty_string(v) for v in receipt["token_vazio"]):
        errors.append("token_vazio entries must be non-empty strings")
    if not _nonempty_string(receipt["next_verifiable_step"]):
        errors.append("next_verifiable_step must be a non-empty string")
    if not _nonempty_string(receipt["timestamp"]):
        errors.append("timestamp must be a non-empty string")
    return errors


def _promotion_errors(receipt: Dict[str, Any]) -> List[str]:
    errors: List[str] = []
    before = EVIDENCE_LEVELS[receipt["evidence_level_before"]]
    after = EVIDENCE_LEVELS[receipt["evidence_level_after"]]

    if after > before:
        if receipt["source_pointer"] == "TOKEN_VAZIO":
            errors.append("promotion requires a supporting source_pointer")
        if not receipt["artifact_hashes"]:
            errors.append("promotion requires at least one artifact hash")

    max_level = EVENT_MAX_LEVEL[receipt["event_type"]]
    if after > EVIDENCE_LEVELS[max_level]:
        errors.append(f"{receipt['event_type']} cannot promote beyond {max_level}")

    if receipt["claim_allowed"] and receipt["token_vazio"]:
        errors.append("claim_allowed=true is forbidden while token_vazio is non-empty")

    if receipt["claim_allowed"] and after < EVIDENCE_LEVELS["INDEPENDENTLY_REPRODUCED"]:
        errors.append(
            "claim_allowed=true requires an independently reproduced evidence level "
            "under this generic cross-repository contract"
        )

    return errors


def validate_bundle(receipts: List[Dict[str, Any]]) -> Dict[str, Any]:
    results: List[Dict[str, Any]] = []
    seen: Dict[str, Dict[str, Any]] = {}

    for index, receipt in enumerate(receipts):
        schema_errors = _schema_errors(receipt)
        if schema_errors:
            results.append({
                "receipt_id": receipt.get("receipt_id", f"INDEX_{index}"),
                "state": "INVALID_SCHEMA",
                "errors": schema_errors,
            })
            continue

        rid = receipt["receipt_id"]
        lineage_errors: List[str] = []
        if rid in seen:
            lineage_errors.append("duplicate receipt_id")

        predecessor = receipt["predecessor_receipt"]
        if predecessor != "TOKEN_VAZIO" and predecessor not in seen:
            lineage_errors.append(
                f"predecessor_receipt {predecessor!r} is not an earlier receipt"
            )

        if lineage_errors:
            state = "BROKEN_LINEAGE"
            errors = lineage_errors
        else:
            promotion_errors = _promotion_errors(receipt)
            if promotion_errors:
                state = "UNSUPPORTED_PROMOTION"
                errors = promotion_errors
            else:
                state = "VALID_WITH_TOKEN_VAZIO" if receipt["token_vazio"] else "VALID"
                errors = []

        results.append({
            "receipt_id": rid,
            "state": state,
            "errors": errors,
        })
        seen.setdefault(rid, receipt)

    accepted = {"VALID", "VALID_WITH_TOKEN_VAZIO"}
    ok = all(item["state"] in accepted for item in results)
    return {
        "schema_version": SCHEMA_VERSION,
        "validator": "rafaelia_receipt_validator",
        "receipt_count": len(receipts),
        "accepted_count": sum(item["state"] in accepted for item in results),
        "rejected_count": sum(item["state"] not in accepted for item in results),
        "claim_promotion_performed": False,
        "ok": ok,
        "results": results,
    }


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("receipt_json", type=Path)
    parser.add_argument("--compact", action="store_true")
    args = parser.parse_args(argv)

    try:
        receipts = _load_json(args.receipt_json)
        report = validate_bundle(receipts)
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        report = {
            "schema_version": SCHEMA_VERSION,
            "validator": "rafaelia_receipt_validator",
            "receipt_count": 0,
            "accepted_count": 0,
            "rejected_count": 1,
            "claim_promotion_performed": False,
            "ok": False,
            "results": [{
                "receipt_id": "INPUT",
                "state": "INVALID_SCHEMA",
                "errors": [str(exc)],
            }],
        }

    if args.compact:
        print(json.dumps(report, sort_keys=True, separators=(",", ":")))
    else:
        print(json.dumps(report, sort_keys=True, indent=2))

    return 0 if report["ok"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
