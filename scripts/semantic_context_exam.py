#!/usr/bin/env python3
"""Dependency-free semantic/context examination inspired by seven operational parables.

This layer sits above model tokenization. It validates typed semantic objects,
context operations, transformations, provenance, execution state, and claim
boundaries. It never changes a model tokenizer and never converts TOKEN_VAZIO
into zero/false/null.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

CONTRACT_SCHEMA = "rafaelia.semantic-context-exam-contract.v1"
MANIFEST_SCHEMA = "rafaelia.semantic-context-exam.v1"
TOKEN_VAZIO = "TOKEN_VAZIO"
EXPECTED_GATES = [
    "G01_BALANCE",
    "G02_CARPENTER",
    "G03_BAMBOO",
    "G04_POTTER",
    "G05_LIBRARY",
    "G06_BLACKSMITH",
    "G07_DOOR",
]


class ExamError(ValueError):
    pass


def load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise ExamError(f"file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise ExamError(f"invalid JSON in {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise ExamError(f"{path}: root must be an object")
    return value


def _nonempty(value: Any, field: str, errors: list[str]) -> None:
    if not isinstance(value, str) or not value.strip():
        errors.append(f"{field} must be a non-empty string")


def _unique(items: Any, field: str, errors: list[str]) -> dict[str, dict[str, Any]]:
    if not isinstance(items, list):
        errors.append(f"{field} must be an array")
        return {}
    out: dict[str, dict[str, Any]] = {}
    for index, item in enumerate(items):
        if not isinstance(item, dict):
            errors.append(f"{field}[{index}] must be an object")
            continue
        identifier = item.get("id")
        if not isinstance(identifier, str) or not identifier:
            errors.append(f"{field}[{index}].id must be a non-empty string")
            continue
        if identifier in out:
            errors.append(f"duplicate {field} id: {identifier}")
            continue
        out[identifier] = item
    return out


def validate_contract(contract: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []
    if contract.get("schema") != CONTRACT_SCHEMA:
        errors.append(f"schema must be {CONTRACT_SCHEMA}")
    authority = contract.get("authority")
    if not isinstance(authority, dict) or authority.get("repository") != "rafaelmeloreisnovo/RafGitTools":
        errors.append("authority.repository must be rafaelmeloreisnovo/RafGitTools")

    gates = _unique(contract.get("parable_gates"), "parable_gates", errors)
    if list(gates) != EXPECTED_GATES:
        errors.append("parable_gates must contain G01..G07 in canonical order")
    for gate_id, gate in gates.items():
        _nonempty(gate.get("parable"), f"parable_gates.{gate_id}.parable", errors)
        if not isinstance(gate.get("checks"), list) or not gate["checks"]:
            errors.append(f"parable_gates.{gate_id}.checks must be non-empty")
        if not isinstance(gate.get("blocks"), list):
            errors.append(f"parable_gates.{gate_id}.blocks must be an array")

    for key in ("object_kinds", "operation_kinds", "epistemic_states", "delivery_required"):
        if not isinstance(contract.get(key), list) or not contract[key]:
            errors.append(f"{key} must be a non-empty array")

    if TOKEN_VAZIO not in contract.get("epistemic_states", []):
        errors.append("epistemic_states must include TOKEN_VAZIO")
    if errors:
        raise ExamError("\n".join(f"- {e}" for e in errors))
    return {
        "gates": gates,
        "object_kinds": set(contract["object_kinds"]),
        "operation_kinds": set(contract["operation_kinds"]),
        "epistemic_states": set(contract["epistemic_states"]),
        "delivery_required": list(contract["delivery_required"]),
    }


def _validate_refs(values: Any, allowed: set[str], field: str, errors: list[str]) -> list[str]:
    if not isinstance(values, list):
        errors.append(f"{field} must be an array")
        return []
    out: list[str] = []
    for value in values:
        if not isinstance(value, str) or not value:
            errors.append(f"{field} entries must be non-empty strings")
            continue
        out.append(value)
        if value not in allowed:
            errors.append(f"{field} references unknown id: {value}")
    return out


def _resolve_binding(
    binding: dict[str, Any],
    objects: dict[str, dict[str, Any]],
    transforms: dict[str, dict[str, Any]],
    evidence_ids: set[str],
    errors: list[str],
    prefix: str,
) -> tuple[str | None, str | None, bool]:
    object_ref = binding.get("object_ref")
    if object_ref not in objects:
        errors.append(f"{prefix}.object_ref references unknown object: {object_ref}")
        return None, None, False

    obj = objects[object_ref]
    unit = obj.get("unit")
    dimension = obj.get("dimension")
    transform_ref = binding.get("transform_ref")
    if transform_ref in (None, ""):
        return unit, dimension, True
    if transform_ref == TOKEN_VAZIO:
        return unit, dimension, False
    if transform_ref not in transforms:
        errors.append(f"{prefix}.transform_ref references unknown transform: {transform_ref}")
        return unit, dimension, False

    transform = transforms[transform_ref]
    if transform.get("from_unit") != unit or transform.get("from_dimension") != dimension:
        errors.append(f"{prefix}: transform {transform_ref} does not match source representation")
        return unit, dimension, False
    invariant = transform.get("invariant")
    evidence_ref = transform.get("evidence_ref")
    ok = True
    if not isinstance(invariant, str) or not invariant.strip() or invariant == TOKEN_VAZIO:
        errors.append(f"{prefix}: transform {transform_ref} requires explicit invariant")
        ok = False
    if evidence_ref not in evidence_ids:
        errors.append(f"{prefix}: transform {transform_ref} requires known evidence_ref")
        ok = False
    return transform.get("to_unit"), transform.get("to_dimension"), ok


def evaluate_manifest(contract: dict[str, Any], manifest: dict[str, Any]) -> dict[str, Any]:
    compiled = validate_contract(contract)
    errors: list[str] = []
    if manifest.get("schema") != MANIFEST_SCHEMA:
        errors.append(f"schema must be {MANIFEST_SCHEMA}")
    for field in ("exam_id", "observed_at", "declared_task"):
        _nonempty(manifest.get(field), field, errors)

    sources = _unique(manifest.get("sources"), "sources", errors)
    evidence = _unique(manifest.get("evidence"), "evidence", errors)
    objects = _unique(manifest.get("objects"), "objects", errors)
    transforms = _unique(manifest.get("transforms"), "transforms", errors)
    operations = _unique(manifest.get("operations"), "operations", errors)
    source_ids = set(sources)
    evidence_ids = set(evidence)

    for source_id, source in sources.items():
        for field in ("locator", "state"):
            _nonempty(source.get(field), f"sources.{source_id}.{field}", errors)

    for evidence_id, item in evidence.items():
        if item.get("source_ref") not in source_ids:
            errors.append(f"evidence.{evidence_id}.source_ref references unknown source")
        _nonempty(item.get("locator"), f"evidence.{evidence_id}.locator", errors)

    for object_id, obj in objects.items():
        if obj.get("kind") not in compiled["object_kinds"]:
            errors.append(f"objects.{object_id}.kind invalid")
        if obj.get("state") not in compiled["epistemic_states"]:
            errors.append(f"objects.{object_id}.state invalid")
        if obj.get("source_ref") not in source_ids:
            errors.append(f"objects.{object_id}.source_ref references unknown source")
        for field in ("semantic_type", "unit", "dimension"):
            _nonempty(obj.get(field), f"objects.{object_id}.{field}", errors)
        if obj.get("unit") == TOKEN_VAZIO or obj.get("dimension") == TOKEN_VAZIO:
            if obj.get("state") != TOKEN_VAZIO:
                errors.append(f"objects.{object_id}: missing representation requires TOKEN_VAZIO state")

    for transform_id, transform in transforms.items():
        for field in ("from_unit", "to_unit", "from_dimension", "to_dimension", "invariant"):
            _nonempty(transform.get(field), f"transforms.{transform_id}.{field}", errors)
        if transform.get("evidence_ref") not in evidence_ids:
            errors.append(f"transforms.{transform_id}.evidence_ref references unknown evidence")

    op_results: list[dict[str, Any]] = []
    for operation_id, operation in operations.items():
        kind = operation.get("kind")
        if kind not in compiled["operation_kinds"]:
            errors.append(f"operations.{operation_id}.kind invalid")
        bindings = operation.get("input_bindings")
        if not isinstance(bindings, list) or not bindings:
            errors.append(f"operations.{operation_id}.input_bindings must be non-empty")
            continue
        resolved: list[tuple[str | None, str | None]] = []
        transform_ok = True
        for index, binding in enumerate(bindings):
            if not isinstance(binding, dict):
                errors.append(f"operations.{operation_id}.input_bindings[{index}] must be object")
                transform_ok = False
                continue
            unit, dimension, ok = _resolve_binding(
                binding, objects, transforms, evidence_ids, errors,
                f"operations.{operation_id}.input_bindings[{index}]"
            )
            resolved.append((unit, dimension))
            transform_ok = transform_ok and ok

        state = "EXECUTABLE"
        reason = "TYPE_UNIT_TRANSFORM_INVARIANT_PASS"
        if any(unit in (None, TOKEN_VAZIO) or dim in (None, TOKEN_VAZIO) for unit, dim in resolved):
            state = "BLOCKED"
            reason = "TYPE_OR_REPRESENTATION_TOKEN_VAZIO"
        elif not transform_ok:
            state = "BLOCKED"
            reason = "TRANSFORM_GATE_FAILED"
        elif kind in {"ADD", "SUBTRACT", "COMPARE", "ASSERT_EQUIVALENCE"}:
            dimensions = {dim for _, dim in resolved}
            units = {unit for unit, _ in resolved}
            if len(dimensions) != 1:
                state = "BLOCKED"
                reason = "DIMENSION_MISMATCH"
            elif len(units) != 1:
                state = "BLOCKED"
                reason = "UNIT_MISMATCH_NO_COMMON_REPRESENTATION"

        expected = operation.get("expected_state")
        if expected not in {"EXECUTABLE", "BLOCKED"}:
            errors.append(f"operations.{operation_id}.expected_state must be EXECUTABLE or BLOCKED")
        elif expected != state:
            errors.append(
                f"operations.{operation_id}: expected_state={expected} but evaluated={state} ({reason})"
            )
        op_results.append({"id": operation_id, "state": state, "reason": reason})

    representations = manifest.get("representation_options")
    if not isinstance(representations, list) or not representations:
        errors.append("representation_options must be non-empty array")
        representations = []
    selection = manifest.get("physical_representation_selection")
    bamboo_state = "PASS_EXPLICIT_OR_SINGLE"
    if len(representations) > 1 and selection == TOKEN_VAZIO:
        bamboo_state = "PASS_ALTERNATIVES_PRESERVED_NO_IMPLICIT_SELECTION"
    elif len(representations) > 1 and selection not in representations:
        errors.append("physical_representation_selection must be an option or TOKEN_VAZIO")

    execution = manifest.get("execution_state")
    if not isinstance(execution, dict):
        errors.append("execution_state must be object")
        execution = {}
    implemented = execution.get("implemented") is True
    tested = execution.get("tested") is True
    physically_proven = execution.get("physically_proven") is True
    test_refs = _validate_refs(execution.get("test_evidence_refs", []), evidence_ids, "execution_state.test_evidence_refs", errors)
    physical_refs = _validate_refs(execution.get("physical_evidence_refs", []), evidence_ids, "execution_state.physical_evidence_refs", errors)
    if tested and not implemented:
        errors.append("execution_state: tested=true requires implemented=true")
    if tested and not test_refs:
        errors.append("execution_state: tested=true requires test evidence")
    if physically_proven and (not tested or not physical_refs):
        errors.append("execution_state: physically_proven=true requires tested=true and physical evidence")
    execution_label = (
        "PHYSICALLY_PROVEN" if physically_proven else
        "TESTED_NOT_PHYSICALLY_PROVEN" if tested else
        "IMPLEMENTED_UNTESTED" if implemented else
        "NOT_IMPLEMENTED"
    )

    claim = manifest.get("claim_gate")
    if not isinstance(claim, dict):
        errors.append("claim_gate must be object")
        claim = {}
    scope_match = claim.get("test_scope_matches_claim") is True
    physical_evidence = claim.get("physical_evidence")
    claim_authority = claim.get("claim_authority")
    claim_allowed = bool(
        scope_match
        and physical_evidence != TOKEN_VAZIO
        and isinstance(physical_evidence, str)
        and physical_evidence in evidence_ids
        and claim_authority != TOKEN_VAZIO
        and isinstance(claim_authority, str)
        and claim_authority.strip()
        and physically_proven
    )
    if manifest.get("claim_allowed") is not claim_allowed:
        errors.append(f"claim_allowed must equal evaluated claim gate: {claim_allowed}")

    delivery = manifest.get("delivery")
    if not isinstance(delivery, dict):
        errors.append("delivery must be object")
        delivery = {}
    for field in compiled["delivery_required"]:
        if field not in delivery:
            errors.append(f"delivery missing required field: {field}")
    _nonempty(delivery.get("answer_state"), "delivery.answer_state", errors)
    _nonempty(delivery.get("what_was_done"), "delivery.what_was_done", errors)
    _nonempty(delivery.get("next_executable_action"), "delivery.next_executable_action", errors)
    if not isinstance(delivery.get("evidence_refs"), list):
        errors.append("delivery.evidence_refs must be array")
    else:
        _validate_refs(delivery["evidence_refs"], evidence_ids, "delivery.evidence_refs", errors)
    if not isinstance(delivery.get("limits"), list):
        errors.append("delivery.limits must be array")
    if not isinstance(delivery.get("token_vazio"), list):
        errors.append("delivery.token_vazio must be array")

    if errors:
        raise ExamError("\n".join(f"- {e}" for e in errors))

    blocked_ops = sum(1 for item in op_results if item["state"] == "BLOCKED")
    gates = {
        "G01_BALANCE": "PASS" if all(item["reason"] != "DIMENSION_MISMATCH" for item in op_results if item["state"] == "EXECUTABLE") else "FAIL",
        "G02_CARPENTER": "PASS",
        "G03_BAMBOO": bamboo_state,
        "G04_POTTER": "PASS_BOUNDARY_PRESERVED" if not claim_allowed else "PASS_WITH_CLAIM",
        "G05_LIBRARY": "PASS",
        "G06_BLACKSMITH": execution_label,
        "G07_DOOR": "OPEN" if claim_allowed else "BLOCKED_CLAIM_GATE",
    }
    return {
        "exam_id": manifest["exam_id"],
        "state": "PASS_SAFE_CONTEXT_EXAM",
        "operations": op_results,
        "blocked_operations": blocked_ops,
        "gates": gates,
        "claim_allowed": claim_allowed,
        "execution_state": execution_label,
        "physical_representation_selection": selection,
        "next_executable_action": delivery["next_executable_action"],
    }


def summarize(report: dict[str, Any]) -> str:
    executable = sum(1 for item in report["operations"] if item["state"] == "EXECUTABLE")
    blocked = report["blocked_operations"]
    return (
        f"exam={report['exam_id']} state={report['state']} "
        f"executable={executable} blocked={blocked} "
        f"claim_allowed={str(report['claim_allowed']).lower()} "
        f"execution_state={report['execution_state']}"
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subs = parser.add_subparsers(dest="command", required=True)
    p1 = subs.add_parser("validate-contract")
    p1.add_argument("contract", type=Path)
    p2 = subs.add_parser("evaluate")
    p2.add_argument("contract", type=Path)
    p2.add_argument("manifest", type=Path)
    p2.add_argument("--report", type=Path)
    return parser


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        contract = load_json(args.contract)
        if args.command == "validate-contract":
            validate_contract(contract)
            print("PASS: semantic context exam contract")
            return 0
        manifest = load_json(args.manifest)
        report = evaluate_manifest(contract, manifest)
        if args.report:
            args.report.parent.mkdir(parents=True, exist_ok=True)
            args.report.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(summarize(report))
        return 0
    except ExamError as exc:
        print(f"FAIL:\n{exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
