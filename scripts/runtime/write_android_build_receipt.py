#!/usr/bin/env python3
"""Emit a deterministic custody receipt for one Android APK build.

This receipt proves only the observed build artifact identity and packaging
contract. It deliberately does not promote physical-device runtime claims.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from zipfile import BadZipFile, ZipFile

SCHEMA = "rafgittools.android-build-receipt.v1"
PASS = "PASS"
FAIL = "FAIL"
SUPPORTED_ABIS = ("armeabi-v7a", "arm64-v8a")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
GIT_SHA_RE = re.compile(r"^[0-9a-f]{40}$")


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def inspect_apk(path: Path) -> dict[str, Any]:
    try:
        with ZipFile(path) as zf:
            corrupt = zf.testzip()
            names = zf.namelist()
    except (BadZipFile, OSError) as exc:
        return {
            "zip_crc": FAIL,
            "reason": f"invalid_zip:{exc}",
            "abis": [],
            "missing_required_abis": list(SUPPORTED_ABIS),
            "dual_abi_gate": FAIL,
        }

    abis = sorted(
        {
            name.split("/", 2)[1]
            for name in names
            if name.startswith("lib/") and len(name.split("/", 2)) >= 3
        }
    )
    missing = [abi for abi in SUPPORTED_ABIS if abi not in abis]
    return {
        "zip_crc": PASS if corrupt is None else FAIL,
        "first_corrupt_member": corrupt,
        "abis": abis,
        "required_abis": list(SUPPORTED_ABIS),
        "missing_required_abis": missing,
        "dual_abi_gate": PASS if not missing else FAIL,
    }


def build_receipt(args: argparse.Namespace) -> dict[str, Any]:
    apk = Path(args.apk).resolve()
    if not apk.is_file():
        raise FileNotFoundError(apk)

    commit = args.commit.strip().lower()
    if not GIT_SHA_RE.fullmatch(commit):
        raise ValueError("--commit must be a full 40-hex Git SHA")

    apk_sha = sha256_file(apk)
    if not SHA256_RE.fullmatch(apk_sha):
        raise AssertionError("internal SHA-256 formatting error")

    packaging = inspect_apk(apk)
    artifact_gate = (
        PASS
        if packaging["zip_crc"] == PASS and packaging["dual_abi_gate"] == PASS
        else FAIL
    )

    return {
        "schema": SCHEMA,
        "captured_at_utc": datetime.now(timezone.utc).isoformat(),
        "claim_allowed": False,
        "release_allowed": False,
        "commit": commit,
        "variant": args.variant,
        "workflow": {
            "run_id": args.workflow_run_id,
            "run_attempt": args.workflow_run_attempt,
        },
        "apk": {
            "filename": apk.name,
            "size_bytes": apk.stat().st_size,
            "sha256": apk_sha,
            **packaging,
        },
        "gates": {
            "artifact": artifact_gate,
            "runtime": "TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED",
        },
        "invariant": "BUILD_ARTIFACT_IDENTITY_DOES_NOT_PROMOTE_RUNTIME",
        "falsifier": "Any APK byte change, corrupt ZIP member, or missing required ABI invalidates this build-artifact receipt.",
    }


def atomic_write_json(path: Path, value: dict[str, Any]) -> None:
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


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    p = argparse.ArgumentParser()
    p.add_argument("--apk", required=True)
    p.add_argument("--output", required=True)
    p.add_argument("--commit", required=True)
    p.add_argument("--variant", default="devDebug")
    p.add_argument("--workflow-run-id")
    p.add_argument("--workflow-run-attempt")
    p.add_argument("--require-artifact-pass", action="store_true")
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        receipt = build_receipt(args)
    except (FileNotFoundError, OSError, ValueError) as exc:
        print(f"build receipt failed: {exc}", file=os.sys.stderr)
        return 1

    atomic_write_json(Path(args.output), receipt)
    print(f"build receipt: {args.output}")
    print(f"artifact gate: {receipt['gates']['artifact']}")
    print(f"apk sha256: {receipt['apk']['sha256']}")
    if args.require_artifact_pass and receipt["gates"]["artifact"] != PASS:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
