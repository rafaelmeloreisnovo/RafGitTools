#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_runtime_lock_successor.py"
SPEC = importlib.util.spec_from_file_location("validate_runtime_lock_successor", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)

PREDECESSOR = ROOT / "data/runtime-lock-candidates/runtime-lock-candidate-20261001T193906Z.json"
OBSERVATION = ROOT / "data/runtime-lock-candidates/runtime-lock-observation-20261003T214824Z.json"
CANDIDATE = ROOT / "data/runtime-lock-candidates/runtime-lock-candidate-20261003T214824Z.json"
RECEIPT = ROOT / "data/runtime-lock-candidates/runtime-lock-refresh-receipt-20261003T214824Z.json"


class RuntimeLockSuccessor20261003Test(unittest.TestCase):
    def test_exact_successor_is_reproducible_and_not_promotable(self) -> None:
        report = module.evaluate(PREDECESSOR, OBSERVATION, CANDIDATE, RECEIPT)
        self.assertEqual("PASS", report["state"])
        self.assertEqual(4, report["changed_count"])
        self.assertEqual(12, report["artifact_hash_blocker_count"])
        self.assertEqual("BLOCKED", report["promotion_state"])
        self.assertEqual("NOT_RUN", report["integration_execution_evidence"])
        self.assertEqual("NOT_RUN", report["promotion_authorization_evidence"])
        self.assertFalse(report["claim_allowed"])

    def test_candidate_byte_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            changed = Path(tmp) / "candidate.json"
            data = json.loads(CANDIDATE.read_text(encoding="utf-8"))
            data["release_state"] = "DRIFT"
            changed.write_bytes(module.refresh.canonical_bytes(data))
            with self.assertRaisesRegex(module.SuccessorGateError, "candidate bytes do not reproduce"):
                module.evaluate(PREDECESSOR, OBSERVATION, changed, RECEIPT)

    def test_receipt_byte_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            changed = Path(tmp) / "receipt.json"
            data = json.loads(RECEIPT.read_text(encoding="utf-8"))
            data["promoted"] = True
            changed.write_bytes(module.refresh.canonical_bytes(data))
            with self.assertRaisesRegex(module.SuccessorGateError, "receipt bytes do not bind"):
                module.evaluate(PREDECESSOR, OBSERVATION, CANDIDATE, changed)

    def test_artifact_hash_invention_breaks_reproducibility(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            changed = Path(tmp) / "candidate.json"
            data = json.loads(CANDIDATE.read_text(encoding="utf-8"))
            data["repositories"][0]["expected_hashes"]["manifest_sha256"] = "1" * 64
            changed.write_bytes(module.refresh.canonical_bytes(data))
            with self.assertRaisesRegex(module.SuccessorGateError, "candidate bytes do not reproduce"):
                module.evaluate(PREDECESSOR, OBSERVATION, changed, RECEIPT)


if __name__ == "__main__":
    unittest.main()
