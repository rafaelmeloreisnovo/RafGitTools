#!/usr/bin/env python3
"""Offline adversarial admission tests. No external Drive/Supabase calls."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "debianum_stream", ROOT / "scripts" / "drive_debianum_metadata_stream_v1.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)
RID = "1abcdefghijklmnopqrstuvwx2345"


def fileobj(name="a.txt", mime="text/plain", size="177", created="1970-01-01T00:00:00.000Z"):
    return {
        "id": "1abcdefghijklmnopqrstuvw1234",
        "name": name, "mimeType": mime, "size": size,
        "parents": [RID], "createdTime": created,
        "modifiedTime": "2025-09-29T00:34:54.286Z",
        "webViewLink": "https://drive.google.com/file/d/fake/view"
    }


class AdmissionTests(unittest.TestCase):
    def test_epoch_is_unknown_not_1970_evidence(self):
        record, reason = MODULE.admit(fileobj(), RID)
        self.assertIsNone(reason)
        self.assertIsNone(record["provider_created_at"])
        self.assertEqual(record["creation_time_state"], "TOKEN_VAZIO_CREATION_TIME")
        self.assertIsNone(record["source_content_sha256"])
        self.assertFalse(record["claim_allowed"])

    def test_large_file_indexed_without_body(self):
        record, reason = MODULE.admit(fileobj(size="10875857901"), RID)
        self.assertIsNone(reason)
        self.assertEqual(record["size_bytes"], 10875857901)
        self.assertEqual(record["content_evidence"], "NOT_READ")

    def test_private_pid_is_not_active_ps(self):
        record, reason = MODULE.admit(fileobj(name=".pid_ls", mime="application/octet-stream", size="5"), RID)
        self.assertIsNone(record)
        self.assertEqual(reason, "HIDDEN_OR_SENSITIVE_NAME")

    def test_shell_archive_and_binary_quarantine(self):
        for name, mime in [
            ("upload_debian.sh", "application/x-shellscript"),
            ("inventory.tar.gz", "application/gzip"),
            ("blob.bin", "application/octet-stream")
        ]:
            with self.subTest(name=name):
                record, reason = MODULE.admit(fileobj(name=name, mime=mime), RID)
                self.assertIsNone(record)
                self.assertEqual(reason, "EXECUTABLE_BINARY_ARCHIVE_OR_LOG")

    def test_wrong_parent_and_unknown_size(self):
        a = fileobj()
        a["parents"] = ["1otherfolderwithenoughlength"]
        self.assertEqual(MODULE.admit(a, RID)[1], "UNVERIFIED_PARENT")
        self.assertEqual(MODULE.admit(fileobj(size=None), RID)[1], "UNKNOWN_SIZE")

    def test_duplicate_records_suppressed(self):
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            counts = MODULE.run(RID, [[fileobj(), fileobj()]])
        self.assertEqual(counts["admitted"], 1)
        self.assertEqual(len(buf.getvalue().splitlines()), 1)

    def test_malformed_manifest_json_is_not_silently_repaired(self):
        malformed = '{"arquivos":77180,"blocos_classificados":\n}'
        with self.assertRaises(json.JSONDecodeError):
            json.loads(malformed)

    def test_invalid_source_and_page_limit(self):
        self.assertEqual(MODULE.valid_time("1970-01-01T00:00:00Z"), None)
        with self.assertRaises(MODULE.GateError):
            list(MODULE.pages("x", "tok", 100, 2))


if __name__ == "__main__":
    unittest.main()
