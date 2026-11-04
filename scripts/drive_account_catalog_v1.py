#!/usr/bin/env python3
"""RAFAELIA Ω: selected-account Drive metadata catalog. NO payload reads/DB writes.

Host-side adapter over drive_debianum_recursive_stream_v2. Output stays in a
user-authorized private directory. Google OAuth tokens are supplied at runtime.
"""
from __future__ import annotations
import argparse
import hashlib
import importlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import tempfile
from urllib.request import Request, urlopen

ALIAS = re.compile(r"^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$")
EMAIL = re.compile(r"^[^\s@]+@[^\s@]+\.[^\s@]+$")
DRIVE_ABOUT = "https://www.googleapis.com/drive/v3/about?fields=user(emailAddress)"
SCHEMA = "rafaelia.drive.selected-account-catalog/v1"

class GateError(ValueError):
    pass

def canonical(value):
    return json.dumps(value, ensure_ascii=True, sort_keys=True, separators=(",", ":"))

def verify_oauth_subject(bearer, expected_email, opener=urlopen):
    if not isinstance(bearer, str) or not bearer.strip():
        raise GateError("TOKEN_VAZIO_GOOGLE_OAUTH")
    req = Request(DRIVE_ABOUT, headers={"Authorization": "Bearer " + bearer})
    try:
        with opener(req, timeout=30) as response:
            data = json.load(response)
    except Exception as exc:
        raise GateError("OAUTH_PRINCIPAL_UNVERIFIED") from exc
    observed = data.get("user", {}).get("emailAddress") if isinstance(data, dict) else None
    if not isinstance(observed, str) or observed.casefold() != expected_email.casefold():
        raise GateError("SELECTED_ACCOUNT_MISMATCH")
    return True

def execute(account_alias, expected_email, root_id, destination, *, fixture=None,
            bearer=None, engine=None, max_records=100000, max_depth=16):
    if not isinstance(account_alias, str) or not ALIAS.fullmatch(account_alias):
        raise GateError("INVALID_ACCOUNT_ALIAS")
    if not isinstance(expected_email, str) or not EMAIL.fullmatch(expected_email):
        raise GateError("EXPECTED_PRINCIPAL_REQUIRED")
    if not isinstance(root_id, str) or not re.fullmatch(r"[A-Za-z0-9_-]{10,256}", root_id):
        raise GateError("INVALID_ROOT_ID")
    if not (1 <= max_records <= 2000000 and 0 <= max_depth <= 64):
        raise GateError("SCAN_BOUNDS_INVALID")
    if engine is None:
        engine = importlib.import_module("drive_debianum_recursive_stream_v2")
    dest = Path(destination).absolute()
    parent = dest.parent
    if dest.exists() or dest.is_symlink():
        raise GateError("OUTPUT_ALREADY_EXISTS_APPEND_ONLY")
    if not parent.is_dir() or parent.is_symlink() or (stat.S_IMODE(parent.stat().st_mode) & 0o077):
        raise GateError("PRIVATE_DESTINATION_REQUIRED_0700")
    if fixture is not None:
        if bearer is not None:
            raise GateError("FIXTURE_AND_OAUTH_MUST_NOT_MIX")
        pages = engine.fixture_pages(str(fixture))
        def load(parent_id):
            if parent_id not in pages:
                raise GateError("MISSING_FIXTURE_FOLDER")
            return iter(pages[parent_id])
        mode = "FIXTURE_ONLY"
    else:
        bearer = bearer if bearer is not None else os.environ.get("GOOGLE_DRIVE_OAUTH_ACCESS_TOKEN")
        verify_oauth_subject(bearer, expected_email)
        def load(parent_id):
            return engine.google_pages(parent_id, bearer, 250, 100)
        mode = "OAUTH_METADATA_ONLY"
    lock_path = str(dest) + ".lock"
    try:
        lock_fd = os.open(lock_path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as exc:
        raise GateError("OUTPUT_LOCKED") from exc
    os.close(lock_fd)
    tmp = None
    try:
        if dest.exists():
            raise GateError("OUTPUT_ALREADY_EXISTS_APPEND_ONLY")
        tmp = Path(tempfile.mkdtemp(prefix=".rafcat-", dir=str(parent)))
        os.chmod(tmp, 0o700)
        with (tmp / "CATALOG.jsonl").open("w", encoding="utf-8", newline="") as sink:
            scan = engine.walk(root_id, load, sink, max_depth=max_depth,
                               max_records=max_records)
        if (scan.get("status") != "METADATA_STREAM_COMPLETE"
                or scan.get("file_contents_read") is not False
                or scan.get("supabase_written") is not False):
            raise GateError("SOURCE_SCAN_RECEIPT_NOT_METADATA_ONLY")
        receipt = dict(scan)
        receipt.update({
            "schema_selected": SCHEMA,
            "account_alias": account_alias,
            "account_binding_sha256": hashlib.sha256(expected_email.casefold().encode()).hexdigest(),
            "mode": mode, "source": "GOOGLE_DRIVE", "published_public": False,
            "claim_allowed": False, "archive_members_scanned": False,
            "rmrcti_executed": False, "semantic_index_executed": False,
            "supabase_written": False, "drive_destination_written": False
        })
        (tmp / "RECEIPT.json").write_text(canonical(receipt) + "\n", encoding="utf-8")
        (tmp / "START_HERE.md").write_text(
            "# RAFAELIA Ω — Selected Drive Catalog (private)\n\n"
            f"Source slot: `{account_alias}`; account identity represented by a one-way digest.\n\n"
            "1. Open `RECEIPT.json` to verify the selection and coverage.\n"
            "2. Traverse `CATALOG.jsonl` via `source_parent_id` ↔ `source_file_id`.\n"
            "3. `version_fingerprint_sha256` is metadata identity, not content hash.\n"
            "4. Items rejected by admission are counted in receipt, not indexed.\n"
            "5. RMRCTI, semantic graph, archive members, Supabase ingestion and Drive export are NOT_RUN.\n"
            "6. Preserve originals; SOURCE≠ARTIFACT≠EXECUTION≠EVIDENCE≠CLAIM.\n"
            "7. Keep this directory PRIVATE; never push it to public GitHub/CI.\n",
            encoding="utf-8")
        if dest.exists():
            raise GateError("OUTPUT_ALREADY_EXISTS_APPEND_ONLY")
        os.rename(tmp, dest)
        tmp = None
        return receipt
    finally:
        if tmp is not None:
            shutil.rmtree(tmp)
        os.unlink(lock_path)

def main():
    ap = argparse.ArgumentParser(description="Selected Google account → private metadata catalog")
    ap.add_argument("--account-alias", required=True, help="private slot label, not an email")
    ap.add_argument("--expected-email", required=True, help="OAuth principal, never written in plaintext")
    ap.add_argument("--root-id", required=True, help="folder ID selected by the user")
    ap.add_argument("--output", required=True, help="new folder inside an existing chmod 700 directory")
    ap.add_argument("--fixture", help="offline fixture: no Google authentication")
    ap.add_argument("--max-records", type=int, default=100000)
    ap.add_argument("--max-depth", type=int, default=16)
    args = ap.parse_args()
    result = execute(args.account_alias, args.expected_email, args.root_id,
                     args.output, fixture=args.fixture, max_records=args.max_records,
                     max_depth=args.max_depth)
    print(canonical({"state": result["status"], "mode": result["mode"],
                     "counts": result["counts"], "claim_allowed": False}))

if __name__ == "__main__":
    try:
        main()
    except (GateError, OSError, ValueError) as exc:
        raise SystemExit("BLOCKED_CATALOG:" + str(exc))
