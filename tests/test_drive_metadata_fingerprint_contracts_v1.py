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



class SizeValidationTests(unittest.TestCase):
    """Synthetic-only; exercise full source modules via importlib, no Drive calls."""

    def setUp(self):
        self.valid = item()

    def each(self, value):
        return (v1.admit({**self.valid, "size": value}, ROOT),
                v2.classify({**self.valid, "size": value}, ROOT, ROOT))

    def test_reject_true(self):
        for record, reason in self.each(True):
            self.assertIsNone(record)
            self.assertEqual(reason, "INVALID_SIZE")

    def test_reject_false(self):
        for record, reason in self.each(False):
            self.assertIsNone(record)
            self.assertEqual(reason, "INVALID_SIZE")

    def test_reject_fractional_float(self):
        for record, reason in self.each(3.8):
            self.assertIsNone(record)
            self.assertEqual(reason, "INVALID_SIZE")

    def test_reject_integral_float(self):
        for record, reason in self.each(3.0):
            self.assertIsNone(record)
            self.assertEqual(reason, "INVALID_SIZE")

    def test_accept_numeric_string(self):
        for record, reason in self.each("42"):
            self.assertIsNone(reason)
            self.assertEqual(record["size_bytes"], 42)

    def test_accept_integer(self):
        for record, reason in self.each(42):
            self.assertIsNone(reason)
            self.assertEqual(record["size_bytes"], 42)

    def test_negative_and_overflow(self):
        for size in (-1, str(2**63)):
            for record, reason in self.each(size):
                self.assertIsNone(record)
                self.assertEqual(reason, "INVALID_SIZE")

    def test_folder_unchanged(self):
        source = {**self.valid, "mimeType": "application/vnd.google-apps.folder", "size": None}
        for record, reason in (v1.admit(source, ROOT), v2.classify(source, ROOT, ROOT)):
            self.assertIsNone(reason)
            self.assertIsNone(record["size_bytes"])

    def test_native_doc_without_size_v2(self):
        source = {**self.valid, "mimeType": "application/vnd.google-apps.document"}
        source.pop("size")
        record, reason = v2.classify(source, ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertIsNone(record["size_bytes"])

if __name__ == "__main__":
    unittest.main()
