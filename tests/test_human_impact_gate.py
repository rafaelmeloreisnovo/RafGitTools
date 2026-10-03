import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("human_impact_gate", ROOT / "scripts" / "human_impact_gate.py")
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


def base_case():
    return {
        "schemaVersion": "rafaelia.human-impact-gate.v1",
        "caseId": "CASE-LOW-RISK-001",
        "owner": "ACCOUNTABLE_HUMAN_OWNER",
        "purpose": "low-risk internal research tool",
        "claimAllowed": False,
        "population": {
            "childrenPossible": False,
            "indigenousPeoplesAffected": False,
            "vulnerableGroupsAffected": False,
            "publicImpact": False,
        },
        "data": {
            "personalData": False,
            "sensitiveData": False,
        },
        "agency": {
            "nonManipulation": True,
            "accessibleNotice": True,
            "redressChannel": True,
            "humanOverrideForConsequentialUse": True,
        },
        "safety": {
            "riskTier": "LOW",
            "rollbackRef": "docs/rollback.md#case-001",
            "incidentResponseRef": "docs/incidents.md#case-001",
        },
        "evidenceRefs": ["receipt:case-001"],
    }


class HumanImpactGateTests(unittest.TestCase):
    def test_low_risk_case_can_reach_authorized_review_but_never_auto_authorizes(self):
        result = MODULE.evaluate(base_case())
        self.assertEqual("READY_FOR_AUTHORIZED_REVIEW", result["gateState"])
        self.assertFalse(result["deploymentAuthorized"])
        self.assertFalse(result["claimAllowed"])

    def test_manipulation_is_fail_closed(self):
        case = base_case()
        case["agency"]["nonManipulation"] = False
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("agency.nonManipulation:REQUIRED_TRUE", result["blockers"])

    def test_personal_data_requires_lawful_basis_minimization_security_retention_and_impact(self):
        case = base_case()
        case["data"]["personalData"] = True
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("data.lawfulBasisReviewRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("data.privacyImpactAssessmentRef:TOKEN_VAZIO", result["blockers"])

    def test_child_context_requires_best_interest_and_safeguarding(self):
        case = base_case()
        case["population"]["childrenPossible"] = True
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("children.bestInterestAssessmentRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("children.childLegalBasisReviewRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("children.safeguardingEscalationRef:TOKEN_VAZIO", result["blockers"])

    def test_indigenous_context_requires_consultation_and_fpic_assessment(self):
        case = base_case()
        case["population"]["indigenousPeoplesAffected"] = True
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("culture.communityConsultationRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("culture.fpicAssessmentRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("culture.traditionalKnowledgeProtection:REQUIRED_TRUE", result["blockers"])

    def test_high_risk_requires_safety_case_independent_review_authority_and_fail_safe(self):
        case = base_case()
        case["safety"]["riskTier"] = "HIGH"
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("safety.safetyCaseRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("safety.independentReviewRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("safety.authorityReviewRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("safety.failSafe:REQUIRED_TRUE", result["blockers"])

    def test_public_or_vulnerable_impact_requires_equity_and_accessibility_analysis(self):
        case = base_case()
        case["population"]["publicImpact"] = True
        result = MODULE.evaluate(case)
        self.assertEqual("BLOCKED", result["gateState"])
        self.assertIn("equity.equityImpactAssessmentRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("equity.accessibilityPlanRef:TOKEN_VAZIO", result["blockers"])
        self.assertIn("equity.benefitHarmDistributionRef:TOKEN_VAZIO", result["blockers"])


if __name__ == "__main__":
    unittest.main()
