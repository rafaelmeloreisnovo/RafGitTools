"""Falsifiers for unknown classification and human-review boundaries.

Synthetic-only. No real people, secrets, network, or private content.
"""
import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("human_impact_gate_watchdog", ROOT / "scripts" / "human_impact_gate.py")
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader
SPEC.loader.exec_module(MODULE)


def safe_fixture():
    return {
        "schemaVersion": "rafaelia.human-impact-gate.v1",
        "caseId": "SYNTHETIC-001",
        "owner": "HUMAN_REVIEW_QUEUE",
        "purpose": "synthetic policy check without real subjects",
        "claimAllowed": False,
        "population": {
            "childrenPossible": False,
            "indigenousPeoplesAffected": False,
            "vulnerableGroupsAffected": False,
            "publicImpact": False,
        },
        "data": {"personalData": False, "sensitiveData": False},
        "agency": {
            "nonManipulation": True,
            "accessibleNotice": True,
            "redressChannel": True,
            "humanOverrideForConsequentialUse": True,
        },
        "safety": {
            "riskTier": "LOW",
            "rollbackRef": "synthetic:rollback:restore-previous-checkpoint",
            "incidentResponseRef": "synthetic:incident:fail-closed",
        },
        "evidenceRefs": ["synthetic:fixture:test-only"],
    }


class HumanImpactUnknownScopeTests(unittest.TestCase):
    def evaluate(self, fixture):
        result = MODULE.evaluate(fixture)
        self.assertFalse(result["deploymentAuthorized"])
        self.assertFalse(result["claimAllowed"])
        return result

    def test_positive_fixture_reaches_only_human_review(self):
        r = self.evaluate(safe_fixture())
        self.assertEqual("READY_FOR_AUTHORIZED_REVIEW", r["gateState"])

    def test_absent_critical_booleans_block(self):
        for group, key in (
            ("data", "personalData"), ("data", "sensitiveData"),
            ("population", "childrenPossible"), ("population", "indigenousPeoplesAffected"),
            ("population", "vulnerableGroupsAffected"), ("population", "publicImpact"),
        ):
            with self.subTest(field=f"{group}.{key}"):
                data = safe_fixture()
                del data[group][key]
                r = self.evaluate(data)
                self.assertEqual("BLOCKED", r["gateState"])
                self.assertIn(f"{group}.{key}:TOKEN_VAZIO_OR_INVALID_BOOLEAN", r["blockers"])

    def test_token_vazio_and_string_false_not_coerced(self):
        for value in ("TOKEN_VAZIO", "false", None, [], {}, 0, 1):
            with self.subTest(value=str(value)):
                data = safe_fixture()
                data["population"]["childrenPossible"] = value
                r = self.evaluate(data)
                self.assertEqual("BLOCKED", r["gateState"])
                self.assertIn("population.childrenPossible:TOKEN_VAZIO_OR_INVALID_BOOLEAN", r["blockers"])

    def test_unknown_risk_tier_blocks(self):
        for value in ("TOKEN_VAZIO", None, "ZERO", [], {}, 0, ""):
            with self.subTest(value=str(value)):
                data = safe_fixture()
                data["safety"]["riskTier"] = value
                r = self.evaluate(data)
                self.assertEqual("BLOCKED", r["gateState"])
                self.assertIn("safety.riskTier:TOKEN_VAZIO_OR_INVALID_TIER", r["blockers"])

    def test_missing_risk_tier_blocks(self):
        data = safe_fixture()
        del data["safety"]["riskTier"]
        self.assertEqual("BLOCKED", self.evaluate(data)["gateState"])

    def test_unhashable_references_fail_closed_without_crash(self):
        for field, value in (("caseId", {}), ("purpose", []), ("owner", {})):
            with self.subTest(field=field):
                data = safe_fixture()
                data[field] = value
                self.assertEqual("BLOCKED", self.evaluate(data)["gateState"])
        data = safe_fixture()
        data["evidenceRefs"] = [{}]
        self.assertEqual("BLOCKED", self.evaluate(data)["gateState"])

    def test_child_case_requires_independent_child_safeguards(self):
        data = safe_fixture()
        data["population"]["childrenPossible"] = True
        r = self.evaluate(data)
        self.assertEqual("BLOCKED", r["gateState"])
        self.assertIn("children.bestInterestAssessmentRef:TOKEN_VAZIO", r["blockers"])
        self.assertIn("children.childLegalBasisReviewRef:TOKEN_VAZIO", r["blockers"])

    def test_high_risk_requires_watchdog_and_rollback_evidence(self):
        data = safe_fixture()
        data["safety"]["riskTier"] = "CRITICAL"
        data["safety"]["rollbackRef"] = "TOKEN_VAZIO"
        r = self.evaluate(data)
        self.assertEqual("BLOCKED", r["gateState"])
        self.assertIn("safety.failSafe:REQUIRED_TRUE", r["blockers"])
        self.assertIn("safety.monitoringDocumented:REQUIRED_TRUE", r["blockers"])
        self.assertIn("safety.rollbackRef:TOKEN_VAZIO", r["blockers"])

    def test_typed_token_vazio_variants_never_count_as_documentary_evidence(self):
        for field, value in (
            ("owner", "TOKEN_VAZIO_UNKNOWN"),
            ("caseId", "TOKEN_VAZIO_UNIDENTIFIABLE"),
            ("purpose", "TOKEN_VAZIO_WITHHELD"),
        ):
            with self.subTest(field=field):
                data = safe_fixture()
                data[field] = value
                self.assertEqual("BLOCKED", self.evaluate(data)["gateState"])
        data = safe_fixture()
        data["evidenceRefs"] = ["TOKEN_VAZIO_UNMEASURED"]
        r = self.evaluate(data)
        self.assertEqual("BLOCKED", r["gateState"])
        self.assertIn("evidenceRefs:NONEMPTY_VERIFIABLE_LIST_REQUIRED", r["blockers"])

    def test_no_case_implicitly_authorizes_deployment(self):
        for children in (False, True, "TOKEN_VAZIO", None):
            data = safe_fixture()
            data["population"]["childrenPossible"] = children
            r = self.evaluate(data)
            self.assertIs(r["deploymentAuthorized"], False)
            self.assertIs(r["claimAllowed"], False)


if __name__ == "__main__":
    unittest.main()
