#!/usr/bin/env python3
"""Fail-closed, secretless RAFAELIA field-lab composition contract.

This validates a pinned REFERENCE PLAN. It NEVER fetches, executes, writes,
copies private content, or authorizes cross-repository operations.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

SCHEMA = "rafaelia.federated-field-lab.v1"
SHA = re.compile(r"[0-9a-f]{40}\Z")
REPO = re.compile(r"rafaelmeloreisnovo/[A-Za-z0-9_.-]+\Z")
KNOWN_CREDENTIALS = {
    "PAT_ACTIONS": "EXISTING_APPROVED_PRIVATE_CI_BRIDGE_ONLY",
    "PAT_AGENTS": "TOKEN_VAZIO_NOT_WIRED",
    "PAT_ENV": "HUMAN_APPROVED_PROTECTED_ENVIRONMENT_ONLY",
    "PAT_ENVIRONMENTS": "TOKEN_VAZIO_PROVIDER_READBACK",
}
ROLES = {
    "ROUTING_AND_STATE", "CI_CONTROL_AND_EVIDENCE",
    "FREESTANDING_COMPILER_AND_APKC", "ANDROID_FIELD_RUNTIME",
    "DEVICE_VM_RESEARCH", "AUDIO_EXPERIMENTS", "PRIVATE_EVIDENCE_CUSTODY",
}
MANDATORY_GATES = {
    "EXACT_SHA", "NO_REMOTE_EXECUTION", "SOURCE_LICENSE",
    "DATA_MINIMIZATION", "PRIVACY", "CHILD_PROTECTION",
    "HUMAN_DIGNITY", "HUMAN_REVIEW", "INDEPENDENT_EVIDENCE", "ROLLBACK",
}
INVARIANTS = {
    "SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM",
    "TOKEN_VAZIO != 0", "SKIPPED != PASS", "HOST_PASS != PHYSICAL_PASS",
}
PRIVATE_FIELDS = {"id", "registry_id", "role", "visibility", "source_sha", "execution"}
PUBLIC_FIELDS = {"id", "repo", "role", "visibility", "source_sha", "execution"}


def validate(doc: object) -> list[str]:
    errors: list[str] = []
    if not isinstance(doc, dict):
        return ["contract root must be a JSON object"]
    expected = {
        "schema", "version", "control_plane", "routing_authority",
        "operation", "claim_allowed", "auto_merge", "auto_release",
        "auto_dispatch", "review_required", "privacy", "credentials",
        "members", "gates", "invariants",
    }
    if set(doc) != expected:
        errors.append("unknown or missing root keys")
    for key, value in {
        "schema": SCHEMA, "version": "1.0.0",
        "control_plane": "rafaelmeloreisnovo/RafGitTools",
        "routing_authority": "rafaelmeloreisnovo/Mapa",
        "operation": "READ_ONLY_COMPOSITION",
        "claim_allowed": False, "auto_merge": False,
        "auto_release": False, "auto_dispatch": False, "review_required": True,
    }.items():
        if doc.get(key) != value or type(doc.get(key)) is not type(value):
            errors.append(f"invalid authority or promotion field: {key}")
    privacy = doc.get("privacy")
    requirements = {
        "minimize", "raw_private_export", "child_protection",
        "human_dignity", "license_check",
    }
    if not isinstance(privacy, dict) or set(privacy) != requirements:
        errors.append("privacy / child protection contract incomplete")
    else:
        for key in requirements:
            expected_value = False if key == "raw_private_export" else True
            if type(privacy[key]) is not bool or privacy[key] is not expected_value:
                errors.append(f"privacy gate disabled: {key}")
    if doc.get("credentials") != KNOWN_CREDENTIALS:
        errors.append("credential capability changed or secret fallback proposed")
    for name, required in (("gates", MANDATORY_GATES), ("invariants", INVARIANTS)):
        vals = doc.get(name)
        if not isinstance(vals, list) or len(vals) != len(required) or set(vals) != required:
            errors.append(f"{name} mismatch")
    members = doc.get("members")
    if not isinstance(members, list) or not 2 <= len(members) <= 16:
        return errors + ["members must be a bounded nonempty list"]
    ids: set[str] = set()
    repos: set[str] = set()
    private_count = 0
    for idx, member in enumerate(members):
        tag = f"member[{idx}]"
        if not isinstance(member, dict):
            errors.append(f"{tag}: not an object")
            continue
        visibility = member.get("visibility")
        if visibility == "private":
            private_count += 1
            if set(member) != PRIVATE_FIELDS:
                errors.append(f"{tag}: private member leaks repo metadata or adds fields")
            if member.get("registry_id") != "rafaelia-private":
                errors.append(f"{tag}: private target not allowlisted")
            if member.get("source_sha") != "TOKEN_VAZIO":
                errors.append(f"{tag}: private SHA must stay undisclosed in public plan")
            if member.get("execution") != "BLOCKED_UNTIL_MANUAL_PRIVATE_CI":
                errors.append(f"{tag}: private execution cannot be automatic")
            if member.get("role") != "PRIVATE_EVIDENCE_CUSTODY":
                errors.append(f"{tag}: private role mismatch")
        elif visibility == "public":
            if set(member) != PUBLIC_FIELDS:
                errors.append(f"{tag}: public member field set mismatch")
            repo = member.get("repo")
            if not isinstance(repo, str) or REPO.fullmatch(repo) is None:
                errors.append(f"{tag}: public repository out of allowlist")
            elif repo in repos:
                errors.append(f"{tag}: duplicate repository")
            else:
                repos.add(repo)
            commit = member.get("source_sha")
            if not isinstance(commit, str) or SHA.fullmatch(commit) is None:
                errors.append(f"{tag}: source must use immutable SHA40")
            if member.get("execution") != "NO_REMOTE_CODE":
                errors.append(f"{tag}: remote code execution forbidden")
        else:
            errors.append(f"{tag}: unknown visibility")
        mid = member.get("id")
        if not isinstance(mid, str) or re.fullmatch(r"[a-z][a-z0-9-]{0,39}", mid) is None:
            errors.append(f"{tag}: invalid id")
        elif mid in ids:
            errors.append(f"{tag}: duplicate id")
        else:
            ids.add(mid)
        if member.get("role") not in ROLES:
            errors.append(f"{tag}: unknown role")
    if private_count != 1:
        errors.append("exactly one opaque private registry reference required")
    if "rafaelmeloreisnovo/RafPolimata" not in repos or "rafaelmeloreisnovo/Mapa" not in repos:
        errors.append("RafPolimata and Mapa are required public participants")
    return errors


def receipt(doc: dict, errors: list[str]) -> dict:
    canonical = (json.dumps(doc, sort_keys=True, ensure_ascii=False, separators=(",", ":")) + "\n").encode()
    public = [m for m in doc.get("members", []) if isinstance(m, dict) and m.get("visibility") == "public"]
    return {
        "schema": "rafaelia.federated-field-lab-receipt.v1",
        "contract_sha256": hashlib.sha256(canonical).hexdigest(),
        "result": "FAIL" if errors else "PASS_SCOPED_CONTRACT_ONLY",
        "public_member_count": len(public),
        "private_member_count": 1 if not errors else 0,
        "private_source_exported": False,
        "remote_code_executed": False,
        "claim_allowed": False, "release_allowed": False,
        "device_executed": False,
        "private_access": "TOKEN_VAZIO_NOT_RUN",
        "gaps": ["REMOTE_HEAD_PROBE_NOT_RUN", "PRIVATE_PROVIDER_PROTECTION_UNVERIFIED",
                 "APK_BUILD_NOT_RUN", "ANDROID_PHYSICAL_NOT_RUN", "INDEPENDENT_REVIEW_PENDING"],
        "errors": errors,
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--contract", type=Path, default=Path("configs/federated-field-lab.v1.json"))
    ap.add_argument("--receipt", type=Path, default=Path("artifacts/federated-field-lab/receipt.json"))
    args = ap.parse_args()
    try:
        doc = json.loads(args.contract.read_text(encoding="utf-8"))
        errors = validate(doc)
    except (OSError, ValueError, UnicodeError) as exc:
        doc, errors = {}, [f"contract unreadable: {type(exc).__name__}"]
    output = receipt(doc, errors)
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(output, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print(f"FIELD_LAB={output['result']} errors={len(errors)} claim_allowed=false")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
