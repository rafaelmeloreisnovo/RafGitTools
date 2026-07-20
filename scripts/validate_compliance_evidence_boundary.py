#!/usr/bin/env python3
"""Fail-closed source contract for evidence-backed compliance reporting."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Mapping

ROOT = Path(__file__).resolve().parents[1]
COMPLIANCE = Path("app/src/main/kotlin/com/rafgittools/core/compliance/ComplianceManager.kt")
MAKEFILE = Path("rafaelia/block1/Makefile")
REPORT = Path("artifacts/compliance-evidence-boundary.json")


def load(root: Path = ROOT) -> dict[str, str]:
    return {
        "compliance": (root / COMPLIANCE).read_text(encoding="utf-8"),
        "makefile": (root / MAKEFILE).read_text(encoding="utf-8"),
    }


def validate_sources(files: Mapping[str, str]) -> list[str]:
    errors: list[str] = []
    source = files["compliance"]
    makefile = files["makefile"]

    required = (
        "ComplianceStandard.entries.associateWith(::evaluateStandard)",
        "EmptyComplianceEvidenceProvider",
        "ComplianceLevel.NOT_ASSESSED",
        "AssessmentState.TOKEN_VAZIO",
        "claimAllowed = false",
        "evidenceProvider.evidenceFor(standard)",
        "evidence.isStructurallyValid()",
        "evidenceRefs.isNotEmpty()",
        "satisfiedCriteria in 0..totalCriteria",
        "do not claim conformity",
    )
    for token in required:
        if token not in source:
            errors.append(f"missing evidence boundary token: {token}")

    forbidden = (
        "getComplianceStatus()[standard]!!",
        "user!!.",
        "username!!",
        "val hasQAProcess = true",
        "val hasReviewProcess = true",
        "val hasTestingProcess = true",
        "val hasVersionControl = true",
        "val hasChangeManagement = true",
        "val hasReleaseManagement = true",
        "implemented = true",
    )
    for token in forbidden:
        if token in source:
            errors.append(f"forbidden unsupported assertion: {token}")

    if "ComplianceLevel.NOT_ASSESSED" not in source or "TOKEN_VAZIO: no evidence package supplied" not in source:
        errors.append("absence must map to NOT_ASSESSED/TOKEN_VAZIO")
    if "percentage = 0" not in source or "assessmentState = AssessmentState.TOKEN_VAZIO" not in source:
        errors.append("unassessed percentage must carry explicit assessment state")
    if "lastAuditDate = Date(0)" not in source:
        errors.append("unobserved audit date must not use current time")
    if "val claimAllowed: Boolean = false" not in source:
        errors.append("public report/status claim boundary missing")

    make_required = (
        "-std=c11",
        "-Wpedantic",
        ".PHONY: all check clean",
        "check: all",
        "tick=42 attractor=",
        "$(CPPFLAGS) $(CFLAGS)",
        "$(LDFLAGS)",
        "$(LDLIBS)",
    )
    for token in make_required:
        if token not in makefile:
            errors.append(f"block1 Makefile missing: {token}")
    cflags_line = next((line for line in makefile.splitlines() if line.startswith("CFLAGS")), "")
    if "-fno-exceptions" in cflags_line:
        errors.append("CFLAGS contains C++-only -fno-exceptions")
    if "-lm" in cflags_line:
        errors.append("link library appears in compile flags")

    return errors


def build_report(root: Path = ROOT) -> dict[str, object]:
    errors = validate_sources(load(root))
    return {
        "schema": "rafgittools.compliance-evidence-boundary-validation.v1",
        "status": "PASS" if not errors else "FAIL",
        "assessment_default": "NOT_ASSESSED",
        "absence_state": "TOKEN_VAZIO",
        "claim_allowed": False,
        "certification_claimed": False,
        "block1_build_contract": "C11_WITH_DETERMINISTIC_SMOKE_GATE",
        "errors": errors,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--strict", action="store_true")
    parser.add_argument("--write-report", action="store_true")
    args = parser.parse_args()
    report = build_report(args.root)
    text = json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.write_report:
        path = args.root / REPORT
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    print(text, end="")
    return 1 if args.strict and report["status"] != "PASS" else 0


if __name__ == "__main__":
    raise SystemExit(main())
