#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path

PROFILE_ID = "RAFAELIA-SESSION-LINEAGE-1"


def digest(value):
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def validate(data):
    errors = []
    if data.get("profile_id") != PROFILE_ID:
        errors.append("profile_id")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed")
    if data.get("automatic_merge") is not False:
        errors.append("automatic_merge")
    if data.get("bridge_active_now") is not False:
        errors.append("bridge_active_now")
    if data.get("drive_periodic_custody_now") is not True:
        errors.append("drive_periodic_custody_now")
    if data.get("every_zip_fully_inspected") != "TOKEN_VAZIO":
        errors.append("every_zip_fully_inspected")
    return errors


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--profile", type=Path, required=True)
    p.add_argument("--report", type=Path, required=True)
    args = p.parse_args()
    data = json.loads(args.profile.read_text(encoding="utf-8"))
    errors = validate(data)
    report = {
        "profile_id": data.get("profile_id"),
        "status": "PASS" if not errors else "FAIL",
        "errors": errors,
        "sha256": digest(data),
        "boundary": "historical bridge is not current runtime evidence"
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
