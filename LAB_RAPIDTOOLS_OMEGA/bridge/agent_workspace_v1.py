#!/usr/bin/env python3
"""Validate RafGitTools APK proposals without granting execution authority.

This is a source-bound *proposal admission* adapter for the existing Middlefield
LAB. It does not call GitHub, Supabase, Drive, model APIs or subprocesses.
JSON supplied by an APK/Drive is untrusted input, never an approval.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
from datetime import datetime, timezone
from uuid import UUID

SCHEMA = "rafaelia.agent-interaction.v1"
REPO = "rafaelmeloreisnovo/RafGitTools"
FIELDS = frozenset({
    "schema", "interaction_id", "origin", "target_repository",
    "requested_action", "intent", "intent_sha256", "created_at_utc",
    "approval_state", "execution_state", "claim_allowed", "receipt_ref",
})
MAX_BYTES = 16384
MAX_INTENT_BYTES = 4096


class AdmissionError(ValueError):
    """A proposal failed the fail-closed admission contract."""


def validate(obj: object) -> dict:
    if not isinstance(obj, dict) or set(obj) != FIELDS:
        raise AdmissionError("DENY_FIELDS")
    if (obj["schema"] != SCHEMA or
        obj["origin"] != "RAFGITTOOLS_APK" or
        obj["target_repository"] != REPO or
        obj["requested_action"] != "PROPOSE_CHANGE"):
        raise AdmissionError("DENY_SCOPE")
    if (obj["approval_state"] != "PENDING_HUMAN" or
        obj["execution_state"] != "NOT_EXECUTED" or
        obj["claim_allowed"] is not False or
        obj["receipt_ref"] != "TOKEN_VAZIO"):
        raise AdmissionError("DENY_PROMOTION")
    raw = obj["intent"]
    if not isinstance(raw, str) or not raw.strip() or raw != raw.strip():
        raise AdmissionError("DENY_INTENT")
    payload = raw.encode("utf-8")
    if len(payload) > MAX_INTENT_BYTES:
        raise AdmissionError("DENY_INTENT_SIZE")
    digest = obj["intent_sha256"]
    if (not isinstance(digest, str) or
        re.fullmatch(r"[0-9a-f]{64}", digest) is None or
        digest != hashlib.sha256(payload).hexdigest()):
        raise AdmissionError("DENY_HASH")
    try:
        ident = UUID(obj["interaction_id"])
        if str(ident) != obj["interaction_id"] or ident.version != 4:
            raise ValueError()
    except (ValueError, TypeError, AttributeError):
        raise AdmissionError("DENY_INTERACTION_ID") from None
    timestamp = obj["created_at_utc"]
    if not isinstance(timestamp, str) or not timestamp.endswith("Z"):
        raise AdmissionError("DENY_TIMESTAMP")
    try:
        ts = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
    except ValueError:
        raise AdmissionError("DENY_TIMESTAMP") from None
    if ts.tzinfo is None or ts.utcoffset().total_seconds() != 0:
        raise AdmissionError("DENY_TIMESTAMP")
    if ts > datetime.now(timezone.utc):
        raise AdmissionError("DENY_FUTURE_TIMESTAMP")
    return obj


def admission_receipt(obj: dict) -> dict:
    """Emit a limited validation receipt, never an execution or user approval."""
    validate(obj)
    return {
        "schema": "rafaelia.agent-admission-receipt.v1",
        "interaction_id": obj["interaction_id"],
        "intent_sha256": obj["intent_sha256"],
        "validation": "PASS_PROPOSAL_SHAPE_ONLY",
        "execution": "NOT_EXECUTED",
        "authority": "TOKEN_VAZIO",
        "approval": "PENDING_HUMAN",
        "claim_allowed": False,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("proposal", type=Path)
    args = parser.parse_args(argv)
    try:
        if not args.proposal.is_file() or args.proposal.stat().st_size > MAX_BYTES:
            raise AdmissionError("DENY_FILE_SIZE")
        raw = args.proposal.read_bytes()
        # Reject duplicate object keys instead of letting JSON last-key-wins.
        def unique_pairs(pairs):
            out = {}
            for key, value in pairs:
                if key in out:
                    raise AdmissionError("DENY_DUPLICATE_KEY")
                out[key] = value
            return out
        obj = json.loads(raw.decode("utf-8"), object_pairs_hook=unique_pairs,
                         parse_constant=lambda _: (_ for _ in ()).throw(AdmissionError("DENY_NONFINITE")))
        receipt = admission_receipt(obj)
    except (AdmissionError, UnicodeError, OSError, json.JSONDecodeError, TypeError, OverflowError) as exc:
        # No source payload or exception message is echoed (may contain secrets).
        print("AGENT_ADMISSION=BLOCKED", file=sys.stderr)
        return 3
    print(json.dumps(receipt, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
