#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

SCHEMA = "rafaelia.provider-capability-environments.v1"
UNKNOWN_SCOPE = "TOKEN_VAZIO_PROVIDER_STORAGE_SCOPE"
UNKNOWN_ENV = "TOKEN_VAZIO_PROVIDER_ENVIRONMENT_NAME_UNVERIFIED"


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
    seen_secrets: set[str] = set()

    for item in caps:
        if not isinstance(item, dict):
            errors.append("capability entry must be object")
            continue

        cap = str(item.get("capability", ""))
        env = str(item.get("environment", ""))
        secret = str(item.get("secret", ""))
        scope = str(item.get("storage_scope", ""))
        state = str(item.get("state", ""))

        if not cap or cap in seen_caps:
            errors.append(f"invalid/duplicate capability: {cap!r}")
        seen_caps.add(cap)

        if not secret or secret in seen_secrets:
            errors.append(f"invalid/duplicate secret reference for {cap}: {secret!r}")
        seen_secrets.add(secret)

        if scope == "ENVIRONMENT_SECRET":
            env_key = env.casefold()
            if not env or env_key in seen_envs:
                errors.append(f"invalid/duplicate environment: {env!r}")
            seen_envs.add(env_key)
        elif scope == "REPOSITORY_SECRET":
            if env != "TOKEN_VAZIO_NOT_ENVIRONMENT_BOUND":
                errors.append(f"{cap}: repository secret must not claim environment binding")
        elif scope == UNKNOWN_SCOPE:
            if state != "REGISTERED_NOT_WIRED":
                errors.append(f"{cap}: unknown storage scope may only be REGISTERED_NOT_WIRED")
            if env != UNKNOWN_ENV:
                errors.append(f"{cap}: unknown storage scope must keep environment unverified")
        else:
            errors.append(f"{cap}: unsupported storage_scope {scope!r}")

        if state == "WIRED_MANUAL_ONLY" and cap != "environments":
            errors.append(f"{cap}: only environments capability may be wired in v1")

    required_human_reported = {
        "legacy_generic": "pat_env",
        "actions": "PAT_actions",
        "agents": "PAT_agents",
        "codespaces": "PAT_codespaces",
        "environments": "PAT_environments",
    }
    routed = {x.get("capability"): x for x in caps if isinstance(x, dict)}
    for cap, secret in required_human_reported.items():
        item = routed.get(cap)
        if not item:
            errors.append(f"missing human-reported capability: {cap}")
        elif item.get("secret") != secret:
            errors.append(f"{cap}: secret reference must preserve human-reported name {secret!r}")

    if "dependabot" in routed:
        errors.append("dependabot capability is not part of the current human-reported PAT inventory")

    env_cap = routed.get("environments")
    if env_cap:
        if str(env_cap.get("environment", "")).casefold() != "pat_environments":
            errors.append("environments capability must bind Pat_environments environment")
        if env_cap.get("secret") != "PAT_environments":
            errors.append("environments capability must bind PAT_environments secret")
        if env_cap.get("storage_scope") != "ENVIRONMENT_SECRET":
            errors.append("environments capability must use ENVIRONMENT_SECRET")
        ops = set(env_cap.get("allowed_operations") or [])
        if "apply_main_protection" not in ops:
            errors.append("environments capability missing apply_main_protection")

    invariants = set(data.get("invariants") or [])
    for required in (
        "HUMAN_REPORTED_SECRET_NAME != PROVIDER_READBACK",
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
