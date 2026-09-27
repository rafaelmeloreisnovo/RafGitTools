import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR = ROOT / "scripts" / "validate_custody_taxonomy.py"
CONTRACT = ROOT / "contracts" / "custody-taxonomy-v1.json"
EVENT_SCHEMA = ROOT / "contracts" / "custody-event-v1.schema.json"

spec = importlib.util.spec_from_file_location("custody_validator", VALIDATOR)
assert spec is not None and spec.loader is not None
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class CustodyFederatedAlignmentTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.contract = json.loads(CONTRACT.read_text(encoding="utf-8"))
        cls.event_schema = json.loads(EVENT_SCHEMA.read_text(encoding="utf-8"))

    def test_canonical_alignment_passes(self):
        self.assertEqual(module.validate(self.contract, self.event_schema), [])

    def test_assistant_is_orchestrator_not_provider_executor(self):
        assistant = self.contract["actors"]["ASSISTANT_TOOL_OPERATOR"]
        self.assertFalse(assistant["may_execute"])
        self.assertTrue(assistant["may_request_provider_mutation"])
        self.assertFalse(assistant["may_execute_provider_mutation"])

    def test_assistant_provider_execution_escalation_fails_closed(self):
        contract = copy.deepcopy(self.contract)
        contract["actors"]["ASSISTANT_TOOL_OPERATOR"]["may_execute_provider_mutation"] = True
        errors = module.validate(contract, self.event_schema)
        self.assertIn("assistant cannot become connector/provider mutation executor", errors)

    def test_crosswalk_must_cover_all_local_classes(self):
        contract = copy.deepcopy(self.contract)
        del contract["federated_profile_crosswalk"]["C10_ASSISTANT_TOOL_ACTION_CUSTODY"]
        errors = module.validate(contract, self.event_schema)
        self.assertTrue(any("federated crosswalk class set mismatch" in e for e in errors))

    def test_crosswalk_rejects_unknown_federated_profile(self):
        contract = copy.deepcopy(self.contract)
        contract["federated_profile_crosswalk"]["C03_CODE_CUSTODY"] = ["NOT_A_REAL_PROFILE"]
        errors = module.validate(contract, self.event_schema)
        self.assertTrue(any("unknown federated profiles" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
