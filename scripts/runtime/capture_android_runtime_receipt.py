#!/usr/bin/env python3
"""Capture a fail-closed Android runtime receipt for a single APK.

The collector never promotes missing evidence. Observation-only mode is the
safe default; installation and launch require explicit flags.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from zipfile import BadZipFile, ZipFile

SCHEMA = "rafgittools.android-runtime-receipt.v1"
PASS = "PASS"
FAIL = "FAIL"
BLOCKED = "BLOCKED"
TOKEN_VAZIO = "TOKEN_VAZIO"
NOT_MEASURED = "NOT_MEASURED"
SUPPORTED_ABIS = ("armeabi-v7a", "arm64-v8a")


def _run(argv: list[str], timeout: int = 30) -> dict[str, Any]:
    try:
        cp = subprocess.run(argv, capture_output=True, text=True, timeout=timeout, check=False)
        return {
            "argv": argv,
            "returncode": cp.returncode,
            "stdout": cp.stdout.strip()[-4000:],
            "stderr": cp.stderr.strip()[-4000:],
        }
    except (OSError, subprocess.TimeoutExpired) as exc:
        return {"argv": argv, "returncode": None, "stdout": "", "stderr": str(exc)}


def _sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def _git_commit(repo: Path) -> str | None:
    result = _run(["git", "-C", str(repo), "rev-parse", "HEAD"])
    if result["returncode"] == 0 and result["stdout"]:
        return result["stdout"].splitlines()[-1].strip()
    return None


def _inspect_apk(apk: Path) -> dict[str, Any]:
    try:
        with ZipFile(apk) as zf:
            corrupt = zf.testzip()
            names = zf.namelist()
    except (BadZipFile, OSError) as exc:
        return {"state": FAIL, "reason": f"invalid_zip:{exc}", "abis": [], "zip_crc": FAIL}

    abis = sorted({name.split("/", 2)[1] for name in names if name.startswith("lib/") and len(name.split("/", 2)) >= 3})
    missing = [abi for abi in SUPPORTED_ABIS if abi not in abis]
    return {
        "state": PASS if corrupt is None else FAIL,
        "zip_crc": PASS if corrupt is None else FAIL,
        "first_corrupt_member": corrupt,
        "abis": abis,
        "required_abis": list(SUPPORTED_ABIS),
        "missing_required_abis": missing,
        "dual_abi_gate": PASS if not missing else FAIL,
    }


def _verify_signature(apk: Path, apksigner: str | None) -> dict[str, Any]:
    tool = shutil.which(apksigner) if apksigner else shutil.which("apksigner")
    if not tool:
        return {"state": TOKEN_VAZIO, "tool": None, "reason": "apksigner_unavailable"}
    result = _run([tool, "verify", "--verbose", str(apk)])
    return {
        "state": PASS if result["returncode"] == 0 else FAIL,
        "tool": tool,
        "returncode": result["returncode"],
        "stderr_tail": result["stderr"],
    }


def _adb_probe(adb_name: str | None) -> tuple[str | None, dict[str, Any]]:
    tool = shutil.which(adb_name) if adb_name else shutil.which("adb")
    if not tool:
        return None, {"state": TOKEN_VAZIO, "reason": "adb_unavailable"}
    state = _run([tool, "get-state"])
    if state["returncode"] != 0 or state["stdout"].strip() != "device":
        return tool, {"state": BLOCKED, "reason": "adb_device_not_ready", "probe": state}

    serial = _run([tool, "get-serialno"])
    abi = _run([tool, "shell", "getprop", "ro.product.cpu.abi"])
    abi_list = _run([tool, "shell", "getprop", "ro.product.cpu.abilist"])
    sdk = _run([tool, "shell", "getprop", "ro.build.version.sdk"])
    release = _run([tool, "shell", "getprop", "ro.build.version.release"])
    page = _run([tool, "shell", "getconf", "PAGESIZE"])
    return tool, {
        "state": PASS,
        "serial_sha256": hashlib.sha256(serial["stdout"].encode("utf-8")).hexdigest() if serial["stdout"] else TOKEN_VAZIO,
        "primary_abi": abi["stdout"] or TOKEN_VAZIO,
        "abi_list": abi_list["stdout"] or TOKEN_VAZIO,
        "sdk": sdk["stdout"] or TOKEN_VAZIO,
        "android_release": release["stdout"] or TOKEN_VAZIO,
        "page_size": page["stdout"] or TOKEN_VAZIO,
    }


def _adb_install(adb: str | None, apk: Path, requested: bool) -> dict[str, Any]:
    if not requested:
        return {"state": NOT_MEASURED, "reason": "install_not_requested"}
    if not adb:
        return {"state": BLOCKED, "reason": "adb_unavailable"}
    result = _run([adb, "install", "-r", str(apk)], timeout=180)
    ok = result["returncode"] == 0 and "Success" in result["stdout"]
    return {"state": PASS if ok else FAIL, "returncode": result["returncode"], "stdout_tail": result["stdout"], "stderr_tail": result["stderr"]}


def _adb_launch(adb: str | None, package: str | None, activity: str | None, requested: bool) -> dict[str, Any]:
    if not requested:
        return {"state": NOT_MEASURED, "reason": "launch_not_requested"}
    if not adb:
        return {"state": BLOCKED, "reason": "adb_unavailable"}
    if not package or not activity:
        return {"state": BLOCKED, "reason": "package_and_activity_required"}
    component = f"{package}/{activity}"
    result = _run([adb, "shell", "am", "start", "-W", "-n", component], timeout=60)
    text = f"{result['stdout']}\n{result['stderr']}"
    ok = result["returncode"] == 0 and "Error:" not in text and "Exception" not in text
    return {"state": PASS if ok else FAIL, "component": component, "returncode": result["returncode"], "stdout_tail": result["stdout"], "stderr_tail": result["stderr"]}


def build_receipt(args: argparse.Namespace) -> dict[str, Any]:
    apk = Path(args.apk).resolve()
    repo = Path(args.repo).resolve()
    now = datetime.now(timezone.utc).isoformat()

    if not apk.is_file():
        raise FileNotFoundError(apk)

    commit = _git_commit(repo)
    commit_gate = PASS
    if args.expected_commit:
        commit_gate = PASS if commit == args.expected_commit else FAIL
    elif commit is None:
        commit_gate = TOKEN_VAZIO

    apk_info = {
        "path": str(apk),
        "size_bytes": apk.stat().st_size,
        "sha256": _sha256(apk),
        **_inspect_apk(apk),
    }
    signature = _verify_signature(apk, args.apksigner)
    adb, device = _adb_probe(args.adb)
    install = _adb_install(adb, apk, args.install)
    launch = _adb_launch(adb, args.package, args.activity, args.launch)

    runtime_prereqs = [
        commit_gate,
        apk_info["state"],
        apk_info["dual_abi_gate"],
        signature["state"],
        device["state"],
        install["state"],
        launch["state"],
    ]
    runtime_requested = args.install and args.launch
    runtime_gate = PASS if runtime_requested and all(x == PASS for x in runtime_prereqs) else BLOCKED

    return {
        "schema": SCHEMA,
        "captured_at_utc": now,
        "claim_allowed": runtime_gate == PASS,
        "repo": {"path": str(repo), "commit": commit or TOKEN_VAZIO, "expected_commit": args.expected_commit or None, "commit_gate": commit_gate},
        "apk": apk_info,
        "signature": signature,
        "device": device,
        "install": install,
        "launch": launch,
        "gates": {
            "custody": PASS if apk_info["state"] == PASS and commit_gate == PASS else BLOCKED,
            "dual_abi": apk_info["dual_abi_gate"],
            "signature": signature["state"],
            "device": device["state"],
            "install": install["state"],
            "launch": launch["state"],
            "runtime": runtime_gate,
        },
        "invariant": "CUSTODY_PASS_DOES_NOT_PROMOTE_RUNTIME",
        "falsifier": "Any commit/APK hash mismatch, signature failure, ABI omission, install failure, or launch failure invalidates runtime promotion.",
    }


def _atomic_write_json(path: Path, value: dict[str, Any]) -> None:
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
    p.add_argument("--repo", default=".")
    p.add_argument("--expected-commit")
    p.add_argument("--adb")
    p.add_argument("--apksigner")
    p.add_argument("--package")
    p.add_argument("--activity")
    p.add_argument("--install", action="store_true", help="Explicitly allow adb install -r")
    p.add_argument("--launch", action="store_true", help="Explicitly allow adb am start")
    p.add_argument("--require-runtime-pass", action="store_true", help="Exit 2 unless the runtime gate is PASS")
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        receipt = build_receipt(args)
    except (FileNotFoundError, OSError) as exc:
        print(f"receipt capture failed: {exc}", file=sys.stderr)
        return 1
    _atomic_write_json(Path(args.output), receipt)
    print(f"runtime receipt: {args.output}")
    print(f"runtime gate: {receipt['gates']['runtime']}")
    print(f"claim_allowed: {str(receipt['claim_allowed']).lower()}")
    if args.require_runtime_pass and receipt["gates"]["runtime"] != PASS:
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
