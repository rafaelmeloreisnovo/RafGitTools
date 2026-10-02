from __future__ import annotations

import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLAN_PATH = ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json"
TRIGGER_PATH = ROOT / "configs" / "provider-enforcement-stage1.once.json"
WORKFLOW = (ROOT / ".github" / "workflows" / "START.yml").read_text(encoding="utf-8")

spec = importlib.util.spec_from_file_location(
    "provider_apply",
    ROOT / "scripts" / "apply_rafgittools_main_protection.py",
)
assert spec and spec.loader
APP = importlib.util.module_from_spec(spec)
spec.loader.exec_module(APP)


class FakeApi:
    def __init__(self) -> None:
        self.deleted = False

    def request(self, method, path, payload=None, allow_404=False):
        if method == "DELETE":
            self.deleted = True
            return {}
        if method == "GET" and path.endswith("/protection") and allow_404:
            return None if self.deleted else {"required_status_checks": {}}
        raise AssertionError((method, path, allow_404))


class PresentFakeApi:
    def __init__(self, current_payload):
        self.current_payload = current_payload
        self.put_payloads = []

    def request(self, method, path, payload=None, allow_404=False):
        if method == "PUT" and path.endswith("/protection"):
            self.current_payload = payload
            self.put_payloads.append(payload)
            return {}
        if method == "GET" and path.endswith("/protection"):
            return self.current_payload
        raise AssertionError((method, path, allow_404))


class ProviderEnforcementStage1Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.plan = json.loads(PLAN_PATH.read_text(encoding="utf-8"))
        cls.trigger = json.loads(TRIGGER_PATH.read_text(encoding="utf-8"))

    def test_stage1_deliberately_does_not_invent_independent_reviewer(self):
        policy = self.plan["required_policy"]
        self.assertFalse(policy["human_review_required"])
        self.assertEqual(policy["minimum_approving_reviews"], 0)
        self.assertEqual(
            self.plan["stage2_gate"]["state"],
            "TOKEN_VAZIO_INDEPENDENT_REVIEWER",
        )
        self.assertEqual(self.plan["stage2_gate"]["target_minimum_approving_reviews"], 1)

    def test_stage1_payload_enforces_pr_checks_admins_and_destructive_guards(self):
        payload = APP.build_payload(self.plan)
        self.assertTrue(payload["required_status_checks"]["strict"])
        self.assertEqual(len(payload["required_status_checks"]["contexts"]), 4)
        self.assertTrue(payload["enforce_admins"])
        self.assertEqual(
            payload["required_pull_request_reviews"]["required_approving_review_count"],
            0,
        )
        self.assertFalse(payload["allow_force_pushes"])
        self.assertFalse(payload["allow_deletions"])
        self.assertTrue(payload["required_conversation_resolution"])

    def test_stage1_readback_accepts_exact_zero_approval_policy(self):
        contexts = [x["context"] for x in self.plan["required_status_checks_global"]]
        observed = {
            "required_status_checks": {"contexts": contexts, "strict": True},
            "enforce_admins": {"enabled": True},
            "required_pull_request_reviews": {
                "required_approving_review_count": 0,
                "dismiss_stale_reviews": False,
                "require_code_owner_reviews": False,
                "require_last_push_approval": False,
            },
            "required_linear_history": {"enabled": False},
            "required_conversation_resolution": {"enabled": True},
            "allow_force_pushes": {"enabled": False},
            "allow_deletions": {"enabled": False},
            "block_creations": {"enabled": False},
            "lock_branch": {"enabled": False},
            "allow_fork_syncing": {"enabled": True},
            "restrictions": None,
        }
        self.assertEqual(APP.verify_readback(observed, self.plan), [])

    def test_readback_rejects_stale_or_extra_required_context(self):
        contexts = [x["context"] for x in self.plan["required_status_checks_global"]]
        observed = {
            "required_status_checks": {
                "contexts": contexts + ["stale-context"],
                "strict": True,
            },
            "enforce_admins": {"enabled": True},
            "required_pull_request_reviews": {
                "required_approving_review_count": 0,
                "dismiss_stale_reviews": False,
                "require_last_push_approval": False,
            },
            "required_conversation_resolution": {"enabled": True},
            "allow_force_pushes": {"enabled": False},
            "allow_deletions": {"enabled": False},
        }
        errors = APP.verify_readback(observed, self.plan)
        self.assertTrue(any("unexpected required contexts" in item for item in errors))

    def test_rollback_restores_absent_prestate(self):
        api = FakeApi()
        ok, result = APP.restore_absent_prestate(api, "owner/repo", "main")
        self.assertTrue(ok)
        self.assertEqual(result, "PASS_ABSENT_RESTORED")
        self.assertTrue(api.deleted)

    def test_present_prestate_normalizes_and_restores_transactionally(self):
        previous_readback = {
            "required_status_checks": {
                "strict": True,
                "contexts": ["old-b", "old-a"],
            },
            "required_pull_request_reviews": {
                "dismiss_stale_reviews": False,
                "require_code_owner_reviews": False,
                "required_approving_review_count": 0,
                "require_last_push_approval": False,
            },
            "enforce_admins": {"enabled": True},
            "restrictions": None,
            "required_linear_history": {"enabled": False},
            "allow_force_pushes": {"enabled": False},
            "allow_deletions": {"enabled": False},
            "block_creations": {"enabled": False},
            "required_conversation_resolution": {"enabled": True},
            "lock_branch": {"enabled": False},
            "allow_fork_syncing": {"enabled": True},
        }
        previous_payload = APP.protection_to_payload(previous_readback)
        self.assertEqual(
            previous_payload["required_status_checks"]["contexts"],
            ["old-a", "old-b"],
        )
        api = PresentFakeApi({})
        ok, result = APP.restore_present_prestate(
            api,
            "owner/repo",
            "main",
            previous_payload,
        )
        self.assertTrue(ok)
        self.assertEqual(result, "PASS_PRESENT_RESTORED")
        self.assertEqual(api.put_payloads, [previous_payload])

    def test_prestate_policy_accepts_absent_or_present_without_other_values(self):
        self.assertEqual(self.plan["required_prestate"], "ABSENT_OR_PRESENT")
        self.assertTrue(APP.prestate_allowed("ABSENT_OR_PRESENT", "ABSENT"))
        self.assertTrue(APP.prestate_allowed("ABSENT_OR_PRESENT", "PRESENT"))
        self.assertTrue(APP.prestate_allowed("ABSENT", "ABSENT"))
        self.assertFalse(APP.prestate_allowed("ABSENT", "PRESENT"))
        self.assertFalse(APP.prestate_allowed("INVALID", "PRESENT"))

    def test_payload_digest_is_stable_for_equivalent_payload(self):
        payload = APP.build_payload(self.plan)
        clone = json.loads(json.dumps(payload))
        self.assertEqual(APP.payload_digest(payload), APP.payload_digest(clone))
        self.assertRegex(APP.payload_digest(payload), r"^[0-9a-f]{64}$")

    def test_stage1_trigger_is_exact_and_not_claim_promotion(self):
        self.assertEqual(self.trigger["operation"], "apply_main_protection")
        self.assertEqual(self.trigger["environment"], "Pat_environments")
        self.assertEqual(self.trigger["secret_reference"], "PAT_ENV")
        self.assertEqual(self.trigger["required_approving_reviews"], 0)
        self.assertTrue(self.trigger["automatic_rollback_on_mismatch"])
        self.assertFalse(self.trigger["claim_allowed"])

    def test_retry_evidence_rearms_only_after_observed_cancelled_push(self):
        retry = self.trigger["retry_evidence"]
        self.assertEqual(retry["supersedes_cancelled_push_run"], 36363833345)
        self.assertEqual(retry["cancelled_run_conclusion"], "cancelled")
        self.assertEqual(retry["cancelled_run_jobs_observed"], 0)
        current_sha = retry["current_main_observed_sha"]
        cancelled_sha = retry["cancelled_run_head_sha"]
        self.assertRegex(current_sha, r"^[0-9a-f]{40}$")
        self.assertRegex(cancelled_sha, r"^[0-9a-f]{40}$")
        self.assertNotEqual(current_sha, cancelled_sha)
        self.assertFalse(retry["branch_protection_observed"])
        self.assertIn("re-arms", retry["reason"])

    def test_workflow_exposes_manual_rollback_before_apply(self):
        self.assertIn("- rollback_main_protection", WORKFLOW)
        self.assertIn("args+=(--rollback)", WORKFLOW)
        self.assertIn("provider-enforcement-stage1.once.json", WORKFLOW)


if __name__ == "__main__":
    unittest.main()
