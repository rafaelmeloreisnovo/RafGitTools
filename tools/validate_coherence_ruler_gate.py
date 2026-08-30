#!/usr/bin/env python3
"""Validate a Mapa coherence-ruler receipt before any limited execution route."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any, Dict


def read_json(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def validate(config: Dict[str, Any], packet: Dict[str, Any]) -> Dict[str, Any]:
    receipt = packet.get("receipt", {})
    failures = []
    if packet.get("source_repo") != config["accepted_source_repo"]:
        failures.append("SOURCE_REPO_MISMATCH")
    if receipt.get("schema") != config["accepted_source_schema"]:
        failures.append("SOURCE_SCHEMA_MISMATCH")
    if receipt.get("status") != config["accepted_status"]:
        failures.append("SOURCE_STATUS_NOT_AUTHORIZED")
    if receipt.get("selected_region") not in config["accepted_regions"]:
        failures.append("REGION_NOT_ALLOWED")
    if config.get("require_random_total_permutation_sweep_false") and receipt.get("random_total_permutation_sweep_required") is not False:
        failures.append("PERMUTATION_SWEEP_NOT_RESTRICTED")
    if config.get("require_output_only_rollback") and receipt.get("rollback") != "OUTPUT_ONLY_NO_AUTONOMOUS_MUTATION":
        failures.append("ROLLBACK_CONTRACT_MISSING")
    if config.get("require_claim_allowed_false") and receipt.get("claim_allowed") is not False:
        failures.append("CLAIM_BOUNDARY_BROKEN")
    if not receipt.get("selected_ruler") or receipt.get("selected_ruler") == "TOKEN_VAZIO":
        failures.append("RULER_NOT_BOUND")
    if not isinstance(receipt.get("watchdog_budget"), dict):
        failures.append("WATCHDOG_BUDGET_MISSING")

    return {
        "schema": "rafgittools.coherence_ruler_gate_receipt.v1",
        "status": "EXECUTION_ROUTE_AUTHORIZED_LIMITED" if not failures else config.get("failure_mode", "HOLD"),
        "claim_allowed": False,
        "selected_region": receipt.get("selected_region", "TOKEN_VAZIO") if not failures else "TOKEN_VAZIO",
        "source_ruler": receipt.get("selected_ruler", "TOKEN_VAZIO"),
        "failures": failures,
        "live_cross_repo_receipt": config.get("live_cross_repo_receipt", "TOKEN_VAZIO"),
        "execution_scope": "ROUTE_ONLY_NO_PRODUCER_TRUTH_PROMOTION",
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--config", default="configs/coherence-ruler-gate.v1.json")
    ap.add_argument("--input", default="fixtures/coherence_ruler_gate/positive.v1.json")
    args = ap.parse_args()
    result = validate(read_json(Path(args.config)), read_json(Path(args.input)))
    print(json.dumps(result, indent=2, ensure_ascii=False, sort_keys=True))
    return 0 if result["status"] == "EXECUTION_ROUTE_AUTHORIZED_LIMITED" else 2


if __name__ == "__main__":
    raise SystemExit(main())
