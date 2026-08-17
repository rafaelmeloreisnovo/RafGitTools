#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONTRACT = ROOT / "configs" / "uncertainty-urgency-ethics-license.v1.json"
DEFAULT_EVIDENCE = ROOT / "data" / "evidence" / "github" / "uncertainty-urgency-snapshot-20260817.v1.json"
EXPECTED_AR_IDS = [f"AR{i:02d}" for i in range(1, 31)]
UNRESOLVED_PREFIXES = ("TOKEN_VAZIO", "BLOCKED_")
PARTIAL_STATES = {"PARTIAL_EVIDENCED"}
EVIDENCED_STATES = {"EVIDENCED_CURRENT_SNAPSHOT", "EVIDENCED_SCOPED", "RESOLVED_NEGATIVE", "PARTIAL_EVIDENCED"}


def load_json(path: Path) -> dict[str, Any]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(obj, dict):
        raise ValueError(f"{path}: root must be an object")
    return obj


def _is_unresolved(state: str) -> bool:
    return state.startswith(UNRESOLVED_PREFIXES) or state in PARTIAL_STATES


def validate_contract(contract: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if contract.get("schema") != "rafaelia.uncertainty-urgency-ethics-license.v1":
        errors.append("wrong contract schema")
    for key in ("claim_allowed", "automatic_promotion", "automatic_merge", "direct_main_mutation"):
        if contract.get(key) is not False:
            errors.append(f"{key} must remain false")
    expected_chain = ["source", "index", "semantic_token", "claim", "evidence", "falsifier", "decision", "artifact", "feedback"]
    if contract.get("canonical_chain") != expected_chain:
        errors.append("canonical chain drift")
    rights = contract.get("rights_policy", {})
    for key in ("public_access_is_public_domain", "public_access_is_redistribution_permission", "public_access_is_training_permission", "repository_license_covers_third_party_payloads_automatically"):
        if rights.get(key) is not False:
            errors.append(f"rights policy must fail closed: {key}")
    if rights.get("upstream_or_asset_specific_terms_take_precedence") is not True:
        errors.append("upstream/asset-specific rights precedence is required")
    ethics = contract.get("ethics_by_design", {})
    for key in ("purpose_limitation_required", "data_minimization_required", "human_review_for_high_risk", "rollback_for_mutating_actions", "no_silent_inference"):
        if ethics.get(key) is not True:
            errors.append(f"ethics invariant missing: {key}")
    if contract.get("parable_bridge", {}).get("evidence_effect") != "NONE":
        errors.append("parable must have zero evidence effect")
    ids = [item.get("id") for item in contract.get("anti_regression_invariants", []) if isinstance(item, dict)]
    if ids != EXPECTED_AR_IDS:
        errors.append("anti-regression invariants must be exactly AR01..AR30 in order")
    return errors


def validate_microcycles(window: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    entries = window.get("entries")
    if not isinstance(entries, list) or len(entries) != 4:
        return ["microcycle window must contain exactly four entries"]
    if window.get("required_count") != 4 or window.get("observed_count") != 4:
        errors.append("microcycle window count must be 4/4")
    if window.get("chain_state") != "EVIDENCED_CONTIGUOUS":
        errors.append("microcycle chain must be EVIDENCED_CONTIGUOUS")
    if window.get("claim_allowed") is not False:
        errors.append("microcycle window claim_allowed must be false")
    seen_runs: set[int] = set()
    for idx, entry in enumerate(entries):
        run_id = entry.get("run_id")
        if not isinstance(run_id, int) or run_id in seen_runs:
            errors.append(f"microcycle[{idx}] run_id invalid or duplicated")
        else:
            seen_runs.add(run_id)
        if entry.get("decision") != "EXECUTED_READ_ONLY":
            errors.append(f"microcycle[{idx}] decision must be EXECUTED_READ_ONLY")
        if entry.get("claim_allowed") is not False:
            errors.append(f"microcycle[{idx}] claim_allowed must be false")
        if entry.get("latest_four_count") != 4:
            errors.append(f"microcycle[{idx}] latest_four_count must be 4")
        n = entry.get("n_mod_42")
        if not isinstance(n, int) or not 0 <= n < 42:
            errors.append(f"microcycle[{idx}] n_mod_42 outside 0..41")
        if entry.get("phase") not in {"psi", "chi", "rho", "delta", "sigma", "omega"}:
            errors.append(f"microcycle[{idx}] invalid phase")
        for hkey in ("previous_entry_sha256", "entry_sha256", "receipt_sha256"):
            val = entry.get(hkey)
            if not isinstance(val, str) or len(val) != 64 or any(c not in "0123456789abcdef" for c in val):
                errors.append(f"microcycle[{idx}] invalid {hkey}")
        if idx > 0 and entry.get("previous_entry_sha256") != entries[idx - 1].get("entry_sha256"):
            errors.append(f"microcycle[{idx}] chain discontinuity")
    return errors


def validate_rights(use: dict[str, Any], contract: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    rights = use.get("rights")
    if not isinstance(rights, dict):
        return [f"{use.get('id')}: rights object required"]
    if rights.get("license_state") not in set(contract.get("rights_states", [])):
        errors.append(f"{use.get('id')}: invalid license_state")
    refs = rights.get("license_evidence_refs")
    if not isinstance(refs, list):
        errors.append(f"{use.get('id')}: license_evidence_refs must be a list")
        refs = []
    verified = rights.get("license_verified") is True
    if verified and not refs:
        errors.append(f"{use.get('id')}: verified license requires evidence refs")
    for field in ("redistribution_allowed", "training_allowed", "commercial_use_allowed"):
        if rights.get(field) is True and (not verified or not refs):
            errors.append(f"{use.get('id')}: {field}=true requires verified explicit rights evidence")
    if rights.get("public_access") is True and rights.get("license_state") == "VERIFIED_PERMITTED" and not refs:
        errors.append(f"{use.get('id')}: public access alone cannot verify permission")
    return errors


def validate_evidence(evidence: dict[str, Any], contract: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if evidence.get("schema") != "rafaelia.uncertainty-urgency-evidence.v1":
        errors.append("wrong evidence schema")
    if evidence.get("claim_allowed") is not False:
        errors.append("evidence claim_allowed must remain false")
    drive = evidence.get("canonical_drive", {})
    if drive.get("state") != "CANONICAL_DRAFT" or drive.get("claim_allowed") is not False:
        errors.append("canonical Drive boundary drift")
    pr110 = evidence.get("mapa_pr_110", {})
    if pr110.get("merged") is not True or pr110.get("automatic_mutation") is not False or pr110.get("automatic_merge") is not False or pr110.get("claim_allowed") is not False:
        errors.append("Mapa PR #110 boundary mismatch")
    errors.extend(validate_microcycles(evidence.get("microcycle_window", {})))

    allowed_states = set(contract.get("allowed_states", []))
    urgency_levels = set(contract.get("urgency_levels", []))
    ids: set[str] = set()
    for item in evidence.get("uncertainties", []):
        if not isinstance(item, dict):
            errors.append("uncertainty record must be an object")
            continue
        iid = item.get("id")
        if not isinstance(iid, str) or not iid or iid in ids:
            errors.append("uncertainty ids must be non-empty and unique")
            continue
        ids.add(iid)
        state = str(item.get("state", ""))
        if state not in allowed_states:
            errors.append(f"{iid}: state not allowed")
        if item.get("urgency") not in urgency_levels:
            errors.append(f"{iid}: invalid urgency")
        for key in ("authority", "owner", "next_action", "exit_criterion", "falsifier"):
            if not isinstance(item.get(key), str) or not item.get(key).strip():
                errors.append(f"{iid}: missing {key}")
        refs = item.get("evidence_refs")
        if not isinstance(refs, list):
            errors.append(f"{iid}: evidence_refs must be a list")
            refs = []
        if state in EVIDENCED_STATES and not refs:
            errors.append(f"{iid}: evidenced/partial state requires evidence_refs")
        if _is_unresolved(state) and (not item.get("next_action") or not item.get("exit_criterion")):
            errors.append(f"{iid}: unresolved state lacks next action or exit criterion")
        risk = item.get("risk_tier")
        if risk not in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}:
            errors.append(f"{iid}: invalid risk_tier")
        if risk in {"HIGH", "CRITICAL"} and item.get("human_review_required") is not True:
            errors.append(f"{iid}: high-risk item requires human review")

    uses = evidence.get("usage_records")
    if not isinstance(uses, list) or not uses:
        errors.append("usage_records are required")
    else:
        use_ids: set[str] = set()
        for use in uses:
            uid = use.get("id") if isinstance(use, dict) else None
            if not isinstance(uid, str) or not uid or uid in use_ids:
                errors.append("usage ids must be non-empty and unique")
                continue
            use_ids.add(uid)
            for key in ("purpose", "data_minimization", "source_asset", "usage_mode"):
                if not isinstance(use.get(key), str) or not use.get(key).strip():
                    errors.append(f"{uid}: missing {key}")
            errors.extend(validate_rights(use, contract))
            ethics = use.get("ethics")
            if not isinstance(ethics, dict):
                errors.append(f"{uid}: ethics object required")
                continue
            if ethics.get("no_silent_inference") is not True:
                errors.append(f"{uid}: no_silent_inference must be true")
            if ethics.get("parable_evidence_effect") != "NONE":
                errors.append(f"{uid}: parable evidence effect must be NONE")
            if ethics.get("risk_tier") in {"HIGH", "CRITICAL"} and ethics.get("human_review_required") is not True:
                errors.append(f"{uid}: high-risk use requires human review")
    return errors


def build_report(contract: dict[str, Any], evidence: dict[str, Any]) -> dict[str, Any]:
    errors = validate_contract(contract) + validate_evidence(evidence, contract)
    states = Counter(str(x.get("state")) for x in evidence.get("uncertainties", []) if isinstance(x, dict))
    urgency_open = Counter()
    for item in evidence.get("uncertainties", []):
        if isinstance(item, dict) and _is_unresolved(str(item.get("state", ""))):
            urgency_open[str(item.get("urgency"))] += 1
    return {
        "schema": "rafaelia.uncertainty-urgency-ethics-license-report.v1",
        "status": "PASS" if not errors else "FAIL",
        "claim_allowed": False,
        "anti_regression_invariants": 30,
        "microcycle_window": {"observed": evidence.get("microcycle_window", {}).get("observed_count"), "required": 4, "chain_state": evidence.get("microcycle_window", {}).get("chain_state")},
        "uncertainty_state_counts": dict(sorted(states.items())),
        "unresolved_by_urgency": dict(sorted(urgency_open.items())),
        "errors": errors
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--contract", type=Path, default=DEFAULT_CONTRACT)
    parser.add_argument("--evidence", type=Path, default=DEFAULT_EVIDENCE)
    parser.add_argument("--write-report", type=Path, default=None)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()
    contract = load_json(args.contract)
    evidence = load_json(args.evidence)
    report = build_report(contract, evidence)
    text = json.dumps(report, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    if args.write_report is not None:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(text, encoding="utf-8")
    print(text, end="")
    return 1 if args.strict and report["status"] != "PASS" else 0


if __name__ == "__main__":
    raise SystemExit(main())
