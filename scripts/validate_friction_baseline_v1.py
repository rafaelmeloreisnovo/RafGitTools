#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "data" / "governance" / "friction-baseline.v1.json"
LICENSE = ROOT / "data" / "governance" / "license-friction-matrix.v1.json"
USES = ROOT / "data" / "governance" / "ethics-by-design-use-registry.v1.json"
RELATIONS = ROOT / "data" / "governance" / "complex-network-design-relations.v1.json"
PROVIDER = ROOT / "data" / "evidence" / "github" / "gaia-provider-execution-friction-20260822.v1.json"
REQUIRED_RELATION_FIELDS = {
    "relation_id", "from", "relation_type", "to", "source_ref", "authority_domain",
    "uncertainty_state", "evidence_effect", "boundary_or_falsifier", "next_gate",
}


def load(path: Path) -> dict[str, Any]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(obj, dict):
        raise ValueError(f"{path}: root must be object")
    return obj


def value(component: dict[str, Any]) -> Any:
    return component.get("value")


def validate(baseline: dict[str, Any], license_obj: dict[str, Any], uses: dict[str, Any], relations: dict[str, Any], provider: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if baseline.get("schema") != "rafaelia.friction-baseline.v1":
        errors.append("wrong baseline schema")
    if baseline.get("claim_allowed") is not False:
        errors.append("baseline claim_allowed must remain false")
    if baseline.get("state") != "PARTIAL_BASELINE":
        errors.append("baseline must remain PARTIAL_BASELINE while a component is unmeasured")

    components = baseline.get("components", {})
    required = {
        "unresolved_gap_count", "unbound_license_unit_count",
        "provider_pre_step_failure_count", "untyped_relation_count",
        "missing_rollback_count", "missing_provenance_count", "manual_rework_count",
    }
    if set(components) != required:
        errors.append("friction component set drift")
        return errors

    unknown_license = sum(
        1 for unit in license_obj.get("units", [])
        if str(unit.get("compatibility_state", "")).startswith("TOKEN_VAZIO")
    )
    if value(components["unbound_license_unit_count"]) != unknown_license:
        errors.append("unbound_license_unit_count does not reconcile")

    zero_step_attempts = sum(
        1 for attempt in provider.get("attempts", [])
        if attempt.get("exposed_steps_count") == 0 and attempt.get("test_execution_observed") is False
    )
    if value(components["provider_pre_step_failure_count"]) != zero_step_attempts:
        errors.append("provider_pre_step_failure_count does not reconcile")

    untyped = 0
    missing_relation_provenance = 0
    for rel in relations.get("relations", []):
        if REQUIRED_RELATION_FIELDS - set(rel):
            untyped += 1
        if not rel.get("source_ref"):
            missing_relation_provenance += 1
    if value(components["untyped_relation_count"]) != untyped:
        errors.append("untyped_relation_count does not reconcile")

    missing_rollback = sum(
        1 for use in uses.get("uses", [])
        if not use.get("rollback_or_irreversible_boundary")
    )
    if value(components["missing_rollback_count"]) != missing_rollback:
        errors.append("missing_rollback_count does not reconcile")

    missing_use_provenance = sum(1 for use in uses.get("uses", []) if not use.get("provenance"))
    expected_missing_provenance = missing_use_provenance + missing_relation_provenance
    if value(components["missing_provenance_count"]) != expected_missing_provenance:
        errors.append("missing_provenance_count does not reconcile")

    unresolved = components["unresolved_gap_count"]
    unresolved_value = value(unresolved)
    unresolved_basis = unresolved.get("basis")
    if not isinstance(unresolved_basis, list) or unresolved_value != len(unresolved_basis):
        errors.append("unresolved_gap_count must reconcile to explicit basis list")

    manual = value(components["manual_rework_count"])
    if not isinstance(manual, str) or not manual.startswith("TOKEN_VAZIO"):
        errors.append("manual_rework_count must remain TOKEN_VAZIO until a definition/window exists")

    aggregate = baseline.get("aggregate", {})
    if isinstance(manual, str) and manual.startswith("TOKEN_VAZIO"):
        if aggregate.get("numeric_total") != "TOKEN_VAZIO_SCORE":
            errors.append("numeric total must remain TOKEN_VAZIO_SCORE while a component is unmeasured")
    if aggregate.get("zero_vector_would_imply_claim") is not False:
        errors.append("zero friction vector must not imply claim")
    if "identical scope_id" not in str(aggregate.get("comparison_rule", "")):
        errors.append("baseline comparisons must preserve identical scope or rebaseline")

    closure = baseline.get("closure", {})
    if not closure.get("residual_gap"):
        errors.append("partial baseline must preserve residual gap")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate bounded friction baseline reconciliation")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    baseline, license_obj, uses, relations, provider = map(load, [BASELINE, LICENSE, USES, RELATIONS, PROVIDER])
    errors = validate(baseline, license_obj, uses, relations, provider)
    report = {
        "schema": "rafaelia.friction-baseline-validation.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "scope_id": baseline.get("scope_id"),
        "errors": errors,
        "components": baseline.get("components"),
        "aggregate": baseline.get("aggregate"),
        "boundary": "Component reconciliation only; friction is not truth, legal authorization, scientific evidence or claim promotion."
    }
    text = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
