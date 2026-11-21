#!/usr/bin/env python3
"""Synthetic-only failure-mode tests for the Drive identity manifest verifier."""
import copy
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "drive_identity_partition_reconcile_v1.py"
spec = importlib.util.spec_from_file_location("partition_reconcile", SCRIPT)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

ACCOUNT = "123456789012345678901"  # synthetic account-shaped test fixture
A = "1abcdefabcdefabcdef1234"
B = "1bbbbbbbbbbbbbbbbbbbbb"
ROOT = "1qXzHJdOXPzRRM-mDXmDMZhEuSMSWiqm3"


def fixture():
    return {"schema": m.SCHEMA, "provider": "google_drive",
            "account_id": ACCOUNT, "query_scope": "mimeType=folder;trashed=false",
            "expected_positions": 2,
            "pages": [
                {"page_index": 0, "incomplete_search": False,
                 "next_page_present": True, "observed_at": "2026-11-21T10:00:00Z",
                 "items": [{"id": A, "modified_time": "2026-11-20T10:00:00Z", "parent_ids": [ROOT]}]},
                {"page_index": 1, "incomplete_search": False,
                 "next_page_present": False, "observed_at": "2026-11-21T10:00:01Z",
                 "items": [{"id": B, "modified_time": "2026-11-20T10:00:00Z", "parent_ids": [ROOT]}]}
            ]}


class ManifestTests(unittest.TestCase):
    def assert_blocked(self, value, token):
        with self.assertRaisesRegex(m.GateError, token):
            m.capture(value)

    def test_same_complete_capture(self):
        result = m.reconcile(fixture(), copy.deepcopy(fixture()))
        self.assertTrue(result["same_identity_set"])
        self.assertTrue(result["same_metadata_if_comparable"])
        self.assertEqual(result["b0160_promotion"], "BLOCKED")
        self.assertEqual(result["provider_snapshot_isolation"], "NOT_PROVEN")
        self.assertFalse(result["claim_allowed"])

    def test_reordered_pages_same_identity_set(self):
        value = fixture()
        value["pages"][0]["items"][0]["id"] = B
        value["pages"][1]["items"][0]["id"] = A
        self.assertTrue(m.reconcile(fixture(), value)["same_identity_set"])

    def test_cross_page_duplicate_rejected(self):
        value = fixture()
        value["pages"][1]["items"][0]["id"] = A
        self.assert_blocked(value, "DUPLICATE_ID")

    def test_same_page_duplicate_rejected(self):
        value = fixture()
        value["pages"][0]["items"].append(copy.deepcopy(value["pages"][0]["items"][0]))
        value["expected_positions"] = 3
        self.assert_blocked(value, "DUPLICATE_ID")

    def test_partial_cursor_rejected(self):
        value = fixture()
        value["pages"][1]["next_page_present"] = True
        self.assert_blocked(value, "CURSOR_CHAIN_INCOMPLETE")

    def test_incomplete_search_rejected(self):
        value = fixture()
        value["pages"][0]["incomplete_search"] = True
        self.assert_blocked(value, "COMPLETENESS")

    def test_page_gap_rejected(self):
        value = fixture()
        value["pages"][1]["page_index"] = 3
        self.assert_blocked(value, "PAGE_ORDER")

    def test_timestamp_missing_not_zero(self):
        value = fixture()
        value["pages"][0].pop("observed_at")
        self.assert_blocked(value, "TIMESTAMP_TOKEN_VAZIO")

    def test_no_hash_if_metadata_missing(self):
        value = fixture()
        value["pages"][0]["items"][0].pop("parent_ids")
        out = m.capture(value)
        self.assertEqual(out["metadata_manifest_sha256"], "TOKEN_VAZIO")
        self.assertNotEqual(out["identity_manifest_sha256"], "TOKEN_VAZIO")
        self.assertEqual(out["source_content_sha256"], "TOKEN_VAZIO")

    def test_drift_not_pass(self):
        value = fixture()
        value["pages"][1]["items"][0]["id"] = "1zzzzzzzzzzzzzzzzzzzzzz"
        result = m.reconcile(fixture(), value)
        self.assertFalse(result["same_identity_set"])
        self.assertEqual(result["temporal_result"], "IDENTITY_DRIFT_OBSERVED")
        self.assertFalse(result["claim_allowed"])

    def test_cross_account_rejected(self):
        value = fixture()
        value["account_id"] = "999888777666555444333"
        with self.assertRaisesRegex(m.GateError, "SNAPSHOT_SCOPE_MISMATCH"):
            m.reconcile(fixture(), value)

    def test_opaque_cursor_not_in_manifest(self):
        value = fixture()
        out = m.capture(value)
        self.assertNotIn("cursor", str(out).lower())


if __name__ == "__main__":
    unittest.main()
