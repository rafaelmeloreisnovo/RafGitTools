#!/usr/bin/env python3
"""Pure metadata check. No network or secret value access."""
from __future__ import annotations
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXPECTED = (
    "PAT_ACTIONS", "PAT_AGENTS", "PAT_CODESPACES",
    "PAT_DEPENDABOT", "PAT_ENV", "PAT_ENVIRONMENTS",
)

def validate(contract: dict, matrix: dict, workflow: str) -> list[str]:
    failures = []
    if contract.get("schema") != "rafaelia.pat-interop-abi.v2":
        failures.append("SCHEMA_MISMATCH")
    if contract.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        failures.append("EXECUTOR_DRIFT")
    if contract.get("route_authority") != "rafaelmeloreisnovo/Mapa":
        failures.append("MAPA_DRIFT")
    if contract.get("claim_allowed") is not False or contract.get("secrets_exposed") is not False:
        failures.append("CLAIM_OR_SECRET_EXPOSURE")
    ids = contract.get("secret_ids")
    mid = matrix.get("secret_ids")
    if not isinstance(ids, list) or tuple(ids) != EXPECTED or len(set(ids)) != 6:
        failures.append("SECRET_IDENTITY_DRIFT")
    if not isinstance(mid, list) or set(mid) != set(EXPECTED):
        failures.append("LEGACY_MATRIX_IDENTITY_DRIFT")
    if not contract.get("no_alias_or_token_fallback"):
        failures.append("PAT_FALLBACK_PROHIBITED")
    if contract.get("provider_metadata_readback") != "TOKEN_VAZIO_NOT_READ":
        failures.append("PROVIDER_READBACK_MUST_BE_EVIDENCED")
    if contract.get("current_environment_display") != "Pat_environments":
        failures.append("CURRENT_ENV_CASE_DRIFT")
    if contract.get("canonical_environment_target") != "PAT_ENVIRONMENTS":
        failures.append("TARGET_ENV_DRIFT")
    if contract.get("environment_rename_authorized") is not False:
        failures.append("ENV_RENAME_NOT_AUTHORIZED")
    caps = contract.get("capabilities")
    if not isinstance(caps, list) or len(caps) != 6:
        failures.append("CAPABILITY_COUNT")
        caps = []
    by_name = {c.get("secret"): c for c in caps if isinstance(c, dict)}
    if set(by_name) != set(EXPECTED):
        failures.append("CAPABILITY_MAPPING_DRIFT")
    for secret in EXPECTED:
        item = by_name.get(secret, {})
        if item.get("provider_scope_state") != "TOKEN_VAZIO_PROVIDER_METADATA":
            failures.append("UNPROVEN_PAT_SCOPE:" + secret)
        if secret != "PAT_ENV" and item.get("mutation") is not False:
            failures.append("ILLEGAL_PAT_MUTATION:" + secret)
    if by_name.get("PAT_ENV", {}).get("route") != "manual_provider_gate_only":
        failures.append("PAT_ENV_MANUAL_ROUTE_REQUIRED")
    if by_name.get("PAT_ENVIRONMENTS", {}).get("route") != "UNWIRED":
        failures.append("UNVERIFIED_PAT_ENVIRONMENTS_WIRED")
    execution = contract.get("execution", {})
    for key in ("never_in_android_apk", "no_workflow_pr_to_admin",
                "no_automatic_secret_or_permission_fallback", "read_only_first"):
        if execution.get(key) is not True:
            failures.append("EXECUTION_BOUNDARY:" + key)
    abi = contract.get("abi", {})
    if set(abi.get("formats", [])) != {"ELF32_ARM_EABI", "ELF64_AARCH64"}:
        failures.append("ABI_MATRIX_DRIFT")
    if abi.get("pure_core") != "FREESTANDING_C11_NO_LIBC_NO_HEAP_NO_GC_NO_IO_NO_SYSCALL":
        failures.append("FREESTANDING_BOUNDARY_DRIFT")
    for name in ("PAT_ACTIONS", "PAT_ENV", "PAT_AGENTS"):
        if "secrets." + name not in workflow:
            failures.append("EXISTING_WIRING_MISSING:" + name)
    if "name: Pat_environments" not in workflow:
        failures.append("EXISTING_ENV_WIRING_MISSING")
    if re.search(r"secrets\.PAT_ENVIRONMENTS\b", workflow):
        failures.append("UNAPPROVED_PAT_ENVIRONMENTS_BINDING")
    return failures

def main() -> int:
    try:
        failures = validate(
            json.loads((ROOT / "contracts/PAT_INTEROP_ABI_V2.json").read_text(encoding="utf-8")),
            json.loads((ROOT / "contracts/PAT_CAPABILITY_MATRIX_V1.json").read_text(encoding="utf-8")),
            (ROOT / ".github/workflows/START.yml").read_text(encoding="utf-8")
        )
    except (OSError, ValueError, TypeError):
        failures = ["SOURCE_NOT_AVAILABLE_OR_MALFORMED"]
    for failure in failures:
        print("FAIL", failure)
    if failures:
        return 2
    print("PASS source-only: 6 PAT identities, no token aliases, ABI ARM32/ARM64 metadata")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
