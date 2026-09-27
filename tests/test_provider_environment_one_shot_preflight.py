from __future__ import annotations
import json
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
WF=(ROOT/".github/workflows/START.yml").read_text(encoding="utf-8")
TRIGGER=json.loads((ROOT/"configs/provider-environment-preflight.once.json").read_text(encoding="utf-8"))

class ProviderEnvironmentOneShotPreflightTests(unittest.TestCase):
    def test_trigger_is_read_only_and_exact(self):
        self.assertEqual(TRIGGER["operation"], "preflight")
        self.assertEqual(TRIGGER["target_branch"], "main")
        self.assertEqual(TRIGGER["expected_actor"], "rafaelmeloreisnovo")
        self.assertEqual(TRIGGER["environment"], "Pat_environments")
        self.assertEqual(TRIGGER["secret_reference"], "PAT_ENV")
        self.assertFalse(TRIGGER["mutation_allowed"])
        self.assertFalse(TRIGGER["claim_allowed"])

    def test_preflight_trigger_remains_read_only_and_separate_from_apply(self):
        self.assertIn("provider-environment-preflight.once.json", WF)
        self.assertIn("one-shot provider trigger only permits preflight", WF)
        self.assertIn("provider_operation=preflight", WF)
        self.assertIn("provider-environment-apply.once.json", WF)
        self.assertIn("provider_operation=apply_main_protection", WF)
        self.assertIn("apply one-shot requires successful preflight evidence", WF)

    def test_owner_and_main_are_required(self):
        self.assertIn("github.actor == 'rafaelmeloreisnovo'", WF)
        self.assertIn("github.ref == 'refs/heads/main'", WF)

    def test_provider_secret_remains_environment_scoped(self):
        self.assertIn("name: Pat_environments", WF)
        self.assertIn("PROVIDER_TOKEN: ${{ secrets.PAT_ENV }}", WF)
        self.assertNotIn("PROVIDER_TOKEN: ${{ secrets.PAT_ACTIONS }}", WF)
        self.assertNotIn("PROVIDER_TOKEN: ${{ secrets.PAT_DEPENDABOT }}", WF)

if __name__=="__main__":
    unittest.main()
