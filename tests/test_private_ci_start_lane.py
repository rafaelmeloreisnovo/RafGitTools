from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
START = ROOT / ".github" / "workflows" / "START.yml"
REGISTRY = ROOT / "configs" / "private-ci" / "execution-registry.v1.json"
CATALOG = ROOT / "configs" / "private-ci" / "catalog" / "index.v1.json"
MODULE_PATH = ROOT / "scripts" / "private_ci_bridge.py"

SPEC = importlib.util.spec_from_file_location("private_ci_bridge", MODULE_PATH)
assert SPEC and SPEC.loader
bridge = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bridge)


class PrivateCiStartLaneTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = START.read_text(encoding="utf-8")
        cls.registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
        cls.catalog = json.loads(CATALOG.read_text(encoding="utf-8"))

    def test_public_registry_is_fail_closed(self):
        bridge.validate_registry(self.registry)
        self.assertFalse(self.registry["claim_allowed"])
        self.assertFalse(self.registry["secret_value_persisted"])

    def test_catalog_is_explicitly_bounded(self):
        self.assertEqual(self.catalog["private_repository_count"], 35)
        self.assertEqual(self.catalog["workflow_count_discovered"], 233)
        self.assertFalse(self.catalog["exhaustive"])
        self.assertFalse(self.catalog["claim_allowed"])

    def test_private_ci_is_manual_exact_sha_lane(self):
        self.assertIn("- private_ci", self.text)
        self.assertIn("private_ci_target_sha", self.text)
        self.assertIn("private_ci requires an exact lowercase 40-hex target SHA", self.text)
        self.assertIn("github.event_name == 'workflow_dispatch'", self.text)
        self.assertIn("private_ci_bridge:", self.text)

    def test_private_ci_executes_hash_only_bridge(self):
        self.assertIn("scripts/private_ci_bridge.py execute", self.text)
        self.assertIn("private-ci-replay-receipt.json", self.text)
        self.assertIn("raw_private_stdout_persisted", self.text)
        self.assertIn("private_source_uploaded_as_public_artifact", self.text)

    def test_pat_is_step_scoped_not_provider_actions_job_scoped(self):
        provider_block = self.text.split("  provider_actions:", 1)[1].split("  private_ci_bridge:", 1)[0]
        before_steps = provider_block.split("    steps:", 1)[0]
        self.assertNotIn("PROVIDER_ACTIONS_TOKEN:", before_steps)
        self.assertIn("PROVIDER_ACTIONS_TOKEN: ${{ secrets.PAT_ACTIONS }}", provider_block)

    def test_private_ci_job_has_no_job_wide_pat(self):
        block = self.text.split("  private_ci_bridge:", 1)[1].split("  provider_environments:", 1)[0]
        before_steps = block.split("    steps:", 1)[0]
        self.assertNotIn("PAT_ACTIONS:", before_steps)
        self.assertNotIn("PROVIDER_ACTIONS_TOKEN:", before_steps)
        self.assertIn("token: ${{ secrets.PAT_ACTIONS }}", block)


if __name__ == "__main__":
    unittest.main()
