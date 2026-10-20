#!/usr/bin/env python3
"""Fail-closed gate for ChatGPT-derived operational correction requests."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SHA40 = re.compile(r"^[0-9a-f]{40}$")
SHA64 = re.compile(r"^[0-9a-f]{64}$")


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def evaluate(evidence: dict[str, Any], request: dict[str, Any], contract: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []

    if evidence.get("schema") != contract.get("accepted_evidence_schema"):
        errors.append("unsupported evidence schema")

    source = evidence.get("source", {})
    if source.get("source_surface") != "ChatGPT":
        errors.append("evidence source_surface must be ChatGPT")
    if source.get("training_use") is not False:
        errors.append("training_use must be false")
    if source.get("dataset_use") is not False:
        errors.append("dataset_use must be false")
    if source.get("operational_evidence_use") is not True:
        errors.append("operational_evidence_use must be true")
    if not SHA64.fullmatch(str(source.get("instruction_sha256_utf8", ""))):
        errors.append("invalid instruction digest")

    if evidence.get("claims", {}).get("allowed") is not False:
        errors.append("intake evidence must not self-authorize a claim")

    required = contract.get("automatic_correction", {}).get("required", [])
    for field in required:
        value = request.get(field)
        if value in (None, "", [], {}):
            errors.append(f"missing correction requirement: {field}")

    if request.get("validated_operational_evidence") is not True:
        errors.append("validated_operational_evidence must be true")

    revision = str(request.get("exact_source_revision", ""))
    if revision and not SHA40.fullmatch(revision):
        errors.append("exact_source_revision must be a 40-hex commit SHA")

    target = request.get("explicit_target")
    if target and (not isinstance(target, dict) or not target.get("repository") or not target.get("path")):
        errors.append("explicit_target must bind repository and path")

    patch = request.get("bounded_patch")
    if patch and isinstance(patch, dict):
        if patch.get("direct_main_write") is True:
            errors.append("direct main write forbidden")
        if patch.get("force_push") is True:
            errors.append("force push forbidden")
        if patch.get("auto_merge") is True:
            errors.append("auto merge forbidden")
    elif patch:
        errors.append("bounded_patch must be an object")

    if errors:
        state = "HOLD_EVIDENCE_ONLY"
        eligible = False
    else:
        state = "AUTO_CORRECT_ELIGIBLE"
        eligible = True

    return {
        "schema": "rafgittools.chatgpt-evidence-correction-gate.decision/v1",
        "state": state,
        "eligible": eligible,
        "errors": errors,
        "allowed_actions": contract["automatic_correction"]["allowed_actions"] if eligible else ["DRY_RUN"],
        "forbidden_actions": contract["automatic_correction"]["forbidden_actions"],
        "claim_allowed": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--evidence", type=Path, required=True)
    parser.add_argument("--request", type=Path, required=True)
    parser.add_argument(
        "--contract",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "contracts/chatgpt_evidence_correction_gate.v1.json",
    )
    args = parser.parse_args()
    result = evaluate(load(args.evidence), load(args.request), load(args.contract))
    print(json.dumps(result, indent=2, sort_keys=True))
    return 0 if result["eligible"] else 2


if __name__ == "__main__":
    raise SystemExit(main())
