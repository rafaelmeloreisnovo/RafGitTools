#!/usr/bin/env python3
"""Offline adversarial tests; no Google API, no database, no real ps."""
import contextlib
import importlib.util
import io
from pathlib import Path
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "drive_debianum_recursive_stream_v2.py"
spec = importlib.util.spec_from_file_location("recursive", SCRIPT)
s = importlib.util.module_from_spec(spec)
spec.loader.exec_module(s)
ROOT = "1qXzHJdOXPzRRM-mDXmDMZhEuSMSWiqm3"
CHILD = "1abcdefabcdefabcdef1234"


def item(id="1abcdefabcdef1234abcde", name="manifest.txt", parent=ROOT,
         mime="text/plain", size="123", created="1970-01-01T00:00:00Z",
         modified="2025-09-29T00:34:54.286Z"):
    result = {"id": id, "name": name, "mimeType": mime, "parents": [parent],
              "size": size, "createdTime": created, "modifiedTime": modified}
    if mime == s.FOLDER:
        result.pop("size", None)
    return result


def fixture_tree():
    return {
        ROOT: [[item(id=CHILD, name="src", mime=s.FOLDER),
                item(id="1aaaaaaaaaaaaaaaaaaaaaaaaaaa", name="inventory.txt", size="10875857901"),
                item(id="1bbbbbbbbbbbbbbbbbbbbbbbbbbb", name=".pid_ls", mime="application/octet-stream"),
                item(id="1ccccccccccccccccccccccccccc", name="run.sh", mime="application/x-shellscript")]],
        CHILD: [[item(id="1ddddddddddddddddddddddddddd", name="source.c",
                      parent=CHILD, mime="text/x-c", size="64")]]
    }


class RecursiveV2Tests(unittest.TestCase):
    def test_typed_creation_gap_not_epoch(self):
        row, reason = s.classify(item(), ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertIsNone(row["provider_created_at"])
        self.assertEqual(row["created_state"], "TOKEN_VAZIO_CREATION_TIME")

    def test_huge_file_stays_metadata_only(self):
        row, reason = s.classify(item(size="10875857901"), ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertEqual(row["size_bytes"], 10875857901)
        self.assertIsNone(row["source_content_sha256"])
        self.assertFalse(row["claim_allowed"])

    def test_uploaded_json_mime_from_drive_is_admitted_without_reading_body(self):
        value = item(name="conversations-049.json", mime="text/x-json", size="47806754")
        row, reason = s.classify(value, ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertEqual(row["mime_type"], "text/x-json")
        self.assertEqual(row["size_bytes"], 47806754)
        self.assertEqual(row["content_evidence"], "NOT_READ")
        self.assertIsNone(row["source_content_sha256"])
        self.assertFalse(row["claim_allowed"])

    def test_uploaded_json_mime_does_not_weaken_rejection_rules(self):
        self.assertEqual(
            s.classify(item(name="password.txt", mime="text/x-json"), ROOT, ROOT)[1],
            "SENSITIVE_NAME"
        )
        self.assertEqual(
            s.classify(item(name="conversations-050.json", mime="text/x-json", size="broken"), ROOT, ROOT)[1],
            "INVALID_SIZE"
        )
        self.assertEqual(
            s.classify(item(name="conversations-050.json", mime="application/octet-stream"), ROOT, ROOT)[1],
            "DISALLOWED_MIME"
        )

    def test_hidden_and_secret_names_rejected(self):
        for name in [".pid_ls", ".ssh", ".bash_history", "auth-token.json",
                     "password.txt", "private_key.pem"]:
            with self.subTest(name=name):
                self.assertEqual(s.classify(item(name=name), ROOT, ROOT)[1],
                                 "SENSITIVE_NAME")

    def test_binary_shell_and_archive_rejected(self):
        for mime in ["application/x-shellscript", "application/gzip",
                     "application/octet-stream", "text/x-shellscript-source"]:
            with self.subTest(mime=mime):
                self.assertEqual(s.classify(item(mime=mime), ROOT, ROOT)[1],
                                 "DISALLOWED_MIME")

    def test_parent_gate(self):
        self.assertEqual(s.classify(item(parent=CHILD), ROOT, ROOT)[1],
                         "UNVERIFIED_PARENT")

    def test_modified_time_gate(self):
        self.assertEqual(s.classify(item(modified="1970-01-01T00:00:00Z"),
                                    ROOT, ROOT)[1], "INVALID_MODIFIED_TIME")
        self.assertEqual(s.classify(item(modified="2025-10-01"), ROOT, ROOT)[1],
                         "INVALID_MODIFIED_TIME")

    def test_file_size_gate(self):
        for size in [-1, "broken", 2**64]:
            with self.subTest(size=size):
                self.assertEqual(s.classify(item(size=size), ROOT, ROOT)[1],
                                 "INVALID_SIZE")

    def test_native_doc_without_size(self):
        row = item(mime="application/vnd.google-apps.document")
        row.pop("size")
        out, err = s.classify(row, ROOT, ROOT)
        self.assertIsNone(err)
        self.assertIsNone(out["size_bytes"])

    def test_recursive_parent_identity_and_counts(self):
        tree = fixture_tree()
        def load(parent):
            return iter(tree[parent])
        out = io.StringIO()
        receipt = s.walk(ROOT, load, out)
        rows = [__import__("json").loads(line) for line in out.getvalue().splitlines()]
        self.assertEqual(receipt["counts"]["folders_scanned"], 2)
        self.assertEqual(receipt["counts"]["observed"], 5)
        self.assertEqual(receipt["counts"]["admitted"], 3)
        self.assertEqual(len(rows), 3)
        self.assertEqual(rows[-1]["source_parent_id"], CHILD)
        self.assertEqual(rows[-1]["source_root_id"], ROOT)
        self.assertFalse(receipt["supabase_written"])
        self.assertFalse(receipt["ps_live_verified"])

    def test_paginated_duplicates_dedup(self):
        one = item()
        pages = {ROOT: [[one], [one]]}
        out = io.StringIO()
        receipt = s.walk(ROOT, lambda p: iter(pages[p]), out)
        self.assertEqual(receipt["counts"]["pages"], 2)
        self.assertEqual(receipt["counts"]["admitted"], 1)
        self.assertEqual(len(out.getvalue().splitlines()), 1)

    def test_failure_staged_no_partial_stdout(self):
        tree = {ROOT: [[item(id=CHILD, name="sub", mime=s.FOLDER)]]}
        out = io.StringIO()
        with self.assertRaises(KeyError):
            s.walk(ROOT, lambda p: iter(tree[p]), out)
        self.assertEqual(out.getvalue(), "")

    def test_max_depth_does_not_emit_partial(self):
        out = io.StringIO()
        with self.assertRaises(s.GateError):
            s.walk(ROOT, lambda p: iter(fixture_tree()[p]), out, max_depth=0)
        self.assertEqual(out.getvalue(), "")

    def test_spool_limit_does_not_emit_partial(self):
        out = io.StringIO()
        with self.assertRaises(s.GateError):
            s.walk(ROOT, lambda p: iter(fixture_tree()[p]), out, max_spool_bytes=30)
        self.assertEqual(out.getvalue(), "")

    def test_bad_page_fails_closed(self):
        out = io.StringIO()
        with self.assertRaises(s.GateError):
            s.walk(ROOT, lambda p: iter([{"not": "a page"}]), out)
        self.assertEqual(out.getvalue(), "")

    def test_no_token_no_network(self):
        with self.assertRaises(s.GateError):
            list(s.google_pages(ROOT, None, 100, 2))

    def test_untrusted_url_rewritten_to_drive(self):
        v = item()
        v["webViewLink"] = "https://some-other-domain.example/other"
        out, reason = s.classify(v, ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertTrue(out["provider_url"].startswith("https://drive.google.com/"))

    def test_checksum_is_metadata_fingerprint_not_content_digest(self):
        v = item()
        v["sha256Checksum"] = "a" * 64
        out, reason = s.classify(v, ROOT, ROOT)
        self.assertIsNone(reason)
        self.assertEqual(out["provider_sha256"], "a" * 64)
        self.assertIsNone(out["source_content_sha256"])


if __name__ == "__main__":
    unittest.main()
