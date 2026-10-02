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
        self.assertEqual(routed["environments"]["secret"], "PAT_ENV")
        self.assertEqual(routed["environments"]["state"], "WIRED_MANUAL_ONLY")
        self.assertEqual(routed["environments"]["storage_scope"], "ENVIRONMENT_SECRET")
        self.assertEqual(routed["environments_secret_reported"]["secret"], "PAT_ENVIRONMENTS")
        self.assertEqual(routed["environments_secret_reported"]["storage_scope"], "TOKEN_VAZIO_PROVIDER_SCOPE_PENDING")
        self.assertEqual(routed["environments_secret_reported"]["environment"], "TOKEN_VAZIO_PROVIDER_BINDING_PENDING")
        self.assertEqual(routed["environments_secret_reported"]["state"], "HUMAN_REPORTED_PROVIDER_READBACK_PENDING")
        self.assertEqual(routed["environments_secret_reported"]["allowed_operations"], [])
        self.assertFalse(routed["environments_secret_reported"]["write_allowed"])
        self.assertEqual(routed["actions"]["secret"], "PAT_ACTIONS")
        self.assertEqual(routed["actions"]["state"], "WIRED_MAIN_ONESHOT_READ_ONLY")
        self.assertFalse(routed["actions"]["write_allowed"])
        self.assertTrue(routed["actions"]["exact_target_sha_required"])
        self.assertIn("exact_commit_read_and_test", routed["actions"]["allowed_operations"])
        self.assertEqual(routed["agents"]["secret"], "PAT_AGENTS")
        self.assertEqual(routed["codespaces"]["secret"], "PAT_CODESPACES")
        self.assertEqual(routed["dependabot"]["secret"], "PAT_DEPENDABOT")
        for key in ("actions", "agents", "codespaces", "dependabot"):
            self.assertEqual(routed[key]["storage_scope"], "REPOSITORY_SECRET")
            self.assertEqual(routed[key]["environment"], "TOKEN_VAZIO_NOT_ENVIRONMENT_BOUND")

    def test_reported_pat_environments_cannot_be_silently_promoted(self):
        data = json.loads((ROOT / "configs" / "provider-capability-environments.v1.json").read_text())
        cap = next(x for x in data["capabilities"] if x["capability"] == "environments_secret_reported")
        cap["allowed_operations"] = ["apply_main_protection"]
        self.assertTrue(any("must remain unwired" in x for x in ENV.validate(data)))

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
        self.assertEqual(APP.verify_readback(readback, plan), [])

    def test_readback_rejects_missing_context_and_admin_bypass(self):
        plan = json.loads((ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v3.json").read_text())
        errors = APP.verify_readback({
            "required_status_checks": {"contexts": [], "strict": False},
            "enforce_admins": {"enabled": False},
            "required_pull_request_reviews": {
                "required_approving_review_count": 0,
                "dismiss_stale_reviews": False,
                "require_last_push_approval": False,
            },
            "required_conversation_resolution": {"enabled": False},
            "allow_force_pushes": {"enabled": True},
            "allow_deletions": {"enabled": True},
        }, plan)
        self.assertGreaterEqual(len(errors), 7)

if __name__ == "__main__":
    unittest.main()
