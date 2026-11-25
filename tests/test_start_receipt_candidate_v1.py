import hashlib
import io
import json
import pathlib
import sys
import unittest
import zipfile
from datetime import datetime, timezone

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1] / "scripts" / "ci"))
from receipt_reuse_gate_v1 import DIGESTS
from start_receipt_candidate_v1 import assess, GATE_ID, FINAL_JOB


class StartCandidateTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 11, 25, 12, tzinfo=timezone.utc)
        self.binding = dict(repository="example/project", execution_sha="a"*40,
                            provider_sha="b"*40, run_id=1, job_id=2, run_attempt=2,
                            artifact_id=3, run_conclusion="success", job_conclusion="success",
                            job_name=FINAL_JOB, artifact_expired=False, artifact_run_id=1,
                            target_branch="main")
        self.summary = dict(schema="RAFAELIA_START_V1", repository="example/project",
                            execution_sha="a"*40, sha="a"*40, provider_sha="b"*40,
                            run_id="1", run_attempt="2", base_ref="main", claim_allowed=False,
                            automatic_promotion=False, state="PASS_WITH_TYPED_SKIPS",
                            missing_required_gates=[], required_gates={"android":True},
                            stages={"android":"SUCCESS"})
        self.current = dict(repository="example/project", target_branch="main", gate_id=GATE_ID,
                            gate_class="STRUCTURAL_OFFLINE", reuse_reviewed=True, source_sha="c"*40,
                            **{k:"d"*64 for k in DIGESTS})
        self.receipt = dict(self.current, source_sha="a"*40, run_id=1, job_id=2, attempt=2,
                            artifact_id=3, conclusion="success", issued_at="2026-11-25T11:00:00Z",
                            expires_at="2026-11-25T13:00:00Z")
        self.observed = dict(self.receipt, producer_authenticated=True, runner_trusted=True,
                             dependency_closure_verified=True, authority_current=True,
                             revoked=False, superseded=False, permission_invalidated=False,
                             artifact_verified=False, consumer_readback=False)

    def run_gate(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("START_RECEIPT.json", json.dumps(self.summary))
        archive = buf.getvalue()
        digest = hashlib.sha256(archive).hexdigest()
        self.binding["zip_sha256"] = digest
        self.receipt["artifact_sha256"] = digest
        self.observed["artifact_sha256"] = digest
        return assess(archive, self.binding, self.receipt, self.current, self.observed, self.now, 7200)

    def deny(self):
        r = self.run_gate()
        self.assertEqual(r["decision"], "RERUN_OR_BLOCK")
        self.assertFalse(r["skip_allowed"])
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["check_status_write_allowed"])

    def test_positive_only_candidate_with_derived_bytes(self):
        r = self.run_gate()
        self.assertEqual(r["decision"], "CANDIDATE_ONLY")
        self.assertEqual(r["byte_readback"]["state"], "BYTE_READBACK_ONLY")
        self.assertFalse(r["skip_allowed"])

    def test_no_underlying_job_inference(self):
        self.current["gate_id"] = self.receipt["gate_id"] = "android"
        self.deny()

    def test_missing_independent_closure(self):
        for k in DIGESTS:
            with self.subTest(k=k):
                old = self.observed.pop(k)
                self.deny()
                self.observed[k] = old

    def test_wrong_provider_attempt_or_job(self):
        self.summary["run_attempt"] = "1"
        self.deny()
        self.summary["run_attempt"] = "2"
        self.binding["job_name"] = "Android"
        self.deny()

    def test_cross_branch_and_expiration(self):
        self.summary["base_ref"] = "other"
        self.deny()
        self.summary["base_ref"] = "main"
        self.binding["artifact_expired"] = True
        self.deny()

    def test_attestations_not_synthesized(self):
        for k in ("producer_authenticated", "runner_trusted", "dependency_closure_verified", "authority_current", "revoked"):
            with self.subTest(k=k):
                old = self.observed.pop(k)
                self.deny()
                self.observed[k] = old

    def test_provider_lifecycle_conflict(self):
        self.observed["job_id"] = 999
        self.deny()
        self.observed["job_id"] = 2
        self.observed["revoked"] = True
        self.deny()
