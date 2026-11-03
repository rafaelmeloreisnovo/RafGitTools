#!/usr/bin/env python3
"""Bounded Drive metadata stream, not a Drive file download or Supabase writer.

Host-side orchestration only. It is NOT a bare-metal primitive. Use an approved
privileged Supabase executor for admission after full-page/receipt validation.
No credential, raw content, shell execution or process-state claim is emitted.
"""
import argparse
import hashlib
import json
import os
import re
import sys
from urllib.parse import urlencode
from urllib.request import Request, urlopen

DRIVE_API = "https://www.googleapis.com/drive/v3/files"
FIELDS = ("nextPageToken,incompleteSearch,files(id,name,mimeType,size,"
          "createdTime,modifiedTime,parents,webViewLink,md5Checksum,sha256Checksum)")
ID_RE = re.compile(r"^[A-Za-z0-9_-]{10,256}$")
BLOCKED_MIME = {"application/x-shellscript", "application/octet-stream",
                "application/gzip", "text/x-log"}


class GateError(ValueError):
    pass


def valid_time(value):
    if not isinstance(value, str) or value.startswith("1970-01-01"):
        return None
    if "T" not in value or not (value.endswith("Z") or "+" in value):
        return None
    return value


def admit(file_obj, root_id):
    """Return (typed record, rejection reason) without fetching file bytes."""
    ident, name = file_obj.get("id"), file_obj.get("name")
    mime = file_obj.get("mimeType")
    if not isinstance(ident, str) or not ID_RE.fullmatch(ident):
        return None, "INVALID_ID"
    if not isinstance(name, str) or not name or len(name) > 500:
        return None, "INVALID_NAME"
    if root_id not in (file_obj.get("parents") or []):
        return None, "UNVERIFIED_PARENT"
    if name.startswith("."):
        return None, "HIDDEN_OR_SENSITIVE_NAME"
    if not isinstance(mime, str) or not mime:
        return None, "INVALID_MIME"
    if mime in BLOCKED_MIME or name.endswith((".sh", ".tar.gz", ".zip", ".deb")):
        return None, "EXECUTABLE_BINARY_ARCHIVE_OR_LOG"
    kind = "FOLDER" if mime == "application/vnd.google-apps.folder" else "FILE"
    size = file_obj.get("size")
    if kind == "FILE":
        if size is None:
            return None, "UNKNOWN_SIZE"
        try:
            size = int(size)
        except (ValueError, TypeError):
            return None, "INVALID_SIZE"
        if size < 0 or size >= 2**63:
            return None, "INVALID_SIZE"
    else:
        size = None
    modified = valid_time(file_obj.get("modifiedTime"))
    if modified is None:
        return None, "INVALID_MODIFIED_TIME"
    created = valid_time(file_obj.get("createdTime"))
    record = {
        "schema": "rafaelia.drive.metadata-admission/v1",
        "source_file_id": ident,
        "source_root_id": root_id,
        "source_parent_id": root_id,
        "source_name": name,
        "source_kind": kind,
        "mime_type": mime,
        "size_bytes": size,
        "provider_created_at": created,
        "creation_time_state": "OBSERVED" if created else "TOKEN_VAZIO_CREATION_TIME",
        "provider_modified_at": modified,
        "provider_url": file_obj.get("webViewLink"),
        "provider_md5": file_obj.get("md5Checksum"),
        "provider_sha256": file_obj.get("sha256Checksum"),
        "source_content_sha256": None,
        "content_evidence": "NOT_READ",
        "index_state": "METADATA_OBSERVED",
        "original_preserved": True,
        "claim_allowed": False,
    }
    body = json.dumps(record, sort_keys=True, separators=(",", ":"), ensure_ascii=True)
    record["version_fingerprint_sha256"] = hashlib.sha256(body.encode("utf8")).hexdigest()
    return record, None


def pages(root_id, access_token, page_size, max_pages):
    if not ID_RE.fullmatch(root_id):
        raise GateError("INVALID_SOURCE_FOLDER")
    if not access_token:
        raise GateError("MISSING_GOOGLE_OAUTH_TOKEN")
    token = None
    for page_number in range(max_pages):
        query = {"q": "'" + root_id + "' in parents and trashed = false",
                 "fields": FIELDS, "pageSize": page_size,
                 "supportsAllDrives": "true", "includeItemsFromAllDrives": "true"}
        if token:
            query["pageToken"] = token
        request = Request(DRIVE_API + "?" + urlencode(query),
                          headers={"Authorization": "Bearer " + access_token})
        with urlopen(request, timeout=30) as handle:
            response = json.load(handle)
        if response.get("incompleteSearch"):
            raise GateError("INCOMPLETE_PROVIDER_SEARCH")
        files = response.get("files")
        if not isinstance(files, list):
            raise GateError("MISSING_PROVIDER_FILES")
        yield files
        token = response.get("nextPageToken")
        if not token:
            return
    raise GateError("PAGE_LIMIT_PARTIAL_NO_PROMOTION")


def run(root_id, page_iterator):
    counts = {"observed": 0, "admitted": 0, "rejected": {}, "pages": 0}
    seen = set()
    for page in page_iterator:
        counts["pages"] += 1
        for item in page:
            counts["observed"] += 1
            record, reason = admit(item, root_id)
            if reason:
                counts["rejected"][reason] = counts["rejected"].get(reason, 0) + 1
                continue
            key = (record["source_file_id"], record["version_fingerprint_sha256"])
            if key in seen:
                continue
            seen.add(key)
            counts["admitted"] += 1
            # A downstream private executor must reject the entire batch
            # unless this process exits 0 and a complete receipt is verified.
            print(json.dumps(record, separators=(",", ":"), ensure_ascii=True))
    print(json.dumps({"stream_state": "PROVIDER_METADATA_STREAM_COMPLETE",
                      "counts": counts, "supabase_write": "NOT_RUN",
                      "client_e2e": "NOT_RUN", "claim_allowed": False}),
          file=sys.stderr)
    return counts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root-id", required=True)
    ap.add_argument("--fixture", help="Local Drive API JSON test fixture; no network")
    ap.add_argument("--page-size", type=int, default=250)
    ap.add_argument("--max-pages", type=int, default=4)
    args = ap.parse_args()
    if not (1 <= args.page_size <= 1000 and 1 <= args.max_pages <= 100):
        raise GateError("INVALID_PAGE_LIMITS")
    if args.fixture:
        with open(args.fixture, encoding="utf8") as f:
            fixture = json.load(f)
        if not isinstance(fixture, dict) or not isinstance(fixture.get("files"), list):
            raise GateError("INVALID_FIXTURE")
        iterator = iter([fixture["files"]])
    else:
        iterator = pages(args.root_id,
                         os.environ.get("GOOGLE_DRIVE_OAUTH_ACCESS_TOKEN"),
                         args.page_size, args.max_pages)
    run(args.root_id, iterator)


if __name__ == "__main__":
    try:
        main()
    except (GateError, OSError, ValueError) as exc:
        print("BLOCKED_METADATA_STREAM:" + type(exc).__name__, file=sys.stderr)
        sys.exit(3)
