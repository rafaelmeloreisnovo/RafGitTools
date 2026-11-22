#!/usr/bin/env python3
"""Fail-closed, offline routing of draft PR changes for the single START workflow.

The caller supplies git diff --name-only -z; no GitHub/PAT/API access.
Source-only / docs drafts retain the fast lane. Android and Gradle changes
must compile/test; pipeline/control-plane changes demand full gates.
This determines *which tests run*, not merge authorization or a PASS claim.
"""
from __future__ import annotations

import sys
from typing import Iterable


FULL_EXACT = {
    ".github/dependabot.yml",
    ".github/dependabot.yaml",
    ".github/workflows/START.yml",
    "scripts/ci/draft_lane_guard.py",
    "tests/test_draft_lane_guard.py",
}
ANDROID_EXACT = {
    "gradlew",
    "gradlew.bat",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
    "gradle.properties",
}
ANDROID_SUFFIXES = (".gradle", ".gradle.kts", ".kt", ".java")
ANDROID_PREFIXES = ("app/", "gradle/")


def _validate(path: str) -> str:
    if (not path or path.startswith("/") or "\\" in path
            or any(part in ("", ".", "..") for part in path.split("/"))
            or any(ord(ch) < 32 or ord(ch) == 127 for ch in path)):
        raise ValueError("unsafe or invalid Git path in draft routing")
    return path


def select_lane(changed_paths: Iterable[str]) -> str:
    paths = [_validate(p) for p in changed_paths]
    if not paths:
        # Missing/incomplete diff is never a reason to silently skip testing.
        return "full"
    if any(
        p in FULL_EXACT or p.startswith(".github/")
        or p.startswith("scripts/ci/") or p.startswith("tests/ci/")
        for p in paths
    ):
        return "full"
    if any(
        p in ANDROID_EXACT or p.startswith(ANDROID_PREFIXES)
        or p.endswith(ANDROID_SUFFIXES)
        for p in paths
    ):
        return "android"
    return "fast"


def main() -> int:
    payload = sys.stdin.buffer.read()
    if payload and not payload.endswith(b"\0"):
        raise ValueError("git diff NUL-delimited input is incomplete")
    paths = [part.decode("utf-8", "strict") for part in payload.split(b"\0") if part]
    sys.stdout.write(select_lane(paths) + "\n")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, UnicodeError) as exc:
        print(f"::error::draft route gate refused input: {exc}", file=sys.stderr)
        raise SystemExit(2)
