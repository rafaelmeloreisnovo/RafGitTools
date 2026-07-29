#!/usr/bin/env python3
"""Validate the EXPLORAR visual-emergence contract using only stdlib."""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys
from typing import Any

EXPECTED_DIRECTIONS = {f"D{i}" for i in range(1, 8)}
EXPECTED_EMERGENCE = [
    "OBSERVE", "ISOLATE", "REPLICATE", "MEASURE",
    "MODEL", "FALSIFY", "PROMOTE_OR_PRESERVE",
]
EXPECTED_EMERGENCY = [
    "DETECT", "FREEZE_CLAIMS", "PRESERVE_EVIDENCE",
    "CONTAIN", "ROLLBACK", "RECOVER", "POSTMORTEM",
]
NON_EVIDENCE_STATUSES = {
    "SYMBOLIC_PARABLE", "ARCHITECTURE_METAPHOR",
    "OPERATIONAL_METAPHOR_STRONG", "CONCEPTUAL_MODEL",
    "CONCEPTUAL_COMPARISON_REQUIRES_CORRECTION",
    "HYPOTHESIS_IMPLEMENTATION_PENDING",
    "VISUAL_SEED_REQUIRES_RECOMPUTATION",
    "MIXED_FORMAL_AND_SYMBOLIC", "RESEARCH_ROUTING_MAP",
    "CONTRADICTION_REQUIRES_CORRECTION",
}


class ContractError(ValueError):
    """Raised when a non-negotiable invariant is violated."""


def _require(condition: bool, message: str) -> None:
    if not condition:
        raise ContractError(message)


def validate(data: dict[str, Any]) -> dict[str, Any]:
    _require(data.get("schema_version") == "explorar-visual-emergence-v1",
             "unsupported schema_version")
    _require(data.get("claim_allowed") is False,
             "top-level claim_allowed must remain false")
    _require(data.get("mode") == "NON_DESTRUCTIVE",
             "mode must be NON_DESTRUCTIVE")

    invariants = data.get("invariants", {})
    _require(invariants.get("TOKEN_VAZIO_is_PASS") is False,
             "TOKEN_VAZIO must never equal PASS")
    _require(invariants.get("symbol_or_parable_is_scientific_evidence") is False,
             "symbol/parabola cannot be scientific evidence")
    _require(invariants.get("negative_result_is_preserved") is True,
             "negative results must be preserved")
    _require(invariants.get("source_epistemic_operational_claim_gate_separated") is True,
             "the four status axes must stay separated")

    sources = data.get("sources")
    _require(isinstance(sources, list) and len(sources) == 10,
             "exactly ten visual sources are required")
    ids: set[str] = set()
    for source in sources:
        source_id = source.get("id")
        _require(isinstance(source_id, str) and source_id not in ids,
                 f"duplicate or invalid source id: {source_id!r}")
        ids.add(source_id)
        digest = source.get("sha256", "")
        _require(isinstance(digest, str) and len(digest) == 64,
                 f"{source_id}: invalid SHA-256 length")
        try:
            bytes.fromhex(digest)
        except ValueError as exc:
            raise ContractError(
                f"{source_id}: SHA-256 is not hexadecimal") from exc
        status = source.get("status")
        _require(status in NON_EVIDENCE_STATUSES,
                 f"{source_id}: unknown or overpromoted status {status!r}")
        _require(source.get("claim_allowed") is False,
                 f"{source_id}: visual source cannot allow a scientific claim")
        if "REQUIRES_CORRECTION" in status or status in {
            "VISUAL_SEED_REQUIRES_RECOMPUTATION",
            "MIXED_FORMAL_AND_SYMBOLIC",
        }:
            _require(source.get("requires_correction") is True,
                     f"{source_id}: correction flag is required")

    directions = data.get("directions")
    _require(isinstance(directions, list) and len(directions) == 7,
             "exactly seven directions are required")
    direction_ids = {item.get("id") for item in directions}
    _require(direction_ids == EXPECTED_DIRECTIONS,
             f"direction ids must be {sorted(EXPECTED_DIRECTIONS)}")
    for item in directions:
        authority = item.get("authority")
        _require(isinstance(authority, list) and authority,
                 f"{item.get('id')}: at least one authority is required")

    _require(data.get("emergence_protocol") == EXPECTED_EMERGENCE,
             "emergence protocol order changed")
    _require(data.get("emergency_protocol") == EXPECTED_EMERGENCY,
             "emergency protocol order changed")

    canonical = json.dumps(
        data, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return {
        "status": "PASS",
        "sources": len(sources),
        "directions": len(directions),
        "canonical_sha256": hashlib.sha256(canonical).hexdigest(),
        "claim_allowed": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("contract", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        data = json.loads(args.contract.read_text(encoding="utf-8"))
        report = validate(data)
    except (OSError, json.JSONDecodeError, ContractError) as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
