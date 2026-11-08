#!/usr/bin/env python3
"""Offline, non-Drive validation; cannot substitute Android/device end-to-end tests."""
from pathlib import Path
import json
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/org/rafaelia/drivesnapshot"
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml")
assert manifest.getroot().tag == "manifest"
source = {p.stem: p.read_text(encoding="utf-8") for p in java.glob("*.java")}
assert {"MainActivity", "ScanService", "DriveClient", "SnapshotDb", "JsonlCapture"} <= set(source)
assert "files(*)" in source["DriveClient"]
assert "pageSize=50" in source["DriveClient"]
assert "nextPageToken" in source["JsonlCapture"]
assert "incompleteSearch" in source["JsonlCapture"]
assert "beginTransaction()" in source["JsonlCapture"]
assert "pending_next" in source["ScanService"]
assert "pending" in source["ScanService"]
assert "REMOTE_SHARD_COLLISION" in source["DriveClient"]
assert "getAccessToken" in source["MainActivity"]
assert "snapshot_owner" in source["DriveClient"]
assert "appProperties" in source["JsonlCapture"]
assert '"__ORGANIZANDO"' in source["ScanService"]
assert "application/vnd.google-apps.folder" in source["SnapshotDb"]
assert "TOKEN_VAZIO" in source["SnapshotDb"]
assert "metadata.readonly" in source["MainActivity"]
assert "drive.file" in source["MainActivity"]
# A JSONL fixture with heterogeneous record shapes; no private conversation data.
fixtures = [
    {"schema": "rafaelia.drive.file.v1", "object": {"id": "folderA", "name": "same", "parents": ["root"], "mimeType": "application/vnd.google-apps.folder"}},
    {"schema": "rafaelia.drive.file.v1", "object": {"id": "fileA", "name": "same", "parents": ["folderA"], "size": "0", "mimeType": "application/zip"}},
    {"schema": "rafaelia.drive.file.v1", "object": {"id": "fileB", "name": "same", "parents": ["folderA"], "mimeType": "application/vnd.google-apps.document"}},
]
wire = "\n".join(json.dumps(f, separators=(",", ":")) for f in fixtures) + "\n"
records = [json.loads(line) for line in wire.splitlines()]
by_id = {r["object"]["id"]: r["object"] for r in records}
assert len(by_id) == 3
assert by_id["fileA"]["size"] == "0"
assert "size" not in by_id["fileB"]
assert by_id["fileA"]["name"] == by_id["fileB"]["name"]
assert all(r["schema"] == "rafaelia.drive.file.v1" for r in records)
for item in source.values():
    assert not re.search(r"AIza[0-9A-Za-z_-]{25,}", item), "embedded API key forbidden"
print("PASS_SCOPED: XML, source guards and JSONL fixture (not an APK/device test)")
