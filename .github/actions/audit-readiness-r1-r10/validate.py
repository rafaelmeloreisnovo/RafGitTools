#!/usr/bin/env python3
import json
import sys
from pathlib import Path

ALLOWED = {"PASS", "PARTIAL", "BLOCKED", "TOKEN_VAZIO", "NOT_APPLICABLE"}
EXPECTED = [f"R{i}" for i in range(1, 11)]


def fail(message):
    raise SystemExit(f"AUDIT_READINESS_INVALID: {message}")


def validate(profile):
    if profile.get("schema") != "rafaelia.audit_readiness_profile.v1":
        fail("unexpected schema")
    if profile.get("non_certification") is not True:
        fail("non_certification must be true")
    if profile.get("claim_allowed") is not False:
        fail("claim_allowed must be false")

    subject = profile.get("subject") or {}
    for key in ("repository", "revision", "observed_at_utc"):
        value = subject.get(key)
        if not isinstance(value, str) or not value.strip() or "TOKEN_VAZIO" in value:
            fail(f"subject.{key} must be concrete")

    levels = profile.get("recognition")
    if not isinstance(levels, list) or len(levels) != 10:
        fail("recognition must contain exactly R1..R10")
    ids = [item.get("id") for item in levels]
    if ids != EXPECTED:
        fail(f"recognition ids must be ordered {EXPECTED}")

    states = {}
    for item in levels:
        rid = item["id"]
        state = item.get("state")
        if state not in ALLOWED:
            fail(f"{rid} invalid state {state!r}")
        states[rid] = state

        evidence = item.get("evidence", [])
        gaps = item.get("gaps", [])
        if not isinstance(evidence, list) or not all(isinstance(x, str) and x.strip() for x in evidence):
            fail(f"{rid} evidence must be a list of non-empty strings")
        if not isinstance(gaps, list) or not all(isinstance(x, str) and x.strip() for x in gaps):
            fail(f"{rid} gaps must be a list of non-empty strings")
        if state == "PASS" and not evidence:
            fail(f"{rid} PASS requires evidence")
        if state in {"BLOCKED", "TOKEN_VAZIO"} and not gaps:
            fail(f"{rid} {state} requires a typed gap")

    blockers = profile.get("blockers", [])
    if not isinstance(blockers, list) or not all(isinstance(x, str) and x.strip() for x in blockers):
        fail("blockers must be a list of non-empty strings")

    if states["R10"] == "PASS":
        if any(states[f"R{i}"] != "PASS" for i in range(1, 10)):
            fail("R10 PASS requires R1..R9 PASS")
        if blockers:
            fail("R10 PASS requires blockers=[]")

    for key in ("certified", "certification_claim", "standards_certified", "legal_compliance_claim"):
        if profile.get(key) is True:
            fail(f"{key}=true is forbidden")

    return states


def main():
    if len(sys.argv) != 2:
        fail("usage: validate.py <profile.json>")
    path = Path(sys.argv[1])
    if not path.is_file():
        fail(f"profile not found: {path}")
    profile = json.loads(path.read_text(encoding="utf-8"))
    states = validate(profile)
    passed = [rid for rid, state in states.items() if state == "PASS"]
    print("AUDIT_READINESS_VALID")
    print("PASS=" + ",".join(passed))
    print("R10=" + states["R10"])
    print("claim_allowed=false")
    print("certification=false")


if __name__ == "__main__":
    main()
