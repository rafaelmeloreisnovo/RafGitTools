#!/usr/bin/env python3
"""Build a descriptor-only Living Book IR transport bundle. Stdlib only."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

FORBIDDEN_KEYS = {
    "seed", "summary", "messages", "conversation", "private_content",
    "raw_private_text", "credential", "credentials", "secret", "secrets",
    "token", "password", "cookie", "authorization"
}
FORBIDDEN_ACTIONS = {"EXECUTE", "PUBLISH", "MERGE", "DELETE", "DISCLOSE_PRIVATE", "PROMOTE_CLAIM"}
ALLOWED_IR_ACTIONS = {"INDEX_ONLY", "PROPOSE_ANALYSIS", "PROPOSE_TRANSLATION", "PROPOSE_TEST"}


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def digests(value: Any) -> dict[str, str]:
    data = canonical_bytes(value)
    return {
        "sha256": hashlib.sha256(data).hexdigest(),
        "sha3_256": hashlib.sha3_256(data).hexdigest(),
        "blake2b_256": hashlib.blake2b(data, digest_size=32).hexdigest(),
    }


def scan_forbidden(value: Any, path: str = "$") -> list[str]:
    errors: list[str] = []
    if isinstance(value, dict):
        for key, child in value.items():
            if str(key).lower() in FORBIDDEN_KEYS:
                errors.append(f"forbidden key at {path}.{key}")
            errors.extend(scan_forbidden(child, f"{path}.{key}"))
    elif isinstance(value, list):
        for index, child in enumerate(value):
            errors.extend(scan_forbidden(child, f"{path}[{index}]"))
    return errors


def validate_ir(ir_doc: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if ir_doc.get("schema") != "rafpolimata.living-book-domain-ir/v1":
        errors.append("unsupported IR schema")
    ir = ir_doc.get("ir")
    if not isinstance(ir, dict):
        return errors + ["IR body must be object"]
    if ir_doc.get("integrity", {}).get("digests") != digests(ir):
        errors.append("IR digest mismatch")
    if ir.get("action") not in ALLOWED_IR_ACTIONS:
        errors.append(f"IR action forbidden: {ir.get('action')}")
    gates = ir.get("policy_gates", {})
    if gates.get("execution_allowed") is not False:
        errors.append("IR execution must remain blocked")
    if gates.get("publication_allowed") is not False:
        errors.append("IR publication must remain blocked")
    if gates.get("claim_allowed") is not False:
        errors.append("IR claim promotion must remain blocked")
    errors.extend(scan_forbidden(ir))
    return errors


def build_bundle(
    ir_doc: dict[str, Any], bundle_id: str, producer_repo: str,
    producer_ref: str, compiler_repo: str, compiler_ref: str,
    previous_bundle_sha256: str | None = None,
) -> dict[str, Any]:
    errors = validate_ir(ir_doc)
    if errors:
        raise ValueError("; ".join(errors))
    ir = ir_doc["ir"]
    body = {
        "source": {
            "producer_repository": producer_repo,
            "producer_ref": producer_ref,
            "compiler_repository": compiler_repo,
            "compiler_ref": compiler_ref,
            "cell_id": ir["cell_id"],
            "cell_digests": ir["cell_digests"],
            "ir_schema": ir_doc["schema"],
            "ir_digests": ir_doc["integrity"]["digests"],
        },
        "payload": {
            "intent_id": ir["intent_id"],
            "module_id": ir["module_id"],
            "module_kind": ir["module_kind"],
            "action": ir["action"],
            "capabilities": ir["capabilities"],
            "forbidden_capabilities": ir["forbidden_capabilities"],
            "output_contract": ir["output_contract"],
            "expected_receipt": ir["expected_receipt"],
            "ir_embedded": False,
            "private_source_embedded": False,
        },
        "policy": {
            "transport_mode": "DESCRIPTOR_ONLY",
            "human_approval_state": "REQUIRED_BEFORE_DISPATCH",
            "human_approval_digest": None,
            "dispatch_allowed": False,
            "execution_allowed": False,
            "publication_allowed": False,
            "claim_allowed": False,
            "network_target": None,
            "automatic_retry": False,
            "previous_bundle_sha256": previous_bundle_sha256,
        },
    }
    forbidden = scan_forbidden(body)
    if forbidden:
        raise ValueError("; ".join(forbidden))
    return {
        "schema": "rafgittools.living-book-ir-bundle/v1",
        "bundle_id": bundle_id,
        "state": "READY_FOR_REVIEW_NOT_DISPATCHED",
        **body,
        "integrity": {
            "canonicalization": "json-sort-keys-utf8-no-whitespace/v1",
            "digests": digests(body),
        },
    }


def validate_bundle(bundle: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if bundle.get("schema") != "rafgittools.living-book-ir-bundle/v1":
        errors.append("unsupported bundle schema")
    if bundle.get("state") != "READY_FOR_REVIEW_NOT_DISPATCHED":
        errors.append("bundle state must remain non-dispatched")
    body = {k: bundle[k] for k in ("source", "payload", "policy") if k in bundle}
    if bundle.get("integrity", {}).get("digests") != digests(body):
        errors.append("bundle digest mismatch")
    policy = bundle.get("policy", {})
    if policy.get("human_approval_state") != "REQUIRED_BEFORE_DISPATCH":
        errors.append("human approval must be required")
    if policy.get("human_approval_digest") is not None:
        errors.append("unreviewed bundle cannot contain approval digest")
    for flag in ("dispatch_allowed", "execution_allowed", "publication_allowed", "claim_allowed"):
        if policy.get(flag) is not False:
            errors.append(f"{flag} must be false")
    if policy.get("network_target") is not None:
        errors.append("network target forbidden before approval")
    if bundle.get("payload", {}).get("ir_embedded") is not False:
        errors.append("IR bytes must not be embedded")
    if bundle.get("payload", {}).get("private_source_embedded") is not False:
        errors.append("private source must not be embedded")
    action = bundle.get("payload", {}).get("action")
    if action in FORBIDDEN_ACTIONS or action not in ALLOWED_IR_ACTIONS:
        errors.append(f"bundle action forbidden: {action}")
    errors.extend(scan_forbidden(body))
    return errors


def load(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        value = json.load(handle)
    if not isinstance(value, dict):
        raise ValueError("top-level JSON must be object")
    return value


def atomic_write(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(path)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ir", required=True, type=Path)
    parser.add_argument("--bundle-id", required=True)
    parser.add_argument("--producer-repo", required=True)
    parser.add_argument("--producer-ref", required=True)
    parser.add_argument("--compiler-repo", required=True)
    parser.add_argument("--compiler-ref", required=True)
    parser.add_argument("--previous-bundle-sha256")
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    try:
        bundle = build_bundle(
            load(args.ir), args.bundle_id, args.producer_repo, args.producer_ref,
            args.compiler_repo, args.compiler_ref, args.previous_bundle_sha256,
        )
        errors = validate_bundle(bundle)
        if errors:
            raise ValueError("; ".join(errors))
        atomic_write(args.out, bundle)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(f"FAIL: {exc}")
        return 1
    print(f"PASS: wrote descriptor-only bundle to {args.out}")
    print(json.dumps(bundle["integrity"]["digests"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
