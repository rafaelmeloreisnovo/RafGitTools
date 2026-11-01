#!/usr/bin/env python3
"""Cloud-only bounded custody receipt for RafGitTools CI; no Drive tokens or remote actions.

Produces evidence ABOUT a GitHub job, not independent certification/approval.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import tempfile
import time

SCHEMA = "rafaelia.cloud_custody.v1"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
RUN = re.compile(r"^[1-9][0-9]{0,18}$")
STATES = {"success": "PASS_HOST_CI", "failure": "FAIL_HOST_CI",
          "cancelled": "CANCELLED", "BLOCKED": "BLOCKED"}


def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=False).encode("utf-8")


def digest(obj):
    return hashlib.sha256(canonical(obj)).hexdigest()


def utc_now():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def meta(env):
    sha = env.get("CUSTODY_SOURCE_SHA") or env.get("GITHUB_SHA", "")
    run = env.get("GITHUB_RUN_ID", "")
    attempt = env.get("GITHUB_RUN_ATTEMPT", "")
    repo = env.get("GITHUB_REPOSITORY", "")
    if not SHA40.fullmatch(sha):
        raise ValueError("TOKEN_VAZIO_SOURCE_SHA")
    if not RUN.fullmatch(run) or not RUN.fullmatch(attempt):
        raise ValueError("TOKEN_VAZIO_PROVIDER_RUN_ID")
    if repo != "rafaelmeloreisnovo/RafGitTools":
        raise ValueError("DENY_WRONG_PRODUCER")
    return {"source_sha": sha, "run_id": int(run), "attempt": int(attempt),
            "repository": repo}


def validate_start(p):
    if not isinstance(p, dict) or set(p) != {"schema", "metadata", "start_utc", "monotonic_ns"}:
        raise ValueError("DENY_START_FIELDS")
    if p["schema"] != SCHEMA or not isinstance(p["metadata"], dict):
        raise ValueError("DENY_START_SCHEMA")
    m = p["metadata"]
    if set(m) != {"source_sha", "run_id", "attempt", "repository"}:
        raise ValueError("DENY_METADATA_FIELDS")
    if not SHA40.fullmatch(m["source_sha"]) or m["repository"] != "rafaelmeloreisnovo/RafGitTools":
        raise ValueError("DENY_SOURCE_BINDING")
    if not isinstance(p["start_utc"], str) or not p["start_utc"].endswith("Z"):
        raise ValueError("DENY_START_TIME")
    if type(p["monotonic_ns"]) is not int or p["monotonic_ns"] < 0:
        raise ValueError("DENY_MONOTONIC_START")
    if type(m["run_id"]) is not int or m["run_id"] <= 0 or type(m["attempt"]) is not int or m["attempt"] <= 0:
        raise ValueError("DENY_PROVIDER_IDS")
    return p


def finish_record(start, status, end_ns, end_utc):
    validate_start(start)
    if status not in STATES:
        raise ValueError("DENY_STATUS")
    if type(end_ns) is not int or end_ns < start["monotonic_ns"]:
        raise ValueError("DENY_NEGATIVE_ELAPSED")
    elapsed_ms = (end_ns - start["monotonic_ns"]) // 1000000
    return {"schema": SCHEMA, "metadata": start["metadata"],
            "started_at_utc": start["start_utc"], "ended_at_utc": end_utc,
            "duration_ms": elapsed_ms, "benchmark": {"metric": "job_elapsed_wall_ms",
                                                     "value": elapsed_ms, "source": "CI_RUNNER_MONOTONIC"},
            "gate_result": STATES[status],
            "pause": {"state": "PAUSED_AT_REVIEW_GATE", "runner_kept_alive": False},
            "custody": {"github_artifact": "PENDING_UPLOAD",
                        "drive_persistence": "TOKEN_VAZIO_UNTIL_PROVIDER_READBACK",
                        "google_vault_hold": "TOKEN_VAZIO_NOT_VERIFIED",
                        "independent_certification": "TOKEN_VAZIO_NOT_VERIFIED"},
            "authority": {"human_identity_verified": False, "provider_enforcement_verified": False,
                          "external_auditor_verified": False},
            "claim_allowed": False, "release_allowed": False, "deletion_allowed": False,
            "next": "HOLD_FOR_HUMAN_AND_PROVIDER_REVIEW"}


def record_valid(r):
    if not isinstance(r, dict) or r.get("schema") != SCHEMA or r.get("claim_allowed") is not False:
        raise ValueError("DENY_RECEIPT")
    if r.get("deletion_allowed") is not False or r.get("release_allowed") is not False:
        raise ValueError("DENY_PREMATURE_DELETION_RELEASE")
    if r.get("gate_result") not in STATES.values():
        raise ValueError("DENY_GATE_RESULT")
    if r.get("custody", {}).get("independent_certification") != "TOKEN_VAZIO_NOT_VERIFIED":
        raise ValueError("DENY_FAKE_CERTIFICATION")
    if r.get("custody", {}).get("drive_persistence") != "TOKEN_VAZIO_UNTIL_PROVIDER_READBACK":
        raise ValueError("DENY_FAKE_DRIVE_CUSTODY")
    if r.get("pause", {}).get("runner_kept_alive") is not False:
        raise ValueError("DENY_FAKE_PAUSE")
    return r


def write_new(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("x", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, sort_keys=True)
        f.write("\n")


def selftest():
    start = {"schema": SCHEMA, "metadata": {"source_sha": "a" * 40, "run_id": 10,
             "attempt": 1, "repository": "rafaelmeloreisnovo/RafGitTools"},
             "start_utc": "2026-11-01T07:00:00.000Z", "monotonic_ns": 1_000_000}
    checks = 0
    def accept(condition):
        nonlocal checks
        assert condition
        checks += 1
    def deny(fn):
        nonlocal checks
        try:
            fn()
        except ValueError:
            checks += 1
            return
        raise AssertionError("NEGATIVE_MUTATION_WAS_ACCEPTED")
    r = finish_record(start, "success", 11_000_000, "2026-11-01T07:00:00.010Z")
    accept(record_valid(r)["gate_result"] == "PASS_HOST_CI")
    accept(r["duration_ms"] == 10)
    accept(r["pause"]["runner_kept_alive"] is False)
    accept(len(digest(r)) == 64)
    accept(r["authority"]["external_auditor_verified"] is False)
    deny(lambda: finish_record(start, "certified", 11_000_000, "Z"))
    deny(lambda: finish_record(start, "success", 10, "Z"))
    deny(lambda: meta({"GITHUB_SHA": "b" * 40, "GITHUB_RUN_ID": "1",
                       "GITHUB_RUN_ATTEMPT": "1", "GITHUB_REPOSITORY": "other/repo"}))
    deny(lambda: meta({"GITHUB_SHA": "", "GITHUB_RUN_ID": "1",
                       "GITHUB_RUN_ATTEMPT": "1", "GITHUB_REPOSITORY": "rafaelmeloreisnovo/RafGitTools"}))
    deny(lambda: validate_start(dict(start, injected_command="curl | sh")))
    deny(lambda: record_valid(dict(r, claim_allowed=True)))
    deny(lambda: record_valid(dict(r, deletion_allowed=True)))
    deny(lambda: record_valid(dict(r, release_allowed=True)))
    deny(lambda: record_valid(dict(r, custody=dict(r["custody"], independent_certification="CERTIFIED"))))
    deny(lambda: record_valid(dict(r, custody=dict(r["custody"], drive_persistence="PASS"))))
    deny(lambda: record_valid(dict(r, pause=dict(r["pause"], runner_kept_alive=True))))
    return checks


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--start", action="store_true")
    p.add_argument("--finish", action="store_true")
    p.add_argument("--selftest", action="store_true")
    p.add_argument("--status", default="BLOCKED")
    p.add_argument("--out-dir", default=os.environ.get("RUNNER_TEMP", "/tmp") + "/rafaelia-cloud-custody")
    args = p.parse_args()
    if args.selftest:
        print("CLOUD_CUSTODY_SELFTEST=PASS", "checks=" + str(selftest()))
        return 0
    if args.start == args.finish:
        p.error("exactly one of --start or --finish")
    out = Path(args.out_dir)
    start_path = out / "start.json"
    result_path = out / "custody-receipt.json"
    if args.start:
        obj = {"schema": SCHEMA, "metadata": meta(os.environ),
               "start_utc": utc_now(), "monotonic_ns": time.monotonic_ns()}
        validate_start(obj)
        write_new(start_path, obj)
        print("CUSTODY_START_UTC=" + obj["start_utc"])
        print("CUSTODY_START_SHA256=" + digest(obj))
    else:
        if not start_path.is_file() or start_path.stat().st_size > 4096:
            raise ValueError("TOKEN_VAZIO_START_EVENT")
        start = json.loads(start_path.read_text(encoding="utf-8"))
        if start["metadata"] != meta(os.environ):
            raise ValueError("DENY_SOURCE_RUN_DRIFT")
        record = record_valid(finish_record(start, args.status,
                    time.monotonic_ns(), utc_now()))
        write_new(result_path, record)
        print("CUSTODY_END_UTC=" + record["ended_at_utc"])
        print("CUSTODY_DURATION_MS=" + str(record["duration_ms"]))
        print("CUSTODY_GATE_RESULT=" + record["gate_result"])
        print("CUSTODY_RECEIPT_SHA256=" + digest(record))
        print("CUSTODY_EXTERNAL_CERTIFICATION=TOKEN_VAZIO_NOT_VERIFIED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
