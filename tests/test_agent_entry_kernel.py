import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "check_agent_entry_kernel", ROOT / "scripts" / "check_agent_entry_kernel.py"
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class AgentEntryKernelTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads((ROOT / "configs" / "agent-entry-kernel.v1.json").read_text(encoding="utf-8"))

    def test_canonical_contract_passes(self):
        self.assertEqual([], MODULE.validate(self.data))

    def test_token_vazio_cannot_disappear(self):
        broken = json.loads(json.dumps(self.data))
        broken["orthogonal_axes"]["knowledge_state"].remove("TOKEN_VAZIO")
        self.assertTrue(any("TOKEN_VAZIO" in e for e in MODULE.validate(broken)))

    def test_ignored_work_must_remain_representable(self):
        broken = json.loads(json.dumps(self.data))
        broken["orthogonal_axes"]["attention_state"].remove("IGNORED_WITH_REASON")
        self.assertTrue(any("ignored-with-reason" in e for e in MODULE.validate(broken)))

    def test_ready_to_test_is_not_a_knowledge_resolution(self):
        self.assertIn("READY_TO_TEST", self.data["orthogonal_axes"]["operational_state"])
        self.assertNotIn("READY_TO_TEST", self.data["orthogonal_axes"]["knowledge_state"])

    def test_every_role_is_explicit(self):
        self.assertEqual(MODULE.REQUIRED_ROLES, set(self.data["federation_roles"]))
        self.assertEqual("rafaelmeloreisnovo/Mapa", self.data["federated_authority"])

    def test_all_twelve_entry_questions_are_required(self):
        self.assertEqual(MODULE.REQUIRED_QUESTIONS, {q["id"] for q in self.data["entry_questions"]})
        broken = json.loads(json.dumps(self.data))
        broken["entry_questions"] = broken["entry_questions"][:-1]
        self.assertTrue(any("Q01..Q12" in e for e in MODULE.validate(broken)))

    def test_governance_data_privacy_security_are_non_optional_dimensions(self):
        self.assertEqual(MODULE.REQUIRED_DIMENSIONS, set(self.data["mandatory_service_dimensions"]))
        for axis in ("governance", "data", "privacy", "security"):
            self.assertGreaterEqual(len(self.data["mandatory_service_dimensions"][axis]), 5)

        broken = json.loads(json.dumps(self.data))
        del broken["mandatory_service_dimensions"]["privacy"]
        self.assertTrue(any("mandatory_service_dimensions" in e for e in MODULE.validate(broken)))

    def test_mapa_indices_must_point_to_mapa_authority(self):
        for key in MODULE.MAPA_INDEX_KEYS:
            self.assertTrue(self.data["canonical_indices"][key].startswith("github:rafaelmeloreisnovo/Mapa/"))

        broken = json.loads(json.dumps(self.data))
        broken["canonical_indices"]["mapa_work_service_contract"] = "configs/local-copy.json"
        self.assertTrue(any("explicit Mapa pointer" in e for e in MODULE.validate(broken)))

    def test_history_receipt_carries_identity_governance_and_stop_reason(self):
        fields = set(self.data["transition_receipt_required"])
        required = {
            "parent_event_id",
            "source_commit",
            "path_scope",
            "authority",
            "risk",
            "governance_class",
            "data_class",
            "privacy_class",
            "security_class",
            "falsifier",
            "exit_criterion",
            "stop_reason",
            "rollback_ref",
            "claim_allowed",
        }
        self.assertTrue(required.issubset(fields))

    def test_security_success_cannot_be_hardcoded(self):
        forbidden = "\n".join(self.data["forbidden_shortcuts"]).lower()
        self.assertIn("hardcode", forbidden)
        self.assertIn("security", forbidden)


if __name__ == "__main__":
    unittest.main()
