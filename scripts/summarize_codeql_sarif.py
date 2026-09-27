#!/usr/bin/env python3
"""Summarize CodeQL SARIF without exposing source excerpts or secret values."""
from __future__ import annotations

import argparse
import collections
import json
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.codeql-sarif-summary.v1"

def summarize(paths: list[Path]) -> dict[str, Any]:
    by_rule: collections.Counter[str] = collections.Counter()
    by_level: collections.Counter[str] = collections.Counter()
    result_count = 0
    runs_seen = 0
    parse_errors: list[str] = []

    for path in paths:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            parse_errors.append(f"{path.name}:{type(exc).__name__}")
            continue
        for run in data.get("runs", []):
            if not isinstance(run, dict):
                continue
            runs_seen += 1
            for result in run.get("results", []) or []:
                if not isinstance(result, dict):
                    continue
                result_count += 1
                rule = str(result.get("ruleId") or "TOKEN_VAZIO_RULE_ID")
                level = str(result.get("level") or "TOKEN_VAZIO_LEVEL")
                by_rule[rule] += 1
                by_level[level] += 1

    state = "OBSERVED"
    if not paths:
        state = "TOKEN_VAZIO_NO_SARIF"
    elif parse_errors:
        state = "AUDIT_PARSE_ERROR"

    return {
        "schema": SCHEMA,
        "state": state,
        "claim_allowed": False,
        "privacy_mode": "SANITIZED_COUNTS_ONLY",
        "source_file_count": len(paths),
        "runs_seen": runs_seen,
        "result_count": result_count,
        "rule_count": len(by_rule),
        "by_level": dict(sorted(by_level.items())),
        "by_rule": [
            {"rule_id": rule, "count": count}
            for rule, count in sorted(by_rule.items(), key=lambda item: (-item[1], item[0]))
        ],
        "parse_errors": parse_errors,
        "excluded_fields": ["message", "locations", "codeFlows", "stacks", "secret_values"],
    }

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    paths = sorted(args.input_dir.glob("*.sarif")) if args.input_dir.exists() else []
    summary = summarize(paths)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({
        "state": summary["state"],
        "result_count": summary["result_count"],
        "rule_count": summary["rule_count"],
        "by_level": summary["by_level"],
        "claim_allowed": False,
    }, sort_keys=True))
    return 2 if summary["state"] == "AUDIT_PARSE_ERROR" else 0

if __name__ == "__main__":
    raise SystemExit(main())
