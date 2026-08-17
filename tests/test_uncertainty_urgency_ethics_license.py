from __future__ import annotations

import copy
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_uncertainty_urgency_ethics_license.py"
spec = importlib.util.spec_from_file_location("guard", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(mod)


class GuardTests(unittest.TestCase):
    def setUp(self):
        self.contract = mod.load_json(ROOT / "configs" / "uncertainty-urgency-ethics-license.v1.json")
        self.evidence = mod.load_json(ROOT / "data" / "evidence" / "github" / "uncertainty-urgency-snapshot-20260817.v1.json")

    def errors(self, c=None, e=None):
        c = self.contract if c is None else c
        e = self.evidence if e is None else e
        return mod.validate_contract(c) + mod.validate_evidence(e, c)

    def test_canonical_snapshot_passes(self):
        self.assertEqual(self.errors(), [])
        self.assertEqual(mod.build_report(self.contract, self.evidence)["status"], "PASS")

    def test_claim_promotion_rejected(self):
        e = copy.deepcopy(self.evidence); e["claim_allowed"] = True
        self.assertTrue(any("claim_allowed" in x for x in self.errors(e=e)))

    def test_microcycle_chain_break_rejected(self):
        e = copy.deepcopy(self.evidence)
        e["microcycle_window"]["entries"][2]["previous_entry_sha256"] = "0" * 64
        self.assertTrue(any("chain discontinuity" in x for x in self.errors(e=e)))

    def test_latest_four_regression_rejected(self):
        e = copy.deepcopy(self.evidence)
        e["microcycle_window"]["entries"][3]["latest_four_count"] = 3
        self.assertTrue(any("latest_four_count" in x for x in self.errors(e=e)))

    def test_token_vazio_requires_next_action(self):
        e = copy.deepcopy(self.evidence)
        item = next(x for x in e["uncertainties"] if x["state"] == "TOKEN_VAZIO")
        item["next_action"] = ""
        self.assertTrue(any("missing next_action" in x for x in self.errors(e=e)))

    def test_token_vazio_requires_exit_criterion(self):
        e = copy.deepcopy(self.evidence)
        item = next(x for x in e["uncertainties"] if x["state"] == "TOKEN_VAZIO")
        item["exit_criterion"] = ""
        self.assertTrue(any("exit_criterion" in x for x in self.errors(e=e)))

    def test_evidenced_state_requires_refs(self):
        e = copy.deepcopy(self.evidence)
        item = next(x for x in e["uncertainties"] if x["state"] == "EVIDENCED_SCOPED")
        item["evidence_refs"] = []
        self.assertTrue(any("requires evidence_refs" in x for x in self.errors(e=e)))

    def test_high_risk_requires_human_review(self):
        e = copy.deepcopy(self.evidence)
        item = next(x for x in e["uncertainties"] if x["risk_tier"] == "HIGH")
        item["human_review_required"] = False
        self.assertTrue(any("requires human review" in x for x in self.errors(e=e)))

    def test_public_access_does_not_verify_license(self):
        e = copy.deepcopy(self.evidence)
        use = e["usage_records"][0]
        use["rights"]["license_state"] = "VERIFIED_PERMITTED"
        use["rights"]["license_verified"] = True
        use["rights"]["license_evidence_refs"] = []
        self.assertTrue(any("verified license requires evidence" in x for x in self.errors(e=e)))

    def test_redistribution_requires_explicit_rights(self):
        e = copy.deepcopy(self.evidence)
        e["usage_records"][0]["rights"]["redistribution_allowed"] = True
        self.assertTrue(any("redistribution_allowed=true" in x for x in self.errors(e=e)))

    def test_training_requires_explicit_rights(self):
        e = copy.deepcopy(self.evidence)
        e["usage_records"][0]["rights"]["training_allowed"] = True
        self.assertTrue(any("training_allowed=true" in x for x in self.errors(e=e)))

    def test_commercial_use_requires_explicit_rights(self):
        e = copy.deepcopy(self.evidence)
        e["usage_records"][0]["rights"]["commercial_use_allowed"] = True
        self.assertTrue(any("commercial_use_allowed=true" in x for x in self.errors(e=e)))

    def test_parable_cannot_gain_evidence_effect(self):
        c = copy.deepcopy(self.contract); c["parable_bridge"]["evidence_effect"] = "PROMOTE"
        self.assertTrue(any("parable" in x for x in self.errors(c=c)))

    def test_antiregression_lenses_cannot_be_removed(self):
        c = copy.deepcopy(self.contract); c["anti_regression_invariants"].pop()
        self.assertTrue(any("AR01..AR30" in x for x in self.errors(c=c)))

    def test_auto_merge_is_rejected(self):
        c = copy.deepcopy(self.contract); c["automatic_merge"] = True
        self.assertTrue(any("automatic_merge" in x for x in self.errors(c=c)))


if __name__ == "__main__":
    unittest.main()
