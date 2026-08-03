#!/usr/bin/env python3
"""Fail-closed crosswalk between RAFAELIA semantic v1 and control-plane v2."""
from __future__ import annotations
import argparse, hashlib, json
from pathlib import Path
from typing import Any

SCHEMA = "rafaelia.workflow-master-index-crosswalk/v1"
SEMANTIC_SCHEMA = "rafaelia.workflow-master-index.v1"
CONTROL_SCHEMA = "2.0.0"
CYCLE = [
    "PSI_INTENT","CHI_OBSERVE","RHO_NOISE","DELTA_TRANSFORM",
    "SIGMA_CUSTODY","OMEGA_CLOSE","PSI_REOPEN",
]
EXPECTED_LAYERS = {f"S{i:02d}" for i in range(1, 31)}

def load(path: Path) -> dict[str, Any]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError(f"{path}: root must be an object")
    return value

def digest(value: Any) -> str:
    data = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(data.encode("utf-8")).hexdigest()

def resolve_dot(value: Any, path: str) -> Any:
    current = value
    for part in path.split("."):
        if not isinstance(current, dict) or part not in current:
            raise KeyError(path)
        current = current[part]
    return current

def validate(semantic: dict[str, Any], control: dict[str, Any], crosswalk: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if semantic.get("schema") != SEMANTIC_SCHEMA:
        errors.append("semantic schema mismatch")
    modules = semantic.get("modules")
    if not isinstance(modules, list) or len(modules) != 13:
        errors.append("semantic index must contain exactly 13 modules")
    layers = semantic.get("support_layers")
    layer_ids = {item.get("id") for item in layers if isinstance(item, dict)} if isinstance(layers, list) else set()
    if layer_ids != EXPECTED_LAYERS or not isinstance(layers, list) or len(layers) != 30:
        errors.append("semantic support layers must be exactly S01..S30")

    if control.get("schema_version") != CONTROL_SCHEMA:
        errors.append("control schema mismatch")
    if control.get("claim_allowed") is not False:
        errors.append("control claim_allowed must remain false")
    if not isinstance(control.get("nodes"), list) or not control["nodes"]:
        errors.append("control nodes must be non-empty")
    policies = control.get("policies")
    if not isinstance(policies, dict) or policies.get("automatic_merge") is not False:
        errors.append("control automatic_merge must remain false")

    if crosswalk.get("schema_version") != SCHEMA:
        errors.append("crosswalk schema mismatch")
    if crosswalk.get("claim_allowed") is not False:
        errors.append("crosswalk claim_allowed must remain false")

    profiles = crosswalk.get("profiles")
    if not isinstance(profiles, dict):
        errors.append("profiles must be an object")
        profiles = {}
    expected_profiles = {
        "semantic_v1": ("workflow-master-index.json", "schema", SEMANTIC_SCHEMA),
        "control_v2": ("configs/workflow-master-index.json", "schema_version", CONTROL_SCHEMA),
    }
    for name, (path, field, version) in expected_profiles.items():
        profile = profiles.get(name)
        if not isinstance(profile, dict):
            errors.append(f"missing profile {name}")
            continue
        if (profile.get("path"), profile.get("schema_field"), profile.get("schema_value")) != (path, field, version):
            errors.append(f"profile {name} authority binding mismatch")
        if not profile.get("authority") or not profile.get("exclusions"):
            errors.append(f"profile {name} must declare authority and exclusions")

    rules = crosswalk.get("selection_rules")
    if not isinstance(rules, list) or not rules:
        errors.append("selection_rules must be non-empty")
    else:
        intents: set[str] = set()
        for item in rules:
            if not isinstance(item, dict):
                errors.append("selection rule must be an object")
                continue
            intent = item.get("intent")
            if not isinstance(intent, str) or intent in intents:
                errors.append("selection rule intents must be unique strings")
            else:
                intents.add(intent)
            required = item.get("required_profiles")
            if not isinstance(required, list) or not required or any(x not in profiles for x in required):
                errors.append(f"selection rule {intent} references unknown profile")
        cross = next((x for x in rules if isinstance(x, dict) and x.get("intent") == "CROSS_SOURCE"), None)
        if not cross or set(cross.get("required_profiles", [])) != {"semantic_v1", "control_v2"}:
            errors.append("CROSS_SOURCE must require both profiles")

    routes = crosswalk.get("cycle_routes")
    route_states = [item.get("cycle_state") for item in routes if isinstance(item, dict)] if isinstance(routes, list) else []
    if route_states != CYCLE:
        errors.append("cycle_routes must preserve the canonical seven-state order")
    covered: list[str] = []
    if isinstance(routes, list):
        for route in routes:
            if not isinstance(route, dict):
                errors.append("cycle route must be an object")
                continue
            semantic_layers = route.get("semantic_layers")
            control_paths = route.get("control_paths")
            if not isinstance(semantic_layers, list) or not semantic_layers:
                errors.append(f"{route.get('cycle_state')}: semantic_layers required")
                continue
            covered.extend(semantic_layers)
            if not isinstance(control_paths, list) or not control_paths:
                errors.append(f"{route.get('cycle_state')}: control_paths required")
                continue
            for path in control_paths:
                try:
                    resolve_dot(control, path)
                except (KeyError, TypeError):
                    errors.append(f"{route.get('cycle_state')}: unknown control path {path}")
    if set(covered) != EXPECTED_LAYERS or len(covered) != 30:
        errors.append("cycle routes must cover each S01..S30 exactly once")

    conflict = crosswalk.get("conflict_policy")
    if not isinstance(conflict, dict):
        errors.append("conflict_policy must be an object")
    else:
        if conflict.get("default_state") != "BLOCKED":
            errors.append("conflict default_state must be BLOCKED")
        if conflict.get("silent_precedence") is not False:
            errors.append("silent precedence must remain false")
        if conflict.get("required_record") != "CONTRADICTION":
            errors.append("conflict must emit CONTRADICTION")

    directive = crosswalk.get("directive_policy")
    if not isinstance(directive, dict):
        errors.append("directive_policy must be an object")
    else:
        if directive.get("record_type") != "DIRECTIVE_EVENT":
            errors.append("directive record type mismatch")
        if "PROMOTE" in directive.get("allowed_effects", []):
            errors.append("PROMOTE cannot be an allowed directive effect")
        if "PROMOTE" not in directive.get("forbidden_effects", []):
            errors.append("PROMOTE must be explicitly forbidden")
        for key in ("retroactive_default","automatic_merge","claim_promotion"):
            if directive.get(key) is not False:
                errors.append(f"directive {key} must remain false")

    normative = crosswalk.get("normative_reference_registry")
    if not isinstance(normative, dict) or normative.get("adoption_state") != "REFERENCE_ONLY":
        errors.append("normative registry must remain REFERENCE_ONLY")
    return errors

def build_report(semantic: dict[str, Any], control: dict[str, Any], crosswalk: dict[str, Any]) -> dict[str, Any]:
    errors = validate(semantic, control, crosswalk)
    return {
        "schema_version": "rafaelia.workflow-master-index-crosswalk-report/v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "semantic_digest": digest(semantic) if not errors else None,
        "control_digest": digest(control) if not errors else None,
        "crosswalk_digest": digest(crosswalk) if not errors else None,
        "semantic_modules": len(semantic.get("modules", [])),
        "semantic_layers": len(semantic.get("support_layers", [])),
        "control_nodes": len(control.get("nodes", [])),
        "cycle_routes": len(crosswalk.get("cycle_routes", [])),
        "defects": errors,
        "boundary": "Crosswalk validation proves routing coherence only; it does not prove runtime, conformity, certification, or scientific truth.",
    }

def main() -> int:
    root = Path(__file__).resolve().parents[2]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--semantic", type=Path, default=root / "workflow-master-index.json")
    parser.add_argument("--control", type=Path, default=root / "configs/workflow-master-index.json")
    parser.add_argument("--crosswalk", type=Path, default=root / "configs/workflow-master-index.crosswalk.v1.json")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    try:
        report = build_report(load(args.semantic), load(args.control), load(args.crosswalk))
    except Exception as exc:
        report = {
            "schema_version": "rafaelia.workflow-master-index-crosswalk-report/v1",
            "status": "FAIL",
            "claim_allowed": False,
            "defects": [f"load failure: {exc}"],
        }
    rendered = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    print(rendered, end="")
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8")
    return 0 if report["status"] == "PASS" else 1

if __name__ == "__main__":
    raise SystemExit(main())
