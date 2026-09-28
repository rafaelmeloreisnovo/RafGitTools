import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "scripts" / "validate_setup_governance_wizard.py"
SPEC = importlib.util.spec_from_file_location("setup_wizard_validator", PATH)
M = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(M)

class SetupGovernanceWizardTests(unittest.TestCase):
    def load(self):
        return json.loads((ROOT / "contracts" / "SETUP_GOVERNANCE_WIZARD_V1.json").read_text(encoding="utf-8"))

    def test_canonical_contract_passes(self):
        self.assertEqual(M.validate(self.load()), [])

    def test_secret_recording_is_rejected(self):
        data = self.load()
        data["local_storage"]["secret_values_recorded"] = True
        self.assertTrue(any("secret values" in x for x in M.validate(data)))

    def test_missing_rollback_is_rejected(self):
        data = self.load()
        data["steps"][0]["rollback"] = ""
        self.assertTrue(any("rollback" in x for x in M.validate(data)))

    def test_fine_print_boundary_cannot_disappear(self):
        data = self.load()
        data["principles"].remove("NO_FINE_PRINT_FOR_MATERIAL_RISK")
        self.assertTrue(any("NO_FINE_PRINT" in x for x in M.validate(data)))

    def test_decision_set_is_explicit(self):
        data = self.load()
        data["decisions"] = ["AGREE"]
        self.assertTrue(any("decision set" in x for x in M.validate(data)))

    def test_runtime_entry_points_and_clarity_controls_exist(self):
        self.assertEqual(M.validate(self.load()), [])

if __name__ == "__main__":
    unittest.main()
