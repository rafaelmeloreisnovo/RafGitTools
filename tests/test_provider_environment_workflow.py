from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "START.yml"

class ProviderEnvironmentWorkflowTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = WORKFLOW.read_text(encoding="utf-8")

    def test_provider_environment_lane_is_bounded_to_manual_or_owner_main_one_shot(self):
        self.assertIn("- provider_environments", self.text)
        self.assertIn("github.event_name == 'workflow_dispatch'", self.text)
        self.assertIn("github.actor == 'rafaelmeloreisnovo'", self.text)
        self.assertIn("github.ref == 'refs/heads/main'", self.text)
        self.assertIn("configs/provider-environment-preflight.once.json", self.text)
        self.assertIn("configs/provider-enforcement-stage1.once.json", self.text)

    def test_only_environment_pat_is_injected_into_active_start(self):
        self.assertIn("PROVIDER_TOKEN: ${{ secrets.PAT_ENV }}", self.text)
        self.assertNotIn("secrets.PAT_ACTIONS", self.text)
        self.assertNotIn("secrets.PAT_AGENTS", self.text)
        self.assertNotIn("secrets.PAT_CODESPACES", self.text)
        self.assertNotIn("secrets.PAT_DEPENDABOT", self.text)

    def test_no_pat_fallback_is_present(self):
        self.assertNotIn("secrets.PAT_ENV ||", self.text)
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

if __name__ == "__main__":
    unittest.main()
