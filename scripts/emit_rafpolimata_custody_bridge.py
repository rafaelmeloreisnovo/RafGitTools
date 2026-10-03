#!/usr/bin/env python3
"""Emit a secret-free RafGitTools -> RafPolimata custody bridge envelope."""
from __future__ import annotations

import argparse
import json
import re
from datetime import datetime, timezone

STATES = {
    "PASS", "FAIL", "NOT_RUN", "PENDING", "AUDIT", "TOKEN_VAZIO",
    "IMPLEMENTED_UNTESTED", "OBSERVED_UNPROMOTED", "ROUTE_STATE_BLOCKED",
}
SECRET_VALUE_PREFIXES = ("ghp_", "github_pat_", "gho_", "ghu_", "ghs_", "ghr_", "sk-")


def canonical_label(value: str) -> str:
    value = value.strip()
    if not value:
        raise ValueError("empty capability name")
    if value.startswith(SECRET_VALUE_PREFIXES):
        raise ValueError("secret value supplied where capability name was expected")
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", value):
        raise ValueError("capability name contains unsupported characters")
    return value[:1].upper() + value[1:].lower()


def canonical_observed_at(value: str | None) -> str:
    """Return a canonical UTC timestamp; None preserves live-emission behavior."""
    if value is None:
        return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    raw = value.strip()
    if not raw:
        raise ValueError("observed-at must be non-empty")
    try:
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError("observed-at must be ISO-8601") from exc
    if parsed.tzinfo is None:
        raise ValueError("observed-at must include a timezone")
    return parsed.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser()
    p.add_argument("--bridge-id", required=True)
    p.add_argument("--source-ref", required=True)
    p.add_argument("--artifact-ref", default="TOKEN_VAZIO")
    p.add_argument("--execution-ref", default="NOT_RUN")
    p.add_argument("--evidence-ref", action="append", default=[])
    p.add_argument("--state", choices=sorted(STATES), default="IMPLEMENTED_UNTESTED")
    p.add_argument("--predecessor", default="TOKEN_VAZIO")
    p.add_argument("--supersedes", default="TOKEN_VAZIO")
    p.add_argument("--capability-name", action="append", default=[])
    p.add_argument(
        "--observed-at",
        default=None,
        help="explicit ISO-8601 timestamp for deterministic replay; omitted means current UTC time",
    )
    return p


def build_envelope(args: argparse.Namespace) -> dict:
    return {
        "schemaVersion": "rafgittools.rafpolimata-custody-bridge.v1",
        "bridgeId": args.bridge_id,
        "producer": "rafaelmeloreisnovo/RafGitTools",
        "consumer": "rafaelmeloreisnovo/RafPolimata",
        "sourceRef": args.source_ref,
        "artifactRef": args.artifact_ref,
        "executionRef": args.execution_ref,
        "evidenceRefs": args.evidence_ref,
        "state": args.state,
        "claimAllowed": False,
        "predecessorReceipt": args.predecessor,
        "supersedesReceipt": args.supersedes,
        "capabilityLabels": sorted({canonical_label(v) for v in args.capability_name}),
        "observedAt": canonical_observed_at(args.observed_at),
    }


def canonical_json(envelope: dict) -> str:
    return json.dumps(envelope, sort_keys=True, separators=(",", ":"))


def main() -> int:
    args = parser().parse_args()
    print(canonical_json(build_envelope(args)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
