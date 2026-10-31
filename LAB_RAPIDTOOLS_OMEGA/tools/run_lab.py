#!/usr/bin/env python3
"""Synthetic CPU/memory test grid; each run validates semantic equivalence."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import sys

LAB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(LAB / "tools"))
from receipts import append, verify  # noqa: E402


def hash_file(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def execute(mode, workers, mib, allow_large=False):
    command = [str(LAB / "build/raf_lab_host"),
               mode, str(workers), str(mib)]
    if allow_large:
        command.append("--allow-large")
    result = subprocess.run(command, cwd=LAB, text=True, capture_output=True,
                            check=False, timeout=180)
    if result.returncode:
        raise RuntimeError("RUN_DENIED_OR_FAILED rc=%d stderr=%s" %
                           (result.returncode, result.stderr[:240]))
    data = json.loads(result.stdout)
    if not data["kat_pass"] or data["checksum"] != data["reference"]:
        raise AssertionError("SEMANTIC_MISMATCH")
    if data["buffer_bytes"] != int(mib * 1048576):
        raise AssertionError("BUFFER_MISMATCH")
    return data


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--smoke", action="store_true")
    p.add_argument("--mib", type=float, default=0.25)
    p.add_argument("--allow-large", action="store_true")
    p.add_argument("--workers", type=int, nargs="+", default=[1, 2, 4, 8])
    a = p.parse_args()
    config = json.loads((LAB / "cases/workloads.json").read_text(encoding="utf-8"))
    if a.mib not in config["buffers_mib"]:
        raise SystemExit("BLOCKED_BUFFER_NOT_ALLOWLISTED")
    if a.mib >= 64 and not a.allow_large:
        raise SystemExit("BLOCKED_LARGE_BUFFER_CONSENT")
    if any(w not in config["workers"] for w in a.workers):
        raise SystemExit("BLOCKED_WORKERS")
    if not (LAB / "build/raf_lab_host").exists():
        subprocess.run(["make", "all"], cwd=LAB, check=True)
    rows = []
    for mode in config["modes"]:
        for w in a.workers:
            row = execute(mode, w, a.mib, a.allow_large)
            rows.append(row)
        candidates = [r for r in rows if r["mode"] == mode]
        hashes = {(r["checksum"], r["fnv64"], r["reference"]) for r in candidates}
        if len(hashes) != 1:
            raise AssertionError("WORKER_DEPENDENT_OUTPUT: " + mode)
    source_sha = hash_file(LAB / "core/raf_lab_core.c")
    env_kind = "TERMUX_POSSIBLE" if "com.termux" in os.environ.get("PREFIX", "") else "HOST_POSSIBLE"
    payload = {
        "schema": "rafaelia.lab.receipt.v1", "status": "PASS_EXECUTED_LOCAL",
        "scope": "LOCAL_PROCESS_ONLY", "device_class": env_kind,
        "machine": platform.machine(), "system": platform.system(),
        "compiler_core_sha256": source_sha,
        "mib": a.mib, "worker_counts": a.workers,
        "trials": rows, "dataset": "SYNTHETIC",
        "claim_allowed": False, "release_allowed": False,
        "hardware_attested": False, "external_anchor": "TOKEN_VAZIO",
        "physical_abi_comparison": "TOKEN_VAZIO"
    }
    output = append(LAB / "build/receipts", payload)
    count, tail = verify(LAB / "build/receipts")
    print("PASS_LOCAL_SEMANTICS", len(rows), "KATs; receipt=", output)
    print("PASS_LOCAL_RECEIPT_CHAIN", count, tail)
    print("NO_ARM32_VS_ARM64_CLAIM / GPU_NOT_TESTED / NOT_AN_APK")


if __name__ == "__main__":
    main()
