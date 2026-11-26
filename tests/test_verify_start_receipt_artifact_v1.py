import hashlib
import io
import json
import pathlib
import sys
import struct
import unittest
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "scripts" / "ci"))
from verify_start_receipt_artifact_v1 import verify


class StartReceiptReadbackTests(unittest.TestCase):
    def setUp(self):
        self.binding = dict(repository="example/project", execution_sha="a"*40,
                            provider_sha="b"*40, run_id=1, job_id=2, run_attempt=2,
                            artifact_id=3, run_conclusion="success", job_conclusion="success")
        self.receipt = dict(schema="RAFAELIA_START_V1", repository="example/project",
                            execution_sha="a"*40, sha="a"*40, provider_sha="b"*40,
                            run_id="1", run_attempt="2", claim_allowed=False,
                            automatic_promotion=False, state="PASS_WITH_TYPED_SKIPS",
                            missing_required_gates=[], required_gates={"android":True},
                            stages={"android":"SUCCESS", "release":"TOKEN_VAZIO_NOT_SELECTED"})

    def archive(self, raw=None, name="START_RECEIPT.json"):
        out = io.BytesIO()
        with zipfile.ZipFile(out, "w") as z:
            z.writestr(name, json.dumps(self.receipt) if raw is None else raw)
        data = out.getvalue()
        self.binding["zip_sha256"] = hashlib.sha256(data).hexdigest()
        return data

    def test_positive_only_readback(self):
        r = verify(self.archive(), self.binding)
        self.assertEqual(r["state"], "BYTE_READBACK_ONLY")
        for k in ("claim_allowed", "skip_allowed", "check_status_write_allowed"):
            self.assertFalse(r[k])

    def test_same_name_old_attempt_rejected_even_with_its_digest(self):
        self.receipt["run_attempt"] = "1"
        self.assertEqual(verify(self.archive(), self.binding)["reason"], "ATTEMPT_OR_SHA_MISMATCH")

    def test_wrong_bytes_and_provider_failure(self):
        data = self.archive()
        self.assertEqual(verify(data+b"x", self.binding)["reason"], "ZIP_DIGEST_MISMATCH")
        self.binding["job_conclusion"] = "failure"
        self.assertEqual(verify(data, self.binding)["reason"], "PROVIDER_NOT_SUCCESS")

    def test_required_gate_and_claim(self):
        self.receipt["stages"]["android"] = "TOKEN_VAZIO_NOT_SELECTED"
        self.assertEqual(verify(self.archive(), self.binding)["reason"], "REQUIRED_GATE_MISSING")
        self.receipt["claim_allowed"] = True
        self.assertEqual(verify(self.archive(), self.binding)["reason"], "CLAIM_BOUNDARY")

    def test_zip_layout_and_duplicate_keys(self):
        self.assertEqual(verify(self.archive(name="../START_RECEIPT.json"), self.binding)["reason"], "INVALID_ARCHIVE_LAYOUT")
        self.assertEqual(verify(self.archive(raw='{"schema":"x","schema":"y"}'), self.binding)["reason"], "INVALID_ARCHIVE_OR_JSON")

    def test_invalid_id(self):
        self.binding["artifact_id"] = True
        self.assertEqual(verify(self.archive(), self.binding)["reason"], "INVALID_PROVIDER_ID")

    def test_non_json_numeric_constants_rejected(self):
        for value in (float("nan"), float("inf"), float("-inf")):
            with self.subTest(value=value):
                self.receipt["unexpected"] = value
                r = verify(self.archive(), self.binding)
                self.assertEqual(r["reason"], "INVALID_ARCHIVE_OR_JSON")
                self.assertEqual(r["state"], "REJECTED")

    def test_corrupted_deflate_is_typed_rejection(self):
        out = io.BytesIO()
        with zipfile.ZipFile(out, "w", compression=zipfile.ZIP_DEFLATED) as z:
            z.writestr("START_RECEIPT.json", json.dumps(self.receipt))
        data = bytearray(out.getvalue())
        self.binding["zip_sha256"] = hashlib.sha256(data).hexdigest()
        self.assertEqual(verify(bytes(data), self.binding)["state"], "BYTE_READBACK_ONLY")
        name_size, extra_size = struct.unpack_from("<HH", data, 26)
        data[30 + name_size + extra_size] = 0xff
        self.binding["zip_sha256"] = hashlib.sha256(data).hexdigest()
        r = verify(bytes(data), self.binding)
        self.assertEqual(r["reason"], "INVALID_ARCHIVE_OR_JSON")
        self.assertEqual(r["state"], "REJECTED")
        for k in ("claim_allowed", "skip_allowed", "check_status_write_allowed"):
            self.assertFalse(r[k])
