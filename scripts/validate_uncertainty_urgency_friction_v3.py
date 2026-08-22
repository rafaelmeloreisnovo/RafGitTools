#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from validate_uncertainty_urgency_ethics_license import validate_contract as validate_v1_contract
from validate_uncertainty_urgency_ethics_license_v2 import validate_v2

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_V1 = ROOT / "configs" / "uncertainty-urgency-ethics-license.v1.json"
DEFAULT_V2 = ROOT / "configs" / "uncertainty-urgency-ethics-license.v2.json"
DEFAULT_V3 = ROOT / "configs" / "uncertainty-urgency-friction-ethics-license.v3.json"
DEFAULT_LICENSE = ROOT / "data" / "governance" / "license-friction-matrix.v1.json"
DEFAULT_USES = ROOT / "data" / "governance" / "ethics-by-design-use-registry.v1.json"
DEFAULT_RELATIONS = ROOT / "data" / "governance" / "complex-network-design-relations.v1.json"
DEFAULT_PROVIDER = ROOT / "data" / "evidence" / "github" / "gaia-provider-execution-friction-20260822.v1.json"
EXPECTED_V2_BLOB = "c4766b2ad8aaf782a0d1f6b5b33ae134d2944efe"
EXPECTED_AR = [f"AR{i}" for i in range(41, 53)]
EXPECTED_FRICTION_CLASSES = {
    "LICENSE_RIGHTS",
    "PROVIDER_RUNNER",
    "AUTHORITY_ROUTING",
    "SEMANTIC_AMBIGUITY",
    "IMPLEMENTATION_BINDING",
    "RUNTIME_REPRODUCIBILITY",
    "EVIDENCE_GAP",
    "RELATION_UNCERTAINTY",
    "ROLLBACK_IRREVERSIBILITY",
    "PRIVACY_MINIMIZATION",
}
REQUIRED_LICENSE_FIELDS = {
    "artifact_id", "origin", "owner", "license_id_or_token_vazio",
    "spdx_or_exact_text_ref", "redistribution_allowed", "modification_allowed",
    "training_allowed", "commercial_use_allowed", "attribution_required",
    "share_alike_required", "compatibility_state", "proof_reference", "scope_boundary",
}
REQUIRED_USE_FIELDS = {
    "use_id", "purpose", "necessity", "minimization", "risk_class", "human_review",
    "rights_state", "privacy_state", "rollback_or_irreversible_boundary", "provenance",
    "evidence_state", "claim_allowed",
}
REQUIRED_RELATION_FIELDS = {
    "relation_id", "from", "relation_type", "to", "source_ref", "authority_domain",
    "uncertainty_state", "evidence_effect", "boundary_or_falsifier", "next_gate",
}
ALLOWED_EFFECTS = {"NONE", "ROUTING_ONLY", "SUPPORT_LIMITED", "CONTRADICTS", "EVIDENCES_BOUNDED"}


def load_json(path: Path) -> dict[str, Any]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(obj, dict):
        raise ValueError(f"{path}: root must be object")
    return obj


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_v3(v3: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if v3.get("schema") != "rafaelia.uncertainty-urgency-friction-ethics-license.v3":
        errors.append("wrong V3 schema")
    if v3.get("authority") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("V3 authority drift")
    for key in ("claim_allowed", "automatic_promotion", "automatic_merge", "direct_main_mutation"):
        if v3.get(key) is not False:
            errors.append(f"{key} must remain false")

    extends = v3.get("extends", {})
    if extends.get("path") != "configs/uncertainty-urgency-ethics-license.v2.json":
        errors.append("V3 must extend canonical V2")
    if extends.get("git_blob_sha") != EXPECTED_V2_BLOB:
        errors.append("V2 predecessor blob drift")
    if set(v3.get("friction_classes", [])) != EXPECTED_FRICTION_CLASSES:
        errors.append("friction class set drift")

    rule = v3.get("friction_rule", {})
    for key in (
        "friction_reduction_is_permission_broadening",
        "friction_reduction_is_claim_promotion",
        "friction_reduction_is_history_rewrite",
    ):
        if rule.get(key) is not False:
            errors.append(f"friction invariant must remain false: {key}")
    if rule.get("partial_closure_preserves_residual_gap") is not True:
        errors.append("partial closure must preserve residual gap")

    provider = v3.get("provider_execution_classifier", {})
    states = set(provider.get("states", []))
    for required in ("PROVIDER_PRE_STEP_FAILURE", "TEST_EXECUTED_PASS", "TEST_EXECUTED_FAIL", "TOKEN_VAZIO_PROVIDER_EXECUTION"):
        if required not in states:
            errors.append(f"provider state missing: {required}")

    license_policy = v3.get("license_unit_policy", {})
    if license_policy.get("unknown_permission_default") is not False:
        errors.append("unknown license permission must fail closed")
    if license_policy.get("repository_license_covers_dependencies_automatically") is not False:
        errors.append("repository license cannot cover dependencies automatically")
    if license_policy.get("repository_license_covers_datasets_or_weights_automatically") is not False:
        errors.append("repository license cannot cover data/weights automatically")

    ethics = v3.get("ethics_by_design_v3", {})
    if ethics.get("high_risk_requires_human_review") is not True:
        errors.append("high risk must require human review")
    if ethics.get("mutating_action_requires_rollback_or_irreversible_boundary") is not True:
        errors.append("mutation must have rollback or irreversible boundary")
    if ethics.get("technical_execution_equals_authorization") is not False:
        errors.append("execution must not imply authorization")

    relations = v3.get("complex_relation_policy", {})
    if relations.get("analogy_evidence_effect") != "NONE":
        errors.append("analogy evidence effect must remain NONE")
    if relations.get("cooccurrence_evidence_effect") != "NONE":
        errors.append("co-occurrence evidence effect must remain NONE")
    if relations.get("similarity_is_equivalence") is not False:
        errors.append("similarity must not imply equivalence")
    if relations.get("decorative_edges_forbidden_in_evidence_graph") is not True:
        errors.append("decorative evidence edges must remain forbidden")

    parable = v3.get("parable_reference_policy", {})
    if parable.get("parable_evidence_effect") != "NONE":
        errors.append("parable evidence effect must remain NONE")
    if parable.get("technical_target_required") is not True:
        errors.append("parable technical target required")

    measurement = v3.get("friction_measurement", {})
    if measurement.get("scope_change_requires_rebaseline") is not True:
        errors.append("scope change must require friction rebaseline")
    if measurement.get("zero_friction_does_not_imply_claim") is not True:
        errors.append("zero friction must not imply claim")

    urgency = v3.get("urgency_queue_v3")
    if not isinstance(urgency, list) or len(urgency) != 6:
        errors.append("V3 urgency queue must contain six bounded entries")
    else:
        ids = [item.get("id") for item in urgency if isinstance(item, dict)]
        if len(ids) != len(set(ids)):
            errors.append("duplicate V3 urgency id")
        if sum(1 for item in urgency if item.get("priority") == "P0") != 3:
            errors.append("V3 urgency queue must retain three P0 entries")
        for item in urgency:
            if not item.get("next_gate"):
                errors.append(f"{item.get('id')}: next_gate required")

    delta = v3.get("anti_regression_invariants_delta")
    ids = [item.get("id") for item in delta] if isinstance(delta, list) else []
    if ids != EXPECTED_AR:
        errors.append("V3 anti-regression delta must be exactly AR41..AR52")
    return errors


def validate_license_matrix(obj: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if obj.get("claim_allowed") is not False:
        errors.append("license matrix claim_allowed must be false")
    units = obj.get("units")
    if not isinstance(units, list) or not units:
        return errors + ["license matrix units required"]
    seen: set[str] = set()
    for unit in units:
        missing = REQUIRED_LICENSE_FIELDS - set(unit)
        if missing:
            errors.append(f"license unit missing fields: {sorted(missing)}")
        aid = unit.get("artifact_id")
        if aid in seen:
            errors.append(f"duplicate license artifact_id: {aid}")
        seen.add(aid)
        if str(unit.get("compatibility_state", "")).startswith("TOKEN_VAZIO"):
            for key in ("redistribution_allowed", "modification_allowed", "training_allowed", "commercial_use_allowed"):
                if unit.get(key) is not False:
                    errors.append(f"{aid}: unknown rights must fail closed for {key}")
    return errors


def validate_use_registry(obj: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if obj.get("claim_allowed") is not False:
        errors.append("use registry claim_allowed must be false")
    uses = obj.get("uses")
    if not isinstance(uses, list) or not uses:
        return errors + ["use registry entries required"]
    seen: set[str] = set()
    for use in uses:
        missing = REQUIRED_USE_FIELDS - set(use)
        if missing:
            errors.append(f"use missing fields: {sorted(missing)}")
        uid = use.get("use_id")
        if uid in seen:
            errors.append(f"duplicate use_id: {uid}")
        seen.add(uid)
        if use.get("claim_allowed") is not False:
            errors.append(f"{uid}: claim_allowed must remain false")
        if use.get("risk_class") == "high" and "required" not in str(use.get("human_review", "")):
            errors.append(f"{uid}: high risk requires human review")
        if not use.get("rollback_or_irreversible_boundary"):
            errors.append(f"{uid}: rollback/boundary required")
    return errors


def validate_relation_graph(obj: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if obj.get("claim_allowed") is not False:
        errors.append("relation graph claim_allowed must be false")
    policy = obj.get("graph_policy", {})
    if policy.get("decorative_edges_allowed") is not False:
        errors.append("decorative edges must be forbidden")
    relations = obj.get("relations")
    if not isinstance(relations, list) or not relations:
        return errors + ["relations required"]
    seen: set[str] = set()
    for rel in relations:
        missing = REQUIRED_RELATION_FIELDS - set(rel)
        if missing:
            errors.append(f"relation missing fields: {sorted(missing)}")
        rid = rel.get("relation_id")
        if rid in seen:
            errors.append(f"duplicate relation_id: {rid}")
        seen.add(rid)
        effect = rel.get("evidence_effect")
        if effect not in ALLOWED_EFFECTS:
            errors.append(f"{rid}: invalid evidence effect {effect}")
        if rel.get("relation_type") == "ANALOGY_OF" and effect != "NONE":
            errors.append(f"{rid}: analogy must have evidence_effect NONE")
        if not rel.get("source_ref"):
            errors.append(f"{rid}: source_ref required")
        if not rel.get("boundary_or_falsifier"):
            errors.append(f"{rid}: boundary/falsifier required")
        if not rel.get("next_gate"):
            errors.append(f"{rid}: next_gate required")
    stats = obj.get("network_statistics", {})
    if stats.get("node_count") != len(obj.get("nodes", [])):
        errors.append("node_count does not reconcile")
    if stats.get("relation_count") != len(relations):
        errors.append("relation_count does not reconcile")
    return errors


def validate_provider_evidence(obj: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if obj.get("claim_allowed") is not False:
        errors.append("provider evidence claim_allowed must be false")
    target = obj.get("target_workflow", {})
    steps = target.get("exposed_steps_count")
    observed = target.get("test_execution_observed")
    classification = target.get("classification")
    if steps == 0 and observed is not False:
        errors.append("zero exposed steps cannot claim observed test execution")
    if steps == 0 and classification == "TEST_EXECUTED_FAIL":
        errors.append("zero exposed steps cannot be classified as test failure")
    if observed is False and obj.get("state") != "TOKEN_VAZIO_PROVIDER_EXECUTION":
        errors.append("unobserved execution must remain TOKEN_VAZIO_PROVIDER_EXECUTION")
    if obj.get("retry", {}).get("requested") is not True:
        errors.append("provider evidence must preserve retry request state")
    return errors


def validate_all(v1: dict[str, Any], v2: dict[str, Any], v3: dict[str, Any], license_obj: dict[str, Any], uses: dict[str, Any], relations: dict[str, Any], provider: dict[str, Any]) -> list[str]:
    errors = [f"V1: {e}" for e in validate_v1_contract(v1)]
    errors += [f"V2: {e}" for e in validate_v2(v2)]
    errors += [f"V3: {e}" for e in validate_v3(v3)]
    errors += [f"LICENSE: {e}" for e in validate_license_matrix(license_obj)]
    errors += [f"USE: {e}" for e in validate_use_registry(uses)]
    errors += [f"REL: {e}" for e in validate_relation_graph(relations)]
    errors += [f"PROVIDER: {e}" for e in validate_provider_evidence(provider)]
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate RAFAELIA V3 friction/ethics/license anti-regression")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    paths = [DEFAULT_V1, DEFAULT_V2, DEFAULT_V3, DEFAULT_LICENSE, DEFAULT_USES, DEFAULT_RELATIONS, DEFAULT_PROVIDER]
    v1, v2, v3, license_obj, uses, relations, provider = [load_json(p) for p in paths]
    errors = validate_all(v1, v2, v3, license_obj, uses, relations, provider)
    report = {
        "schema": "rafaelia.friction-v3-validation-report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "errors": errors,
        "v1_invariants": 30,
        "v2_invariants": 10,
        "v3_invariants": 12,
        "total_guarded_invariants": 52,
        "license_units": len(license_obj.get("units", [])),
        "governed_uses": len(uses.get("uses", [])),
        "relation_nodes": len(relations.get("nodes", [])),
        "relations": len(relations.get("relations", [])),
        "provider_execution_state": provider.get("state"),
        "sha256": {str(p.relative_to(ROOT)): sha256(p) for p in paths},
        "boundary": "Contract and registry consistency only; not scientific truth, legal advice, provider root-cause proof, global graph completeness or claim promotion."
    }
    text = json.dumps(report, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8")
    print(text, end="")
    return 0 if not errors else 1


if __name__ == "__main__":
    raise SystemExit(main())
