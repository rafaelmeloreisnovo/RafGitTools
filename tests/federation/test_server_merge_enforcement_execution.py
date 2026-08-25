from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "validate_server_merge_enforcement_execution",
    ROOT / "scripts" / "federation" / "validate_server_merge_enforcement_execution.py",
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ServerMergeEnforcementExecutionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.data = json.loads(
            (ROOT / "configs" / "server-merge-enforcement-execution.v1.json").read_text(
                encoding="utf-8"
            )
        )

    def clone(self) -> dict:
        return json.loads(json.dumps(self.data))

    def test_canonical_plan_passes_structurally_only(self) -> None:
        self.assertEqual([], MODULE.validate(self.data))
        report = MODULE.build_report(self.data, [])
        self.assertEqual("PASS_PLAN_ONLY", report["status"])
        self.assertTrue(report["structurally_ready"])
        self.assertFalse(report["provider_apply_allowed"])
        self.assertFalse(report["claim_allowed"])

    def test_claim_promotion_is_rejected(self) -> None:
        broken = self.clone()
        broken["claim_allowed"] = True
        self.assertTrue(any("claim_allowed" in error for error in MODULE.validate(broken)))

    def test_automatic_merge_is_rejected_at_both_layers(self) -> None:
        broken = self.clone()
        broken["automatic_merge"] = True
        broken["desired_state"]["automatic_merge"] = True
        errors = MODULE.validate(broken)
        self.assertGreaterEqual(sum("automatic_merge" in error for error in errors), 2)

    def test_target_requires_exact_commit(self) -> None:
        broken = self.clone()
        broken["target"]["observed_commit"] = "main"
        self.assertTrue(any("40-hex" in error for error in MODULE.validate(broken)))

    def test_producer_ref_must_equal_target_commit(self) -> None:
        broken = self.clone()
        broken["target"]["producer"]["ref"] = "0" * 40
        self.assertTrue(any("producer.ref" in error for error in MODULE.validate(broken)))

    def test_point_in_time_unprotected_observation_cannot_be_rewritten(self) -> None:
        broken = self.clone()
        broken["target"]["branch_observation"]["protected"] = True
        self.assertTrue(any("protected=false" in error for error in MODULE.validate(broken)))

    def test_required_context_is_exact_and_unique(self) -> None:
        broken = self.clone()
        broken["desired_state"]["required_contexts"].append("CI")
        self.assertTrue(any("one exact promotion context" in error for error in MODULE.validate(broken)))

    def test_zero_approval_policy_is_rejected(self) -> None:
        broken = self.clone()
        broken["desired_state"]["required_approving_review_count"] = 0
        self.assertTrue(any("one independent approval" in error for error in MODULE.validate(broken)))

    def test_public_plan_cannot_assert_admin_credential(self) -> None:
        broken = self.clone()
        broken["authority_gate"]["credential_state"] = "AVAILABLE"
        broken["authority_gate"]["apply_allowed"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("credential_state" in error for error in errors))
        self.assertTrue(any("apply_allowed" in error for error in errors))

    def test_admin_precondition_cannot_be_promoted_to_pass(self) -> None:
        broken = self.clone()
        for item in broken["preconditions"]:
            if item["id"] == "ADMIN_WRITE_AUTHORITY":
                item["state"] = "PASS"
        self.assertTrue(any("state vector" in error for error in MODULE.validate(broken)))

    def test_plan_command_cannot_mutate(self) -> None:
        broken = self.clone()
        broken["execution"]["plan_command"] += " --apply"
        self.assertTrue(any("plan_command must be non-mutating" in error for error in MODULE.validate(broken)))

    def test_merge_endpoint_is_forbidden(self) -> None:
        broken = self.clone()
        broken["execution"]["apply_command"] += " https://api.github.com/repos/x/y/merges"
        self.assertTrue(any("merge endpoint" in error for error in MODULE.validate(broken)))

    def test_rollback_is_concrete_but_not_rehearsed(self) -> None:
        self.assertEqual("SPECIFIED_NOT_REHEARSED", self.data["rollback"]["state"])
        broken = self.clone()
        broken["rollback"]["command"] = "TOKEN_VAZIO"
        self.assertTrue(any("rollback.command" in error for error in MODULE.validate(broken)))

    def test_provider_receipts_remain_token_vazio(self) -> None:
        broken = self.clone()
        broken["outputs"]["target_apply_receipt"] = "PASS"
        broken["outputs"]["zero_approval_rejection_receipt"] = "PASS"
        errors = MODULE.validate(broken)
        self.assertEqual(2, sum("receipt must remain TOKEN_VAZIO" in error for error in errors))

    def test_four_temporal_discriminants_are_required(self) -> None:
        broken = self.clone()
        broken["evidence"] = [
            item
            for item in broken["evidence"]
            if not item["id"].startswith("MAPA-PR396-")
        ]
        self.assertTrue(
            any(
                "four temporal discriminants" in error
                for error in MODULE.validate(broken)
            )
        )


if __name__ == "__main__":
    unittest.main()
