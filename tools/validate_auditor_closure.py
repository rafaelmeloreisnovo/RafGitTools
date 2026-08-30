#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY = ROOT / "contracts/auditor-closure-policy.v1.json"
RECORD = ROOT / "audits/AUDITOR_CLOSURE_PR390_20260830.v1.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("FAIL: " + message)


def main() -> int:
    policy = json.loads(POLICY.read_text(encoding="utf-8"))
    record = json.loads(RECORD.read_text(encoding="utf-8"))

    require(policy["claim_allowed"] is False, "policy must remain claim_allowed=false")
    require(record["claim_allowed"] is False, "record must remain claim_allowed=false")
    require(record["merge_authorized"] is False, "merge must remain HOLD while a gate is failed")

    invariants = set(policy["invariants"])
    for item in (
        "TOKEN_VAZIO != PASS",
        "DOCUMENTED != IMPLEMENTED",
        "IMPLEMENTED != EXECUTED",
        "EXECUTED != REPRODUCED",
        "LOCAL_PASS != LIVE_CROSS_REPO_PASS",
        "FAILED_GATE != MERGE_AUTHORIZED",
    ):
        require(item in invariants, f"missing invariant: {item}")

    observed = {item["name"]: item["conclusion"] for item in record["observed_evidence"]}
    require(observed.get("Human Impact Cross-Repo Gate V1") == "success", "human-impact structural gate not evidenced")
    require(observed.get("PR Validation") == "success", "PR Validation not evidenced")
    require(observed.get("Source Gap Audit") == "success", "Source Gap Audit not evidenced")
    require(observed.get("Documentation") == "failure", "documentation failure must be preserved")

    allowed = set(policy["allowed_decisions"])
    for collection in ("closures", "open_items"):
        for item in record[collection]:
            require(item["decision"] in allowed, f"unsupported decision: {item['decision']}")

    for item in record["closures"]:
        require(item["decision"] == "CLOSED_LIMITED", f"closure {item['id']} must be limited")
        require(item.get("scope"), f"closure {item['id']} missing scope")
        require(item.get("evidence_refs"), f"closure {item['id']} missing evidence")
        require(item.get("claim_boundary"), f"closure {item['id']} missing claim boundary")

    open_by_id = {item["id"]: item for item in record["open_items"]}
    require(open_by_id["OP-001"]["decision"] == "TOKEN_VAZIO", "live ethics receipt fabricated")
    require(open_by_id["OP-002"]["decision"] == "TOKEN_VAZIO", "external compliance fabricated")
    require(open_by_id["OP-003"]["decision"] == "BLOCKED", "documentation failure erased")
    require(open_by_id["OP-004"]["decision"] == "HOLD", "merge prematurely authorized")
    require(open_by_id["OP-005"]["decision"] == "TOKEN_VAZIO", "branch protection inferred")

    print("PASS: auditor closure record preserves evidence-scoped closure and fail-closed gaps")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
