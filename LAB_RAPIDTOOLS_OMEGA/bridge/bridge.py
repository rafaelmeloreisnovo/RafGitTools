#!/usr/bin/env python3
"""RAFAELIA Middlefield V1: declarative change control and scoped CI execution.

Trusted boundary: GitHub Actions with contents:read only. This bridge does NOT
connect to Google APIs, read K-Secrets, run arbitrary commands or modify repos.
ChatGPT connected tools can write proposals to Drive and, when authorized,
stage matching GitHub changes during an interactive chat.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time

LAB = Path(__file__).resolve().parents[1]
BRIDGE = Path(__file__).resolve().parent
PIPELINE = BRIDGE / "pipelines/lab-quality.v1.json"
REQUEST = BRIDGE / "inbox/REQ-0001.json"

def canonical(data):
    return json.dumps(data, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=False).encode("utf-8")

def sha(data):
    return hashlib.sha256(canonical(data)).hexdigest()

def load(path):
    p = Path(path)
    if not p.is_file() or p.stat().st_size not in range(1, 16385):
        raise ValueError("DENY_FILE_SIZE")
    return json.loads(p.read_text(encoding="utf-8"))

def fields(data, expected, tag):
    if not isinstance(data, dict) or set(data) != set(expected):
        raise ValueError("DENY_" + tag + "_FIELDS")

STEP_COMMANDS = {
    "atlas_check": [sys.executable, str(LAB / "tools/atlas_graph.py"), "ARM32", "TLB"],
    "unit_checks": [sys.executable, "-m", "unittest", "discover", "-s",
                    str(LAB / "tests"), "-p", "test_*.py"],
    "ci_host_smoke": [sys.executable, str(LAB / "tools/run_lab.py"), "--smoke"]
}
REPOSITORY = "rafaelmeloreisnovo/RafGitTools"

def pipeline_contract(p):
    fields(p, ("schema", "id", "version", "repository", "allowed_scope",
               "steps", "claim_allowed", "release_allowed"), "PIPELINE")
    if (p["schema"] != "bridge.pipeline.v1" or
        p["repository"] != REPOSITORY or p["id"] != "LAB-QUALITY-001" or
        p["allowed_scope"] != "LOCAL_CI_READONLY" or
        p["claim_allowed"] is not False or p["release_allowed"] is not False):
        raise ValueError("DENY_PIPELINE_PRIVILEGES")
    stages = p["steps"]
    if (type(p["version"]) is not int or not 1 <= p["version"] <= 10000
        or not isinstance(stages, list) or not 1 <= len(stages) <= 3
        or len(set(stages)) != len(stages)
        or any(step not in STEP_COMMANDS for step in stages)):
        raise ValueError("DENY_PIPELINE_STEPS")
    return p

def request_contract(r, p):
    fields(r, ("schema", "request_id", "repository", "module", "operation",
               "pipeline_id", "pipeline_sha256", "desired_scope", "state",
               "data_class", "approval_id", "claim_allowed"), "REQUEST")
    if (r["schema"] != "bridge.request.v1"
        or not isinstance(r["request_id"], str)
        or len(r["request_id"]) > 64 or not r["request_id"].startswith("REQ-")
        or r["repository"] != REPOSITORY or r["module"] != "LAB_RAPIDTOOLS_OMEGA"
        or r["operation"] != "run_ci_local" or r["pipeline_id"] != p["id"]
        or r["pipeline_sha256"] != sha(p)
        or r["desired_scope"] != "LOCAL_CI_READONLY"
        or r["state"] != "PROPOSED" or r["data_class"] != "SYNTHETIC"
        or r["approval_id"] is not None or r["claim_allowed"] is not False):
        raise ValueError("DENY_REQUEST_SCOPE_OR_STALE_PIPELINE")
    return r

def approval_contract(a, r, p):
    fields(a, ("schema", "request_id", "request_sha256", "pipeline_sha256",
               "decision", "scope", "source", "valid_until_utc",
               "nonce"), "APPROVAL")
    if (a["schema"] != "bridge.approval.v1"
        or a["request_id"] != r["request_id"]
        or a["request_sha256"] != sha(r)
        or a["pipeline_sha256"] != sha(p)
        or a["scope"] != "LOCAL_CI_READONLY"
        or a["decision"] != "APPROVE"
        or a["source"] != "EXPLICIT_CHAT_USER_TURN"
        or not isinstance(a["nonce"], str) or len(a["nonce"]) < 16):
        raise ValueError("DENY_APPROVAL_BINDING")
    try:
        expiry = datetime.fromisoformat(a["valid_until_utc"].replace("Z","+00:00"))
    except (TypeError, ValueError) as e:
        raise ValueError("DENY_APPROVAL_EXPIRY") from e
    if expiry.tzinfo is None or datetime.now(timezone.utc) > expiry:
        raise ValueError("DENY_APPROVAL_EXPIRED")
    # This verifies a file contract, not human identity or nonrepudiation.
    # Never accept this file as authority for remote writes / privileged agents.
    return a

def run_steps(p):
    events = []
    env = {"PATH": os.environ.get("PATH", "/usr/bin:/bin"),
           "HOME": os.environ.get("HOME", "/tmp"), "LANG": "C.UTF-8"}
    for step in p["steps"]:
        result = subprocess.run(STEP_COMMANDS[step], cwd=LAB, env=env,
                                stdin=subprocess.DEVNULL,
                                capture_output=True, text=True, timeout=180)
        events.append({
            "step": step, "exit_code": result.returncode,
            "stdout_sha256": hashlib.sha256(result.stdout.encode()).hexdigest(),
            "stderr_sha256": hashlib.sha256(result.stderr.encode()).hexdigest(),
            "output_tail": (result.stdout + result.stderr)[-500:]
        })
        if result.returncode != 0:
            return events, False
    return events, True

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pipeline", default=str(PIPELINE))
    parser.add_argument("--request", default=str(REQUEST))
    parser.add_argument("--approval", default=None)
    parser.add_argument("--check-request", action="store_true")
    parser.add_argument("--run-safe", action="store_true")
    parser.add_argument("--out-dir", default=str(LAB / "build/bridge_receipts"))
    args = parser.parse_args()
    data = {"schema": "bridge.receipt.v1", "state": "DENIED",
            "source": "CI_LOCAL_ONLY", "request_sha256": None,
            "pipeline_sha256": None, "events": [],
            "actor_verified": False, "remote_write_allowed": False,
            "claim_allowed": False, "release_allowed": False,
            "external_anchor": "TOKEN_VAZIO"}
    try:
        p = pipeline_contract(load(args.pipeline))
        data["pipeline_sha256"] = sha(p)
        if args.check_request or args.approval is not None:
            req = request_contract(load(args.request), p)
            data["request_sha256"] = sha(req)
        if args.approval:
            approval_contract(load(args.approval), req, p)
        if args.check_request and not args.approval:
            data["state"] = "HOLD_APPROVAL_REQUIRED"
        elif args.run_safe or args.approval:
            events, ok = run_steps(p)
            data["events"] = events
            data["state"] = "PASS_HOST_CI" if ok else "FAIL_HOST_CI"
        else:
            raise ValueError("DENY_NO_ACTION")
    except (ValueError, OSError, subprocess.TimeoutExpired) as exc:
        data["state"] = "DENIED"
        data["reason"] = str(exc)
    folder = Path(args.out_dir)
    folder.mkdir(parents=True, exist_ok=True)
    target = folder / (str(time.time_ns()) + ".json")
    with target.open("x", encoding="utf-8") as h:
        json.dump(data, h, indent=2, sort_keys=True)
        h.write("\n")
    print("MIDDLEFIELD_RECEIPT " + json.dumps({
        "state": data["state"], "pipeline_sha256": data["pipeline_sha256"],
        "request_sha256": data["request_sha256"],
        "receipt_sha256": sha(data), "steps": [e["step"] for e in data["events"]],
        "claim_allowed": False
    }, sort_keys=True))
    return 0 if data["state"] in ("HOLD_APPROVAL_REQUIRED", "PASS_HOST_CI") else 3

if __name__ == "__main__":
    sys.exit(main())
