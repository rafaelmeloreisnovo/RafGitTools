#!/usr/bin/env python3
"""Offline RAFAELIA relay: scheduled-email or calendar metadata -> typed route.

This is a non-executing control-plane contract. The email/event is a wake-up
signal, not an executable instruction, and event validation is not identity
verification. The broker, not this adapter, must authenticate providers, confirm
standing grants, enforce deduplication and dispatch authorized work.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import sys

from event_router_v1 import EMPTY, EventError, MAX_BYTES, load, receipt

SCHEMA = "rafaelia.route-relay.v1"
ROUTES = {
    "MIDDLEFIELD_BRIDGE_CHECK_V1": {
        # This is the actual canonical START HERE doc, not an invented folder.
        "drive_route": "START_HERE_OMEGA_V2_2",
        "drive_document_id": "1T4OIN2RC1I-KFsdaQTCqieeZUGYBXissWrdrZMG0DN8",
        "repository": "rafaelmeloreisnovo/RafGitTools",
        "workflow": "rafaelia-middlefield-bridge-scoped.yml",
        "ref": "main",
        "action": "PREPARE_WORKFLOW_DISPATCH",
        "needs_drive_read_scope": True,
        "needs_workflow_dispatch_grant": True,
        "requires_standing_policy": True,
    },
    "MIDDLEFIELD_STATUS_READ_V1": {
        "drive_route": "START_HERE_OMEGA_V2_2",
        "drive_document_id": "1T4OIN2RC1I-KFsdaQTCqieeZUGYBXissWrdrZMG0DN8",
        "repository": "rafaelmeloreisnovo/RafGitTools",
        "workflow": "rafaelia-middlefield-bridge-scoped.yml",
        "ref": "main",
        "action": "READ_PUBLIC_WORKFLOW_STATUS",
        "needs_drive_read_scope": False,
        "needs_workflow_dispatch_grant": False,
        "requires_standing_policy": True,
    },
}
RELAY_SOURCES = frozenset({
    "GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT", "GITHUB_EVENT",
    "SUPABASE_EVENT", "GPT_USER_INTENT", "RAFGITTOOLS_APK",
})
# Explicitly restrict the stage sequence; this is a trace proposal,
# not a claim about completed asynchronous work.
STAGES = (
    "OBSERVE_PROVIDER_EVENT",
    "AUTHENTICATE_PROVIDER_AND_TENANT",
    "RESOLVE_FIXED_ROUTE",
    "VERIFY_STANDING_POLICY",
    "DEDUPLICATE_IN_DURABLE_QUEUE",
    "AUTHORIZE_DRIVE_READ_IF_REQUIRED",
    "AUTHORIZE_GITHUB_ACTION_IF_REQUIRED",
    "EXECUTE_WITH_PROVIDER_RECEIPT",
    "APPEND_SUPABASE_AND_DRIVE_RECEIPTS",
    "EXPOSE_STATUS_TO_GPT_AND_RAPIDTOOLS",
)


def prepare(event: dict, route_id: str, now=None) -> dict:
    """Return a typed plan; never fetch Drive, invoke GitHub or send Gmail."""
    source_receipt = receipt(event, now=now)
    if not isinstance(route_id, str) or route_id not in ROUTES:
        raise EventError("DENY_UNKNOWN_ROUTE")
    if event["source_kind"] not in RELAY_SOURCES:
        raise EventError("DENY_SOURCE")
    if event["requested_capability"] != "OBSERVE_EVENT":
        # The scheduled email is notification/observation, NOT direct authority
        # for changing code, running shell or dispatching CI.
        raise EventError("DENY_SIGNAL_CAPABILITY")
    route = ROUTES[route_id]
    event_key = source_receipt["idempotency_key"]
    correlation = hashlib.sha256((event_key + "|" + route_id).encode("ascii")).hexdigest()
    return {
        "schema": SCHEMA,
        "route_id": route_id,
        "origin_kind": event["source_kind"],
        "correlation_sha256": correlation,
        "event_sha256": source_receipt["event_sha256"],
        "source_idempotency_key": event_key,
        "schedule_is_not_authority": True,
        "email_text_is_not_code": True,
        "route": dict(route),
        "ordered_stages": list(STAGES),
        "per_event_confirmation_needed_if_standing_grant_valid": False,
        "standing_grant_verified": False,
        "provider_identity_verified": False,
        "durable_delivery_verified": False,
        "route_state": "BLOCKED_PENDING_BROKER_AND_STANDING_GRANT",
        "approval_reference": EMPTY,
        "dispatch_allowed": False,
        "execution_state": "NOT_EXECUTED",
        "claim_allowed": False,
    }


def reminder_subject(route_id: str) -> str:
    """Safe, non-authorizing subject string for an optional self-reminder."""
    if not isinstance(route_id, str) or route_id not in ROUTES:
        raise EventError("DENY_UNKNOWN_ROUTE")
    return "RAFAELIA-ROUTE-V1 " + route_id


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("envelope", type=Path)
    parser.add_argument("--route", choices=sorted(ROUTES), required=True)
    args = parser.parse_args(argv)
    try:
        if not args.envelope.is_file() or args.envelope.stat().st_size > MAX_BYTES:
            raise EventError("DENY_FILE_SIZE")
        result = prepare(load(args.envelope.read_bytes()), args.route)
    except (EventError, OSError, TypeError, OverflowError):
        print("ROUTE_RELAY=BLOCKED", file=sys.stderr)
        return 3
    print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
