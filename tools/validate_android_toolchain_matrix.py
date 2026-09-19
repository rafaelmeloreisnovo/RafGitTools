#!/usr/bin/env python3
"""Fail-closed Android toolchain coherence gate for RafGitTools."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MATRIX = ROOT / "configs" / "android-toolchain-matrix.json"


def _one(pattern: str, text: str, label: str) -> str:
    match = re.search(pattern, text, flags=re.MULTILINE)
    if not match:
        raise ValueError(f"missing {label}")
    return match.group(1)


def observe(root_build: str, app_build: str) -> dict[str, str]:
    kotlin = _one(
        r"org\.jetbrains\.kotlin:kotlin-gradle-plugin:([^'\"]+)",
        root_build,
        "Kotlin Gradle plugin version",
    )
    ksp = _one(
        r"id\s+['\"]com\.google\.devtools\.ksp['\"]\s+version\s+['\"]([^'\"]+)",
        root_build,
        "KSP plugin version",
    )

    mockk_match = re.search(
        r"^\s*testImplementation\s+['\"]io\.mockk:(mockk(?:-jvm)?):([^'\"]+)",
        app_build,
        flags=re.MULTILINE,
    )
    if not mockk_match:
        raise ValueError("missing MockK test dependency")
    mockk_artifact, mockk = mockk_match.groups()

    compose_plugin_match = re.search(
        r"id\s*\(?\s*['\"]org\.jetbrains\.kotlin\.plugin\.compose['\"]\s*\)?"
        r"(?:\s+version\s+['\"]([^'\"]+)['\"])?",
        root_build + "\n" + app_build,
        flags=re.MULTILINE,
    )
    legacy_match = re.search(
        r"kotlinCompilerExtensionVersion\s+['\"]([^'\"]+)['\"]",
        app_build,
        flags=re.MULTILINE,
    )

    if compose_plugin_match:
        compose_mode = "compose_plugin"
        compose_compiler = compose_plugin_match.group(1) or kotlin
    elif legacy_match:
        compose_mode = "legacy_extension"
        compose_compiler = legacy_match.group(1)
    else:
        compose_mode = "TOKEN_VAZIO"
        compose_compiler = "TOKEN_VAZIO"

    return {
        "kotlin": kotlin,
        "ksp": ksp,
        "compose_mode": compose_mode,
        "compose_compiler": compose_compiler,
        "mockk": mockk,
        "mockk_artifact": mockk_artifact,
    }


def validate(observed: dict[str, str], matrix: dict) -> dict:
    approved = matrix.get("approved") or []
    tuple_fields = ("kotlin", "ksp", "compose_mode", "compose_compiler", "mockk", "mockk_artifact")

    for entry in approved:
        if all(observed.get(key) == entry.get(key) for key in tuple_fields):
            return {
                "status": "PASS",
                "state": entry.get("state", "PASS_APPROVED_TUPLE"),
                "claim_allowed": False,
                "observed": observed,
                "policy": matrix.get("policy"),
                "reasons": [],
            }

    reasons = ["UNAPPROVED_ATOMIC_TOOLCHAIN_TUPLE"]
    kotlin = observed.get("kotlin", "")
    ksp = observed.get("ksp", "")
    compose_mode = observed.get("compose_mode")

    try:
        kotlin_major = int(kotlin.split(".", 1)[0])
    except (TypeError, ValueError):
        kotlin_major = -1

    if kotlin_major >= 2 and compose_mode != "compose_plugin":
        reasons.append("KOTLIN_2_PLUS_REQUIRES_COMPOSE_COMPILER_GRADLE_PLUGIN")

    legacy_ksp_prefix = re.match(r"^(\d+\.\d+\.\d+)-", ksp)
    if legacy_ksp_prefix and legacy_ksp_prefix.group(1) != kotlin:
        reasons.append("LEGACY_KSP_KOTLIN_PREFIX_MISMATCH")

    return {
        "status": "FAIL",
        "state": "FAIL_CLOSED_UNAPPROVED_TOOLCHAIN",
        "claim_allowed": False,
        "observed": observed,
        "policy": matrix.get("policy"),
        "reasons": sorted(set(reasons)),
    }


def run(
    root_build_path: Path = ROOT / "build.gradle",
    app_build_path: Path = ROOT / "app" / "build.gradle",
    matrix_path: Path = DEFAULT_MATRIX,
) -> dict:
    matrix = json.loads(matrix_path.read_text(encoding="utf-8"))
    observed = observe(
        root_build_path.read_text(encoding="utf-8"),
        app_build_path.read_text(encoding="utf-8"),
    )
    return validate(observed, matrix)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    args = parser.parse_args()

    result = run(matrix_path=args.matrix)
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if result["status"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
