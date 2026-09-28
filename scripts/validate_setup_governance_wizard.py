#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "contracts" / "SETUP_GOVERNANCE_WIZARD_V1.json"
CATALOG = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "rafgittools" / "setupwizard" / "SetupWizardCatalog.kt"
LEDGER = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "rafgittools" / "setupwizard" / "SetupWizardCustodyLedger.kt"

EXPECTED_STEPS = {
    "WELCOME", "PRIVACY", "IDENTITY_AND_ACCESS", "EXTERNAL_MODULES",
    "READ_WRITE_BOUNDARY", "AUDIT_AND_CUSTODY", "AUTOMATION",
    "REVIEW_AND_ROLLBACK", "FINAL_REVIEW",
}

def validate(data: dict) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != "rafgittools.setup-governance-wizard.v1":
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")

    principles = set(data.get("principles") or [])
    for required in {
        "NO_FINE_PRINT_FOR_MATERIAL_RISK",
        "ZERO_TRUST_BY_DEFAULT",
        "PRIVACY_BY_DEFAULT",
        "NO_SECRET_VALUES_IN_LEDGER",
        "ROLLBACK_ONLY_WHEN_DECLARED_AND_VERIFIABLE",
        "REVIEWABLE_ANY_TIME",
    }:
        if required not in principles:
            errors.append(f"missing principle: {required}")

    if data.get("decisions") != ["AGREE", "DISAGREE", "LATER"]:
        errors.append("decision set mismatch")

    storage = data.get("local_storage") or {}
    if storage.get("secret_values_recorded") is not False:
        errors.append("ledger must never record secret values")
    if storage.get("ledger_mode") != "APPEND_ONLY_HASH_CHAIN":
        errors.append("ledger must be append-only hash chain")

    steps = data.get("steps")
    if not isinstance(steps, list):
        return errors + ["steps must be list"]
    ids = {step.get("id") for step in steps}
    if ids != EXPECTED_STEPS or len(steps) != len(EXPECTED_STEPS):
        errors.append("step set mismatch")
    for step in steps:
        for field in ("id", "risk", "rollback", "data_touched", "zero_trust"):
            if not step.get(field):
                errors.append(f"{step.get('id')}: missing {field}")

    catalog = CATALOG.read_text(encoding="utf-8")
    for step_id in EXPECTED_STEPS:
        if f'id = "{step_id}"' not in catalog:
            errors.append(f"catalog missing step: {step_id}")

    ledger = LEDGER.read_text(encoding="utf-8")
    for required in ('"secret_values_recorded", false', '"claim_allowed", false', '"previous_hash"', '"hash"'):
        if required not in ledger:
            errors.append(f"ledger invariant missing: {required}")
    return errors

def main() -> int:
    data = json.loads(CONTRACT.read_text(encoding="utf-8"))
    errors = validate(data)
    if errors:
        for error in errors:
            print(f"FAIL: {error}")
        return 1
    print("PASS SETUP_GOVERNANCE_WIZARD_V1")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
