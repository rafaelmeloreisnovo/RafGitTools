#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = ROOT / "contracts" / "PAT_CAPABILITY_MATRIX_V1.json"
EXPECTED = {"PAT_ACTIONS","PAT_AGENTS","PAT_CODESPACES","PAT_DEPENDABOT","PAT_ENV"}

def validate(data: dict) -> list[str]:
    errors: list[str] = []
    if data.get("schema") != "rafgittools.pat-capability-matrix.v1":
        errors.append("schema mismatch")
    if data.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("executor authority must remain RafGitTools")
    if data.get("federated_authority") != "rafaelmeloreisnovo/Mapa":
        errors.append("federated authority must remain Mapa")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed must remain false")
    if data.get("canonical_environment") != "PAT_ENVIRONMENTS":
        errors.append("canonical environment display must be PAT_ENVIRONMENTS")

    ids = data.get("secret_ids")
    if not isinstance(ids, list) or set(ids) != EXPECTED or len(ids) != len(EXPECTED):
        errors.append("secret id set mismatch")
    else:
        for secret in ids:
            if secret != secret.upper():
                errors.append(f"secret id not uppercase: {secret}")

    caps = data.get("capabilities")
    if not isinstance(caps, list):
        return errors + ["capabilities must be list"]
    by_secret = {x.get("secret_id"): x for x in caps if isinstance(x, dict)}
    if set(by_secret) != EXPECTED:
        errors.append("capability secret set mismatch")

    actions = by_secret.get("PAT_ACTIONS", {})
    if actions.get("current_state") != "WIRED_MAIN_ONESHOT_READ_ONLY":
        errors.append("PAT_ACTIONS state mismatch")
    if actions.get("mutation_allowed") is not False:
        errors.append("PAT_ACTIONS must remain read-only")
    if "exact_commit_read_and_test" not in set(actions.get("wired_operations") or []):
        errors.append("PAT_ACTIONS missing exact_commit_read_and_test")

    env = by_secret.get("PAT_ENV", {})
    if env.get("storage_scope") != "ENVIRONMENT_SECRET":
        errors.append("PAT_ENV must be environment scoped")
    if env.get("environment") != "PAT_ENVIRONMENTS":
        errors.append("PAT_ENV environment must use canonical uppercase display")
    if env.get("mutation_allowed") != "MANUAL_GATED_ONLY":
        errors.append("PAT_ENV mutation must remain manual gated")

    for secret in ("PAT_AGENTS","PAT_CODESPACES","PAT_DEPENDABOT"):
        cap = by_secret.get(secret, {})
        if cap.get("current_state") != "REGISTERED_NOT_WIRED":
            errors.append(f"{secret} must remain REGISTERED_NOT_WIRED until evidence")
        if cap.get("permission_state") != "TOKEN_VAZIO_NOT_PROBED":
            errors.append(f"{secret} permission state must remain TOKEN_VAZIO_NOT_PROBED")

    inv = set(data.get("invariants") or [])
    for required in (
        "SECRET_IDENTIFIER == UPPERCASE",
        "SECRET_VALUE_NEVER_PERSISTED_OR_PRINTED",
        "CAPABILITY != PERMISSION",
        "NO_PAT_FALLBACK",
        "RafGitTools == PAT_BACKED_EXECUTOR",
        "Mapa == FEDERATED_ROUTING_AUTHORITY",
        "TOKEN_VAZIO != PASS",
    ):
        if required not in inv:
            errors.append(f"missing invariant: {required}")
    return errors

def main(argv: list[str]) -> int:
    path = Path(argv[1]) if len(argv) > 1 else DEFAULT
    errors = validate(json.loads(path.read_text(encoding="utf-8")))
    if errors:
        for error in errors:
            print(f"FAIL: {error}")
        return 1
    print("PASS PAT_CAPABILITY_MATRIX_V1")
    return 0

if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
