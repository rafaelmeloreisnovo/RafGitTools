#!/usr/bin/env python3
"""Preflight for contextual actions. A valid request can still be BLOCKED."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

SHA256 = re.compile(r"^[0-9a-f]{64}$")
MODES = {"read", "write", "execute", "publish"}
MUTATING = {"write", "execute", "publish"}


class PreflightError(ValueError):
    pass


def validate_request(request: dict[str, Any]) -> dict[str, Any]:
    errors: list[str] = []

    def require(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)

    require(
        request.get("schema_version") == "rafaelia.contextual-execution-request/v1",
        "schema_version",
    )
    require(request.get("claim_allowed") is False, "claim_allowed_must_be_false")
    require(
        bool(SHA256.fullmatch(str(request.get("packet_sha256", "")))),
        "packet_sha256",
    )
    require(
        bool(SHA256.fullmatch(str(request.get("semantic_result_sha256", "")))),
        "semantic_result_sha256",
    )

    operation = request.get("operation", {})
    mode = operation.get("mode")
    require(mode in MODES, "operation.mode")
    require(bool(operation.get("effect")), "operation.effect")
    require(bool(operation.get("target_repository")), "operation.target_repository")

    sources = request.get("sources")
    require(isinstance(sources, list) and len(sources) > 0, "sources")
    unresolved: list[str] = []
    if isinstance(sources, list):
        seen: set[str] = set()
        for index, source in enumerate(sources):
            sid = source.get("source_id") if isinstance(source, dict) else None
            require(
                isinstance(sid, str) and sid.startswith("src:"),
                f"source[{index}].id",
            )
            require(sid not in seen, f"duplicate_source:{sid}")
            seen.add(sid)
            if (
                source.get("observed") is not True
                or source.get("authorization") not in {"authorized", "public"}
            ):
                unresolved.append(sid)

    gate = request.get("semantic_gate", {})
    blockers = gate.get("blocking_gaps")
    require(isinstance(gate.get("answer_allowed"), bool), "semantic_gate.answer_allowed")
    require(isinstance(blockers, list), "semantic_gate.blocking_gaps")

    authorization = request.get("authorization", {})
    review = authorization.get("human_review_status")
    require(
        review in {"pending", "approved", "rejected", "not_required"},
        "authorization.human_review_status",
    )

    rollback = request.get("rollback", {})
    require(isinstance(rollback, dict), "rollback")

    limits = request.get("resource_limits", {})
    for field in ("max_files", "max_bytes", "timeout_seconds"):
        require(
            isinstance(limits.get(field), int) and limits.get(field) > 0,
            f"resource_limits.{field}",
        )

    if errors:
        raise PreflightError(";".join(errors))

    reasons: list[str] = []
    if gate.get("answer_allowed") is not True:
        reasons.append("semantic_gate_closed")
    if blockers:
        reasons.append("blocking_gaps_present")
    if unresolved:
        reasons.append("unresolved_or_unauthorized_sources")
    if mode in MUTATING and review != "approved":
        reasons.append("human_review_not_approved")
    if mode in MUTATING and not rollback.get("strategy"):
        reasons.append("rollback_missing")
    if mode in MUTATING and rollback.get("direct_default_branch_write") is not False:
        reasons.append("default_branch_write_not_denied")

    decision = "AUTHORIZED" if not reasons else "BLOCKED"
    return {
        "schema": "rafaelia.contextual-execution-preflight/v1",
        "request_id": request.get("request_id"),
        "contract_status": "PASS",
        "decision": decision,
        "reasons": reasons,
        "unresolved_sources": unresolved,
        "operation_mode": mode,
        "claim_allowed": False,
        "safe_state": (
            "no_effects_observed"
            if decision == "BLOCKED"
            else "authorized_not_yet_executed"
        ),
        "F_ok": ["contract_valid", "capability_separated_from_authorization"],
        "F_gap": reasons,
        "F_next": (
            ["resolve_preflight_reasons"]
            if reasons
            else ["execute_bounded_operation_and_capture_receipt"]
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("request", type=Path)
    parser.add_argument("--out", type=Path)
    parser.add_argument("--require-authorized", action="store_true")
    args = parser.parse_args()

    try:
        result = validate_request(json.loads(args.request.read_text(encoding="utf-8")))
        code = 0
        if args.require_authorized and result["decision"] != "AUTHORIZED":
            code = 2
    except (OSError, json.JSONDecodeError, PreflightError) as exc:
        result = {
            "schema": "rafaelia.contextual-execution-preflight/v1",
            "contract_status": "FAIL",
            "decision": "BLOCKED",
            "reason": str(exc),
            "claim_allowed": False,
            "safe_state": "no_effects_observed",
        }
        code = 1

    text = json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n"
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(text, encoding="utf-8")
    print(text, end="")
    return code


if __name__ == "__main__":
    raise SystemExit(main())
