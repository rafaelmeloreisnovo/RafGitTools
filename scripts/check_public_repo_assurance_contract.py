#!/usr/bin/env python3
import json
import sys
from pathlib import Path

CONTRACT = Path("configs/public-repo-assurance-executor.v1.json")
REQUIRED_ITEM_KEYS = {
    "schema", "repository", "matrix_binding", "receipt_binding", "dimensions",
    "risk", "gaps", "rollback", "requested_transition", "claim_allowed"
}
VALID_TRANSITIONS = {"OBSERVED", "HOLD", "READY_FOR_GATE", "PASS", "FAIL", "BLOCKED"}


def fail(msg: str) -> None:
    print(f"FAIL: {msg}", file=sys.stderr)
    raise SystemExit(1)


def has_token_vazio(value) -> bool:
    return "TOKEN_VAZIO" in json.dumps(value, sort_keys=True)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: check_public_repo_assurance_contract.py <work-item.json>")

    contract = json.loads(CONTRACT.read_text(encoding="utf-8"))
    item = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))

    missing = sorted(REQUIRED_ITEM_KEYS - set(item))
    if missing:
        fail(f"work item missing {missing}")
    if item["schema"] != "rafaelia.public-repo-assurance-work-item.v1":
        fail("unexpected work item schema")
    if item["claim_allowed"] is not False:
        fail("executor never accepts claim_allowed=true as an input assertion")

    transition = item["requested_transition"]
    if transition not in VALID_TRANSITIONS:
        fail(f"invalid transition {transition}")

    dims = item["dimensions"]
    required = contract["required_dimensions"]
    missing_dims = [d for d in required if d not in dims]
    if missing_dims:
        fail(f"missing assurance dimensions: {missing_dims}")

    critical = contract["critical_dimensions"]
    critical_unknown = [d for d in critical if has_token_vazio(dims[d]) or dims[d] in (None, "", "UNKNOWN")]
    risk = item["risk"]
    gaps = item["gaps"]

    if item["repository"].get("classification", "").startswith("UPSTREAM"):
        if has_token_vazio(dims["provenance"]) or not dims["provenance"].get("authorial_delta_boundary_bound", False):
            critical_unknown.append("provenance.authorial_delta_boundary")

    if risk["severity"] in {"HIGH", "CRITICAL"}:
        if not item["rollback"].get("available") or not item["rollback"].get("procedure"):
            fail("high/critical risk mutation requires concrete rollback")

    if critical_unknown and transition not in {"HOLD", "BLOCKED", "FAIL"}:
        fail(f"critical unknowns require HOLD/BLOCKED/FAIL: {sorted(set(critical_unknown))}")

    if gaps and transition == "PASS":
        fail("PASS forbidden while explicit gaps remain")

    if transition == "PASS":
        gate = dims["gate"]
        if gate.get("state") != "PASS" or not gate.get("evidence") or not gate.get("falsifier"):
            fail("PASS requires named terminal gate evidence and falsifier")

    if item["matrix_binding"].get("schema") != contract["accepted_matrix_schema"]:
        fail("matrix schema mismatch")
    if item["receipt_binding"].get("schema") != contract["accepted_receipt_schema"]:
        fail("receipt schema mismatch")

    print(
        "PASS",
        f"repo={item['repository']['name']}",
        f"transition={transition}",
        f"gaps={len(gaps)}",
        f"critical_unknowns={len(set(critical_unknown))}",
    )


if __name__ == "__main__":
    main()
