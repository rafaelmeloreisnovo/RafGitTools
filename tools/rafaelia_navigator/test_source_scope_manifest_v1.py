"""Offline falsifiers for source-scope manifest. Synthetic records only."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from source_scope_manifest_v1 import SCHEMA, build, normalize, archive_kind


def drive():
    return {
        "schema": SCHEMA, "provider": "google_drive", "scope_id": "synthetic_folder",
        "observed_at": "2026-11-07T10:00:00Z", "complete": False,
        "items": [
            {"id": "a", "name": "big.zip", "mime_type": "application/zip", "size": "20000000000", "parents": ["folder"]},
            {"id": "b", "name": "unknown.zipraf", "mime_type": "application/x-zip", "size": None, "parents": ["folder"]},
            {"id": "c", "name": "part.zip.001", "mime_type": "application/octet-stream", "size": "0", "parents": ["folder"]},
        ],
    }


def org():
    return {
        "schema": SCHEMA, "provider": "github_installation", "scope_id": "installation:synthetic",
        "observed_at": "2026-11-07T10:00:00Z", "complete": False,
        "account": {"login": "ExampleOrg", "type": "Organization"},
        "items": [{"id": "42", "full_name": "ExampleOrg/private", "visibility": "private", "default_branch": "main"}],
    }


class SourceScopeTests(unittest.TestCase):
    def test_large_size_without_download(self):
        rows, receipt = normalize(drive())
        self.assertEqual(rows[0]["size_bytes"], "20000000000")
        self.assertFalse(rows[0]["content_read"])
        self.assertEqual(receipt["coverage"], "PARTIAL_PAGE")

    def test_null_is_not_zero(self):
        rows, _ = normalize(drive())
        self.assertIsNone(rows[1]["size_bytes"])
        self.assertEqual(rows[2]["size_bytes"], "0")

    def test_zipraf_not_automatically_zip(self):
        self.assertEqual(archive_kind("a.zipraf", "application/x-zip"), "ZIPRAF_CANDIDATE_UNVERIFIED_FORMAT")
        self.assertEqual(archive_kind("foo.zip.001", "application/octet-stream"), "SPLIT_ARCHIVE_PART")

    def test_org_type_retained(self):
        rows, receipt = normalize(org())
        self.assertEqual(rows[0]["owner_type"], "Organization")
        self.assertEqual(rows[0]["visibility"], "private")
        self.assertFalse(receipt["claim_allowed"])

    def test_wrong_owner_rejected(self):
        s = org()
        s["items"][0]["full_name"] = "SomeoneElse/private"
        with self.assertRaisesRegex(ValueError, "OWNER_SCOPE_MISMATCH"):
            normalize(s)

    def test_unknown_owner_type_rejected(self):
        s = org()
        s["account"]["type"] = "TOKEN_VAZIO"
        with self.assertRaisesRegex(ValueError, "EXPLICIT_OWNER_TYPE_REQUIRED"):
            normalize(s)

    def test_no_completeness_inference(self):
        s = drive()
        del s["complete"]
        with self.assertRaisesRegex(ValueError, "EXPLICIT_COMPLETENESS_REQUIRED"):
            normalize(s)

    def test_duplicate_id_rejected(self):
        s = drive()
        s["items"].append(copy.deepcopy(s["items"][0]))
        with self.assertRaisesRegex(ValueError, "DUPLICATE_SOURCE_ID"):
            normalize(s)

    def test_negative_size_rejected(self):
        s = drive()
        s["items"][0]["size"] = -1
        with self.assertRaisesRegex(ValueError, "INVALID_SIZE_RANGE"):
            normalize(s)

    def test_append_only_output_and_sha(self):
        with tempfile.TemporaryDirectory() as base:
            root = Path(base)
            src = root / "input.json"
            src.write_text(json.dumps(drive()), encoding="utf-8")
            out = root / "private"
            receipt = build(src, out)
            self.assertEqual(hashlib.sha256((out / "INDEX.jsonl").read_bytes()).hexdigest(), receipt["index_sha256"])
            with self.assertRaisesRegex(ValueError, "OUTPUT_EXISTS_APPEND_ONLY"):
                build(src, out)

    def test_explicit_complete_only_scope(self):
        s = org()
        s["complete"] = True
        _, r = normalize(s)
        self.assertEqual(r["coverage"], "ENUMERATED_SCOPE")
        self.assertEqual(r["gaps"], [])


if __name__ == "__main__":
    unittest.main()
