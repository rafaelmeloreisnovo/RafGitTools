#!/usr/bin/env python3
"""Fail-closed validator for RAFAELIA cross-repository impact reviews.

Stdlib-only by design. Structural JSON Schema is published separately; this
validator enforces the cross-repository semantic falsifiers that JSON Schema
alone cannot express.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import pathlib
import re
import sys
from collections import defaultdict

SUPPORTED_SCHEMA_ID = "rafaelia.cross_repo_impact_review"
SUPPORTED_SCHEMA_VERSION = 1
TOKEN_VAZIO = "TOKEN_VAZIO"
SHA40 = re.compile(r"^[0-9a-f]{40}$")
SHA256_TAGGED = re.compile(r"^sha256:[0-9a-f]{64}$")
ALLOWED_CHANGE_CLASSES = {
    "SCHEMA", "STATE_SEMANTICS", "RELATION_SEMANTICS",
    "DIGEST_CANONICALIZATION", "BINARY_ABI", "RUNTIME_PREFIX",
    "CLI_PROTOCOL", "AUTH_POLICY", "PRIVACY_RETENTION",
    "CLAIM_PROMOTION", "ROLLBACK", "METRIC_IDENTITY",
}
REQUIRED_FIELDS = (
    "schema_id", "schema_version", "producer_repo", "producer_commit",
    "contract_id", "contract_version", "consumers_known", "change_classes",
    "backward_compatible", "migration_required", "rollback_anchor",
    "golden_fixture_ref", "golden_fixture_digest", "negative_fixture_refs",
    "cross_repo_execution_receipt", "claim_allowed_from_interop", "observations",
)
REQUIRED_OBSERVATIONS = (
    "golden", "token_states", "artifacts", "provider", "devices",
    "runtime", "metric_series",
)
LEGACY_PREFIX = "/data/data/com.termux/files/usr"


def issue(code: str, path: str, detail: str) -> dict:
    return {"code": code, "path": path, "detail": detail}


def load_json(path: pathlib.Path):
    with path.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def validate(data: object, source_path: pathlib.Path | None = None) -> list[dict]:
    errors: list[dict] = []
    if not isinstance(data, dict):
        return [issue("STRUCT_ROOT_NOT_OBJECT", "$", "root must be a JSON object")]
    for key in REQUIRED_FIELDS:
        if key not in data:
            errors.append(issue("STRUCT_REQUIRED_FIELD_MISSING", f"$.{key}", "required field is missing"))
    if errors:
        return errors
    if data["schema_id"] != SUPPORTED_SCHEMA_ID:
        errors.append(issue("SCHEMA_ID_UNSUPPORTED", "$.schema_id", f"expected {SUPPORTED_SCHEMA_ID!r}"))
    if data["schema_version"] != SUPPORTED_SCHEMA_VERSION:
        errors.append(issue("SCHEMA_VERSION_UNSUPPORTED", "$.schema_version", "only schema version 1 is accepted"))
    if not isinstance(data["producer_repo"], str) or "/" not in data["producer_repo"]:
        errors.append(issue("PRODUCER_REPO_INVALID", "$.producer_repo", "expected owner/repository"))
    if not isinstance(data["producer_commit"], str) or not SHA40.fullmatch(data["producer_commit"]):
        errors.append(issue("PRODUCER_COMMIT_INVALID", "$.producer_commit", "expected lowercase 40-hex commit SHA"))
    if not isinstance(data["consumers_known"], list):
        errors.append(issue("CONSUMERS_INVALID", "$.consumers_known", "must be an array"))
    if not isinstance(data["change_classes"], list) or not data["change_classes"]:
        errors.append(issue("CHANGE_CLASSES_INVALID", "$.change_classes", "must be a non-empty array"))
    else:
        unknown = sorted(set(data["change_classes"]) - ALLOWED_CHANGE_CLASSES)
        if unknown:
            errors.append(issue("CHANGE_CLASS_UNSUPPORTED", "$.change_classes", f"unsupported: {unknown}"))
    if not (isinstance(data["backward_compatible"], bool) or data["backward_compatible"] == TOKEN_VAZIO):
        errors.append(issue("BACKWARD_COMPATIBILITY_INVALID", "$.backward_compatible", "expected true, false, or TOKEN_VAZIO"))
    if not isinstance(data["migration_required"], bool):
        errors.append(issue("MIGRATION_REQUIRED_INVALID", "$.migration_required", "must be boolean"))
    if not isinstance(data["golden_fixture_digest"], str) or not SHA256_TAGGED.fullmatch(data["golden_fixture_digest"]):
        errors.append(issue("GOLDEN_DIGEST_FORMAT_INVALID", "$.golden_fixture_digest", "expected sha256:<64 lowercase hex>"))
    if not isinstance(data["negative_fixture_refs"], list) or not data["negative_fixture_refs"]:
        errors.append(issue("NEGATIVE_FIXTURES_MISSING", "$.negative_fixture_refs", "at least one negative fixture is required"))
    receipt = data["cross_repo_execution_receipt"]
    if data["claim_allowed_from_interop"] is True and (receipt == TOKEN_VAZIO or not isinstance(receipt, str) or not receipt.strip()):
        errors.append(issue("CLAIM_WITHOUT_CROSS_REPO_RECEIPT", "$.claim_allowed_from_interop", "interop claim cannot be promoted without a concrete execution receipt"))
    if data["backward_compatible"] is False:
        if data["migration_required"] is not True:
            errors.append(issue("INCOMPATIBLE_WITHOUT_MIGRATION", "$.migration_required", "an incompatible contract requires explicit migration"))
        if data["rollback_anchor"] == TOKEN_VAZIO or not isinstance(data["rollback_anchor"], str) or not data["rollback_anchor"].strip():
            errors.append(issue("INCOMPATIBLE_WITHOUT_ROLLBACK", "$.rollback_anchor", "an incompatible contract requires a concrete rollback anchor"))
    obs = data["observations"]
    if not isinstance(obs, dict):
        errors.append(issue("OBSERVATIONS_INVALID", "$.observations", "must be an object"))
        return errors
    for key in REQUIRED_OBSERVATIONS:
        if key not in obs:
            errors.append(issue("STRUCT_REQUIRED_FIELD_MISSING", f"$.observations.{key}", "required observation is missing"))
    if any(key not in obs for key in REQUIRED_OBSERVATIONS):
        return errors
    golden = obs["golden"]
    if not isinstance(golden, dict) or "expected_digest" not in golden or "observed_digest" not in golden:
        errors.append(issue("GOLDEN_OBSERVATION_INVALID", "$.observations.golden", "expected/observed digest required"))
    else:
        expected = golden["expected_digest"]
        observed = golden["observed_digest"]
        if expected != data["golden_fixture_digest"]:
            errors.append(issue("GOLDEN_DECLARATION_DRIFT", "$.observations.golden.expected_digest", "observation does not match declared golden_fixture_digest"))
        if expected != observed:
            errors.append(issue("GOLDEN_DIGEST_MISMATCH", "$.observations.golden.observed_digest", "observed golden digest differs from expected"))
        if source_path is not None and isinstance(data.get("golden_fixture_ref"), str):
            golden_path = (source_path.parent / data["golden_fixture_ref"]).resolve()
            try:
                payload = golden_path.read_bytes()
                actual_tag = "sha256:" + hashlib.sha256(payload).hexdigest()
                if actual_tag != data["golden_fixture_digest"]:
                    errors.append(issue("GOLDEN_FILE_DIGEST_MISMATCH", "$.golden_fixture_digest", "declared digest does not match the referenced golden fixture bytes"))
            except OSError as exc:
                errors.append(issue("GOLDEN_FIXTURE_UNREADABLE", "$.golden_fixture_ref", str(exc)))
        if source_path is not None and isinstance(data.get("negative_fixture_refs"), list):
            for idx, ref in enumerate(data["negative_fixture_refs"]):
                if not isinstance(ref, str) or not (source_path.parent / ref).is_file():
                    errors.append(issue("NEGATIVE_FIXTURE_UNREADABLE", f"$.negative_fixture_refs[{idx}]", "referenced negative fixture is missing or unreadable"))
    if not isinstance(obs["token_states"], list):
        errors.append(issue("TOKEN_STATES_INVALID", "$.observations.token_states", "must be an array"))
    else:
        for idx, state in enumerate(obs["token_states"]):
            p = f"$.observations.token_states[{idx}]"
            if not isinstance(state, dict):
                errors.append(issue("TOKEN_STATE_INVALID", p, "must be an object"))
                continue
            if state.get("expected_state") == TOKEN_VAZIO and state.get("raw_value") != TOKEN_VAZIO:
                errors.append(issue("TOKEN_VAZIO_COERCED", p + ".raw_value", "TOKEN_VAZIO was replaced by another representation"))
    by_identity = {}
    if not isinstance(obs["artifacts"], list):
        errors.append(issue("ARTIFACTS_INVALID", "$.observations.artifacts", "must be an array"))
    else:
        for idx, art in enumerate(obs["artifacts"]):
            if not isinstance(art, dict):
                errors.append(issue("ARTIFACT_INVALID", f"$.observations.artifacts[{idx}]", "must be an object"))
                continue
            required = ("path", "content_hash", "producer_commit", "revision_id")
            if any(k not in art for k in required):
                errors.append(issue("ARTIFACT_FIELDS_MISSING", f"$.observations.artifacts[{idx}]", "artifact identity fields missing"))
                continue
            key = (art["producer_commit"], art["revision_id"])
            previous = by_identity.get(key)
            if previous is not None and previous["content_hash"] != art["content_hash"]:
                errors.append(issue("PATH_IDENTITY_COLLISION", f"$.observations.artifacts[{idx}]", "same producer/revision_id maps to divergent content hashes"))
            by_identity[key] = art
    provider = obs["provider"]
    if not isinstance(provider, dict):
        errors.append(issue("PROVIDER_OBSERVATION_INVALID", "$.observations.provider", "must be an object"))
    else:
        expected_ctx = provider.get("required_context_expected")
        observed_ctx = provider.get("required_context_observed")
        if expected_ctx != observed_ctx or provider.get("provider_enforcement_observed") is not True:
            errors.append(issue("PROVIDER_CONTEXT_UNBOUND", "$.observations.provider", "required context is not observed as provider-enforced"))
        head = provider.get("head_sha")
        approval = provider.get("approval_sha")
        if approval not in (TOKEN_VAZIO, head):
            errors.append(issue("APPROVAL_SHA_STALE", "$.observations.provider.approval_sha", "approval is not bound to the current head SHA"))
        if data["claim_allowed_from_interop"] is True and provider.get("independent_approval_observed") is not True:
            errors.append(issue("INDEPENDENT_APPROVAL_MISSING", "$.observations.provider.independent_approval_observed", "claim promotion requires independently observed approval"))
    receipt_roles = defaultdict(set)
    if not isinstance(obs["devices"], list):
        errors.append(issue("DEVICES_INVALID", "$.observations.devices", "must be an array"))
    else:
        for idx, dev in enumerate(obs["devices"]):
            if not isinstance(dev, dict) or "receipt_id" not in dev or "role" not in dev:
                errors.append(issue("DEVICE_RECEIPT_INVALID", f"$.observations.devices[{idx}]", "receipt_id and role required"))
                continue
            receipt_roles[dev["receipt_id"]].add(dev["role"])
        for receipt_id, roles in receipt_roles.items():
            if "arm32-legacy" in roles and "arm64-modern" in roles:
                errors.append(issue("DEVICE_RECEIPT_ROLE_REUSE", "$.observations.devices", f"receipt {receipt_id!r} is reused across ARM32 and ARM64 roles"))
    runtime = obs["runtime"]
    if not isinstance(runtime, dict):
        errors.append(issue("RUNTIME_OBSERVATION_INVALID", "$.observations.runtime", "must be an object"))
    else:
        expected_prefix = runtime.get("expected_prefix")
        scanned = runtime.get("scanned_strings", [])
        if expected_prefix != LEGACY_PREFIX and isinstance(scanned, list):
            for idx, value in enumerate(scanned):
                if isinstance(value, str) and LEGACY_PREFIX in value:
                    errors.append(issue("LEGACY_RUNTIME_PREFIX_PRESENT", f"$.observations.runtime.scanned_strings[{idx}]", "legacy com.termux prefix found in a promoted runtime surface"))
    pools = defaultdict(set)
    fields = ("metric_id", "workload_id", "input_id", "unit", "method_version", "environment_class")
    if not isinstance(obs["metric_series"], list):
        errors.append(issue("METRIC_SERIES_INVALID", "$.observations.metric_series", "must be an array"))
    else:
        for idx, row in enumerate(obs["metric_series"]):
            if not isinstance(row, dict) or "pool_id" not in row or any(k not in row for k in fields):
                errors.append(issue("METRIC_SERIES_ROW_INVALID", f"$.observations.metric_series[{idx}]", "pool identity dimensions are incomplete"))
                continue
            pools[row["pool_id"]].add(tuple(row[k] for k in fields))
        for pool_id, identities in pools.items():
            if len(identities) > 1:
                errors.append(issue("METRIC_SERIES_HETEROGENEOUS", "$.observations.metric_series", f"pool {pool_id!r} mixes metric/workload/input/unit/method/environment identities"))
    return errors


def validate_file(path: pathlib.Path) -> dict:
    try:
        data = load_json(path)
    except json.JSONDecodeError as exc:
        return {"ok": False, "errors": [issue("JSON_INVALID", "$", str(exc))]}
    except OSError as exc:
        return {"ok": False, "errors": [issue("FILE_IO_ERROR", str(path), str(exc))]}
    errors = validate(data, path)
    return {"ok": not errors, "errors": errors}


def apply_mutations(data: object, mutations: list[dict]) -> object:
    out = copy.deepcopy(data)
    for mutation in mutations:
        path = mutation.get("path")
        if not isinstance(path, list) or not path:
            raise ValueError("mutation path must be a non-empty JSON array")
        parent = out
        for key in path[:-1]:
            parent = parent[key]
        leaf = path[-1]
        op = mutation.get("op", "set")
        if op == "set":
            parent[leaf] = mutation.get("value")
        elif op == "delete":
            del parent[leaf]
        elif op == "append":
            parent[leaf].append(mutation.get("value"))
        else:
            raise ValueError(f"unsupported mutation op: {op}")
    return out


def run_suite(suite_path: pathlib.Path) -> dict:
    suite = load_json(suite_path)
    root = suite_path.parent
    results = []
    all_ok = True
    for case in suite.get("cases", []):
        if "file" in case:
            target = (root / case["file"]).resolve()
            result = validate_file(target)
            file_label = case["file"]
        else:
            base_path = (root / case["base_file"]).resolve()
            source = load_json(base_path)
            mutated = apply_mutations(source, case.get("mutations", []))
            errors = validate(mutated, base_path)
            result = {"ok": not errors, "errors": errors}
            file_label = case["base_file"] + "#mutated:" + case["id"]
        codes = [e["code"] for e in result["errors"]]
        expected_ok = case["expected_ok"]
        expected_code = case.get("expected_code")
        case_ok = result["ok"] == expected_ok
        if expected_code is not None:
            case_ok = case_ok and expected_code in codes
        results.append({"id": case["id"], "file": file_label, "expected_ok": expected_ok, "expected_code": expected_code, "actual_ok": result["ok"], "actual_codes": codes, "case_pass": case_ok})
        all_ok = all_ok and case_ok
    return {"ok": all_ok, "suite": str(suite_path), "results": results}


def main() -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--file", type=pathlib.Path)
    group.add_argument("--suite", type=pathlib.Path)
    parser.add_argument("--json", action="store_true", help="emit machine-readable result")
    args = parser.parse_args()
    result = run_suite(args.suite) if args.suite else validate_file(args.file)
    if args.json:
        print(json.dumps(result, ensure_ascii=False, sort_keys=True))
    else:
        print("PASS" if result["ok"] else "FAIL")
        if "results" in result:
            for row in result["results"]:
                print(f"{'PASS' if row['case_pass'] else 'FAIL'} {row['id']} actual={row['actual_codes']}")
        else:
            for err in result["errors"]:
                print(f"{err['code']} {err['path']}: {err['detail']}")
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
