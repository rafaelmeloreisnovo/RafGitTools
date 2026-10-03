#!/usr/bin/env python3
"""RAFAELIA Human Impact Safety Gate V1.

Deterministic, dependency-free gate for human-facing research/software changes.
This gate is necessary but never sufficient for deployment authorization.
It preserves TOKEN_VAZIO as a blocking epistemic state.
"""

from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Dict, List

SCHEMA_VERSION = "rafaelia.human-impact-gate.v1"
EMPTY_STATES = {None, "", "TOKEN_VAZIO", "TOKEN_VAZIO_NORMATIVE", "NOT_EXAMINED", "PENDING"}
HIGH_RISK_TIERS = {"HIGH", "CRITICAL"}


def _get(mapping: Dict[str, Any], key: str, default: Any = None) -> Any:
    value: Any = mapping
    for part in key.split("."):
        if not isinstance(value, dict):
            return default
        value = value.get(part, default)
    return value


def _missing_ref(value: Any) -> bool:
    return value in EMPTY_STATES or not isinstance(value, str) or not value.strip()


def _require_true(case: Dict[str, Any], key: str, blockers: List[str]) -> None:
    if _get(case, key) is not True:
        blockers.append(f"{key}:REQUIRED_TRUE")


def _require_ref(case: Dict[str, Any], key: str, blockers: List[str]) -> None:
    if _missing_ref(_get(case, key)):
        blockers.append(f"{key}:TOKEN_VAZIO")


def evaluate(case: Dict[str, Any]) -> Dict[str, Any]:
    blockers: List[str] = []

    if not isinstance(case, dict):
        return {
            "schemaVersion": SCHEMA_VERSION,
            "gateState": "BLOCKED",
            "deploymentAuthorized": False,
            "blockers": ["root:OBJECT_REQUIRED"],
        }

    if case.get("schemaVersion") != SCHEMA_VERSION:
        blockers.append("schemaVersion:INVALID")

    for key in ("caseId", "owner", "purpose"):
        _require_ref(case, key, blockers)

    # Epistemic boundary: a governance gate cannot promote scientific or safety claims.
    if case.get("claimAllowed") is not False:
        blockers.append("claimAllowed:MUST_BE_FALSE")

    # Human agency and transparency are baseline requirements for every human-facing case.
    for key in (
        "agency.nonManipulation",
        "agency.accessibleNotice",
        "agency.redressChannel",
        "agency.humanOverrideForConsequentialUse",
    ):
        _require_true(case, key, blockers)

    _require_ref(case, "safety.rollbackRef", blockers)
    _require_ref(case, "safety.incidentResponseRef", blockers)

    personal_data = bool(_get(case, "data.personalData", False))
    sensitive_data = bool(_get(case, "data.sensitiveData", False))
    if personal_data:
        _require_ref(case, "data.lawfulBasisReviewRef", blockers)
        _require_true(case, "data.minimizationDocumented", blockers)
        _require_true(case, "data.retentionDocumented", blockers)
        _require_true(case, "data.securityControlsDocumented", blockers)
        _require_ref(case, "data.privacyImpactAssessmentRef", blockers)

    children_possible = bool(_get(case, "population.childrenPossible", False))
    if children_possible:
        # Best interests and child-rights review are mandatory project gates.
        _require_ref(case, "children.bestInterestAssessmentRef", blockers)
        _require_ref(case, "children.childLegalBasisReviewRef", blockers)
        _require_ref(case, "children.ageAssuranceAssessmentRef", blockers)
        _require_true(case, "children.childAppropriateExplanation", blockers)
        _require_true(case, "children.noBehavioralManipulation", blockers)
        _require_true(case, "children.progressiveAutonomyRespected", blockers)
        _require_ref(case, "children.safeguardingEscalationRef", blockers)
        if personal_data or sensitive_data:
            _require_ref(case, "children.dataProtectionImpactRef", blockers)

    indigenous_affected = bool(_get(case, "population.indigenousPeoplesAffected", False))
    if indigenous_affected:
        # FPIC applicability is context dependent; the assessment itself may not be omitted.
        _require_ref(case, "culture.communityConsultationRef", blockers)
        _require_ref(case, "culture.representativeInstitutionRef", blockers)
        _require_ref(case, "culture.fpicAssessmentRef", blockers)
        _require_ref(case, "culture.localLanguagePlanRef", blockers)
        _require_true(case, "culture.traditionalKnowledgeProtection", blockers)
        _require_true(case, "culture.communityWithdrawalRespected", blockers)

    vulnerable = bool(_get(case, "population.vulnerableGroupsAffected", False))
    public_impact = bool(_get(case, "population.publicImpact", False))
    if vulnerable or public_impact:
        _require_ref(case, "equity.equityImpactAssessmentRef", blockers)
        _require_ref(case, "equity.accessibilityPlanRef", blockers)
        _require_ref(case, "equity.benefitHarmDistributionRef", blockers)

    risk_tier = str(_get(case, "safety.riskTier", "TOKEN_VAZIO")).upper()
    if risk_tier in HIGH_RISK_TIERS:
        _require_ref(case, "safety.safetyCaseRef", blockers)
        _require_ref(case, "safety.independentReviewRef", blockers)
        _require_ref(case, "safety.authorityReviewRef", blockers)
        _require_true(case, "safety.failSafe", blockers)
        _require_true(case, "safety.monitoringDocumented", blockers)

    evidence_refs = case.get("evidenceRefs")
    if not isinstance(evidence_refs, list) or not evidence_refs or any(_missing_ref(v) for v in evidence_refs):
        blockers.append("evidenceRefs:NONEMPTY_VERIFIABLE_LIST_REQUIRED")

    gate_state = "BLOCKED" if blockers else "READY_FOR_AUTHORIZED_REVIEW"
    return {
        "schemaVersion": SCHEMA_VERSION,
        "caseId": case.get("caseId", "TOKEN_VAZIO"),
        "gateState": gate_state,
        # Never make the machine-readable gate itself a deployment authority.
        "deploymentAuthorized": False,
        "claimAllowed": False,
        "blockers": sorted(set(blockers)),
        "principle": "NECESSARY_NOT_SUFFICIENT_FOR_DEPLOYMENT",
    }


def main(argv: List[str]) -> int:
    if len(argv) != 2:
        print("usage: human_impact_gate.py CASE.json", file=sys.stderr)
        return 64
    case_path = Path(argv[1])
    case = json.loads(case_path.read_text(encoding="utf-8"))
    result = evaluate(case)
    print(json.dumps(result, sort_keys=True, ensure_ascii=False))
    return 0 if result["gateState"] == "READY_FOR_AUTHORIZED_REVIEW" else 2


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
