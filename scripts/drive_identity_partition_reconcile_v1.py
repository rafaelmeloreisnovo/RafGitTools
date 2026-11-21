#!/usr/bin/env python3
"""Offline, fail-closed verifier for two complete Drive folder identity captures.

Inputs are PRIVATE local metadata manifests exported by an authorized reader.
This tool never fetches source bytes, reads secrets, changes Drive or writes SQL.
A terminal cursor is NOT a consistent provider snapshot guarantee.
"""
import argparse
from datetime import datetime
import hashlib
import json
import re
import sys

ID = re.compile(r"^[A-Za-z0-9_-]{10,256}$")
SCHEMA = "rafaelia.drive.identity.pages.v1"


class GateError(ValueError):
    pass


def utc(value):
    if not isinstance(value, str):
        raise GateError("TIMESTAMP_TOKEN_VAZIO")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise GateError("TIMESTAMP_INVALID") from exc
    if parsed.utcoffset() is None:
        raise GateError("TIMESTAMP_WITHOUT_ZONE")
    return parsed


def sha_rows(rows):
    digest = hashlib.sha256()
    for row in sorted(rows):
        encoded = row.encode("utf-8")
        digest.update(len(encoded).to_bytes(4, "big"))
        digest.update(encoded)
    return digest.hexdigest()


def capture(data):
    if not isinstance(data, dict) or data.get("schema") != SCHEMA:
        raise GateError("MANIFEST_SCHEMA_INVALID")
    account, scope = data.get("account_id"), data.get("query_scope")
    if data.get("provider") != "google_drive" or not isinstance(account, str) or not ID.fullmatch(account):
        raise GateError("PROVIDER_OR_ACCOUNT_INVALID")
    if scope != "mimeType=folder;trashed=false":
        raise GateError("QUERY_SCOPE_UNVERIFIED")
    expected = data.get("expected_positions")
    if type(expected) is not int or expected < 0:
        raise GateError("EXPECTED_POSITIONS_INVALID")
    pages = data.get("pages")
    if not isinstance(pages, list) or not pages or len(pages) > 256:
        raise GateError("PAGES_MISSING_OR_UNBOUNDED")
    identities, metadata = set(), []
    first = last = None
    positions = 0
    for index, page in enumerate(pages):
        if not isinstance(page, dict) or type(page.get("page_index")) is not int or page["page_index"] != index:
            raise GateError("PAGE_ORDER_OR_GAP")
        if page.get("incomplete_search") is not False:
            raise GateError("PROVIDER_COMPLETENESS_UNVERIFIED")
        if page.get("next_page_present") is not (index < len(pages) - 1):
            raise GateError("CURSOR_CHAIN_INCOMPLETE")
        timestamp = utc(page.get("observed_at"))
        if last is not None and timestamp < last:
            raise GateError("NONMONOTONIC_OBSERVATION_TIME")
        first = first or timestamp
        last = timestamp
        items = page.get("items")
        if not isinstance(items, list) or len(items) > 1000:
            raise GateError("PAGE_ITEMS_INVALID")
        for item in items:
            if not isinstance(item, dict) or not isinstance(item.get("id"), str) or not ID.fullmatch(item["id"]):
                raise GateError("IDENTITY_INVALID")
            key = "google_drive\0" + account + "\0" + item["id"]
            if key in identities:
                raise GateError("DUPLICATE_ID_ACROSS_OR_WITHIN_PAGES")
            identities.add(key)
            positions += 1
            if "modified_time" in item and "parent_ids" in item:
                mod = item["modified_time"]
                parents = item["parent_ids"]
                utc(mod)
                if not isinstance(parents, list) or any(not isinstance(p, str) or not ID.fullmatch(p) for p in parents):
                    raise GateError("PARENT_IDS_INVALID")
                metadata.append(key + "\0" + mod + "\0" + "\0".join(sorted(parents)))
    if data.get("expected_positions") != positions:
        raise GateError("EXPECTED_POSITION_MISMATCH")
    return {
        "provider": "google_drive", "account": account, "scope": scope,
        "pages": len(pages), "positions": positions,
        "unique_identities": len(identities),
        "identity_manifest_sha256": sha_rows(identities),
        "metadata_manifest_sha256": sha_rows(metadata) if len(metadata) == positions else "TOKEN_VAZIO",
        "first_observed_at": first.isoformat(), "last_observed_at": last.isoformat(),
        "source_content_sha256": "TOKEN_VAZIO", "provider_snapshot_isolation": "NOT_PROVEN",
        "claim_allowed": False,
    }


def reconcile(first, second):
    a, b = capture(first), capture(second)
    if a["provider"] != b["provider"] or a["account"] != b["account"] or a["scope"] != b["scope"]:
        raise GateError("SNAPSHOT_SCOPE_MISMATCH")
    # Compare the actual validated ID sets, not merely equal digests.
    # A digest is an external receipt, not an internal equality oracle.
    ids_a = {item["id"] for page in first["pages"] for item in page["items"]}
    ids_b = {item["id"] for page in second["pages"] for item in page["items"]}
    same_ids = ids_a == ids_b
    comparable_metadata = a["metadata_manifest_sha256"] != "TOKEN_VAZIO" and b["metadata_manifest_sha256"] != "TOKEN_VAZIO"
    selected_a = {item["id"]: (item["modified_time"], tuple(sorted(item["parent_ids"])))
                  for page in first["pages"] for item in page["items"]} if comparable_metadata else None
    selected_b = {item["id"]: (item["modified_time"], tuple(sorted(item["parent_ids"])))
                  for page in second["pages"] for item in page["items"]} if comparable_metadata else None
    return {
        "capture_a": a, "capture_b": b,
        "same_identity_set": same_ids,
        "same_metadata_if_comparable": (selected_a == selected_b) if comparable_metadata else "TOKEN_VAZIO",
        "metadata_fields_compared": ["modified_time", "parent_ids"] if comparable_metadata else "TOKEN_VAZIO",
        "temporal_result": "REOBSERVED_EQUAL_SCOPED" if same_ids else "IDENTITY_DRIFT_OBSERVED",
        "provider_snapshot_isolation": "NOT_PROVEN",
        "global_drive_completeness": "TOKEN_VAZIO",
        "b0160_promotion": "BLOCKED",
        "claim_allowed": False,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--first", required=True)
    parser.add_argument("--second", required=True)
    args = parser.parse_args()
    with open(args.first, encoding="utf-8") as stream:
        first = json.load(stream)
    with open(args.second, encoding="utf-8") as stream:
        second = json.load(stream)
    print(json.dumps(reconcile(first, second), sort_keys=True))


if __name__ == "__main__":
    try:
        main()
    except (GateError, OSError, json.JSONDecodeError) as exc:
        print("BLOCKED_RECONCILIATION:" + (str(exc) if isinstance(exc, GateError) else type(exc).__name__), file=sys.stderr)
        sys.exit(2)
