from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "START.yml"
RIVER7_VALIDATOR = ROOT / "scripts" / "validate_river7_crossrepo_receipts.py"

class ProviderEnvironmentWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = WORKFLOW.read_text(encoding="utf-8")
        cls.river7_validator = RIVER7_VALIDATOR.read_text(encoding="utf-8")

    def test_provider_environment_lane_is_bounded_to_manual_or_owner_main_one_shot(self):
        self.assertIn("- provider_environments", self.text)
        self.assertIn("github.event_name == 'workflow_dispatch'", self.text)
        self.assertIn("github.actor == 'rafaelmeloreisnovo'", self.text)
        self.assertIn("github.ref == 'refs/heads/main'", self.text)
        self.assertIn("configs/provider-environment-preflight.once.json", self.text)
        self.assertIn("configs/provider-enforcement-stage1.once.json", self.text)

    def test_only_governed_provider_pats_are_injected_into_active_start(self):
        self.assertIn("PROVIDER_TOKEN: ${{ secrets.PAT_ENV }}", self.text)
        self.assertIn("PROVIDER_ACTIONS_TOKEN: ${{ secrets.PAT_ACTIONS }}", self.text)
        self.assertNotIn("PROVIDER_TOKEN: ${{ secrets.PAT_ENVIRONMENTS }}", self.text)
        self.assertNotIn("secrets.PAT_AGENTS", self.text)
        self.assertNotIn("secrets.PAT_CODESPACES", self.text)
        self.assertNotIn("secrets.PAT_DEPENDABOT", self.text)

    def test_actions_lane_is_main_one_shot_exact_sha_and_read_only(self):
        self.assertIn("configs/provider-actions-preflight.once.json", self.text)
        self.assertIn("exact_commit_read_and_test", self.text)
        self.assertIn("scripts/private_provider_access_gate.py", self.text)
        self.assertIn("TARGET_SHA", self.text)
        self.assertIn("persist-credentials: false", self.text)
        self.assertIn("RIVER7_LINEAR_ERASURE_743_CROSSREPO_RECEIPT.json", self.text)

    def test_actions_lane_validates_all_river7_tests_and_burst_receipt(self):
        self.assertIn("test_river7_*.py", self.text)
        self.assertIn("simulate_burst_erasure.py", self.text)
        self.assertIn("RIVER7_BURST_ERASURE_PLACEMENT_CROSSREPO_RECEIPT.json", self.text)
        self.assertIn("scripts/validate_river7_crossrepo_receipts.py", self.text)
        self.assertIn("search_space_permutations", self.river7_validator)
        self.assertIn("[0, 1, 2, 5, 4, 6, 3]", self.river7_validator)

    def test_no_pat_fallback_is_present(self):
        self.assertNotIn("secrets.PAT_ENV ||", self.text)
        self.assertNotIn("secrets.PAT_ENVIRONMENTS ||", self.text)
        self.assertNotIn("secrets.PAT_ACTIONS ||", self.text)
        self.assertNotIn("secrets.PAT_AGENTS ||", self.text)
        self.assertNotIn("secrets.PAT_CODESPACES ||", self.text)
        self.assertNotIn("secrets.PAT_DEPENDABOT ||", self.text)

    def test_environment_gate_precedes_secret_use(self):
        self.assertIn("name: Pat_environments", self.text)
        self.assertIn("deployment: false", self.text)
        self.assertIn("PAT_ENV secret is unavailable after Pat_environments gate", self.text)

    def test_apply_is_sha_guarded_and_receipted(self):
        self.assertIn("--expected-main-sha", self.text)
        self.assertIn("apply_main_protection", self.text)
        self.assertIn("rollback_main_protection", self.text)
        self.assertIn("--rollback", self.text)
        self.assertIn("MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json", self.text)
        self.assertIn("provider-main-enforcement.json", self.text)
        self.assertIn("scripts/apply_rafgittools_main_protection.py", self.text)
        self.assertIn("expected_target_sha", self.text)
        self.assertIn("rafaelmeloreisnovo/RafPolimata", self.text)
        self.assertIn("target_plan", self.text)

if __name__ == "__main__":
    unittest.main()
