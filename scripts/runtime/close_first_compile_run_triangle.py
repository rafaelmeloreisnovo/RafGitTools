#!/usr/bin/env python3
"""Close the first compile-run triangle with hash-bound evidence.

Vertices:
  SOURCE  -> exact Git commit
  BUILD   -> build receipt + exact APK SHA-256
  DEVICE  -> physical install/launch runtime receipt

The triangle closes only when all three vertices refer to the same commit and
APK bytes and the runtime collector reports PASS.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

BUILD_SCHEMA = "rafgittools.android-build-receipt.v1"
RUNTIME_SCHEMA = "rafgittools.android-runtime-receipt.v1"
TRIANGLE_SCHEMA = "rafgittools.first-compile-run-triangle.v1"
PASS = "PASS"
FAIL = "FAIL"
BLOCKED = "BLOCKED"


def _reject_duplicates(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    out: dict[str, Any] = {}
    for key, value in pairs:
        if key in out:
            raise ValueError(f"duplicate JSON key: {key}")
        out[key] = value
    return out


def read_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as fh:
        value = json.load(fh, object_pairs_hook=_reject_duplicates)
    if not isinstance(value, dict):
        raise ValueError(f"top-level JSON object required: {path}")
    return value


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def git_head(repo: Path) -> str | None:
    cp = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"],
        capture_output=True,
        text=True,
        check=False,
    )
    if cp.returncode != 0:
        return None
    return cp.stdout.strip().splitlines()[-1] if cp.stdout.strip() else None


def atomic_write(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=path.name + ".", suffix=".tmp", dir=str(path.parent))
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as fh:
            json.dump(value, fh, indent=2, sort_keys=True)
            fh.write("\n")
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)


def validate_triangle(
    build: dict[str, Any],
    runtime: dict[str, Any],
    actual_apk_sha256: str,
    repo_head: str | None,
    build_receipt_sha256: str,
    runtime_receipt_sha256: str,
) -> dict[str, Any]:
    build_schema_gate = PASS if build.get("schema") == BUILD_SCHEMA else FAIL
    runtime_schema_gate = PASS if runtime.get("schema") == RUNTIME_SCHEMA else FAIL

    build_commit = build.get("commit")
    build_apk_sha = (build.get("apk") or {}).get("sha256")
    build_artifact_gate = (build.get("gates") or {}).get("artifact")
    runtime_commit = (runtime.get("repo") or {}).get("commit")
    runtime_expected_commit = (runtime.get("repo") or {}).get("expected_commit")
    runtime_apk_sha = (runtime.get("apk") or {}).get("sha256")
    runtime_gate = (runtime.get("gates") or {}).get("runtime")

    source_gate = (
        PASS
        if isinstance(build_commit, str)
        and repo_head == build_commit
        and runtime_commit == build_commit
        and runtime_expected_commit == build_commit
        else FAIL
    )
    apk_hash_gate = (
        PASS
        if isinstance(build_apk_sha, str)
        and actual_apk_sha256 == build_apk_sha
        and runtime_apk_sha == build_apk_sha
        else FAIL
    )
    build_gate = (
        PASS
        if build_schema_gate == PASS
        and build_artifact_gate == PASS
        and apk_hash_gate == PASS
        else FAIL
    )
    device_gate = PASS if runtime_schema_gate == PASS and runtime_gate == PASS else FAIL

    closure = PASS if all(x == PASS for x in (source_gate, build_gate, device_gate)) else BLOCKED
    gaps: list[str] = []
    if source_gate != PASS:
        gaps.append("TOKEN_VAZIO_OR_MISMATCH_SOURCE_COMMIT")
    if build_gate != PASS:
        gaps.append("TOKEN_VAZIO_OR_MISMATCH_BUILD_ARTIFACT")
    if device_gate != PASS:
        gaps.append("TOKEN_VAZIO_PHYSICAL_DEVICE_RUNTIME")

    return {
        "schema": TRIANGLE_SCHEMA,
        "claim_allowed": closure == PASS,
        "release_allowed": False,
        "vertices": {
            "source": {
                "gate": source_gate,
                "repo_head": repo_head or "TOKEN_VAZIO",
                "build_commit": build_commit,
                "runtime_commit": runtime_commit,
            },
            "build": {
                "gate": build_gate,
                "artifact_gate": build_artifact_gate,
                "apk_hash_gate": apk_hash_gate,
                "expected_apk_sha256": build_apk_sha,
                "actual_apk_sha256": actual_apk_sha256,
            },
            "device": {
                "gate": device_gate,
                "runtime_gate": runtime_gate,
                "device_abi": (runtime.get("device") or {}).get("primary_abi"),
            },
        },
        "edges": {
            "source_to_build": source_gate,
            "build_to_device": apk_hash_gate,
            "device_to_source": PASS if runtime_expected_commit == build_commit else FAIL,
        },
        "receipts": {
            "build_receipt_sha256": build_receipt_sha256,
            "runtime_receipt_sha256": runtime_receipt_sha256,
        },
        "gates": {"triangle_closure": closure},
        "token_vazio": gaps,
        "F_ok": ["source identity", "build artifact identity"] if closure == PASS else [],
        "F_gap": gaps,
        "F_next": "GOVERNED_INTEGRATION_RECEIPT" if closure == PASS else "CLOSE_FAILED_VERTEX",
        "invariant": "SOURCE_COMMIT == BUILD_COMMIT == RUNTIME_EXPECTED_COMMIT AND BUILD_APK_SHA256 == DEVICE_APK_SHA256",
        "falsifier": "Any commit mismatch, APK SHA-256 mismatch, or runtime gate != PASS keeps the triangle open.",
    }


def run_runtime_capture(args: argparse.Namespace, build: dict[str, Any]) -> int:
    commit = build.get("commit")
    cmd = [
        sys.executable,
        str(Path(__file__).with_name("capture_android_runtime_receipt.py")),
        "--apk",
        str(Path(args.apk).resolve()),
        "--output",
        str(Path(args.runtime_output).resolve()),
        "--repo",
        str(Path(args.repo).resolve()),
        "--expected-commit",
        str(commit),
        "--package",
        args.package,
        "--activity",
        args.activity,
    ]
    if args.adb:
        cmd += ["--adb", args.adb]
    if args.apksigner:
        cmd += ["--apksigner", args.apksigner]
    if args.install:
        cmd.append("--install")
    if args.launch:
        cmd.append("--launch")
    if args.require_runtime_pass:
        cmd.append("--require-runtime-pass")
    return subprocess.run(cmd, check=False).returncode


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--build-receipt", required=True)
    p.add_argument("--apk", required=True)
    p.add_argument("--runtime-output", required=True)
    p.add_argument("--triangle-output", required=True)
    p.add_argument("--repo", default=".")
    p.add_argument("--adb")
    p.add_argument("--apksigner")
    # devDebug combines the dev flavor suffix (.dev) with the debug build-type
    # suffix (.debug): applicationId = com.rafgittools.dev.debug.
    p.add_argument("--package", default="com.rafgittools.dev.debug")
    p.add_argument("--activity", default="com.rafgittools.MainActivity")
    p.add_argument("--install", action="store_true")
    p.add_argument("--launch", action="store_true")
    p.add_argument("--require-runtime-pass", action="store_true")
    p.add_argument("--require-triangle-pass", action="store_true")
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    build_path = Path(args.build_receipt).resolve()
    apk_path = Path(args.apk).resolve()
    runtime_path = Path(args.runtime_output).resolve()
    triangle_path = Path(args.triangle_output).resolve()
    repo = Path(args.repo).resolve()

    try:
        build = read_json(build_path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"build receipt invalid: {exc}", file=sys.stderr)
        return 1
    if build.get("schema") != BUILD_SCHEMA:
        print("build receipt schema mismatch", file=sys.stderr)
        return 1
    if (build.get("gates") or {}).get("artifact") != PASS:
        print("build artifact gate is not PASS", file=sys.stderr)
        return 2
    if not apk_path.is_file():
        print(f"APK missing: {apk_path}", file=sys.stderr)
        return 1

    expected_sha = (build.get("apk") or {}).get("sha256")
    actual_sha = sha256_file(apk_path)
    if expected_sha != actual_sha:
        print(
            f"APK SHA-256 mismatch: expected={expected_sha} actual={actual_sha}",
            file=sys.stderr,
        )
        return 2

    runtime_rc = run_runtime_capture(args, build)
    if not runtime_path.is_file():
        print("runtime receipt was not produced", file=sys.stderr)
        return runtime_rc or 1

    try:
        runtime = read_json(runtime_path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"runtime receipt invalid: {exc}", file=sys.stderr)
        return 1

    triangle = validate_triangle(
        build=build,
        runtime=runtime,
        actual_apk_sha256=actual_sha,
        repo_head=git_head(repo),
        build_receipt_sha256=sha256_file(build_path),
        runtime_receipt_sha256=sha256_file(runtime_path),
    )
    atomic_write(triangle_path, triangle)
    print(f"triangle receipt: {triangle_path}")
    print(f"triangle closure: {triangle['gates']['triangle_closure']}")
    print(f"claim_allowed: {str(triangle['claim_allowed']).lower()}")

    if args.require_triangle_pass and triangle["gates"]["triangle_closure"] != PASS:
        return 2
    if runtime_rc != 0:
        return runtime_rc
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
