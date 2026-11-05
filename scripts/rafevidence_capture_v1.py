#!/usr/bin/env python3
"""RAFEVIDENCE v1: local, consented metadata receipt. No network, no raw capture store."""
import argparse
import datetime
import fcntl
import hashlib
import json
import os
import re
import stat
import sys

SCHEMA = "rafevidence.capture.v1"
OUT_SCHEMA = "rafevidence.receipt.v1"
ALLOWED_KEYS = frozenset({
    "schema", "source_kind", "source_ref", "source_sha256", "observed_at",
    "operation", "capture_state", "classification", "owner_attested",
    "contains_child_data", "contains_sensitive_data", "contains_secrets",
    "claim_allowed",
})
CATEGORIES = frozenset({"PUBLIC_METADATA", "PRIVATE_METADATA"})
SOURCES = frozenset({"USER_SCREENSHOT", "USER_EXPORT", "GITHUB_READBACK", "DRIVE_READBACK"})
OPERATIONS = frozenset({"UI_TOOL_ARGS", "PR_METADATA", "COMMIT_METADATA", "FILE_METADATA", "EXPORT_METADATA"})
STATES = frozenset({"UI_OBSERVED", "PROVIDER_READBACK", "PARTIAL", "NOT_VISIBLE"})
SOURCE_RE = re.compile(r"^(?:local:capture|github:[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+@[0-9a-f]{40}|drive:[A-Za-z0-9_-]{10,90})$")
SHA_RE = re.compile(r"[0-9a-f]{64}\Z")
TIME_RE = re.compile(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})\Z")
MAX_INPUT = 4096
MAX_LEDGER_BYTES = 5242880


class Rejected(ValueError):
    pass


def canonical(obj):
    return json.dumps(obj, ensure_ascii=True, separators=(",", ":"), sort_keys=True).encode("utf-8")


def digest(obj):
    return hashlib.sha256(canonical(obj)).hexdigest()


def validate(obj):
    if not isinstance(obj, dict) or set(obj) != ALLOWED_KEYS:
        raise Rejected("Invalid or missing field set; raw payload is not accepted")
    if obj["schema"] != SCHEMA or obj["claim_allowed"] is not False:
        raise Rejected("Schema/claim gate rejected")
    if obj["owner_attested"] is not True:
        raise Rejected("Owner's explicit attestation required")
    if any(obj[k] is not False for k in ("contains_child_data", "contains_sensitive_data", "contains_secrets")):
        raise Rejected("Child/sensitive/secret material refused, including its digest")
    if obj["source_kind"] not in SOURCES or obj["operation"] not in OPERATIONS:
        raise Rejected("Unsupported source/operation")
    if obj["capture_state"] not in STATES or obj["classification"] not in CATEGORIES:
        raise Rejected("Unsupported evidence or privacy classification")
    if not isinstance(obj["source_ref"], str) or not SOURCE_RE.fullmatch(obj["source_ref"]):
        raise Rejected("Source ref must be a restricted provider ID, never a URL, token or local path")
    if not isinstance(obj["source_sha256"], str) or not SHA_RE.fullmatch(obj["source_sha256"]):
        raise Rejected("Source digest must be 64 lowercase hex digits")
    ts = obj["observed_at"]
    if not isinstance(ts, str) or not TIME_RE.fullmatch(ts):
        raise Rejected("Timestamp must be explicit timezone ISO8601")
    try:
        when = datetime.datetime.fromisoformat(ts.replace("Z", "+00:00"))
    except ValueError as exc:
        raise Rejected("Invalid timestamp") from exc
    if when.utcoffset() is None:
        raise Rejected("Timezone required")
    kind = obj["source_kind"]
    ref = obj["source_ref"]
    if kind in ("USER_SCREENSHOT", "USER_EXPORT") and ref != "local:capture":
        raise Rejected("Local evidence paths/identifiers may not leave the capture device")
    if kind == "USER_SCREENSHOT" and obj["capture_state"] == "PROVIDER_READBACK":
        raise Rejected("A screenshot is never a provider readback")
    if kind == "GITHUB_READBACK" and not ref.startswith("github:"):
        raise Rejected("GitHub source must bind a repository and exact commit")
    if kind == "DRIVE_READBACK" and not ref.startswith("drive:"):
        raise Rejected("Drive source must bind a Drive ID")
    if kind in ("GITHUB_READBACK", "DRIVE_READBACK") and obj["capture_state"] != "PROVIDER_READBACK":
        raise Rejected("Provider-sourced event must be explicitly classified as readback")
    return obj


def reject_duplicate_keys(pairs):
    """Fail closed if JSON aliases an authorization or classification field."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise Rejected("Duplicate JSON key rejected")
        result[key] = value
    return result


def parse_input(path):
    with open(path, "rb") as f:
        raw = f.read(MAX_INPUT + 1)
    if len(raw) > MAX_INPUT:
        raise Rejected("Oversize request; no bulk content ingestion")
    try:
        return validate(json.loads(raw, object_pairs_hook=reject_duplicate_keys))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise Rejected("Invalid UTF-8 JSON") from exc


def receipt(request, previous):
    body = {
        "schema": OUT_SCHEMA,
        "event_id": "RAF-" + digest(request)[:24],
        "capture_state": request["capture_state"],
        "classification": request["classification"],
        "source_kind": request["source_kind"],
        "source_ref": request["source_ref"],
        "source_sha256": request["source_sha256"],
        "observed_at": request["observed_at"],
        "operation": request["operation"],
        "claim_allowed": False,
        "input_sha256": digest(request),
        "previous_record_sha256": previous,
        "evidence_scope": "ATTESTED_METADATA_ONLY_NOT_PROVIDER_PROOF",
    }
    return dict(body, record_sha256=digest(body))


def verify_chain(lines):
    previous = None
    seen = set()
    for line in lines:
        try:
            record = json.loads(line)
        except json.JSONDecodeError as exc:
            raise Rejected("Ledger JSON corruption") from exc
        if not isinstance(record, dict) or not isinstance(record.get("record_sha256"), str):
            raise Rejected("Ledger record missing digest")
        saved = record.pop("record_sha256")
        if not SHA_RE.fullmatch(saved) or saved != digest(record) or record.get("previous_record_sha256") != previous:
            raise Rejected("Ledger chain mismatch; refuse append")
        if record.get("event_id") in seen:
            raise Rejected("Ledger duplicate identity")
        seen.add(record["event_id"])
        previous = saved
    return previous, seen


def append_receipt(path, request):
    flags = os.O_RDWR | os.O_CREAT | os.O_APPEND
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    fd = os.open(path, flags, 0o600)
    try:
        with os.fdopen(fd, "r+", encoding="utf-8", newline="\n") as f:
            fd = -1
            fcntl.flock(f.fileno(), fcntl.LOCK_EX)
            metadata = os.fstat(f.fileno())
            if not stat.S_ISREG(metadata.st_mode) or stat.S_IMODE(metadata.st_mode) & 0o077:
                raise Rejected("Ledger is not a private regular file")
            if metadata.st_size > MAX_LEDGER_BYTES:
                raise Rejected("Ledger quota reached")
            previous, seen = verify_chain(f.readlines())
            item = receipt(request, previous)
            if item["event_id"] in seen:
                return item["event_id"], "DUPLICATE_NO_WRITE"
            f.write(canonical(item).decode("ascii") + "\n")
            f.flush()
            os.fsync(f.fileno())
            return item["event_id"], "APPENDED"
    finally:
        if fd >= 0:
            os.close(fd)


def main(argv=None):
    parser = argparse.ArgumentParser(description="Privacy-gated local evidence metadata ledger")
    parser.add_argument("--input", required=True, help="Attested, pre-minimized JSON request")
    parser.add_argument("--ledger", help="Local private JSONL ledger")
    parser.add_argument("--commit", action="store_true", help="Explicit opt-in append to local ledger")
    args = parser.parse_args(argv)
    try:
        request = parse_input(args.input)
        if args.commit:
            if not args.ledger:
                raise Rejected("--ledger required for --commit")
            event_id, state = append_receipt(args.ledger, request)
        else:
            event_id = receipt(request, None)["event_id"]
            state = "DRY_RUN_NO_WRITE"
        print(json.dumps({"event_id": event_id, "state": state, "claim_allowed": False}, sort_keys=True))
        return 0
    except (Rejected, OSError) as exc:
        print("BLOCKED: " + str(exc), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
