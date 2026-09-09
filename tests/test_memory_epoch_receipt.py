from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = ROOT / "tools" / "validate_memory_epoch_receipt.py"
FIXTURE = ROOT / "fixtures" / "memory_epoch" / "positive.v1.json"

spec = importlib.util.spec_from_file_location("validate_memory_epoch_receipt", VALIDATOR_PATH)
validator = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(validator)


class MemoryEpochReceiptTests(unittest.TestCase):
    def fixture(self):
        return json.loads(FIXTURE.read_text(encoding="utf-8"))

    def assertRejected(self, data, text):
        with self.assertRaises(validator.ValidationError) as ctx:
            validator.validate(data)
        self.assertIn(text, str(ctx.exception))

    def test_positive_fixture_passes(self):
        result = validator.validate(self.fixture())
        self.assertEqual(result["decision"], "PROMOTE")
        self.assertTrue(result["promotion_allowed"])
        self.assertFalse(result["claim_allowed"])

    def test_claimed_scope_cannot_exceed_evidence(self):
        data = self.fixture()
        data["memory_epoch"]["claimed_scope"] = "HOST"
        data["memory_epoch"]["evidenced_scope"] = "CONTAINER"
        data["decision"] = "BLOCK"
        self.assertRejected(data, "claimed scope exceeds evidenced scope")

    def test_reset_class_cannot_exceed_evidence(self):
        data = self.fixture()
        data["reset"]["class"] = "HOST"
        data["decision"] = "BLOCK"
        self.assertRejected(data, "reset.class: exceeds evidenced reset scope")

    def test_token_vazio_blocks_promotion(self):
        data = self.fixture()
        data["residual_state"] = "TOKEN_VAZIO"
        self.assertRejected(data, "TOKEN_VAZIO blocks promotion")

    def test_observed_residual_state_blocks_promotion(self):
        data = self.fixture()
        data["residual_state"] = True
        self.assertRejected(data, "observed residual state blocks promotion")

    def test_observer_must_be_independent(self):
        data = self.fixture()
        data["observer"]["independent"] = False
        data["decision"] = "BLOCK"
        self.assertRejected(data, "observer.independent")

    def test_runtime_cannot_modify_control_plane(self):
        data = self.fixture()
        data["observer"]["runtime_can_modify_control_plane"] = True
        data["decision"] = "BLOCK"
        self.assertRejected(data, "runtime_can_modify_control_plane")

    def test_marker_must_be_external_and_authorized(self):
        data = self.fixture()
        data["cross_epoch_probe"]["marker_owner"] = "MODEL_RUNTIME"
        data["decision"] = "BLOCK"
        self.assertRejected(data, "externally owned")

        data = self.fixture()
        data["cross_epoch_probe"]["authorized_interface"] = False
        data["decision"] = "BLOCK"
        self.assertRejected(data, "authorized_interface")

    def test_replay_binding_is_exact(self):
        data = self.fixture()
        data["replay_binding"]["artifact_sha256"] = "d" * 64
        data["decision"] = "BLOCK"
        self.assertRejected(data, "artifact_sha256: mismatch")

    def test_reproducible_unexpected_correlation_requires_quarantine(self):
        data = self.fixture()
        data["cross_epoch_probe"]["unexpected_correlation"] = True
        data["cross_epoch_probe"]["reproduced"] = True
        self.assertRejected(data, "requires QUARANTINE")

        data["decision"] = "QUARANTINE"
        result = validator.validate(data)
        self.assertEqual(result["decision"], "QUARANTINE")
        self.assertFalse(result["promotion_allowed"])


if __name__ == "__main__":
    unittest.main()
