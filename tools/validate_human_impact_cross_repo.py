#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "contracts/human_impact_cross_repo.v1.json"
DOC = ROOT / "docs/HUMAN_IMPACT_CROSS_REPO_GATE_V1.md"


def require(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)


def main():
    c = json.loads(CONTRACT.read_text(encoding="utf-8"))
    doc = DOC.read_text(encoding="utf-8")

    require(c["claim_allowed"] is False, "contract must not self-promote")
    require(c["authority"]["producer_truth_remains_with_producer"] is True, "producer authority drift")
    require(c["authority"]["live_cross_repo_ethics_receipt"] == "TOKEN_VAZIO", "live ethics receipt fabricated")

    inv = set(c["invariants"])
    for item in (
        "LOCAL_PASS != HUMAN_IMPACT_PASS",
        "TECHNICAL_CORRECTNESS != ETHICAL_PERMISSION",
        "MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION",
        "TOKEN_VAZIO != PASS",
        "UNKNOWN_RISK != SAFE",
        "PERFORMANCE_GAIN != RIGHTS_OVERRIDE",
        "FIXTURE != LIVE",
    ):
        require(item in inv, f"invariant missing: {item}")

    required = set(c["required_for_material_human_impact"])
    must = {
        "affected_people_or_groups",
        "protected_domains_touched",
        "unknown_risks",
        "distribution_of_benefit_and_harm",
        "privacy_data_surface",
        "child_impact",
        "health_impact",
        "education_impact",
        "culture_and_belief_impact",
        "environmental_impact",
        "consequence_radius",
        "reversibility_or_mitigation",
        "appeal_path",
        "review_roles",
        "evidence_refs",
        "falsifier",
        "decision_state",
    }
    require(must.issubset(required), "human-impact payload weakened")

    forbidden = c["forbidden_promotions"]
    for key, value in forbidden.items():
        require(value is True, f"forbidden promotion disabled: {key}")

    anti = c["anti_regression"]
    require(anti["latest_wins"] is False, "latest-wins regression")
    require(anti["backward_compatibility_or_versioned_migration_required"] is True, "migration guard removed")
    require(anti["rollback_required_for_incompatible_change"] is True, "rollback guard removed")
    require(anti["human_protection_may_not_be_silently_weakened"] is True, "human protection may silently weaken")

    for phrase in (
        "LOCAL_PASS != HUMAN_IMPACT_PASS",
        "MODEL_RECOMMENDATION != HUMAN_VALUE_DECISION",
        "TOKEN_VAZIO",
        "LATEST != STRONGER",
    ):
        require(phrase in doc, f"documentation invariant missing: {phrase}")

    print("PASS: cross-repo human impact gate v1")


if __name__ == "__main__":
    main()
