#!/usr/bin/env python3
"""RAFAELIA Ω: pure, fail-closed normalization of Drive folder metadata.

Host-side stdlib adapter; no network, files, secrets, provider mutations or data export.
This is not a source-byte integrity verifier and makes no global inventory claim.
"""
from datetime import datetime
import re

ID = re.compile(r"^[A-Za-z0-9_-]+$")


class MetadataGateError(ValueError):
    pass


def _alias(record, candidates, label):
    present = [(name, record[name]) for name in candidates if name in record]
    if not present:
        return None, "FIELD_ABSENT"
    value = present[0][1]
    if any(other != value for _, other in present[1:]):
        raise MetadataGateError("CONFLICTING_" + label + "_ALIASES")
    return value, "EXPLICIT_NULL" if value is None else "OBSERVED"


def _time(value, label, required=False):
    if value is None:
        if required:
            raise MetadataGateError("MISSING_" + label)
        return None
    if not isinstance(value, str) or "T" not in value:
        raise MetadataGateError("INVALID_" + label)
    try:
        dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise MetadataGateError("INVALID_" + label) from exc
    if dt.tzinfo is None or dt.utcoffset() is None:
        raise MetadataGateError("TIMEZONE_MISSING_" + label)
    return value


def _id(value):
    return isinstance(value, str) and 10 <= len(value) <= 256 and bool(ID.fullmatch(value))


def normalize(item):
    """Return (canonical_fields, typed_status); never guess a missing parent."""
    if not isinstance(item, dict) or not _id(item.get("id")):
        raise MetadataGateError("INVALID_STABLE_ID")
    created, created_state = _alias(item, ("created_at", "created_time", "createdTime"), "CREATED")
    modified, modified_state = _alias(item, ("updated_at", "modified_time", "modifiedTime"), "MODIFIED")
    parents, parent_state = _alias(item, ("parent_ids", "parents"), "PARENT")
    created = _time(created, "CREATED_TIME")
    modified = _time(modified, "MODIFIED_TIME", required=True)
    if parent_state == "FIELD_ABSENT":
        normalized_parents = None
        parent_state = "TOKEN_VAZIO_PARENT_FIELD_ABSENT"
    elif parents is None:
        normalized_parents = None
        parent_state = "PROVIDER_EXPLICIT_NULL"
    elif isinstance(parents, list):
        if any(not _id(p) for p in parents) or len(set(parents)) != len(parents):
            raise MetadataGateError("INVALID_PARENT_ID_SET")
        normalized_parents = sorted(parents)
        parent_state = "EMPTY_ARRAY" if not parents else "OBSERVED_ARRAY"
    else:
        raise MetadataGateError("INVALID_PARENT_TYPE")
    return (
        {"id": item["id"], "created_time": created, "modified_time": modified,
         "parent_ids": normalized_parents},
        {"created_state": created_state, "modified_state": modified_state,
         "parent_state": parent_state, "source_content_sha256": "TOKEN_VAZIO",
         "claim_allowed": False},
    )
