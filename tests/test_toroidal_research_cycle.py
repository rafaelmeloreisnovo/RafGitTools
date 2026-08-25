from __future__ import annotations

import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("cycle", ROOT / "scripts" / "toroidal_research_cycle.py")
assert SPEC and SPEC.loader
cycle = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(cycle)


def load(name: str):
    return json.loads((ROOT / name).read_text(encoding="utf-8"))


class ToroidalResearchCycleTests(unittest.TestCase):
    def setUp(self):
        self.contract = load("configs/toroidal_research_cycle_contract.json")
        self.manifest = load("examples/toroidal_research_cycle.example.json")

    def assert_invalid(self, manifest=None, contract=None, contains=None):
        with self.assertRaises(cycle.CycleError) as context:
            if manifest is None:
                cycle.validate_contract(contract or self.contract)
            else:
                cycle.validate_manifest(contract or self.contract, manifest)
        if contains:
            self.assertIn(contains, str(context.exception))

    def test_contract_is_valid(self):
        compiled = cycle.validate_contract(self.contract)
        self.assertIn("GOVERNANCE", compiled["roles"])

    def test_example_is_valid_and_fail_closed(self):
        summary = cycle.validate_manifest(self.contract, self.manifest)
        self.assertEqual("PASS", summary["status"])
        self.assertFalse(summary["claim_allowed"])
        self.assertTrue(summary["cycle_closed"])

    def test_contract_rejects_universal_sine_stabilizer(self):
        contract = copy.deepcopy(self.contract)
        contract["sine_reference_policy"]["pure_sine_is_universal_stabilizer"] = True
        self.assert_invalid(contract=contract, contains="universal stabilizer")

    def test_manifest_rejects_universal_sine_claim(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["sine_reference"]["universal_stabilizer_claim"] = True
        self.assert_invalid(manifest=manifest, contains="must be false")

    def test_manifest_requires_feedback_model(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["sine_reference"]["feedback_model"] = ""
        self.assert_invalid(manifest=manifest, contains="feedback_model")

    def test_cycle_must_close_feedback_to_void(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["cycle"]["transitions"][-1] = ["FEEDBACK", "QUERY"]
        self.assert_invalid(manifest=manifest, contains="toroidal feedback loop")

    def test_unknown_source_reference_is_rejected(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["claims"][0]["source_refs"] = ["S-MISSING"]
        self.assert_invalid(manifest=manifest, contains="unknown id")

    def test_promoted_claim_requires_evidence(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["claims"][0]["evidence_refs"] = []
        self.assert_invalid(manifest=manifest, contains="requires sources and evidence")

    def test_token_vazio_requires_ledger(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["token_vazio_ledger"] = [manifest["token_vazio_ledger"][1]]
        self.assert_invalid(manifest=manifest, contains="requires a ledger entry")

    def test_formula_pass_requires_test(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["formulas"][0]["test_refs"] = []
        self.assert_invalid(manifest=manifest, contains="requires a test reference")

    def test_execution_pass_requires_receipt(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["tests"][0]["execution_receipt"] = ""
        self.assert_invalid(manifest=manifest, contains="execution_receipt")

    def test_claim_allowed_rejects_hard_residual_and_vazio(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["claim_allowed"] = True
        self.assert_invalid(manifest=manifest, contains="unresolved hard residuals")

    def test_repository_role_must_match_authority(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["repositories"][0]["role"] = "SCIENCE"
        self.assert_invalid(manifest=manifest, contains="canonical authority")

    def test_frequency_must_be_positive(self):
        manifest = copy.deepcopy(self.manifest)
        manifest["sine_reference"]["frequency_hz"] = 0
        self.assert_invalid(manifest=manifest, contains="finite and positive")


if __name__ == "__main__":
    unittest.main()
