#!/usr/bin/env python3
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_PATH = ROOT / "configs" / "gap-closure-execution.v1.json"
LEDGER_PATH = ROOT / "data" / "evidence" / "github" / "cross-repo-gap-closure-20260819.v1.json"
EXPECTED_IDS = [f"GC{i:02d}" for i in range(1, 13)]
PRIORITY = {"P0": 0, "P1": 1, "P2": 2}


def main() -> int:
    contract = json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))
    ledger = json.loads(LEDGER_PATH.read_text(encoding="utf-8"))
    errors: list[str] = []

    if contract.get("schema") != "rafaelia.gap-closure-execution.v1":
        errors.append("wrong contract schema")
    if ledger.get("schema") != "rafaelia.gap-closure-ledger.v1":
        errors.append("wrong ledger schema")
    if contract.get("claim_allowed") is not False or ledger.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")

    ids = [x.get("id") for x in contract.get("anti_regression_invariants", []) if isinstance(x, dict)]
    if ids != EXPECTED_IDS:
        errors.append("invariants must remain GC01..GC12")

    allowed_states = set(contract.get("allowed_states", []))
    resolved_states = set(contract.get("resolution_states", []))
    required = contract.get("required_fields", [])
    seen: set[str] = set()
    last_priority = -1
    state_counts: Counter[str] = Counter()
    open_by_urgency: Counter[str] = Counter()

    for item in ledger.get("items", []):
        iid = item.get("id")
        if not isinstance(iid, str) or not iid or iid in seen:
            errors.append(f"invalid or duplicate id: {iid}")
            continue
        seen.add(iid)

        for key in required:
            if key not in item:
                errors.append(f"{iid}: missing {key}")

        state = item.get("state")
        urgency = item.get("urgency")
        risk = item.get("risk")
        refs = item.get("evidence_refs")

        if state not in allowed_states:
            errors.append(f"{iid}: invalid state")
        if urgency not in PRIORITY:
            errors.append(f"{iid}: invalid urgency")
        else:
            current = PRIORITY[urgency]
            if current < last_priority:
                errors.append(f"{iid}: urgency ordering regression")
            last_priority = max(last_priority, current)
        if risk not in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}:
            errors.append(f"{iid}: invalid risk")
        if item.get("claim_allowed") is not False:
            errors.append(f"{iid}: claim_allowed must remain false")
        if not isinstance(refs, list):
            errors.append(f"{iid}: evidence_refs must be a list")
            refs = []
        if state in resolved_states and not refs:
            errors.append(f"{iid}: resolved/evidenced state requires evidence_refs")
        if state == "READY_TO_TEST" and not refs:
            errors.append(f"{iid}: READY_TO_TEST requires implementation evidence")
        if item.get("mutating") is True and risk in {"HIGH", "CRITICAL"} and not item.get("rollback_ref"):
            errors.append(f"{iid}: high-risk mutation requires rollback_ref")

        for key in ("repo", "surface", "authority", "owner", "provenance", "next_action", "exit_criterion", "falsifier"):
            if not isinstance(item.get(key), str) or not item.get(key).strip():
                errors.append(f"{iid}: missing {key}")

        state_counts[str(state)] += 1
        if state in set(contract.get("unresolved_states", [])):
            open_by_urgency[str(urgency)] += 1

    report = {
        "schema": "rafaelia.gap-closure-execution-report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "invariant_count": len(EXPECTED_IDS),
        "item_count": len(seen),
        "state_counts": dict(sorted(state_counts.items())),
        "unresolved_by_urgency": dict(sorted(open_by_urgency.items())),
        "errors": errors,
    }
    print(json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
