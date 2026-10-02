#!/usr/bin/env python3
from __future__ import annotations
import argparse
import json
from pathlib import Path

SCHEMA = "rafaelia.provider-capability-environments.v1"
EXPECTED_CAPABILITIES = {"environments", "environments_secret_reported", "actions", "agents", "codespaces", "dependabot"}

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
        scope = str(item.get("storage_scope", ""))
        state = str(item.get("state", ""))
        if not cap or cap in seen_caps:
            errors.append(f"invalid/duplicate capability: {cap!r}")
        seen_caps.add(cap)
        if not secret:
            errors.append(f"{cap}: missing secret name")
        if cap == "environments_secret_reported":
            if scope != "TOKEN_VAZIO_PROVIDER_SCOPE_PENDING":
                errors.append("environments_secret_reported: scope must remain pending provider readback")
            if env != "TOKEN_VAZIO_PROVIDER_BINDING_PENDING":
                errors.append("environments_secret_reported: binding must remain pending provider readback")
        elif scope == "ENVIRONMENT_SECRET":
            env_key = env.casefold()
            if not env or env_key in seen_envs:
                errors.append(f"invalid/duplicate environment: {env!r}")
            seen_envs.add(env_key)
        elif scope == "REPOSITORY_SECRET":
            if env != "TOKEN_VAZIO_NOT_ENVIRONMENT_BOUND":
                errors.append(f"{cap}: repository secret must not claim environment binding")
        else:
            errors.append(f"{cap}: unsupported storage_scope {scope!r}")
        if state.startswith("WIRED") and cap not in {"environments", "actions"}:
            errors.append(f"{cap}: unsupported wired capability in v1")
    if seen_caps != EXPECTED_CAPABILITIES:
        errors.append("capability set mismatch")

    env_cap = next((x for x in caps if x.get("capability") == "environments"), None)
    if not env_cap:
        errors.append("missing environments capability")
    else:
        if str(env_cap.get("environment", "")).casefold() != "pat_environments":
            errors.append("environments capability must bind Pat_environments environment")
        if str(env_cap.get("secret", "")).casefold() != "pat_env":
            errors.append("environments capability must bind PAT_ENV secret")
        if env_cap.get("storage_scope") != "ENVIRONMENT_SECRET":
            errors.append("environments capability must use ENVIRONMENT_SECRET")
        ops = set(env_cap.get("allowed_operations") or [])
        if "apply_main_protection" not in ops:
            errors.append("environments capability missing apply_main_protection")

    reported = next((x for x in caps if x.get("capability") == "environments_secret_reported"), None)
    if not reported:
        errors.append("missing environments_secret_reported capability")
    else:
        if reported.get("secret") != "PAT_ENVIRONMENTS":
            errors.append("environments_secret_reported must bind PAT_ENVIRONMENTS")
        if reported.get("state") != "HUMAN_REPORTED_PROVIDER_READBACK_PENDING":
            errors.append("PAT_ENVIRONMENTS reported state must remain provider-readback pending")
        if reported.get("allowed_operations") != []:
            errors.append("PAT_ENVIRONMENTS must remain unwired before provider readback")
        if reported.get("write_allowed") is not False:
            errors.append("PAT_ENVIRONMENTS must remain write-disabled before provider readback")
        if reported.get("permission_probe_required") is not True:
            errors.append("PAT_ENVIRONMENTS must require provider permission probe")

    actions_cap = next((x for x in caps if x.get("capability") == "actions"), None)
    if not actions_cap:
        errors.append("missing actions capability")
    else:
        if actions_cap.get("storage_scope") != "REPOSITORY_SECRET":
            errors.append("actions capability must use REPOSITORY_SECRET")
        if str(actions_cap.get("environment", "")) != "TOKEN_VAZIO_NOT_ENVIRONMENT_BOUND":
            errors.append("actions capability must not claim environment binding")
        if str(actions_cap.get("secret", "")).casefold() != "pat_actions":
            errors.append("actions capability must bind PAT_ACTIONS secret")
        if actions_cap.get("state") != "WIRED_MAIN_ONESHOT_READ_ONLY":
            errors.append("actions capability must be WIRED_MAIN_ONESHOT_READ_ONLY")
        if actions_cap.get("write_allowed") is not False:
            errors.append("actions cross-repo lane must be read-only")
        if actions_cap.get("exact_target_sha_required") is not True:
            errors.append("actions cross-repo lane must require exact target SHA")
        actions_ops = set(actions_cap.get("allowed_operations") or [])
        if "exact_commit_read_and_test" not in actions_ops:
            errors.append("actions capability missing exact_commit_read_and_test")

    invariants = set(data.get("invariants") or [])
    for required in (
        "NO_PAT_FALLBACK_BETWEEN_CAPABILITIES",
        "SECRET_VALUE_NEVER_PERSISTED_OR_PRINTED",
        "PAT_ENV_DISTINCT_FROM_PAT_ENVIRONMENTS",
        "HUMAN_REPORTED_PAT_REQUIRES_PROVIDER_READBACK_BEFORE_WIRING",
        "PROVIDER_WRITE_REQUIRES_WORKFLOW_DISPATCH",
        "PROVIDER_WRITE_REQUIRES_ENVIRONMENT_PROTECTION_PASS",
        "PROVIDER_WRITE_REQUIRES_EXACT_MAIN_SHA",
        "PROVIDER_WRITE_REQUIRES_READBACK",
        "TOKEN_VAZIO != PASS",
        "ACTIONS_CROSS_REPO_EXECUTION_IS_READ_ONLY",
        "ACTIONS_EXACT_TARGET_SHA_REQUIRED",
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
