#!/usr/bin/env python3
"""Fail-closed, offline preflight. No dispatch, Drive I/O or secret access."""
import hashlib
import json
import re
import sys
from pathlib import Path

SCHEMA = "rafgittools.session-queue-preflight.v1"
MAPA_COMMIT = "ad2efc2b9a4599bdcc37ab416c84bd99dc2a1e09"
MAPA_BLOB = "c1d1d8f1692c74957f8b70968fa863f855d93cb0"
FIELDS = frozenset((
    "schema", "envelope_id", "session_id", "target_id", "packet_id",
    "mapa_commit", "mapa_blob", "operation", "state", "privacy_class",
    "payload_mode", "execution_enabled", "human_approved", "claim_allowed",
    "idempotency_key",
))
OPAQUE = re.compile(r"[0-9a-f]{32}\Z")
PACKET = re.compile(r"SP[0-9]{2}_[A-Z0-9_]{3,60}\Z")
PRIVACY = frozenset((
    "PUBLIC_CANDIDATE", "PRIVATE_REQUIRED", "RESTRICTED_PERSONAL",
    "UNKNOWN_REVIEW_REQUIRED",
))
STATES = frozenset(("IN", "GATES", "HOLD"))
TRANSITIONS = {"IN": frozenset(("GATES", "HOLD")),
               "GATES": frozenset(("HOLD",)),
               "HOLD": frozenset(("GATES",))}


def digest(record):
    """Stable *correlation* hash; NOT a signature, MAC or authorization."""
    body = {key: record[key] for key in sorted(FIELDS - {"idempotency_key"})}
    encoded = json.dumps(body, sort_keys=True, separators=(",", ":"), ensure_ascii=True)
    return hashlib.sha256(b"RAFAELIA-PREFLIGHT-V1\x00" + encoded.encode("utf-8")).hexdigest()


def validate(record):
    """Return only bounded error codes, never reflect private input."""
    if not isinstance(record, dict) or set(record) != FIELDS:
        return ["SCHEMA_FIELDS_BLOCKED"]
    errors = []
    if record["schema"] != SCHEMA:
        errors.append("SCHEMA_BLOCKED")
    if any(type(record[key]) is not str or not OPAQUE.fullmatch(record[key])
           for key in ("envelope_id", "session_id", "target_id")):
        errors.append("OPAQUE_ID_BLOCKED")
    if type(record["packet_id"]) is not str or not PACKET.fullmatch(record["packet_id"]):
        errors.append("PACKET_ID_BLOCKED")
    if record["mapa_commit"] != MAPA_COMMIT or record["mapa_blob"] != MAPA_BLOB:
        errors.append("AUTHORITY_PIN_BLOCKED")
    if record["operation"] != "PLAN_ONLY" or record["state"] not in STATES:
        errors.append("EXECUTION_STATE_BLOCKED")
    if record["privacy_class"] not in PRIVACY:
        errors.append("PRIVACY_BLOCKED")
    if record["payload_mode"] != "NONE":
        errors.append("PAYLOAD_BLOCKED")
    if any(record[key] is not False for key in
           ("execution_enabled", "human_approved", "claim_allowed")):
        errors.append("PROMOTION_BLOCKED")
    if type(record["idempotency_key"]) is not str or record["idempotency_key"] != digest(record):
        errors.append("IDEMPOTENCY_BLOCKED")
    return errors


def allowed_transition(before, after):
    """Candidate state planning only; must not move Drive objects."""
    return before in TRANSITIONS and after in TRANSITIONS[before]


def main(argv):
    if len(argv) != 2:
        print('{"state":"HOLD","error":"USAGE"}')
        return 2
    try:
        raw = Path(argv[1]).read_bytes()
        if len(raw) > 4096:
            raise ValueError("size")
        obj = json.loads(raw)
        codes = validate(obj)
    except (OSError, UnicodeError, ValueError, TypeError, KeyError):
        codes = ["INPUT_BLOCKED"]
    print(json.dumps({"state": "PREFLIGHT_ONLY" if not codes else "HOLD",
                      "errors": codes, "dispatch_enabled": False,
                      "claim_allowed": False}, separators=(",", ":")))
    return 0 if not codes else 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
