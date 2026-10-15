"""Adversarial offline gate tests: no network, no credentials, no state changes."""
import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/validate_observation_pr_receipt_v1.py"
SPEC = importlib.util.spec_from_file_location("observation_receipt_gate", SCRIPT)
M = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(M)


def sample():
    return {
        "repository": "rafaelmeloreisnovo/Mapa",
        "pr": 758,
        "head_sha": "ccd25c9cf256f3b7cee221cc28954b0185e33e3a",
        "draft": True,
        "merged": False,
        "title": "[DRAFT][NÃO PROMOVER] teste de fechamento",
        "checks": [{"name": "test", "conclusion": "success"}],
        "provider_enforcement": True,
        "independent_review": True,
        "license_verified": True,
    }


class ReadOnlyEvidenceGateTests(unittest.TestCase):
    def test_all_inputs_verified_still_never_authorizes_merge(self):
        r = M.assess(sample())
        self.assertEqual(r["state"], "EVIDENCE_OBSERVABLE_NO_PROMOTION")
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["promotion_allowed"])
        self.assertFalse(r["mutation_allowed"])

    def assert_block(self, x, reason):
        r = M.assess(x)
        self.assertEqual(r["state"], "BLOCKED")
        self.assertIn(reason, r["gaps"])
        self.assertFalse(r["promotion_allowed"])

    def test_unknown_and_null(self):
        self.assertEqual(M.assess(None)["state"], "BLOCKED_INPUT")
        self.assert_block({}, "TARGET_NOT_PINNED")

    def test_boolean_pr_is_not_integer(self):
        x = sample(); x["pr"] = True
        self.assert_block(x, "TARGET_NOT_PINNED")

    def test_unpinned_repo(self):
        x = sample(); x["repository"] = "attacker/Mapa"
        self.assert_block(x, "TARGET_NOT_PINNED")

    def test_stale_head(self):
        x = sample(); x["head_sha"] = "0" * 40
        self.assert_block(x, "HEAD_DRIFT")

    def test_bad_sha(self):
        x = sample(); x["head_sha"] = "TOKEN_VAZIO"
        self.assert_block(x, "MALFORMED_HEAD")

    def test_missing_hold_title(self):
        x = sample(); x["title"] = "Tudo OK"
        self.assert_block(x, "MISSING_HOLD_TITLE")

    def test_not_draft(self):
        x = sample(); x["draft"] = False
        self.assert_block(x, "NOT_DRAFT_OPEN")

    def test_merged(self):
        x = sample(); x["merged"] = True
        self.assert_block(x, "NOT_DRAFT_OPEN")

    def test_skipped_is_not_success(self):
        x = sample(); x["checks"][0]["conclusion"] = "skipped"
        self.assert_block(x, "CHECK_NOT_PASS")

    def test_failure_is_not_success(self):
        x = sample(); x["checks"][0]["conclusion"] = "failure"
        self.assert_block(x, "CHECK_NOT_PASS")

    def test_no_checks(self):
        x = sample(); x["checks"] = []
        self.assert_block(x, "CHECKS_UNOBSERVED")

    def test_duplicate_check_not_independent(self):
        x = sample(); x["checks"].append(dict(x["checks"][0]))
        self.assert_block(x, "DUPLICATE_CHECK_NAME")

    def test_no_provider_enforcement(self):
        x = sample(); x["provider_enforcement"] = "TOKEN_VAZIO"
        self.assert_block(x, "PROVIDER_ENFORCEMENT_UNVERIFIED")

    def test_no_independent_review(self):
        x = sample(); x["independent_review"] = False
        self.assert_block(x, "INDEPENDENT_REVIEW_UNVERIFIED")

    def test_unverified_rights(self):
        x = sample(); x["license_verified"] = None
        self.assert_block(x, "RIGHTS_UNVERIFIED")


if __name__ == "__main__":
    unittest.main()
