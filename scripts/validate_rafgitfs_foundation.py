#!/usr/bin/env python3
"""Deterministic, dependency-free gate for the RafGitFS Prompt 1 foundation."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

HEX_64 = re.compile(r"^[a-f0-9]{64}$")
PROFILE_ID = re.compile(r"^[a-z0-9][a-z0-9._-]{2,63}$")
READ_ONLY_OPERATIONS = {
    "LIST_REPOSITORIES",
    "LIST_REFS",
    "LIST_TREE",
    "READ_CONTENT",
    "CACHE_CONTENT",
    "PIN_OFFLINE",
}


class ValidationError(ValueError):
    """Raised when a fail-closed RafGitFS invariant is violated."""


def load_json(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValidationError(f"{path}: root must be an object")
    return value


def validate_schema_document(document: dict[str, Any], name: str) -> None:
    if document.get("$schema") != "https://json-schema.org/draft/2020-12/schema":
        raise ValidationError(f"{name}: JSON Schema Draft 2020-12 is required")
    if document.get("type") != "object":
        raise ValidationError(f"{name}: root type must be object")
    if document.get("additionalProperties") is not False:
        raise ValidationError(f"{name}: additionalProperties must be false")
    required = document.get("required")
    if not isinstance(required, list) or not required:
        raise ValidationError(f"{name}: non-empty required list is mandatory")


def validate_profile(profile: dict[str, Any]) -> None:
    required = {
        "schema_version",
        "profile_id",
        "display_name",
        "provider",
        "scope",
        "scope_value",
        "default_ref",
        "access_mode",
        "cache_policy",
        "write_policy",
        "protected_branch_patterns",
        "receipt_required",
        "claim_allowed",
        "enabled",
    }
    missing = sorted(required - profile.keys())
    if missing:
        raise ValidationError(f"profile: missing fields: {', '.join(missing)}")
    if profile["schema_version"] != "1.0.0":
        raise ValidationError("profile: schema_version must be 1.0.0")
    if not PROFILE_ID.fullmatch(str(profile["profile_id"])):
        raise ValidationError("profile: invalid profile_id")
    if profile["provider"] != "GITHUB":
        raise ValidationError("profile: V1 supports only GITHUB")
    if profile["receipt_required"] is not True:
        raise ValidationError("profile: receipt_required must be true")
    if profile["claim_allowed"] is not False:
        raise ValidationError("profile: claim_allowed must be false")
    if profile["access_mode"] == "READ_ONLY" and profile["write_policy"] != "BLOCKED":
        raise ValidationError("profile: READ_ONLY requires write_policy=BLOCKED")
    protected = profile["protected_branch_patterns"]
    if not isinstance(protected, list) or not protected:
        raise ValidationError("profile: protected_branch_patterns must not be empty")
    if not {"main", "master"}.intersection(protected):
        raise ValidationError("profile: main or master must be protected")
    if profile["scope"] == "SELECTED_REPOSITORIES" and not profile.get("selected_repositories"):
        raise ValidationError("profile: selected scope requires selected_repositories")


def validate_receipt(receipt: dict[str, Any]) -> None:
    required = {
        "schema_version",
        "receipt_id",
        "request_id",
        "profile_id",
        "operation",
        "terminal_phase",
        "success",
        "risk_level",
        "request_sha256",
        "f_ok",
        "f_gap",
        "f_next",
        "epistemic_state",
        "claim_allowed",
        "created_at",
    }
    missing = sorted(required - receipt.keys())
    if missing:
        raise ValidationError(f"receipt: missing fields: {', '.join(missing)}")
    if receipt["schema_version"] != "1.0.0":
        raise ValidationError("receipt: schema_version must be 1.0.0")
    if receipt["terminal_phase"] != "RECEIPT":
        raise ValidationError("receipt: terminal_phase must be RECEIPT")
    if receipt["claim_allowed"] is not False:
        raise ValidationError("receipt: claim_allowed must be false")
    if not HEX_64.fullmatch(str(receipt["request_sha256"])):
        raise ValidationError("receipt: invalid request_sha256")
    if receipt["success"]:
        if not HEX_64.fullmatch(str(receipt.get("result_sha256", ""))):
            raise ValidationError("receipt: successful operation requires result_sha256")
    if receipt["operation"] not in READ_ONLY_OPERATIONS:
        raise ValidationError("receipt: Prompt 1 fixture may represent read-only operations only")
    if receipt["epistemic_state"] == "TOKEN_VAZIO":
        if not receipt["f_gap"] or not receipt["f_next"]:
            raise ValidationError("receipt: TOKEN_VAZIO requires f_gap and f_next")


def digest(paths: list[Path]) -> str:
    hasher = hashlib.sha256()
    for path in sorted(paths, key=lambda item: item.as_posix()):
        hasher.update(path.as_posix().encode("utf-8"))
        hasher.update(b"\0")
        hasher.update(path.read_bytes())
        hasher.update(b"\0")
    return hasher.hexdigest()


def validate(root: Path) -> dict[str, Any]:
    profile_schema_path = root / "contracts/rafgitfs-storage-profile-v1.schema.json"
    receipt_schema_path = root / "contracts/rafgitfs-operation-receipt-v1.schema.json"
    profile_path = root / "configs/rafgitfs/default-readonly-profile.json"
    receipt_path = root / "examples/rafgitfs/operation-receipt.readonly.json"
    paths = [profile_schema_path, receipt_schema_path, profile_path, receipt_path]
    for path in paths:
        if not path.is_file():
            raise ValidationError(f"missing file: {path.relative_to(root)}")

    validate_schema_document(load_json(profile_schema_path), "profile schema")
    validate_schema_document(load_json(receipt_schema_path), "receipt schema")
    validate_profile(load_json(profile_path))
    validate_receipt(load_json(receipt_path))

    return {
        "status": "PASS",
        "foundation_version": "1.0.0",
        "files_validated": len(paths),
        "profile_mode": "READ_ONLY",
        "write_policy": "BLOCKED",
        "claim_allowed": False,
        "sha256": digest(paths),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--write-report", type=Path)
    args = parser.parse_args()

    try:
        report = validate(args.repo_root.resolve())
    except (OSError, json.JSONDecodeError, ValidationError) as exc:
        print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False, indent=2))
        return 1

    output = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    print(output)
    if args.write_report:
        args.write_report.parent.mkdir(parents=True, exist_ok=True)
        args.write_report.write_text(output + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
