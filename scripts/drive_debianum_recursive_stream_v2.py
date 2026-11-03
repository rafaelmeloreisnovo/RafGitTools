#!/usr/bin/env python3
"""RAFAELIA Debianum: fail-closed recursive Drive metadata-only traversal.

No file contents, scripts, process inspection, Supabase writes or tokens in output.
Stdlib host adapter; NOT an ARM bare-metal component. Output is withheld until
all scoped folders/pages pass, preventing partial JSONL promotion.
"""
import argparse
from collections import deque
from datetime import datetime
import hashlib
import json
import os
import re
import shutil
import sys
import tempfile
from urllib.parse import urlencode
from urllib.request import Request, urlopen

API = "https://www.googleapis.com/drive/v3/files"
FIELDS = ("nextPageToken,incompleteSearch,files(id,name,mimeType,size,"
          "createdTime,modifiedTime,parents,webViewLink,md5Checksum,sha256Checksum)")
ID = re.compile(r"^[A-Za-z0-9_-]{10,256}$")
SENSITIVE = re.compile(
    r"(^\.|(^|[-_.])(ssh|secret|token|password|credential|private.?key|"
    r"auth|history|keystore|session|env)([-_.]|$)|\.(pem|key|p12|pfx|kdbx)$)",
    re.I)
ALLOWED = {
    "application/vnd.google-apps.folder",
    "application/vnd.google-apps.document",
    "application/vnd.google-apps.spreadsheet",
    "application/vnd.google-apps.presentation",
    "application/json", "text/plain", "text/markdown", "text/csv",
    "text/x-python", "text/x-c", "text/x-asm", "text/x-shellscript-source"
}
FOLDER = "application/vnd.google-apps.folder"


class GateError(ValueError):
    pass


def time_utc(raw):
    if not isinstance(raw, str) or raw.startswith("1970-01-01"):
        return None
    try:
        dt = datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        return None
    return raw if dt.tzinfo is not None else None


def canonical(obj):
    return json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=True)


def classify(item, source_root, parent):
    """Returns (private metadata record, rejection reason); never inspect bytes."""
    ident, name, mime = item.get("id"), item.get("name"), item.get("mimeType")
    if not isinstance(ident, str) or not ID.fullmatch(ident):
        return None, "INVALID_ID"
    if not isinstance(name, str) or not name or len(name) > 500:
        return None, "INVALID_NAME"
    if parent not in (item.get("parents") or []):
        return None, "UNVERIFIED_PARENT"
    if SENSITIVE.search(name):
        return None, "SENSITIVE_NAME"
    if mime not in ALLOWED:
        return None, "DISALLOWED_MIME"
    kind = "FOLDER" if mime == FOLDER else "FILE"
    if kind == "FILE":
        if "size" not in item and mime.startswith("application/vnd.google-apps."):
            size = None   # Native Docs have no authoritative byte size.
        else:
            try:
                raw_size = item["size"]
                if isinstance(raw_size, bool):
                    raise ValueError("boolean-size")
                size = int(raw_size)
            except (KeyError, ValueError, TypeError):
                return None, "INVALID_SIZE"
            if size < 0 or size >= 2**63:
                return None, "INVALID_SIZE"
    else:
        size = None
    modified = time_utc(item.get("modifiedTime"))
    if not modified:
        return None, "INVALID_MODIFIED_TIME"
    created = time_utc(item.get("createdTime"))
    url = item.get("webViewLink")
    if not isinstance(url, str) or not url.startswith("https://"):
        url = ("https://drive.google.com/drive/folders/" if kind == "FOLDER"
               else "https://drive.google.com/file/d/") + ident
        if kind == "FILE":
            url += "/view"
    record = {
        "schema": "rafaelia.drive.metadata-admission/v2",
        "source_file_id": ident,
        "source_root_id": source_root,
        "source_parent_id": parent,
        "source_name": name,
        "source_kind": kind,
        "mime_type": mime,
        "size_bytes": size,
        "provider_created_at": created,
        "created_state": "OBSERVED" if created else "TOKEN_VAZIO_CREATION_TIME",
        "provider_modified_at": modified,
        "provider_url": url,
        "provider_md5": item.get("md5Checksum"),
        "provider_sha256": item.get("sha256Checksum"),
        "source_content_sha256": None,
        "content_evidence": "NOT_READ",
        "original_preserved": True,
        "claim_allowed": False
    }
    record["version_fingerprint_sha256"] = hashlib.sha256(
        canonical(record).encode("utf-8")).hexdigest()
    return record, None


def google_pages(parent, bearer, page_size, per_folder_limit):
    if not bearer:
        raise GateError("OAUTH_TOKEN_MISSING")
    cursor = None
    for _ in range(per_folder_limit):
        args = {
            "q": "'" + parent + "' in parents and trashed = false",
            "fields": FIELDS, "pageSize": page_size,
            "supportsAllDrives": "true", "includeItemsFromAllDrives": "true"
        }
        if cursor:
            args["pageToken"] = cursor
        request = Request(
            API + "?" + urlencode(args),
            headers={"Authorization": "Bearer " + bearer}
        )
        with urlopen(request, timeout=30) as response:
            payload = json.load(response)
        if not isinstance(payload, dict) or payload.get("incompleteSearch"):
            raise GateError("PROVIDER_PAGE_INCOMPLETE")
        page = payload.get("files")
        if not isinstance(page, list):
            raise GateError("PROVIDER_FILES_MISSING")
        yield page
        cursor = payload.get("nextPageToken")
        if not cursor:
            return
    raise GateError("PAGES_PER_FOLDER_LIMIT_REACHED")


def fixture_pages(path):
    with open(path, encoding="utf-8") as stream:
        obj = json.load(stream)
    if not isinstance(obj, dict) or not isinstance(obj.get("folders"), dict):
        raise GateError("INVALID_FIXTURE")
    for folder, pages in obj["folders"].items():
        if not ID.fullmatch(folder) or not isinstance(pages, list):
            raise GateError("INVALID_FIXTURE_FOLDER")
        if not all(isinstance(p, list) for p in pages):
            raise GateError("INVALID_FIXTURE_PAGE")
    return obj["folders"]


def walk(root, load_pages, output, *, max_depth=32,
         max_folders=100000, max_pages=100000,
         max_records=2000000, max_spool_bytes=268435456):
    """Atomic output. Raise without publishing rows on ANY pagination failure.

    load_pages(parent) returns a finite iterable of pages. Empty folders valid.
    stdout receives JSONL only after queue exhausted and count gates passed.
    """
    if not ID.fullmatch(root):
        raise GateError("INVALID_ROOT_ID")
    queue = deque([(root, 0)])
    discovered = {root}
    seen = set()
    counts = {"folders_scanned": 0, "pages": 0, "observed": 0,
              "admitted": 0, "rejected": {}, "max_depth_seen": 0}
    digest = hashlib.sha256()
    staged_bytes = 0
    with tempfile.TemporaryFile(mode="w+t", encoding="utf-8", newline="") as spool:
        while queue:
            parent, depth = queue.popleft()
            counts["folders_scanned"] += 1
            if counts["folders_scanned"] > max_folders:
                raise GateError("FOLDER_LIMIT_REACHED")
            counts["max_depth_seen"] = max(counts["max_depth_seen"], depth)
            for page in load_pages(parent):
                counts["pages"] += 1
                if counts["pages"] > max_pages:
                    raise GateError("GLOBAL_PAGE_LIMIT_REACHED")
                if not isinstance(page, list):
                    raise GateError("INVALID_PAGE_TYPE")
                for item in page:
                    counts["observed"] += 1
                    if counts["observed"] > max_records:
                        raise GateError("RECORD_LIMIT_REACHED")
                    if not isinstance(item, dict):
                        raise GateError("MALFORMED_PROVIDER_RECORD")
                    record, reason = classify(item, root, parent)
                    if reason:
                        counts["rejected"][reason] = counts["rejected"].get(reason, 0) + 1
                        continue
                    if record["source_kind"] == "FOLDER":
                        ident = record["source_file_id"]
                        if ident not in discovered:
                            if depth >= max_depth:
                                raise GateError("DEPTH_LIMIT_REACHED")
                            discovered.add(ident)
                            queue.append((ident, depth + 1))
                    key = (record["source_file_id"],
                           record["version_fingerprint_sha256"])
                    if key in seen:
                        continue
                    seen.add(key)
                    line = canonical(record) + "\n"
                    raw = line.encode("utf-8")
                    staged_bytes += len(raw)
                    if staged_bytes > max_spool_bytes:
                        raise GateError("SPOOL_LIMIT_REACHED")
                    spool.write(line)
                    digest.update(raw)
                    counts["admitted"] += 1
        spool.seek(0)
        shutil.copyfileobj(spool, output, length=65536)
    receipt = {
        "schema": "rafaelia.drive.metadata-scan-receipt/v2",
        "status": "METADATA_STREAM_COMPLETE",
        "source_root_id": root,
        "counts": counts,
        "metadata_jsonl_sha256": digest.hexdigest(),
        "metadata_bytes": staged_bytes,
        "file_contents_read": False, "supabase_written": False,
        "ps_live_verified": False, "claim_allowed": False
    }
    return receipt


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root-id", required=True)
    ap.add_argument("--fixture", help="Offline fixture: {'folders': {id: [[rows]]}}")
    ap.add_argument("--page-size", type=int, default=250)
    ap.add_argument("--max-pages-per-folder", type=int, default=100)
    ap.add_argument("--max-depth", type=int, default=32)
    ap.add_argument("--max-folders", type=int, default=100000)
    ap.add_argument("--max-total-pages", type=int, default=100000)
    ap.add_argument("--max-records", type=int, default=2000000)
    ap.add_argument("--max-spool-bytes", type=int, default=268435456)
    options = ap.parse_args()
    if not (1 <= options.page_size <= 1000 and
            1 <= options.max_pages_per_folder <= 1000 and
            0 <= options.max_depth <= 256 and
            1 <= options.max_folders <= 1000000 and
            1 <= options.max_total_pages <= 1000000 and
            1 <= options.max_records <= 10000000 and
            1 <= options.max_spool_bytes <= 1073741824):
        raise GateError("INVALID_SCAN_LIMITS")
    if options.fixture:
        fixture = fixture_pages(options.fixture)
        def loader(parent):
            if parent not in fixture:
                raise GateError("MISSING_FIXTURE_FOLDER")
            return iter(fixture[parent])
    else:
        bearer = os.environ.get("GOOGLE_DRIVE_OAUTH_ACCESS_TOKEN")
        def loader(parent):
            return google_pages(parent, bearer, options.page_size,
                                options.max_pages_per_folder)
    receipt = walk(
        options.root_id, loader, sys.stdout,
        max_depth=options.max_depth,
        max_folders=options.max_folders,
        max_pages=options.max_total_pages,
        max_records=options.max_records,
        max_spool_bytes=options.max_spool_bytes)
    print(canonical(receipt), file=sys.stderr)


if __name__ == "__main__":
    try:
        main()
    except (GateError, OSError, ValueError) as exc:
        print("BLOCKED_METADATA_STREAM:" + type(exc).__name__ + ":" + str(exc),
              file=sys.stderr)
        sys.exit(3)
