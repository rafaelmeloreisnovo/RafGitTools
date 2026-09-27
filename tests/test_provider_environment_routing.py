from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    assert spec and spec.loader
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

ENV = load_module("provider_env", ROOT / "scripts" / "validate_provider_capability_environments.py")
APP = load_module("provider_apply", ROOT / "scripts" / "apply_rafgittools_main_protection.py")

class ProviderEnvironmentRoutingTests(unittest.TestCase):
    def test_capability_registry_is_fail_closed(self):
        data = json.loads((ROOT / "configs" / "provider-capability-environments.v1.json").read_text())
        self.assertEqual(ENV.validate(data), [])
        routed = {x["capability"]: x for x in data["capabilities"]}
        self.assertEqual(routed["environments"]["environment"].casefold(), "pat_environments")
        self.assertEqual(routed["environments"]["secret"], "PAT_environments")
        self.assertEqual(routed["environments"]["state"], "WIRED_MANUAL_ONLY")
        self.assertEqual(routed["environments"]["storage_scope"], "ENVIRONMENT_SECRET")
        self.assertEqual(routed["actions"]["secret"], "PAT_actions")
        self.assertEqual(routed["agents"]["secret"], "PAT_agents")
        self.assertEqual(routed["codespaces"]["secret"], "PAT_codespaces")
        self.assertEqual(routed["legacy_generic"]["secret"], "pat_env")
        self.assertNotIn("dependabot", routed)
        for key in ("legacy_generic", "actions", "agents", "codespaces"):
            self.assertEqual(routed[key]["storage_scope"], "TOKEN_VAZIO_PROVIDER_STORAGE_SCOPE")
            self.assertEqual(routed[key]["environment"], "TOKEN_VAZIO_PROVIDER_ENVIRONMENT_NAME_UNVERIFIED")

    def test_branch_protection_payload_matches_canonical_start_contexts(self):
        plan = json.loads((ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v3.json").read_text())
        payload = APP.build_payload(plan)
        contexts = payload["required_status_checks"]["contexts"]
        self.assertEqual(len(contexts), 4)
        self.assertTrue(payload["required_status_checks"]["strict"])
        self.assertTrue(payload["enforce_admins"])
        self.assertEqual(payload["required_pull_request_reviews"]["required_approving_review_count"], 1)
        self.assertTrue(payload["required_pull_request_reviews"]["dismiss_stale_reviews"])
        self.assertTrue(payload["required_pull_request_reviews"]["require_last_push_approval"])
        self.assertTrue(payload["required_conversation_resolution"])
        self.assertFalse(payload["allow_force_pushes"])
        self.assertFalse(payload["allow_deletions"])

    def test_readback_accepts_required_state(self):
        plan = json.loads((ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v3.json").read_text())
        contexts = [x["context"] for x in plan["required_status_checks_global"]]
        readback = {
            "required_status_checks": {"contexts": contexts, "strict": True},
            "enforce_admins": {"enabled": True},
            "required_pull_request_reviews": {
                "required_approving_review_count": 1,
                "dismiss_stale_reviews": True,
                "require_last_push_approval": True,
            },
            "required_conversation_resolution": {"enabled": True},
            "allow_force_pushes": {"enabled": False},
            "allow_deletions": {"enabled": False},
        }
        self.assertEqual(APP.verify_readback(readback, contexts), [])

    def test_readback_rejects_missing_context_and_admin_bypass(self):
        errors = APP.verify_readback({
            "required_status_checks": {"contexts": []},
            "enforce_admins": {"enabled": False},
            "required_pull_request_reviews": {
                "required_approving_review_count": 0,
                "dismiss_stale_reviews": False,
                "require_last_push_approval": False,
            },
            "required_conversation_resolution": {"enabled": False},
            "allow_force_pushes": {"enabled": True},
            "allow_deletions": {"enabled": True},
        }, ["required/check"])
        self.assertGreaterEqual(len(errors), 7)

if __name__ == "__main__":
    unittest.main()
