import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_federated_work_item", ROOT / "scripts" / "validate_federated_work_item.py"
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class FederatedWorkItemTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(
            (ROOT / "examples" / "federated_work_item.fgap-fnext-20260819.json").read_text(encoding="utf-8")
        )

    def clone(self):
        return json.loads(json.dumps(self.data))

    def test_canonical_fixture_passes(self):
        self.assertEqual([], MODULE.validate(self.data))

    def test_claim_promotion_is_rejected(self):
        broken = self.clone()
        broken["claim_allowed"] = True
        self.assertTrue(any("claim_allowed" in e for e in MODULE.validate(broken)))

    def test_privacy_axis_is_mandatory(self):
        broken = self.clone()
        del broken["service_classification"]["privacy"]
        self.assertTrue(any("privacy" in e for e in MODULE.validate(broken)))

    def test_unknown_privacy_blocks_mutation(self):
        broken = self.clone()
        broken["service_classification"]["privacy"] = {
            "class": "TOKEN_VAZIO",
            "basis": "classification has not yet been established",
        }
        self.assertTrue(any("TOKEN_VAZIO privacy" in e for e in MODULE.validate(broken)))

    def test_unknown_security_blocks_mutation(self):
        broken = self.clone()
        broken["service_classification"]["security"] = {
            "class": "TOKEN_VAZIO",
            "basis": "security scope has not yet been established",
        }
        self.assertTrue(any("TOKEN_VAZIO security" in e for e in MODULE.validate(broken)))

    def test_high_risk_mutation_requires_concrete_rollback(self):
        broken = self.clone()
        broken["execution"]["risk"] = "HIGH"
        broken["execution"]["rollback_ref"] = "TOKEN_VAZIO_ROLLBACK"
        self.assertTrue(any("concrete rollback_ref" in e for e in MODULE.validate(broken)))

    def test_mutation_requires_exact_source_commit(self):
        broken = self.clone()
        broken["object_binding"]["source_commit"] = "TOKEN_VAZIO_SOURCE_COMMIT"
        self.assertTrue(any("mutating work requires exact source_commit" in e for e in MODULE.validate(broken)))

    def test_resolved_state_requires_evidence(self):
        broken = self.clone()
        broken["state"] = "RESOLVED"
        broken["evidence"]["evidence_refs"] = []
        self.assertTrue(any("require evidence_refs" in e for e in MODULE.validate(broken)))

    def test_token_vazio_object_hash_is_allowed_when_identity_is_multi_object(self):
        self.assertTrue(self.data["object_binding"]["object_hash_or_TOKEN_VAZIO"].startswith("TOKEN_VAZIO"))
        self.assertEqual([], MODULE.validate(self.data))


if __name__ == "__main__":
    unittest.main()
