#!/usr/bin/env python3
from __future__ import annotations
import argparse
import json
from pathlib import Path

SCHEMA = "rafaelia.provider-capability-environments.v1"

def validate(data: dict) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != SCHEMA:
        errors.append("schema mismatch")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must be false")
    caps = data.get("capabilities")
    if not isinstance(caps, list) or not caps:
        return errors + ["capabilities must be a non-empty list"]
    seen_caps: set[str] = set()
    seen_envs: set[str] = set()
    for item in caps:
        if not isinstance(item, dict):
            errors.append("capability entry must be object")
            continue
        cap = str(item.get("capability", ""))
        env = str(item.get("environment", ""))
        secret = str(item.get("secret", ""))
        state = str(item.get("state", ""))
        if not cap or cap in seen_caps:
            errors.append(f"invalid/duplicate capability: {cap!r}")
        seen_caps.add(cap)
        env_key = env.casefold()
        if not env or env_key in seen_envs:
            errors.append(f"invalid/duplicate environment: {env!r}")
        seen_envs.add(env_key)
        if not secret:
            errors.append(f"{cap}: missing secret name")
        if "WIRED" in state and cap != "environments":
            errors.append(f"{cap}: only environments capability may be wired in v1")
    env_cap = next((x for x in caps if x.get("capability") == "environments"), None)
    if not env_cap:
        errors.append("missing environments capability")
    else:
        if str(env_cap.get("environment", "")).casefold() != "pat_environments":
            errors.append("environments capability must bind PAT_environments environment")
        if str(env_cap.get("secret", "")).casefold() != "pat_environments":
            errors.append("environments capability must bind PAT_environments secret")
        ops = set(env_cap.get("allowed_operations") or [])
        if "apply_main_protection" not in ops:
            errors.append("environments capability missing apply_main_protection")
    invariants = set(data.get("invariants") or [])
    for required in (
        "NO_PAT_FALLBACK_BETWEEN_CAPABILITIES",
        "SECRET_VALUE_NEVER_PERSISTED_OR_PRINTED",
        "PROVIDER_WRITE_REQUIRES_WORKFLOW_DISPATCH",
        "PROVIDER_WRITE_REQUIRES_ENVIRONMENT_PROTECTION_PASS",
        "PROVIDER_WRITE_REQUIRES_EXACT_MAIN_SHA",
        "PROVIDER_WRITE_REQUIRES_READBACK",
        "TOKEN_VAZIO != PASS",
    ):
        if required not in invariants:
            errors.append(f"missing invariant: {required}")
    return errors

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("config", type=Path)
    args = ap.parse_args()
    data = json.loads(args.config.read_text(encoding="utf-8"))
    errors = validate(data)
    if errors:
        for err in errors:
            print(f"FAIL {err}")
        return 1
    print("PASS provider-capability-environments-v1 claim_allowed=false")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
