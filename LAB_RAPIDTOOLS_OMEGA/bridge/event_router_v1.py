#!/usr/bin/env python3
"""Fail-closed, offline intake of proposed RAFAELIA event envelopes.

This module validates *metadata and intent shape only*. It never executes a
command, sends email, opens Calendar, invokes GPT or calls a network API.
Provider authentication, approval and queue delivery are outside this boundary.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import re
import sys
from uuid import UUID

SCHEMA = "rafaelia.event-envelope.v1"
REPOSITORY = "rafaelmeloreisnovo/RafGitTools"
EMPTY = "TOKEN_VAZIO"
MAX_BYTES = 8192
FIELDS = frozenset({
    "schema", "event_id", "source_kind", "source_ref_sha256",
    "interaction_id", "intent_sha256", "requested_capability",
    "target_adapter", "data_class", "created_at_utc", "expires_at_utc",
    "approval_state", "execution_state", "authority_ref", "claim_allowed",
})
SOURCES = frozenset({
    "GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT", "GITHUB_EVENT",
    "SUPABASE_EVENT", "GPT_USER_INTENT", "RAFGITTOOLS_APK",
})
HUMAN_SOURCES = frozenset({"GPT_USER_INTENT", "RAFGITTOOLS_APK"})
CAPABILITIES = frozenset({
    "OBSERVE_EVENT", "PREPARE_CI_READ", "PREPARE_NOTIFICATION",
})


class EventError(ValueError):
    """An untrusted envelope failed a deterministic structural gate."""


def _digest(value: object) -> bool:
    return (
        isinstance(value, str)
        and re.fullmatch(r"[0-9a-f]{64}", value) is not None
        and value != "0" * 64
    )


def _uuid(value: object) -> bool:
    if not isinstance(value, str):
        return False
    try:
        identifier = UUID(value)
    except (ValueError, TypeError, AttributeError):
        return False
    return identifier.version == 4 and str(identifier) == value


def _utc(value: object) -> datetime:
    if not isinstance(value, str) or not value.endswith("Z"):
        raise EventError("DENY_TIMESTAMP")
    try:
        stamp = datetime.fromisoformat(value[:-1] + "+00:00")
    except ValueError:
        raise EventError("DENY_TIMESTAMP") from None
    if stamp.utcoffset() != timedelta(0):
        raise EventError("DENY_TIMESTAMP")
    return stamp


def validate(event: object, now: datetime | None = None) -> dict:
    """Validate shape without granting identity, queue or dispatch authority."""
    if not isinstance(event, dict) or set(event) != FIELDS:
        raise EventError("DENY_FIELDS")
    if (event["schema"] != SCHEMA
            or event["target_adapter"] != "RAFGITTOOLS_BRIDGE"
            or event["data_class"] != "METADATA_ONLY"):
        raise EventError("DENY_SCOPE")
    if (event["approval_state"] != "PENDING_HUMAN"
            or event["execution_state"] != "NOT_EXECUTED"
            or event["authority_ref"] != EMPTY
            or event["claim_allowed"] is not False):
        raise EventError("DENY_PROMOTION")
    if not _uuid(event["event_id"]):
        raise EventError("DENY_EVENT_ID")
    if (event["source_kind"] not in SOURCES
            or not _digest(event["source_ref_sha256"])):
        raise EventError("DENY_SOURCE")
    if event["requested_capability"] not in CAPABILITIES:
        raise EventError("DENY_CAPABILITY")
    if event["source_kind"] in HUMAN_SOURCES:
        if not _uuid(event["interaction_id"]) or not _digest(event["intent_sha256"]):
            raise EventError("DENY_INTENT_BINDING")
    else:
        # Email, calendar, database, and webhooks can signal only an observation.
        # They cannot impersonate a human instruction or executable authorization.
        if (event["interaction_id"] != EMPTY
                or event["intent_sha256"] != EMPTY
                or event["requested_capability"] != "OBSERVE_EVENT"):
            raise EventError("DENY_EXTERNAL_EXECUTION")
    created = _utc(event["created_at_utc"])
    expires = _utc(event["expires_at_utc"])
    clock = now if now is not None else datetime.now(timezone.utc)
    if clock.tzinfo is None or clock.utcoffset() != timedelta(0):
        raise EventError("DENY_CLOCK")
    if (created > clock + timedelta(minutes=5)
            or expires <= created
            or expires - created > timedelta(hours=24)
            or expires <= clock):
        raise EventError("DENY_EXPIRY")
    return event


def receipt(event: dict, now: datetime | None = None) -> dict:
    validate(event, now=now)
    wire = json.dumps(event, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":")).encode("utf-8")
    # Deduplication key is advisory until a privileged durable queue enforces it.
    key = "|".join((event["source_kind"], event["source_ref_sha256"],
                    event["requested_capability"]))
    return {
        "schema": "rafaelia.event-intake-receipt.v1",
        "event_id": event["event_id"],
        "event_sha256": hashlib.sha256(wire).hexdigest(),
        "idempotency_key": hashlib.sha256(key.encode("ascii")).hexdigest(),
        "state": "PASS_SHAPE_ONLY",
        "identity_verified": False,
        "approval_verified": False,
        "queued": False,
        "dispatch_allowed": False,
        "execution": "NOT_EXECUTED",
        "claim_allowed": False,
        "authority": EMPTY,
    }


def _unique_pairs(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise EventError("DENY_DUPLICATE_KEY")
        result[key] = value
    return result


def load(raw: bytes) -> object:
    if not raw or len(raw) > MAX_BYTES:
        raise EventError("DENY_FILE_SIZE")
    try:
        return json.loads(
            raw.decode("utf-8"),
            object_pairs_hook=_unique_pairs,
            parse_constant=lambda _: (_ for _ in ()).throw(EventError("DENY_NONFINITE")),
        )
    except (UnicodeError, json.JSONDecodeError):
        raise EventError("DENY_JSON") from None


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("envelope", type=Path)
    args = parser.parse_args(argv)
    try:
        if not args.envelope.is_file() or args.envelope.stat().st_size > MAX_BYTES:
            raise EventError("DENY_FILE_SIZE")
        result = receipt(load(args.envelope.read_bytes()))
    except (EventError, OSError, TypeError, OverflowError):
        # Never echo source payloads or provider IDs into CI logs.
        print("EVENT_INTAKE=BLOCKED", file=sys.stderr)
        return 3
    print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
