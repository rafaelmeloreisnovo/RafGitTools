#!/usr/bin/env python3
"""Active bridge request consumer for low-privilege GitHub CI only.

Dynamically chooses exactly the registered REQ-NNNN. Never lists arbitrary
folders, accepts commands, reads secrets or obtains Drive API credentials.
"""
import json
from pathlib import Path
import re
import subprocess
import sys

HERE = Path(__file__).resolve().parent


def active_request():
    data = json.loads((HERE / "active.json").read_text(encoding="utf-8"))
    if (set(data) != {"schema", "request_id"} or
        data["schema"] != "bridge.active.v1" or
        not isinstance(data["request_id"], str) or
        not re.fullmatch(r"REQ-[0-9]{4}", data["request_id"])):
        raise ValueError("DENY_ACTIVE_POINTER")
    return data["request_id"]


def main():
    try:
        ident = active_request()
    except (ValueError, OSError) as exc:
        print("BRIDGE_DENIED", str(exc))
        return 3
    request = HERE / "inbox" / (ident + ".json")
    approval = HERE / "approvals" / (ident + ".json")

    # Append-only consumed marker enforces local replay stop. This file references
    # the one verified prior CI run; a later approval cannot re-run this request.
    consumed = HERE / "consumed" / (ident + ".json")
    if consumed.exists():
        try:
            from bridge import load, pipeline_contract, request_contract, sha, PIPELINE
            record = load(consumed)
            expected_fields = {"schema", "request_id", "state",
                               "request_sha256", "pipeline_sha256",
                               "result", "run_id", "commit_sha", "receipt_sha256"}
            p = pipeline_contract(load(PIPELINE))
            r = request_contract(load(request), p)
            if (set(record) != expected_fields or
                record["schema"] != "bridge.consumption.v1" or
                record["request_id"] != ident or record["state"] != "CONSUMED" or
                record["result"] != "PASS_HOST_CI" or
                record["request_sha256"] != sha(r) or
                record["pipeline_sha256"] != sha(p) or
                not isinstance(record["run_id"], int) or
                not isinstance(record["commit_sha"], str) or
                len(record["commit_sha"]) != 40 or
                not isinstance(record["receipt_sha256"], str) or
                len(record["receipt_sha256"]) != 64):
                raise ValueError("DENY_CONSUMPTION_INTEGRITY")
        except (ValueError, OSError, KeyError, TypeError) as exc:
            print("BRIDGE_DENIED", str(exc))
            return 3
        print("BRIDGE_PATH=HOLD_ALREADY_CONSUMED")
        print("BRIDGE_RECEIPT=PREVIOUSLY_EXECUTED_RUN_" + str(record["run_id"]))
        return 0
    command = [sys.executable, str(HERE / "bridge.py"),
               "--request", str(request)]
    if approval.exists():
        command.extend(["--approval", str(approval)])
        print("BRIDGE_PATH=APPROVAL_FILE_PRESENT_REVALIDATE")
    else:
        command.append("--check-request")
        print("BRIDGE_PATH=HOLD_PENDING_HUMAN_APPROVAL")
    result = subprocess.run(command, cwd=HERE.parent, check=False,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            text=True, timeout=200)
    print(result.stdout, end="")
    if result.stderr:
        print(result.stderr[-300:], file=sys.stderr)
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
