#!/usr/bin/env python3
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
STATE = ROOT / "configs" / "ecosystem-operational-state.v2.json"

REQUIRED_TOP = {
    "schema", "observed_on", "control_repository", "invariants", "materialized",
    "errors", "urgencies", "risks", "gates", "token_vazio", "provider_live_snapshot",
    "provisions", "rollback", "claim_allowed"
}
REQUIRED_INVARIANTS = {
    "SOURCE!=ARTIFACT",
    "ARTIFACT!=EXECUTION",
    "EXECUTION!=EVIDENCE",
    "EVIDENCE!=CLAIM",
    "TOKEN_VAZIO!=0",
    "IMPLEMENTED_UNTESTED!=PASS",
    "RELATIONSHIP!=AUTHORITY_MERGE",
}

def fail(msg):
    print("FAIL:", msg)
    return 1

def main():
    try:
        data = json.loads(STATE.read_text(encoding="utf-8"))
    except Exception as exc:
        return fail(f"cannot parse {STATE}: {exc}")

    missing = REQUIRED_TOP - set(data)
    if missing:
        return fail("missing top-level fields: " + ",".join(sorted(missing)))

    if data.get("claim_allowed") is not False:
        return fail("claim_allowed must remain false")

    if not REQUIRED_INVARIANTS.issubset(set(data.get("invariants", []))):
        return fail("required epistemic invariants missing")

    errors = data.get("errors", [])
    if not errors:
        return fail("error/gap inventory may not be empty")

    for item in errors:
        for key in ("id", "repo", "state", "evidence"):
            if not item.get(key):
                return fail(f"error entry missing {key}: {item}")

    urgencies = data.get("urgencies", [])
    if not any(x.get("priority") == "P0" for x in urgencies):
        return fail("at least one current P0 must be explicitly represented")

    roll = data.get("rollback", {})
    if not roll.get("global"):
        return fail("global rollback anchor missing")

    provider = data.get("provider_live_snapshot", {})
    if provider.get("apply_state") != "BLOCKED_EXTERNAL_ADMIN_AUTHORITY":
        return fail("provider apply boundary unexpectedly changed")
    if provider.get("plan_stale") is not False:
        return fail("provider plan staleness is not explicitly false")

    print("PASS: ecosystem operational state v2 is structurally fail-closed")
    print(f"errors={len(errors)} urgencies={len(urgencies)} gates={len(data.get('gates', []))}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
