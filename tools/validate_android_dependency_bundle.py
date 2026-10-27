#!/usr/bin/env python3
"""Android build tuple admission gate; no network, credentials, or changes.

Ensures Hilt runtime/processor and Hilt Gradle plugin are an atomic unit.
Kotlin/KSP/Compose/MockK approval remains in the independent matrix validator.
"""
from __future__ import annotations

import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def one(pattern: str, source: str, name: str) -> str:
    matches = re.findall(pattern, source, flags=re.MULTILINE)
    if len(matches) != 1:
        raise ValueError(f"Expected exactly one {name}, observed {len(matches)}")
    return matches[0]


def validate(root: str, app: str) -> dict:
    reasons = []
    try:
        gradle_plugin = one(
            r"^\s*classpath\s+['\"]com\.google\.dagger:hilt-android-gradle-plugin:([^'\"]+)",
            root, "Hilt Gradle plugin")
        runtime = one(
            r"^\s*implementation\s+['\"]com\.google\.dagger:hilt-android:([^'\"]+)",
            app, "Hilt runtime")
        processor = one(
            r"^\s*ksp\s+['\"]com\.google\.dagger:hilt-compiler:([^'\"]+)",
            app, "Hilt KSP compiler")
        kotlin = one(
            r"^\s*classpath\s+['\"]org\.jetbrains\.kotlin:kotlin-gradle-plugin:([^'\"]+)",
            root, "Kotlin Gradle plugin")
        lint_abort = one(r"^\s*abortOnError\s+(true|false)\s*$",
                         app, "Android lint abortOnError")
    except ValueError as err:
        return {"status": "FAIL", "claim_allowed": False, "reasons": [str(err)]}

    if len({gradle_plugin, runtime, processor}) != 1:
        reasons.append("HILT_GRADLE_RUNTIME_KSP_VERSION_SPLIT")
    # Exact negative runtime evidence: Hilt 2.60.1 includes Kotlin-2.x
    # metadata incompatible with this project's Kotlin 1.9.x compiler.
    if kotlin.startswith("1.9.") and any(
        version == "2.60.1" for version in (gradle_plugin, runtime, processor)
    ):
        reasons.append("KNOWN_HILT_2601_KOTLIN19_METADATA_FAILURE")
    if lint_abort != "true":
        reasons.append("ANDROID_LINT_ERRORS_NOT_BLOCKING")
    return {
        "status": "FAIL" if reasons else "PASS",
        "claim_allowed": False,
        "observed": {
            "kotlin": kotlin, "hilt_gradle": gradle_plugin,
            "hilt_runtime": runtime, "hilt_ksp": processor,
            "lint_abort_on_error": lint_abort
        },
        "reasons": reasons,
    }


def main() -> int:
    try:
        result = validate((ROOT / "build.gradle").read_text(encoding="utf-8"),
                          (ROOT / "app/build.gradle").read_text(encoding="utf-8"))
    except (OSError, UnicodeError) as exc:
        result = {"status": "FAIL", "reasons": [str(exc)], "claim_allowed": False}
    print(json.dumps(result, sort_keys=True))
    return 0 if result["status"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
