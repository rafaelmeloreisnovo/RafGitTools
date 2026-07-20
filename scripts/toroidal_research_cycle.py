#!/usr/bin/env python3
"""Validate RAFAELIA toroidal research-cycle contracts and manifests.

The toroidal/sine language is operational: it models recurrence, phase and
feedback. It never promotes a geometry or reference waveform into physical
truth. The validator is dependency-free and fail-closed.
"""

from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path
from typing import Any

CONTRACT_SCHEMA = "rafaelia.toroidal-research-cycle-contract.v1"
MANIFEST_SCHEMA = "rafaelia.toroidal-research-cycle-manifest.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
EXPECTED_PHASES = [
    "VOID", "QUERY", "SOURCE", "CLAIM", "FORMULA", "TEST",
    "EVIDENCE", "RESIDUAL", "NEW_VOID", "FEEDBACK",
]
EXPECTED_AXES = ["inner_research_cycle", "federation_cycle"]
PROMOTED_CLAIM_STATES = {"SUPPORTED_LIMITED", "RESTRICTED"}
HARD_RESIDUAL_SEVERITIES = {"HARD_BLOCK", "CRITICAL"}
POSITIVE_TEST_STATES = {"EXECUTED_PASS"}


class CycleError(ValueError):
    """Raised when a contract or manifest violates a cycle invariant."""


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise CycleError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise CycleError(f"invalid JSON in {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise CycleError(f"{path}: root must be an object")
    return value


def nonempty(value: Any, field: str, errors: list[str]) -> None:
    if not isinstance(value, str) or not value.strip():
        errors.append(f"{field} must be a non-empty string")


def unique(items: Any, field: str, errors: list[str]) -> dict[str, dict[str, Any]]:
    if not isinstance(items, list):
        errors.append(f"{field} must be an array")
        return {}
    result: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            errors.append(f"{field}[{index}] must be an object")
            continue
        identifier = item.get("id")
        if not isinstance(identifier, str) or not identifier:
            errors.append(f"{field}[{index}].id must be a non-empty string")
            continue
        if identifier in result:
            errors.append(f"duplicate {field} id: {identifier}")
            continue
        result[identifier] = item
    return result


def refs(values: Any, allowed: set[str], field: str, errors: list[str]) -> list[str]:
    if not isinstance(values, list):
        errors.append(f"{field} must be an array")
        return []
    output: list[str] = []
    seen: set[str] = set()
    for value in values:
        if not isinstance(value, str) or not value:
            errors.append(f"{field} entries must be non-empty strings")
            continue
        if value in seen:
            errors.append(f"{field} contains duplicate reference: {value}")
            continue
        seen.add(value)
        output.append(value)
        if value not in allowed:
            errors.append(f"{field} references unknown id: {value}")
    return output


def validate_contract(contract: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []
    if contract.get("schema") != CONTRACT_SCHEMA:
        errors.append(f"schema must be {CONTRACT_SCHEMA!r}")

    authority = contract.get("authority")
    if not isinstance(authority, dict):
        errors.append("authority must be an object")
    elif authority.get("repository") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("authority.repository must be rafaelmeloreisnovo/RafGitTools")

    phases = contract.get("cycle_phases")
    if phases != EXPECTED_PHASES:
        errors.append("cycle_phases must match the canonical VOID..FEEDBACK order")

    edges = contract.get("required_cycle_edges")
    expected_edges = [[EXPECTED_PHASES[i], EXPECTED_PHASES[(i + 1) % len(EXPECTED_PHASES)]] for i in range(len(EXPECTED_PHASES))]
    if edges != expected_edges:
        errors.append("required_cycle_edges must close the canonical toroidal loop exactly")

    if contract.get("toroidal_state_axes") != EXPECTED_AXES:
        errors.append("toroidal_state_axes must be inner_research_cycle/federation_cycle")

    sine = contract.get("sine_reference_policy")
    if not isinstance(sine, dict):
        errors.append("sine_reference_policy must be an object")
    else:
        if sine.get("pure_sine_is_universal_stabilizer") is not False:
            errors.append("pure sine must not be declared a universal stabilizer")
        if sine.get("feedback_required") is not True:
            errors.append("sine reference policy must require feedback")
        if sine.get("damping_or_boundedness_required") is not True:
            errors.append("sine reference policy must require damping or boundedness")
        purposes = sine.get("allowed_purpose")
        if not isinstance(purposes, list) or not purposes:
            errors.append("sine_reference_policy.allowed_purpose must be non-empty")

    roles = unique(contract.get("repository_roles"), "repository_roles", errors)
    repositories: set[str] = set()
    for role_id, role in roles.items():
        nonempty(role.get("authority_repository"), f"repository_roles.{role_id}.authority_repository", errors)
        nonempty(role.get("responsibility"), f"repository_roles.{role_id}.responsibility", errors)
        repository = role.get("authority_repository")
        if isinstance(repository, str):
            if repository in repositories:
                errors.append(f"duplicate repository authority: {repository}")
            repositories.add(repository)
    for required in ("GOVERNANCE", "MAP", "SCIENCE", "ORCHESTRATION"):
        if required not in roles:
            errors.append(f"missing required repository role: {required}")

    for field in ("source_classes", "claim_states", "formula_states", "test_states", "promotion_requirements"):
        value = contract.get(field)
        if not isinstance(value, list) or not value:
            errors.append(f"{field} must be a non-empty array")
    if TOKEN_VAZIO not in set(contract.get("claim_states", [])):
        errors.append("claim_states must include TOKEN_VAZIO")
    if TOKEN_VAZIO not in set(contract.get("formula_states", [])):
        errors.append("formula_states must include TOKEN_VAZIO")
    if TOKEN_VAZIO not in set(contract.get("test_states", [])):
        errors.append("test_states must include TOKEN_VAZIO")

    aggregation = contract.get("aggregation")
    if not isinstance(aggregation, dict):
        errors.append("aggregation must be an object")
    else:
        if aggregation.get("non_compensatory") is not True:
            errors.append("aggregation must be non-compensatory")
        if aggregation.get("missing_value") != TOKEN_VAZIO:
            errors.append("aggregation.missing_value must be TOKEN_VAZIO")
        if aggregation.get("claim_allowed_default") is not False:
            errors.append("claim_allowed_default must be false")

    if errors:
        raise CycleError("\n".join(f"- {error}" for error in errors))
    return {
        "roles": roles,
        "source_classes": set(contract["source_classes"]),
        "claim_states": set(contract["claim_states"]),
        "formula_states": set(contract["formula_states"]),
        "test_states": set(contract["test_states"]),
        "sine_purposes": set(contract["sine_reference_policy"]["allowed_purpose"]),
    }


def validate_manifest(contract: dict[str, Any], manifest: dict[str, Any]) -> dict[str, Any]:
    compiled = validate_contract(contract)
    errors: list[str] = []
    if manifest.get("schema") != MANIFEST_SCHEMA:
        errors.append(f"schema must be {MANIFEST_SCHEMA!r}")
    for field in ("manifest_id", "title", "observed_at", "declared_scope"):
        nonempty(manifest.get(field), field, errors)

    repositories = unique(manifest.get("repositories"), "repositories", errors)
    queries = unique(manifest.get("queries"), "queries", errors)
    sources = unique(manifest.get("sources"), "sources", errors)
    claims = unique(manifest.get("claims"), "claims", errors)
    formulas = unique(manifest.get("formulas"), "formulas", errors)
    tests = unique(manifest.get("tests"), "tests", errors)
    evidence = unique(manifest.get("evidence"), "evidence", errors)
    residuals = unique(manifest.get("residuals"), "residuals", errors)
    vazio = unique(manifest.get("token_vazio_ledger"), "token_vazio_ledger", errors)

    repository_ids = set(repositories)
    query_ids = set(queries)
    source_ids = set(sources)
    claim_ids = set(claims)
    formula_ids = set(formulas)
    test_ids = set(tests)
    evidence_ids = set(evidence)
    residual_ids = set(residuals)

    authority_by_repo = {role["authority_repository"]: role_id for role_id, role in compiled["roles"].items()}
    for repository_id, item in repositories.items():
        full_name = item.get("repository_full_name")
        role = item.get("role")
        nonempty(full_name, f"repositories.{repository_id}.repository_full_name", errors)
        if role not in compiled["roles"]:
            errors.append(f"repositories.{repository_id}.role is invalid: {role!r}")
        if isinstance(full_name, str) and full_name in authority_by_repo and authority_by_repo[full_name] != role:
            errors.append(f"repositories.{repository_id}: role does not match canonical authority")
        if item.get("state") not in {"ACTIVE", "ADAPTER_PLANNED", TOKEN_VAZIO}:
            errors.append(f"repositories.{repository_id}.state is invalid")

    cycle = manifest.get("cycle")
    if not isinstance(cycle, dict):
        errors.append("cycle must be an object")
        cycle = {}
    if cycle.get("phases") != EXPECTED_PHASES:
        errors.append("cycle.phases must match canonical order")
    transitions = cycle.get("transitions")
    expected_edges = [[EXPECTED_PHASES[i], EXPECTED_PHASES[(i + 1) % len(EXPECTED_PHASES)]] for i in range(len(EXPECTED_PHASES))]
    if transitions != expected_edges:
        errors.append("cycle.transitions must close the toroidal feedback loop")
    coordinates = cycle.get("coordinates")
    if not isinstance(coordinates, dict):
        errors.append("cycle.coordinates must be an object")
    else:
        for field in ("theta_rad", "phi_rad"):
            value = coordinates.get(field)
            if not isinstance(value, (int, float)) or not math.isfinite(value):
                errors.append(f"cycle.coordinates.{field} must be a finite number")

    sine = manifest.get("sine_reference")
    if not isinstance(sine, dict):
        errors.append("sine_reference must be an object")
    else:
        if sine.get("purpose") not in compiled["sine_purposes"]:
            errors.append("sine_reference.purpose is not allowed")
        for field in ("amplitude", "frequency_hz"):
            value = sine.get(field)
            if not isinstance(value, (int, float)) or not math.isfinite(value) or value <= 0:
                errors.append(f"sine_reference.{field} must be finite and positive")
        phase = sine.get("phase_rad")
        if not isinstance(phase, (int, float)) or not math.isfinite(phase):
            errors.append("sine_reference.phase_rad must be finite")
        for field in ("amplitude_unit", "feedback_model", "boundedness_model"):
            nonempty(sine.get(field), f"sine_reference.{field}", errors)
        if sine.get("universal_stabilizer_claim") is not False:
            errors.append("sine_reference.universal_stabilizer_claim must be false")

    for query_id, item in queries.items():
        nonempty(item.get("question"), f"queries.{query_id}.question", errors)
        nonempty(item.get("domain"), f"queries.{query_id}.domain", errors)
        if item.get("repository_ref") not in repository_ids:
            errors.append(f"queries.{query_id}.repository_ref references unknown repository")

    for source_id, item in sources.items():
        if item.get("class") not in compiled["source_classes"]:
            errors.append(f"sources.{source_id}.class is invalid")
        for field in ("title", "locator", "observed_at", "verification_status"):
            nonempty(item.get(field), f"sources.{source_id}.{field}", errors)
        refs(item.get("query_refs"), query_ids, f"sources.{source_id}.query_refs", errors)
        if item.get("repository_ref") not in repository_ids:
            errors.append(f"sources.{source_id}.repository_ref references unknown repository")

    vazio_fields = {record.get("field") for record in vazio.values() if isinstance(record.get("field"), str)}
    for record_id, item in vazio.items():
        for field in ("field", "reason", "owner", "next_action", "exit_criteria"):
            nonempty(item.get(field), f"token_vazio_ledger.{record_id}.{field}", errors)

    for claim_id, item in claims.items():
        state = item.get("state")
        if state not in compiled["claim_states"]:
            errors.append(f"claims.{claim_id}.state is invalid")
        nonempty(item.get("text"), f"claims.{claim_id}.text", errors)
        nonempty(item.get("scope"), f"claims.{claim_id}.scope", errors)
        source_refs = refs(item.get("source_refs"), source_ids, f"claims.{claim_id}.source_refs", errors)
        evidence_refs = refs(item.get("evidence_refs"), evidence_ids, f"claims.{claim_id}.evidence_refs", errors)
        refs(item.get("formula_refs"), formula_ids, f"claims.{claim_id}.formula_refs", errors)
        refs(item.get("test_refs"), test_ids, f"claims.{claim_id}.test_refs", errors)
        refs(item.get("residual_refs"), residual_ids, f"claims.{claim_id}.residual_refs", errors)
        if item.get("authority_repository_ref") not in repository_ids:
            errors.append(f"claims.{claim_id}.authority_repository_ref references unknown repository")
        for field in ("baseline", "falsifier", "uncertainty_model"):
            nonempty(item.get(field), f"claims.{claim_id}.{field}", errors)
        if state in PROMOTED_CLAIM_STATES and (not source_refs or not evidence_refs):
            errors.append(f"claims.{claim_id}: promoted state requires sources and evidence")
        if state == TOKEN_VAZIO:
            if source_refs or evidence_refs:
                errors.append(f"claims.{claim_id}: TOKEN_VAZIO cannot carry positive support")
            if f"claims.{claim_id}" not in vazio_fields:
                errors.append(f"claims.{claim_id}: TOKEN_VAZIO requires a ledger entry")

    for formula_id, item in formulas.items():
        state = item.get("state")
        if state not in compiled["formula_states"]:
            errors.append(f"formulas.{formula_id}.state is invalid")
        for field in ("expression", "declared_domain", "unit_contract"):
            nonempty(item.get(field), f"formulas.{formula_id}.{field}", errors)
        refs(item.get("source_refs"), source_ids, f"formulas.{formula_id}.source_refs", errors)
        test_refs = refs(item.get("test_refs"), test_ids, f"formulas.{formula_id}.test_refs", errors)
        if state in {"DIMENSIONAL_PASS", "TESTED_LIMITED"} and not test_refs:
            errors.append(f"formulas.{formula_id}: {state} requires a test reference")
        if state == TOKEN_VAZIO and f"formulas.{formula_id}" not in vazio_fields:
            errors.append(f"formulas.{formula_id}: TOKEN_VAZIO requires a ledger entry")

    for test_id, item in tests.items():
        state = item.get("state")
        if state not in compiled["test_states"]:
            errors.append(f"tests.{test_id}.state is invalid")
        for field in ("method", "environment", "expected_observable"):
            nonempty(item.get(field), f"tests.{test_id}.{field}", errors)
        refs(item.get("formula_refs"), formula_ids, f"tests.{test_id}.formula_refs", errors)
        refs(item.get("claim_refs"), claim_ids, f"tests.{test_id}.claim_refs", errors)
        if state in POSITIVE_TEST_STATES:
            nonempty(item.get("execution_receipt"), f"tests.{test_id}.execution_receipt", errors)
        if state == TOKEN_VAZIO and f"tests.{test_id}" not in vazio_fields:
            errors.append(f"tests.{test_id}: TOKEN_VAZIO requires a ledger entry")

    for evidence_id, item in evidence.items():
        if item.get("source_ref") not in source_ids:
            errors.append(f"evidence.{evidence_id}.source_ref references unknown source")
        if item.get("test_ref") not in test_ids:
            errors.append(f"evidence.{evidence_id}.test_ref references unknown test")
        for field in ("kind", "locator", "observed_at", "digest"):
            nonempty(item.get(field), f"evidence.{evidence_id}.{field}", errors)

    unresolved_hard = 0
    for residual_id, item in residuals.items():
        refs(item.get("claim_refs"), claim_ids, f"residuals.{residual_id}.claim_refs", errors)
        for field in ("description", "severity", "state", "next_action"):
            nonempty(item.get(field), f"residuals.{residual_id}.{field}", errors)
        if item.get("severity") in HARD_RESIDUAL_SEVERITIES and item.get("state") != "RESOLVED":
            unresolved_hard += 1

    claim_allowed = manifest.get("claim_allowed")
    if not isinstance(claim_allowed, bool):
        errors.append("claim_allowed must be boolean")
    if claim_allowed:
        if unresolved_hard:
            errors.append("claim_allowed=true is forbidden with unresolved hard residuals")
        if vazio:
            errors.append("claim_allowed=true is forbidden while TOKEN_VAZIO ledger is non-empty")
        if any(item.get("state") not in PROMOTED_CLAIM_STATES for item in claims.values()):
            errors.append("claim_allowed=true requires every claim to be in a promoted state")
        if any(item.get("state") not in POSITIVE_TEST_STATES for item in tests.values()):
            errors.append("claim_allowed=true requires every test to be EXECUTED_PASS")

    if errors:
        raise CycleError("\n".join(f"- {error}" for error in errors))

    promoted = sum(item.get("state") in PROMOTED_CLAIM_STATES for item in claims.values())
    executed = sum(item.get("state") in POSITIVE_TEST_STATES for item in tests.values())
    return {
        "status": "PASS",
        "manifest_id": manifest["manifest_id"],
        "repository_count": len(repositories),
        "query_count": len(queries),
        "source_count": len(sources),
        "claim_count": len(claims),
        "promoted_claim_count": promoted,
        "formula_count": len(formulas),
        "test_count": len(tests),
        "executed_pass_count": executed,
        "evidence_count": len(evidence),
        "residual_count": len(residuals),
        "unresolved_hard_residuals": unresolved_hard,
        "token_vazio_count": len(vazio),
        "claim_allowed": claim_allowed,
        "cycle_closed": True,
    }


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    contract_parser = sub.add_parser("validate-contract")
    contract_parser.add_argument("contract", type=Path)
    manifest_parser = sub.add_parser("validate-manifest")
    manifest_parser.add_argument("contract", type=Path)
    manifest_parser.add_argument("manifest", type=Path)
    summary_parser = sub.add_parser("summarize")
    summary_parser.add_argument("contract", type=Path)
    summary_parser.add_argument("manifest", type=Path)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        contract = load_json(args.contract)
        if args.command == "validate-contract":
            validate_contract(contract)
            result = {"status": "PASS", "schema": CONTRACT_SCHEMA}
        else:
            manifest = load_json(args.manifest)
            result = validate_manifest(contract, manifest)
    except CycleError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    print(json.dumps(result, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
