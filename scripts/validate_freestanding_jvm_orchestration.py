#!/usr/bin/env python3
"""Validate freestanding L0 authority plus optional hosted adapters.

This gate validates topology/evidence semantics only. It does not prove device
behavior, cryptographic correctness, audio quality, VM boot, or performance.
"""

from __future__ import annotations

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "configs" / "freestanding-jvm-orchestration.v1.json"

BOUNDARIES = {
    "JVM_CONTROL_PLANE",
    "HOSTED_ADAPTER",
    "FREESTANDING_CORE",
    "PLATFORM_GATE",
}
STATES = {"REFERENCE", "IMPLEMENTED_UNTESTED", "PASS", "FAIL", "TOKEN_VAZIO", "BLOCKED"}
URGENCIES = {"P0", "P1", "P2", "P3"}
REQUIRED_FLAGS = {"SOURCE_BOUND", "AUTHORITY_BOUND", "FAIL_CLOSED", "SHADOW_GUARD"}
FORBIDDEN_L0 = {
    "JVM",
    "JNI",
    "NDK",
    "libc",
    "libstdc++",
    "POSIX",
    "malloc",
    "syscall",
    "filesystem",
    "threads",
    "external_library",
}


def error(errors: list[str], message: str) -> None:
    errors.append(message)


def validate(path: pathlib.Path) -> list[str]:
    errors: list[str] = []
    try:
        document = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return [f"manifest unreadable: {exc}"]

    if document.get("schema") != "rafaelia.freestanding-jvm-orchestration/v1":
        error(errors, "schema must be rafaelia.freestanding-jvm-orchestration/v1")
    if document.get("claim_allowed") is not False:
        error(errors, "claim_allowed must remain false at orchestration-contract scope")
    if document.get("jvm_is_freestanding") is not False:
        error(errors, "jvm_is_freestanding must be false")
    if document.get("root_authority") != "FREESTANDING_L0":
        error(errors, "root_authority must be FREESTANDING_L0")
    if document.get("root_authority_path") != "freestanding/orchestration/raf_orchestrator_l0.c":
        error(errors, "root_authority_path must point to raf_orchestrator_l0.c")
    if document.get("hosted_adapter_optional") is not True:
        error(errors, "hosted_adapter_optional must be true")
    if document.get("runtime_dependencies") != []:
        error(errors, "freestanding root runtime_dependencies must be empty")

    forbidden = set(document.get("forbidden_l0_dependencies", []))
    if not FORBIDDEN_L0.issubset(forbidden):
        error(errors, f"forbidden_l0_dependencies missing {sorted(FORBIDDEN_L0 - forbidden)}")

    policy = document.get("execution_policy", {})
    if policy.get("dispatch") != "TYPED_ITERATIVE_NO_REFLECTION":
        error(errors, "execution_policy.dispatch must be TYPED_ITERATIVE_NO_REFLECTION")
    if policy.get("tail_recursion") is not False:
        error(errors, "tail_recursion must be false")
    if policy.get("shadow_state") != "SINGLE_CONTROL_CONTEXT":
        error(errors, "shadow_state must be SINGLE_CONTROL_CONTEXT")
    if policy.get("allocation") != "CALLER_OWNED_ONLY":
        error(errors, "allocation must be CALLER_OWNED_ONLY")

    modules = document.get("modules")
    if not isinstance(modules, list) or not modules:
        return errors + ["modules must be a non-empty list"]

    seen_ids: set[str] = set()
    seen_repositories: set[str] = set()
    for index, module in enumerate(modules):
        prefix = f"modules[{index}]"
        module_id = module.get("id")
        repository = module.get("repository")
        boundary = module.get("boundary")
        state = module.get("state")
        urgency = module.get("urgency")
        flags = set(module.get("required_flags", []))
        gaps = module.get("token_vazio", [])

        if not isinstance(module_id, str) or not module_id:
            error(errors, f"{prefix}.id missing")
        elif module_id in seen_ids:
            error(errors, f"{prefix}.id duplicated: {module_id}")
        else:
            seen_ids.add(module_id)

        if not isinstance(repository, str) or "/" not in repository:
            error(errors, f"{prefix}.repository invalid")
        elif repository in seen_repositories:
            error(errors, f"{prefix}.repository duplicated: {repository}")
        else:
            seen_repositories.add(repository)

        if boundary not in BOUNDARIES:
            error(errors, f"{prefix}.boundary invalid: {boundary}")
        if state not in STATES:
            error(errors, f"{prefix}.state invalid: {state}")
        if urgency not in URGENCIES:
            error(errors, f"{prefix}.urgency invalid: {urgency}")
        if not REQUIRED_FLAGS.issubset(flags):
            missing = sorted(REQUIRED_FLAGS - flags)
            error(errors, f"{prefix}.required_flags missing {missing}")

        if repository == "rafaelmeloreisnovo/RafGitTools":
            if module.get("authority_path") != "freestanding/orchestration/":
                error(errors, f"{prefix}: RafGitTools authority must be freestanding/orchestration/")
            if boundary != "FREESTANDING_CORE":
                error(errors, f"{prefix}: RafGitTools root boundary must be FREESTANDING_CORE")

        if state == "PASS" and not module.get("evidence_refs"):
            error(errors, f"{prefix}: PASS requires evidence_refs")
        if not isinstance(gaps, list):
            error(errors, f"{prefix}.token_vazio must be a list")
        else:
            for gap_index, gap in enumerate(gaps):
                gap_prefix = f"{prefix}.token_vazio[{gap_index}]"
                for key in ("id", "missing", "evidence_needed", "next"):
                    if not isinstance(gap.get(key), str) or not gap.get(key):
                        error(errors, f"{gap_prefix}.{key} missing")
                if gap.get("urgency") not in URGENCIES:
                    error(errors, f"{gap_prefix}.urgency invalid")

        if repository == "rafaelmeloreisnovo/BLAKE3":
            authority_path = module.get("authority_path", "")
            if not authority_path.startswith("rmr/"):
                error(errors, f"{prefix}: BLAKE3 authorial integration must remain under rmr/")
            if module.get("upstream_crypto_mutation") is not False:
                error(errors, f"{prefix}: upstream_crypto_mutation must be false")

    expected = {
        "rafaelmeloreisnovo/RafGitTools",
        "rafaelmeloreisnovo/RafPolimata",
        "rafaelmeloreisnovo/Est-dio-de-udio",
        "rafaelmeloreisnovo/BLAKE3",
        "rafaelmeloreisnovo/Vectras-VM-Android",
        "rafaelmeloreisnovo/termux-app-rafacodephi",
    }
    missing_repositories = sorted(expected - seen_repositories)
    if missing_repositories:
        error(errors, f"federation missing repositories: {missing_repositories}")

    return errors


def main() -> int:
    manifest = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_MANIFEST
    errors = validate(manifest)
    if errors:
        for item in errors:
            print(f"FAIL: {item}")
        return 1
    print(f"PASS_SCOPED: {manifest}")
    print("root=FREESTANDING_L0; runtime_dependencies=0; hosted_adapters=OPTIONAL")
    print("scope=structure+evidence-semantics; device/performance=TOKEN_VAZIO")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
