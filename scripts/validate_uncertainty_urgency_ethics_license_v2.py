#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from validate_uncertainty_urgency_ethics_license import validate_contract as validate_v1_contract

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_V1 = ROOT / "configs" / "uncertainty-urgency-ethics-license.v1.json"
DEFAULT_V2 = ROOT / "configs" / "uncertainty-urgency-ethics-license.v2.json"
EXPECTED_V1_BLOB = "45630c859b29dcc0e58242fd424798bedcd5688a"
EXPECTED_DELTA_IDS = [f"AR{i:02d}" for i in range(31, 41)]
EXPECTED_URGENCY_IDS = {
    "TV-V2-LICENSE-PRODUCER-001",
    "TV-V2-GAIA-COMPLEX-EXEC-001",
    "TV-V2-NOISE-NULL-001",
    "TV-V2-B7-T2-001",
    "TV-V2-PARABLE-LINK-001",
    "TV-V2-RELATION-COVERAGE-001",
}
EXPECTED_ROUTE = [
    "statistics",
    "tokens",
    "metaphors",
    "vectors_words",
    "promise_contract",
    "execution",
    "receipt",
    "delta",
    "omega_n",
]
EXPECTED_SIGNAL_TESTS = {
    "recurrence_across_independent_windows",
    "direction_or_asymmetry_persistence",
    "autocorrelation_or_cross_correlation_against_frozen_baseline",
    "distribution_shift_against_declared_null_model",
    "reproducibility_under_new_seed_source_or_run",
    "adversarial_falsifier_not_triggered",
}
EXPECTED_RELATION_FIELDS = {
    "DERIVES_FROM",
    "IMPLEMENTS",
    "EXECUTES",
    "MEASURES",
    "SUPPORTS",
    "CONTRADICTS",
    "CORRESPONDS_TO",
    "ANALOGY_OF",
    "INDEXES",
    "ROUTES_TO",
    "HAS_GAP",
    "BLOCKED_BY",
    "SUPERSEDES_WITHOUT_ERASING",
    "LICENSED_BY",
    "REQUIRES_REVIEW",
}
EXPECTED_CLOSURE_FIELDS = {
    "source_identity",
    "repo_ref_path_hash_or_drive_revision",
    "gap_id",
    "method_or_test",
    "inputs",
    "runtime_environment",
    "gate_result",
    "falsifier_result",
    "rights_state_if_material",
    "output_or_receipt_hash",
    "predecessor_link",
}


def load_json(path: Path) -> dict[str, Any]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(obj, dict):
        raise ValueError(f"{path}: root must be an object")
    return obj


def validate_v2(v2: dict[str, Any]) -> list[str]:
    errors: list[str] = []

    if v2.get("schema") != "rafaelia.uncertainty-urgency-ethics-license.v2":
        errors.append("wrong V2 schema")
    if v2.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("V2 authority drift")
    for key in ("claim_allowed", "automatic_promotion", "automatic_merge", "direct_main_mutation"):
        if v2.get(key) is not False:
            errors.append(f"{key} must remain false")

    extends = v2.get("extends", {})
    if extends.get("path") != "configs/uncertainty-urgency-ethics-license.v1.json":
        errors.append("V2 must extend the canonical V1 contract")
    if extends.get("git_blob_sha") != EXPECTED_V1_BLOB:
        errors.append("V1 predecessor blob drift")
    if "preserve AR01..AR30 verbatim" not in str(extends.get("policy", "")):
        errors.append("V2 must explicitly preserve AR01..AR30")

    if v2.get("reconstructible_route") != EXPECTED_ROUTE:
        errors.append("reconstructible route drift")

    noise = v2.get("noise_residual_policy", {})
    if noise.get("residual_is_signal_by_default") is not False:
        errors.append("residual must fail closed as signal")
    if noise.get("correlation_is_causality") is not False:
        errors.append("correlation must not imply causality")
    if noise.get("similarity_is_equivalence") is not False:
        errors.append("similarity must not imply equivalence")
    tests = noise.get("structured_signal_candidate_requires")
    if not isinstance(tests, list) or set(tests) != EXPECTED_SIGNAL_TESTS:
        errors.append("structured-signal falsifier/test set drift")
    states = noise.get("states")
    if not isinstance(states, list) or "TOKEN_VAZIO" not in states or "REFUTED" not in states:
        errors.append("noise state machine must retain TOKEN_VAZIO and REFUTED")

    rights = v2.get("rights_policy_v2", {})
    for key in (
        "public_access_is_public_domain",
        "public_access_is_redistribution_permission",
        "public_access_is_training_permission",
        "repository_license_covers_third_party_payloads_automatically",
    ):
        if rights.get(key) is not False:
            errors.append(f"rights fail-closed invariant missing: {key}")
    if rights.get("unknown_permissions_fail_closed") is not True:
        errors.append("unknown permissions must fail closed")
    if rights.get("license_conflict_preserves_reference_provenance_and_gap") is not True:
        errors.append("license conflict must preserve custody")
    rights_states = set(rights.get("states", []))
    expected_rights_states = {
        "LICENSE_CLEAR",
        "LICENSE_CONDITIONAL",
        "LICENSE_CONFLICT",
        "TOKEN_VAZIO_LICENSE",
    }
    if rights_states != expected_rights_states:
        errors.append("V2 rights state set drift")

    ethics = v2.get("ethics_by_design_v2", {})
    if ethics.get("high_risk_requires_human_review") is not True:
        errors.append("high risk must require human review")
    if ethics.get("mutating_action_requires_rollback_or_irreversible_boundary") is not True:
        errors.append("mutation must require rollback or explicit irreversible boundary")
    if ethics.get("scientific_validity_waives_rights") is not False:
        errors.append("scientific validity must not waive rights")
    if ethics.get("technical_executability_equals_ethical_authorization") is not False:
        errors.append("technical execution must not imply ethical authorization")

    parable = v2.get("parable_bridge_v2", {})
    if parable.get("evidence_effect") != "NONE":
        errors.append("parable evidence effect must remain NONE")
    if parable.get("technical_target_required") is not True:
        errors.append("parable must require a technical target")
    limits = set(parable.get("real_limits_remain_binding", []))
    if not {"hardware", "mathematics", "license", "safety", "evidence", "authority", "runtime"}.issubset(limits):
        errors.append("parable must preserve real operational limits")

    relations = v2.get("relation_types_v2")
    if not isinstance(relations, list) or set(relations) != EXPECTED_RELATION_FIELDS:
        errors.append("typed relation set drift")

    producers = v2.get("producer_anchors", {})
    gaia = producers.get("gaia_complex_feedback", {})
    if gaia.get("repo") != "rafaelmeloreisnovo/GAIA_phi":
        errors.append("GAIA producer repo drift")
    if gaia.get("commit") != "d3f49c10b74f740ee2024314dff91e9a0ef20b2f":
        errors.append("GAIA producer commit drift")
    if gaia.get("path") != "dados/cognitive_symbiotic.py":
        errors.append("GAIA producer path drift")
    if gaia.get("execution_state") != "TOKEN_VAZIO_EXECUTION" or gaia.get("claim_allowed") is not False:
        errors.append("GAIA execution/claim boundary drift")

    b7 = producers.get("rafaelia_b7", {})
    if b7.get("bridge_gap") != "B7_TO_T2_BRIDGE":
        errors.append("B7 bridge gap identity drift")
    if b7.get("bridge_state") != "TOKEN_VAZIO_BRIDGE" or b7.get("claim_allowed") is not False:
        errors.append("B7 bridge must remain TOKEN_VAZIO and claim-disallowed")

    urgency = v2.get("urgency_queue")
    if not isinstance(urgency, list):
        errors.append("urgency_queue must be a list")
    else:
        ids = {item.get("id") for item in urgency if isinstance(item, dict)}
        if ids != EXPECTED_URGENCY_IDS:
            errors.append("urgency ID set drift")
        priorities = [item.get("priority") for item in urgency if isinstance(item, dict)]
        if priorities.count("P0") != 3 or priorities.count("P1") != 3:
            errors.append("urgency priorities must remain 3xP0 and 3xP1")
        for item in urgency:
            if not isinstance(item, dict):
                errors.append("urgency entry must be an object")
                continue
            state = item.get("state")
            if not isinstance(state, str) or not state.startswith("TOKEN_VAZIO"):
                errors.append(f"{item.get('id')}: urgency state must remain TOKEN_VAZIO until receipt-backed closure")
            if not item.get("next_gate"):
                errors.append(f"{item.get('id')}: next_gate required")

    delta = v2.get("anti_regression_invariants_delta")
    if not isinstance(delta, list):
        errors.append("anti_regression_invariants_delta must be a list")
    else:
        ids = [item.get("id") for item in delta if isinstance(item, dict)]
        if ids != EXPECTED_DELTA_IDS:
            errors.append("V2 anti-regression delta must be exactly AR31..AR40 in order")
        if any(not item.get("rule") for item in delta if isinstance(item, dict)):
            errors.append("every V2 anti-regression invariant requires a rule")

    closure = v2.get("token_closure_required_fields")
    if not isinstance(closure, list) or set(closure) != EXPECTED_CLOSURE_FIELDS:
        errors.append("TOKEN_VAZIO closure receipt field set drift")

    mirror = v2.get("drive_editorial_mirror", {})
    if mirror.get("file_id") != "1QrJmd7xsd8-zJVfNr8MMoHsFf-X-WvNXPmHTR_SlyRg":
        errors.append("Drive editorial mirror ID drift")
    if not mirror.get("revision_id"):
        errors.append("Drive editorial mirror revision required")

    if v2.get("mapa_routing_target") != "indices/RAFAELIA_NOISE_UNCERTAINTY_ETHICS_LICENSE_ROUTING_V1.md":
        errors.append("Mapa routing target drift")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate RAFAELIA uncertainty/urgency/ethics/license V2 anti-regression contract")
    parser.add_argument("--v1", type=Path, default=DEFAULT_V1)
    parser.add_argument("--v2", type=Path, default=DEFAULT_V2)
    args = parser.parse_args()

    v1 = load_json(args.v1)
    v2 = load_json(args.v2)

    errors = [f"V1: {e}" for e in validate_v1_contract(v1)]
    errors.extend(f"V2: {e}" for e in validate_v2(v2))

    if errors:
        print("FAIL uncertainty/urgency/ethics/license V2 anti-regression")
        for error in errors:
            print(f"- {error}")
        return 1

    print("PASS uncertainty/urgency/ethics/license V2 anti-regression")
    print("v1_invariants=30 v2_delta=10 total_guarded=40")
    print("claim_allowed=false automatic_merge=false direct_main_mutation=false")
    print("urgencies=6 p0=3 p1=3")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
