from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WF = (ROOT / ".github" / "workflows" / "START.yml").read_text(encoding="utf-8")
TRIGGER = json.loads((ROOT / "configs" / "provider-environment-apply.once.json").read_text(encoding="utf-8"))


class ProviderEnvironmentApplyOnceTests(unittest.TestCase):
    def test_apply_trigger_is_bound_to_successful_preflight(self):
        self.assertEqual(TRIGGER["operation"], "apply_main_protection")
        self.assertEqual(TRIGGER["target_repository"], "rafaelmeloreisnovo/RafGitTools")
        self.assertEqual(TRIGGER["target_branch"], "main")
        self.assertEqual(TRIGGER["expected_actor"], "rafaelmeloreisnovo")
        self.assertEqual(TRIGGER["environment"], "Pat_environments")
        self.assertEqual(TRIGGER["secret_reference"], "PAT_ENV")
        self.assertTrue(TRIGGER["mutation_allowed"])
        self.assertFalse(TRIGGER["claim_allowed"])
        evidence = TRIGGER["preflight_evidence"]
        self.assertEqual(evidence["run_id"], 36359250986)
        self.assertEqual(evidence["job_id"], 108733398068)
        self.assertEqual(evidence["artifact_id"], 10945320713)
        self.assertEqual(evidence["state"], "PLAN_ONLY_AUTHORITY_AND_SHA_PRECONDITIONS_PASS")
        self.assertTrue(evidence["admin_permission_observed"])
        self.assertEqual(evidence["protection_prestate"], "ABSENT")
        self.assertFalse(evidence["provider_mutation"])

    def test_push_apply_route_requires_explicit_one_shot_file(self):
        self.assertIn("provider-environment-apply.once.json", WF)
        self.assertIn("apply one-shot requires successful preflight evidence", WF)
        self.assertIn("provider_operation=apply_main_protection", WF)

    def test_push_provider_lane_remains_owner_main_and_environment_gated(self):
        self.assertIn("github.actor == 'rafaelmeloreisnovo'", WF)
        self.assertIn("github.ref == 'refs/heads/main'", WF)
        self.assertIn("name: Pat_environments", WF)
        self.assertIn("PROVIDER_TOKEN: ${{ secrets.PAT_ENV }}", WF)

    def test_apply_script_is_fail_closed_and_receipted(self):
        script = (ROOT / "scripts" / "apply_rafgittools_main_protection.py").read_text(encoding="utf-8")
        self.assertIn("record_api_error", script)
        self.assertIn('phase="APPLY"', script)
        self.assertIn('phase="READBACK"', script)
        self.assertIn("provider_mutation_known", script)
        self.assertIn("secret_value_recorded", script)


if __name__ == "__main__":
    unittest.main()
