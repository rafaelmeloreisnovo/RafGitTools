#!/usr/bin/env python3
"""Bounded Android lint debt ratchet for the Privacy Wizard hotfix.

This does NOT make the 68 pre-existing lint errors disappear. It refuses a
missing/malformed report, any new error in Privacy sources, or growth above
the exact observed global error count. Warnings stay visible and counted.
Historical debt must be fixed in separately reviewed, bounded PRs.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

PRIVACY_PREFIXES = (
    "/ui/screens/privacy/",
    "/data/privacy/",
)
OBSERVED_LEGACY_ERROR_CEILING = 68


def inspect_lint_xml(xml_text: str, maximum: int = OBSERVED_LEGACY_ERROR_CEILING) -> dict:
    if maximum < 0:
        raise ValueError("invalid approved historical lint error ceiling")
    root = ET.fromstring(xml_text)
    if root.tag != "issues":
        raise ValueError("unexpected lint XML root")
    issues = list(root.findall("issue"))
    errors = []
    warnings = 0
    hints = 0
    privacy_errors = []
    for issue in issues:
        severity = (issue.get("severity") or "").lower()
        if severity == "error" or severity == "fatal":
            errors.append(issue)
            locations = [loc.get("file", "").replace("\\", "/")
                         for loc in issue.findall("location")]
            if any(any(prefix in path for prefix in PRIVACY_PREFIXES)
                   for path in locations):
                privacy_errors.append({
                    "id": issue.get("id", "UNKNOWN"),
                    "locations": [path for path in locations if
                                  any(prefix in path for prefix in PRIVACY_PREFIXES)]
                })
        elif severity == "warning":
            warnings += 1
        else:
            hints += 1
    reasons = []
    if len(errors) > maximum:
        reasons.append("ANDROID_LINT_ERROR_COUNT_REGRESSION")
    if privacy_errors:
        reasons.append("PRIVACY_SOURCE_LINT_ERRORS")
    return {
        "schema": "RAFGITTOOLS_SCOPED_LINT_RATCHET_V1",
        "status": "PASS_SCOPED_LEGACY_DEBT" if not reasons else "FAIL",
        "claim_allowed": False,
        "legacy_global_errors_observed": len(errors),
        "legacy_global_error_ceiling": maximum,
        "global_errors_resolved": len(errors) == 0,
        "warnings_observed": warnings,
        "hints_observed": hints,
        "privacy_errors": privacy_errors,
        "reasons": reasons,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xml", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--max-existing-errors", type=int,
                        default=OBSERVED_LEGACY_ERROR_CEILING)
    args = parser.parse_args()
    try:
        receipt = inspect_lint_xml(args.xml.read_text(encoding="utf-8"),
                                   args.max_existing_errors)
    except (OSError, UnicodeError, ValueError, ET.ParseError) as error:
        receipt = {"schema": "RAFGITTOOLS_SCOPED_LINT_RATCHET_V1",
                   "status": "FAIL", "claim_allowed": False,
                   "reasons": ["LINT_REPORT_UNAVAILABLE_OR_MALFORMED"],
                   "detail": str(error)[:200]}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(receipt, ensure_ascii=False,
                                     sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, ensure_ascii=False, sort_keys=True))
    return 0 if receipt["status"] == "PASS_SCOPED_LEGACY_DEBT" else 2


if __name__ == "__main__":
    raise SystemExit(main())
