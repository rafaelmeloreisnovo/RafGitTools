#!/usr/bin/env python3
from __future__ import annotations
import argparse, csv, json
from pathlib import Path


def validate(rows):
    errors = []
    ids = set()
    last = -1
    for row in rows:
        item = row["id"]
        seq = int(row["sequence"])
        pred = row["predecessor"]
        if item in ids:
            errors.append(f"duplicate:{item}")
        if seq <= last:
            errors.append(f"sequence:{item}")
        if pred and pred not in ids:
            errors.append(f"predecessor:{item}:{pred}")
        if row["state"] == "SUPERSEDED" and row["normative"] != "false":
            errors.append(f"superseded_normative:{item}")
        ids.add(item)
        last = seq
    return errors


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--table", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    with args.table.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    errors = validate(rows)
    report = {"status":"PASS" if not errors else "FAIL", "records":len(rows), "errors":errors}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
