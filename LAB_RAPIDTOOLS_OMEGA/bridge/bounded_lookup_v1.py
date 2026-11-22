#!/usr/bin/env python3
"""Policy-eligible, bounded read-only commands derived from event observations.

A synthetic Gmail/Calendar/GitHub/Supabase signal can select a preapproved
public lookup without repeated human approval. A validated envelope is NOT
provider-authenticated. This pure module never performs network or shell I/O;
a separately authenticated, quota-enforcing broker must deliver the command.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys

from event_router_v1 import EventError, load, receipt

# Immutable policy registry: no URL, path, host, parameters or credentials
# from event text, email subject, calendar description or caller JSON.
POLICIES = {
    "GITHUB_ACTIONS_PUBLIC_STATUS_V1": {
        "source_kinds": frozenset({
            "GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT", "GITHUB_EVENT",
            "SUPABASE_EVENT", "GPT_USER_INTENT", "RAFGITTOOLS_APK",
        }),
        "command_id": "GET_RAFGITTOOLS_PUBLIC_ACTIONS",
        "url": "https://api.github.com/repos/rafaelmeloreisnovo/RafGitTools/actions/runs?per_page=5",
    },
    "RAFGITTOOLS_PUBLIC_GUIDE_V1": {
        "source_kinds": frozenset({
            "GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT",
            "GPT_USER_INTENT", "RAFGITTOOLS_APK",
        }),
        "command_id": "GET_RAFGITTOOLS_PUBLIC_README",
        "url": "https://raw.githubusercontent.com/rafaelmeloreisnovo/RafGitTools/main/README.md",
    },
}
MAX_RESPONSE_BYTES = 65536
MAX_REQUESTS_PER_HOUR = 6


def prepare(event: dict, policy_id: str, now=None) -> dict:
    """Build a *fixed* read-only command descriptor, never run the command.

    Policy selection is itself untrusted in this helper: only repo-coded policy
    IDs are allowed. Identity, event authenticity, replay enforcement, quotas,
    repository allowlists and network egress belong to a separate trusted broker.
    """
    r = receipt(event, now=now)
    if not isinstance(policy_id, str) or policy_id not in POLICIES:
        raise EventError("DENY_POLICY")
    policy = POLICIES[policy_id]
    if event["source_kind"] not in policy["source_kinds"]:
        raise EventError("DENY_POLICY_SOURCE")
    if event["requested_capability"] not in ("OBSERVE_EVENT", "PREPARE_CI_READ"):
        raise EventError("DENY_NOT_READ_ONLY")
    if (event["source_kind"] in ("GMAIL_METADATA", "GOOGLE_CALENDAR_EVENT",
                                  "GITHUB_EVENT", "SUPABASE_EVENT")
            and event["requested_capability"] != "OBSERVE_EVENT"):
        raise EventError("DENY_EXTERNAL_CAPABILITY")

    # This is a command *plan* with a trusted fixed implementation, not a
    # claim that the source is authenticated or that a page was retrieved.
    return {
        "schema": "rafaelia.policy-read-command.v1",
        "policy_id": policy_id,
        "source_event_sha256": r["event_sha256"],
        "idempotency_key": r["idempotency_key"] + ":" + policy_id,
        "policy_result": "ALLOW_BOUNDED_READ_CANDIDATE",
        "per_event_human_approval_required": False,
        "provider_authentication_required": True,
        "durable_deduplication_required": True,
        "quota_enforcement_required": True,
        "max_requests_per_hour": MAX_REQUESTS_PER_HOUR,
        "command": {
            "id": policy["command_id"],
            "method": "GET",
            "url": policy["url"],
            "max_response_bytes": MAX_RESPONSE_BYTES,
            "timeout_seconds": 8,
            "follow_redirects": False,
            "attach_credentials": False,
        },
        "route_state": "AWAITING_VERIFIED_BROKER",
        "dispatch_allowed": False,
        "network_executed": False,
        "claim_allowed": False,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("envelope", type=Path)
    parser.add_argument("--policy", required=True, choices=sorted(POLICIES))
    args = parser.parse_args(argv)
    try:
        if not args.envelope.is_file() or args.envelope.stat().st_size > 8192:
            raise EventError("DENY_FILE_SIZE")
        command = prepare(load(args.envelope.read_bytes()), args.policy)
    except (EventError, OSError, TypeError, OverflowError):
        print("BOUNDED_LOOKUP=BLOCKED", file=sys.stderr)
        return 3
    print(json.dumps(command, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
