"""Offline falsifiers for Drive↔GitHub custody. BLAKE3 mocked here only.

Real cryptographic KAT and dual-file readback are executed in separate CI
with the pinned RMR pai source. These unit tests never certify cryptography.
"""
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest import mock

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/drive_custody_rmr_pair_v1.py"
SPEC = importlib.util.spec_from_file_location("drive_rmr_custody_v1", SCRIPT)
M = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(M)
REPO = "rafaelmeloreisnovo/RafGitTools"
COMMIT = "4f3425a3930b03621b3a0a82165429ef2cf5170f"


class DriveCustodyTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.origin = Path(self.tmp.name) / "origin.zip"
        self.readback = Path(self.tmp.name) / "download.zip"
        self.origin.write_bytes(b"PUBLIC-TEST-FIXTURE-NO-PRIVATE-PAYLOAD")
        self.readback.write_bytes(self.origin.read_bytes())
        self.d = hashlib.sha256(self.origin.read_bytes()).hexdigest()

    def run_gate(self, digest=None, readback=None, previous="0" * 64):
        return M.verify(
            self.origin, readback or self.readback,
            digest or self.d, COMMIT, REPO, 38711297966, 11961380420,
            "1fVDfnCtgp5GTBrWJ7erD6m9kJlMjI2Hj", previous,
            "/fake/never-passed-as-real",
        )

    def test_binary_integrity_fixture_with_mock_not_crypto_claim(self):
        with mock.patch.object(M, "rmr_pai_preflight"), mock.patch.object(
            M, "rmr_pai_blake3", return_value="a" * 64
        ):
            r = self.run_gate()
        self.assertEqual(r["readback_result"], "BYTES_MATCH_DUAL_HASH")
        self.assertEqual(r["content_sha256"], self.d)
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["promotion_allowed"])
        self.assertEqual(r["previous_receipt_sha256"], "0" * 64)

    def test_changed_drive_payload_fails(self):
        self.readback.write_bytes(b"CHANGED")
        with self.assertRaisesRegex(M.CustodyError, "TRANSFER_BYTE_COUNT_MISMATCH"):
            self.run_gate()

    def test_equal_size_changed_drive_payload_fails(self):
        self.readback.write_bytes(b"X" * self.origin.stat().st_size)
        with self.assertRaisesRegex(M.CustodyError, "DRIVE_READBACK_SHA256_MISMATCH"):
            self.run_gate()

    def test_falsified_artifact_digest_fails(self):
        with self.assertRaisesRegex(M.CustodyError, "PROVIDER_SOURCE_SHA256_MISMATCH"):
            self.run_gate(digest="f" * 64)

    def test_wrong_sha_length_fails(self):
        with self.assertRaisesRegex(M.CustodyError, "INVALID_PROVIDER_DIGEST"):
            self.run_gate(digest="token-vazio")

    def test_rmr_absent_fails_closed_even_if_sha256_matches(self):
        with self.assertRaisesRegex(M.CustodyError, "RMR_PAI_EXECUTABLE_NOT_FOUND"):
            self.run_gate()

    def test_blake3_mismatch_fails_closed(self):
        with mock.patch.object(M, "rmr_pai_preflight"), mock.patch.object(
            M, "rmr_pai_blake3", side_effect=["a" * 64, "b" * 64]
        ):
            with self.assertRaisesRegex(M.CustodyError, "DRIVE_READBACK_BLAKE3_MISMATCH"):
                self.run_gate()

    def test_source_changed_during_blake3_fails_closed(self):
        # Simulate an attacker or a concurrent writer after the first SHA-256.
        calls = [0]
        def mutate_then_return(pai, path):
            calls[0] += 1
            if calls[0] == 1:
                self.origin.write_bytes(b"MUTATED" + self.origin.read_bytes())
            return "a" * 64
        with mock.patch.object(M, "rmr_pai_preflight"), mock.patch.object(
            M, "rmr_pai_blake3", side_effect=mutate_then_return
        ):
            with self.assertRaisesRegex(M.CustodyError, "BYTES_CHANGED_DURING_HASH_VERIFICATION"):
                self.run_gate()

    def test_drive_readback_changed_during_blake3_fails_closed(self):
        calls = [0]
        def mutate_then_return(pai, path):
            calls[0] += 1
            if calls[0] == 2:
                self.readback.write_bytes(b"MUTATED" + self.readback.read_bytes())
            return "a" * 64
        with mock.patch.object(M, "rmr_pai_preflight"), mock.patch.object(
            M, "rmr_pai_blake3", side_effect=mutate_then_return
        ):
            with self.assertRaisesRegex(M.CustodyError, "BYTES_CHANGED_DURING_HASH_VERIFICATION"):
                self.run_gate()

    def test_predecessor_invalid(self):
        with mock.patch.object(M, "rmr_pai_preflight"), mock.patch.object(
            M, "rmr_pai_blake3", return_value="a" * 64
        ):
            with self.assertRaisesRegex(M.CustodyError, "INVALID_PREDECESSOR_DIGEST"):
                self.run_gate(previous="TOKEN_VAZIO")

    def test_chain_is_deterministic_and_domain_separated(self):
        body = {"content_sha256": "b" * 64}
        c1 = M.chain_digest("0" * 64, body)
        self.assertEqual(c1, M.chain_digest("0" * 64, body))
        self.assertNotEqual(c1, M.chain_digest("1" * 64, body))
        self.assertNotEqual(c1, M.chain_digest("0" * 64, {"content_sha256": "c" * 64}))

    def test_source_sha_and_byte_count_streaming(self):
        digest, n = M.sha256_file(self.origin)
        self.assertEqual(digest, self.d)
        self.assertEqual(n, len(self.origin.read_bytes()))

    def test_rmr_pai_router_arguments_are_exact(self):
        completed = mock.Mock(returncode=0,
            stdout="a" * 64 + "  " + str(self.origin) + "\n")
        with mock.patch.object(M.subprocess, "run", return_value=completed) as run:
            p = Path(self.tmp.name) / "pai"
            p.write_text("fixture")
            M.rmr_pai_blake3(p, self.origin)
            argv = run.call_args.args[0]
            self.assertEqual(argv, [
                str(p), "run", "--mode", "cli", "hash",
                "--file", str(self.origin), "--algo", "blake3",
            ])
            self.assertFalse(run.call_args.kwargs["shell"])

    def test_rmr_output_binds_exact_filename(self):
        p = Path(self.tmp.name) / "pai"
        p.write_text("#!/bin/sh\nprintf '%064d  WRONG_FILENAME\\n' 0\n")
        p.chmod(0o700)
        with self.assertRaisesRegex(M.CustodyError, "RMR_PAI_BAD_DIGEST_OR_PATH"):
            M.rmr_pai_blake3(p, self.origin)


if __name__ == "__main__":
    unittest.main()
