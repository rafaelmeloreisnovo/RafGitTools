import copy
import hashlib
import pathlib
import sys
import unittest
from datetime import datetime, timezone

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "scripts" / "ci"))
from receipt_reuse_gate_v1 import evaluate, DIGESTS, ATTESTATIONS


class ReceiptReuseTests(unittest.TestCase):
    def setUp(self):
        self.artifact = b"synthetic evidence"
        self.now = datetime(2026, 11, 25, 12, tzinfo=timezone.utc)
        self.current = dict(repository="example/project", target_branch="main",
                            gate_id="structural-check", gate_class="STRUCTURAL_OFFLINE",
                            reuse_reviewed=True, source_sha="b" * 40)
        self.current.update({k: "c" * 64 for k in DIGESTS})
        self.receipt = dict(self.current, source_sha="a" * 40, run_id=1, job_id=2,
                            attempt=1, artifact_id=3, conclusion="success",
                            artifact_sha256=hashlib.sha256(self.artifact).hexdigest(),
                            issued_at="2026-11-25T11:00:00Z", expires_at="2026-11-25T13:00:00Z")
        self.observed = {k: self.receipt[k] for k in ("repository", "source_sha", "artifact_sha256", "run_id", "job_id", "attempt", "artifact_id", "conclusion")}
        self.observed.update({k: True for k in ATTESTATIONS})
        self.observed.update(revoked=False, superseded=False, permission_invalidated=False)

    def run_gate(self):
        return evaluate(self.receipt, self.current, self.observed, self.artifact, self.now, 7200)

    def deny(self):
        r = self.run_gate()
        self.assertEqual(r["decision"], "RERUN_OR_BLOCK")
        self.assertFalse(r["skip_allowed"])
        self.assertFalse(r["claim_allowed"])

    def test_positive_never_authorizes_skip(self):
        r = self.run_gate()
        self.assertEqual(r["decision"], "CANDIDATE_ONLY")
        for k in ("skip_allowed", "check_status_write_allowed", "claim_allowed"):
            self.assertFalse(r[k])

    def test_each_closure_dimension(self):
        for k in DIGESTS:
            with self.subTest(k=k):
                old = self.current[k]
                self.current[k] = "d" * 64
                self.deny()
                self.current[k] = old

    def test_unknown_and_revoked_fail_closed(self):
        for k in (*ATTESTATIONS, "revoked", "superseded", "permission_invalidated"):
            with self.subTest(k=k):
                old = self.observed.pop(k)
                self.deny()
                self.observed[k] = old
        self.observed["revoked"] = True
        self.deny()

    def test_provider_identity_and_conclusion(self):
        for k, v in (("run_id", True), ("attempt", 2), ("source_sha", "f" * 40), ("conclusion", "skipped")):
            with self.subTest(k=k):
                old = self.observed[k]
                self.observed[k] = v
                self.deny()
                self.observed[k] = old

    def test_expiry_and_bytes(self):
        self.receipt["expires_at"] = "2026-11-25T12:00:00Z"
        self.deny()
        self.receipt["expires_at"] = "2026-11-25T13:00:00Z"
        self.artifact = b"tampered"
        self.deny()

    def test_sensitive_gate_or_other_repo(self):
        self.current["gate_class"] = "SECURITY"
        self.deny()
        self.current["gate_class"] = "STRUCTURAL_OFFLINE"
        self.current["repository"] = "example/other"
        self.deny()

    def test_unknown_age_and_unreviewed_policy(self):
        self.current["reuse_reviewed"] = False
        self.deny()
        self.current["reuse_reviewed"] = True
        self.receipt["issued_at"] = "TOKEN_VAZIO"
        self.deny()


if __name__ == "__main__":
    unittest.main()
