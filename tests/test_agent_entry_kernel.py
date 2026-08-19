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

    def test_history_receipt_carries_parent_and_source_commit(self):
        fields = set(self.data["transition_receipt_required"])
        self.assertIn("parent_event_id", fields)
        self.assertIn("source_commit", fields)
        self.assertIn("rollback_ref", fields)
        self.assertIn("claim_allowed", fields)


if __name__ == "__main__":
    unittest.main()
