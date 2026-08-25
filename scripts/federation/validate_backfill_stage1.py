#!/usr/bin/env python3
from __future__ import annotations
import argparse, json
from pathlib import Path

ALLOWED = {"PARTIAL", "TOKEN_VAZIO", "BLOCKED", "OPEN_GAP", "CONTRADICTION", "CLOSED"}
REQUIRED = {"id", "state", "owner", "next_action", "exit_criteria"}


def validate(data):
    errors = []
    if data.get("profile_id") != "RAFAELIA-BACKFILL-STAGE-1":
        errors.append("profile_id")
    if data.get("claim_allowed") is not False:
        errors.append("claim_allowed")
    if data.get("automatic_merge") is not False:
        errors.append("automatic_merge")
    records = data.get("records")
    if not isinstance(records, list) or not records:
        return errors + ["records"]
    ids = set()
    for item in records:
        missing = REQUIRED - set(item)
        if missing:
            errors.append(f"{item.get('id','?')}:missing:{','.join(sorted(missing))}")
            continue
        if item["id"] in ids:
            errors.append(f"duplicate:{item['id']}")
        ids.add(item["id"])
        if item["state"] not in ALLOWED:
            errors.append(f"{item['id']}:state")
        for field in ("owner", "next_action", "exit_criteria"):
            if not isinstance(item[field], str) or not item[field].strip():
                errors.append(f"{item['id']}:{field}")
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    data = json.loads(args.profile.read_text(encoding="utf-8"))
    errors = validate(data)
    report = {"status": "PASS" if not errors else "FAIL", "errors": errors, "records": len(data.get("records", [])), "claim_allowed": False}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
