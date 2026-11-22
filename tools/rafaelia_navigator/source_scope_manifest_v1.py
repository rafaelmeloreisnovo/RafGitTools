#!/usr/bin/env python3
"""Offline, metadata-only admission for Drive files and GitHub installations.

No credentials, network requests, extraction, cloud writes or content copies.
Input is a PRIVATE connector-exported JSON snapshot, never a GitHub artifact.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path

SCHEMA = "rafgittools.hyperatlas.source-scope/v1"
MAX_INPUT_BYTES = 8 * 1024 * 1024
MAX_ITEMS = 10_000
GITHUB_FULL_NAME = re.compile(r"^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")


def require_text(value: object, label: str) -> str:
    if not isinstance(value, str) or not value or len(value) > 1024 or "\x00" in value:
        raise ValueError("INVALID_" + label)
    return value


def size_decimal(value: object) -> str | None:
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, (str, int)):
        raise ValueError("INVALID_SIZE_TYPE")
    s = str(value)
    if not s.isdecimal() or len(s) > 20 or int(s) > (1 << 64) - 1:
        raise ValueError("INVALID_SIZE_RANGE")
    return str(int(s))


def archive_kind(name: str, mime: str) -> str:
    lowered = name.lower()
    if re.search(r"\.(?:zip|zipraf)\.\d{3,}$", lowered):
        return "SPLIT_ARCHIVE_PART"
    if lowered.endswith(".zipraf"):
        return "ZIPRAF_CANDIDATE_UNVERIFIED_FORMAT"
    if lowered.endswith(".zip") or mime in ("application/zip", "application/x-zip", "application/x-zip-compressed"):
        return "ZIP_CANDIDATE"
    if lowered.endswith((".tgz", ".tar.gz", ".tar")):
        return "TAR_CANDIDATE"
    return "OTHER_FILE"


def normalize(snapshot: dict) -> tuple[list[dict], dict]:
    if not isinstance(snapshot, dict) or snapshot.get("schema") != SCHEMA:
        raise ValueError("SCHEMA_MISMATCH")
    provider = snapshot.get("provider")
    if provider not in ("google_drive", "github_installation"):
        raise ValueError("INVALID_PROVIDER")
    scope = require_text(snapshot.get("scope_id"), "SCOPE_ID")
    observed = require_text(snapshot.get("observed_at"), "OBSERVED_AT")
    if type(snapshot.get("complete")) is not bool:
        raise ValueError("EXPLICIT_COMPLETENESS_REQUIRED")
    items = snapshot.get("items")
    if not isinstance(items, list) or len(items) > MAX_ITEMS:
        raise ValueError("ITEMS_BOUND_EXCEEDED")
    account = snapshot.get("account")
    if provider == "github_installation":
        if not isinstance(account, dict) or account.get("type") not in ("User", "Organization"):
            raise ValueError("EXPLICIT_OWNER_TYPE_REQUIRED")
        login = require_text(account.get("login"), "OWNER_LOGIN")
    else:
        login = None
    keys: set[str] = set()
    out: list[dict] = []
    for item in items:
        if not isinstance(item, dict):
            raise ValueError("INVALID_ITEM")
        source_id = require_text(item.get("id"), "SOURCE_ID")
        key = provider + ":" + source_id
        if key in keys:
            raise ValueError("DUPLICATE_SOURCE_ID")
        keys.add(key)
        if provider == "google_drive":
            name = require_text(item.get("name"), "FILE_NAME")
            mime = require_text(item.get("mime_type"), "MIME_TYPE")
            parents = item.get("parents")
            if not isinstance(parents, list) or any(not isinstance(p, str) or not p for p in parents):
                raise ValueError("EXPLICIT_PARENTS_REQUIRED")
            out.append({
                "provider": provider, "scope_id": scope, "id": source_id,
                "name": name, "mime_type": mime, "parents": parents,
                "size_bytes": size_decimal(item.get("size")),
                "kind": archive_kind(name, mime), "privacy": "PRIVATE_UNVERIFIED_ACL",
                "content_hash": None, "content_read": False,
                "evidence_state": "METADATA_ONLY", "observed_at": observed,
            })
        else:
            full = require_text(item.get("full_name"), "FULL_NAME")
            if not GITHUB_FULL_NAME.fullmatch(full) or full.split("/")[0].casefold() != login.casefold():
                raise ValueError("OWNER_SCOPE_MISMATCH")
            visibility = item.get("visibility")
            if visibility not in ("public", "private", "internal"):
                raise ValueError("VISIBILITY_UNKNOWN_BLOCKED")
            out.append({
                "provider": provider, "scope_id": scope, "id": source_id,
                "full_name": full, "owner_login": login, "owner_type": account["type"],
                "visibility": visibility, "default_branch": item.get("default_branch"),
                "content_hash": None, "content_read": False,
                "evidence_state": "METADATA_ONLY", "observed_at": observed,
            })
    out.sort(key=lambda row: (row["provider"], row["id"]))
    receipt = {
        "schema": SCHEMA, "provider": provider, "scope_id": scope,
        "observed_at": observed, "items": len(out),
        "coverage": "ENUMERATED_SCOPE" if snapshot["complete"] else "PARTIAL_PAGE",
        "source_complete": snapshot["complete"], "payload_transferred": False,
        "claim_allowed": False, "gaps": [] if snapshot["complete"] else ["UNVERIFIED_PAGINATION_OR_SCOPE"],
    }
    return out, receipt


def build(input_path: Path, output_path: Path) -> dict:
    if input_path.is_symlink() or not input_path.is_file() or input_path.stat().st_size > MAX_INPUT_BYTES:
        raise ValueError("INPUT_NOT_REGULAR_OR_TOO_LARGE")
    if output_path.exists():
        raise ValueError("OUTPUT_EXISTS_APPEND_ONLY")
    if output_path == input_path or input_path in output_path.parents:
        raise ValueError("INVALID_OUTPUT_PATH")
    with input_path.open("r", encoding="utf-8") as fh:
        records, receipt = normalize(json.load(fh))
    lines = "".join(json.dumps(row, sort_keys=True, ensure_ascii=False, separators=(",", ":")) + "\n" for row in records)
    index_bytes = lines.encode("utf-8")
    receipt["index_sha256"] = hashlib.sha256(index_bytes).hexdigest()
    # The caller controls destination privacy. Never put this output under a public checkout.
    output_path.mkdir(mode=0o700, parents=True, exist_ok=False)
    os.chmod(output_path, 0o700)
    with (output_path / "INDEX.jsonl").open("xb") as fh:
        fh.write(index_bytes)
    with (output_path / "RECEIPT.json").open("x", encoding="utf-8") as fh:
        json.dump(receipt, fh, sort_keys=True, indent=2)
        fh.write("\n")
    os.chmod(output_path / "INDEX.jsonl", 0o600)
    os.chmod(output_path / "RECEIPT.json", 0o600)
    return receipt


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    # Do not print source names or IDs; only a safe local status summary.
    r = build(args.input.resolve(), args.output.resolve())
    print(json.dumps({"coverage": r["coverage"], "items": r["items"], "claim_allowed": False}))


if __name__ == "__main__":
    main()
