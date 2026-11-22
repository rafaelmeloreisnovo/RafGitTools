"""Adversarial tests for the APK-to-Middlefield proposal boundary."""
import copy
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from uuid import uuid4

HERE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(HERE))
import agent_workspace_v1 as sut


def valid_proposal():
    intent = "Revisar PR e preparar uma proposta de correção sem executar."
    return {
        "schema": sut.SCHEMA,
        "interaction_id": str(uuid4()),
        "origin": "RAFGITTOOLS_APK",
        "target_repository": sut.REPO,
        "requested_action": "PROPOSE_CHANGE",
        "intent": intent,
        "intent_sha256": hashlib.sha256(intent.encode("utf-8")).hexdigest(),
        "created_at_utc": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "approval_state": "PENDING_HUMAN",
        "execution_state": "NOT_EXECUTED",
        "claim_allowed": False,
        "receipt_ref": "TOKEN_VAZIO",
    }


class AgentWorkspaceAdmissionTest(unittest.TestCase):
    def test_valid_proposal_remains_unexecuted(self):
        r = sut.admission_receipt(valid_proposal())
        self.assertEqual(r["validation"], "PASS_PROPOSAL_SHAPE_ONLY")
        self.assertEqual(r["execution"], "NOT_EXECUTED")
        self.assertEqual(r["authority"], "TOKEN_VAZIO")
        self.assertFalse(r["claim_allowed"])

    def test_intent_edit_invalidates_hash(self):
        d = valid_proposal()
        d["intent"] += " execute!"
        with self.assertRaisesRegex(sut.AdmissionError, "HASH"):
            sut.validate(d)

    def test_untrusted_input_cannot_grant_approval(self):
        for field, value in [
            ("approval_state", "APPROVED"), ("execution_state", "RUNNING"),
            ("claim_allowed", True), ("receipt_ref", "fake:pass"),
        ]:
            with self.subTest(field=field):
                d = valid_proposal()
                d[field] = value
                with self.assertRaisesRegex(sut.AdmissionError, "PROMOTION"):
                    sut.validate(d)

    def test_target_and_operation_are_bound(self):
        for field, value in [
            ("target_repository", "owner/private-repo"),
            ("requested_action", "EXECUTE_SHELL"),
            ("origin", "UNKNOWN"),
        ]:
            with self.subTest(field=field):
                d = valid_proposal()
                d[field] = value
                with self.assertRaisesRegex(sut.AdmissionError, "SCOPE"):
                    sut.validate(d)

    def test_unknown_or_missing_field(self):
        d = valid_proposal()
        d["PAT_SECRET"] = "not-a-secret"
        with self.assertRaisesRegex(sut.AdmissionError, "FIELDS"):
            sut.validate(d)
        d = valid_proposal()
        del d["receipt_ref"]
        with self.assertRaisesRegex(sut.AdmissionError, "FIELDS"):
            sut.validate(d)

    def test_utf8_byte_size_is_enforced(self):
        d = valid_proposal()
        d["intent"] = "空" * 1500
        d["intent_sha256"] = hashlib.sha256(d["intent"].encode()).hexdigest()
        with self.assertRaisesRegex(sut.AdmissionError, "INTENT_SIZE"):
            sut.validate(d)

    def test_empty_and_whitespace_are_distinct(self):
        for text in ["", " ", " a", "a "]:
            with self.subTest(text=text):
                d = valid_proposal()
                d["intent"] = text
                d["intent_sha256"] = hashlib.sha256(text.encode()).hexdigest()
                with self.assertRaisesRegex(sut.AdmissionError, "INTENT"):
                    sut.validate(d)

    def test_invalid_uuid_or_future_timestamp(self):
        d = valid_proposal()
        d["interaction_id"] = "not-a-uuid"
        with self.assertRaisesRegex(sut.AdmissionError, "INTERACTION_ID"):
            sut.validate(d)
        d = valid_proposal()
        d["created_at_utc"] = (datetime.now(timezone.utc) +
                               timedelta(days=10)).isoformat().replace("+00:00", "Z")
        with self.assertRaisesRegex(sut.AdmissionError, "FUTURE_TIMESTAMP"):
            sut.validate(d)

    def test_cli_accepts_valid_json_without_dispatch(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "proposal.json"
            p.write_text(json.dumps(valid_proposal()), encoding="utf-8")
            self.assertEqual(sut.main([str(p)]), 0)

    def test_cli_rejects_duplicate_keys(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "proposal.json"
            d = valid_proposal()
            raw = json.dumps(d)
            p.write_text(raw[:-1] + ', "claim_allowed": true}', encoding="utf-8")
            self.assertEqual(sut.main([str(p)]), 3)

    def test_cli_rejects_non_json_and_oversized_file(self):
        with tempfile.TemporaryDirectory() as td:
            p = Path(td) / "proposal.json"
            p.write_text("not json", encoding="utf-8")
            self.assertEqual(sut.main([str(p)]), 3)
            p.write_bytes(b"a" * (sut.MAX_BYTES + 1))
            self.assertEqual(sut.main([str(p)]), 3)

    def test_unicode_hash_matches_android_utf8(self):
        d = valid_proposal()
        d["intent"] = "Δ Ω 空 🌱"
        d["intent_sha256"] = hashlib.sha256(d["intent"].encode("utf-8")).hexdigest()
        self.assertEqual(sut.validate(d)["intent_sha256"], d["intent_sha256"])


if __name__ == "__main__":
    unittest.main()
