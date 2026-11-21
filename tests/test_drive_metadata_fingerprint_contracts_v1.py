#!/usr/bin/env python3
"""Synthetic-only producer contract tests; no credentials or live Drive reads."""
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest

SCRIPTS = Path(__file__).resolve().parents[1] / "scripts"


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, SCRIPTS / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

v1 = load("drive_meta_v1", "drive_debianum_metadata_stream_v1.py")
v2 = load("drive_meta_v2", "drive_debianum_recursive_stream_v2.py")
ROOT = "1xxxxxxxxxxxxxxxxxxxxxxx"
CHILD = "1yyyyyyyyyyyyyyyyyyyyyyy"
FILE = "1zzzzzzzzzzzzzzzzzzzzzzz"


def item(parent=ROOT):
    return {"id": FILE, "name": "synthetic.txt", "mimeType": "text/plain",
            "size": "42", "createdTime": "2026-11-20T08:00:00Z",
            "modifiedTime": "2026-11-20T09:00:00Z", "parents": [parent],
            "webViewLink": "https://drive.google.com/file/d/" + FILE + "/view",
            "sha256Checksum": "f" * 64}


def recompute(record):
    payload = {k: v for k, v in record.items() if k != "version_fingerprint_sha256"}
    canonical = json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=True)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


class ProducerFingerprintTests(unittest.TestCase):
    def test_sha256_canonical_v1(self):
        rec, err = v1.admit(item(), ROOT)
        self.assertIsNone(err)
        self.assertEqual(rec["version_fingerprint_sha256"], recompute(rec))

    def test_sha256_canonical_v2(self):
        rec, err = v2.classify(item(), ROOT, ROOT)
        self.assertIsNone(err)
        self.assertEqual(rec["version_fingerprint_sha256"], recompute(rec))

    def test_v1_and_v2_contracts_distinct_not_content(self):
        a, ea = v1.admit(item(), ROOT)
        b, eb = v2.classify(item(), ROOT, ROOT)
        self.assertIsNone(ea)
        self.assertIsNone(eb)
        self.assertNotEqual(a["schema"], b["schema"])
        self.assertNotEqual(a["version_fingerprint_sha256"], b["version_fingerprint_sha256"])
        self.assertIsNone(a["source_content_sha256"])
        self.assertIsNone(b["source_content_sha256"])
        self.assertEqual(a["provider_sha256"], "f" * 64)
        self.assertEqual(b["provider_sha256"], "f" * 64)

    def test_current_producers_never_emit_null_parent_when_admitted(self):
        a, ea = v1.admit(item(), ROOT)
        b, eb = v2.classify(item(), ROOT, ROOT)
        self.assertIsNone(ea)
        self.assertIsNone(eb)
        self.assertEqual(a["source_parent_id"], ROOT)
        self.assertEqual(b["source_parent_id"], ROOT)

    def test_change_real_parent_changes_v2_fingerprint(self):
        a, ea = v2.classify(item(), ROOT, ROOT)
        b, eb = v2.classify(item(CHILD), ROOT, CHILD)
        self.assertIsNone(ea)
        self.assertIsNone(eb)
        self.assertNotEqual(a["version_fingerprint_sha256"], b["version_fingerprint_sha256"])
        self.assertEqual(a["source_file_id"], b["source_file_id"])
        self.assertIsNone(a["source_content_sha256"])
        self.assertIsNone(b["source_content_sha256"])

    def test_same_input_deterministic(self):
        a, _ = v2.classify(item(), ROOT, ROOT)
        b, _ = v2.classify(item(), ROOT, ROOT)
        self.assertEqual(a["version_fingerprint_sha256"], b["version_fingerprint_sha256"])

    def test_invalid_parent_rejected_not_normalized_to_root(self):
        _, reason = v2.classify(item(CHILD), ROOT, ROOT)
        self.assertEqual(reason, "UNVERIFIED_PARENT")


if __name__ == "__main__":
    unittest.main()
