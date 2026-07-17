#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from actions_execution_evidence import (
    TOKEN_VAZIO,
    classify_incident,
    summarize,
    validate_contract,
    validate_manifest,
)


class ActionsExecutionEvidenceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.contract = json.loads(
            (ROOT / "configs/actions_execution_evidence_contract.json").read_text(encoding="utf-8")
        )
        cls.manifest = json.loads(
            (ROOT / "examples/actions_execution_evidence.example.json").read_text(encoding="utf-8")
        )

    def incident(self) -> dict:
        return copy.deepcopy(self.manifest["incidents"][0])

    def test_contract_is_valid(self) -> None:
        self.assertEqual(validate_contract(self.contract), [])

    def test_example_manifest_is_valid(self) -> None:
        self.assertEqual(validate_manifest(self.contract, self.manifest), [])

    def test_zero_step_no_logs_is_not_billing(self) -> None:
        self.assertEqual(
            classify_incident(self.incident(), self.contract),
            "ZERO_STEP_NO_LOGS",
        )

    def test_billing_requires_explicit_evidence_code(self) -> None:
        item = self.incident()
        item["evidence_code"] = "BILLING_OR_SPENDING_LIMIT_MESSAGE"
        self.assertEqual(classify_incident(item, self.contract), "BILLING_BLOCKED")

    def test_failure_with_steps_is_executed_failure(self) -> None:
        item = self.incident()
        item["steps_observed"] = 3
        item["logs_state"] = "AVAILABLE"
        self.assertEqual(classify_incident(item, self.contract), "WORKFLOW_EXECUTED_FAILURE")

    def test_success_requires_positive_steps(self) -> None:
        item = self.incident()
        item["conclusion"] = "success"
        item["declared_classification"] = TOKEN_VAZIO
        errors = validate_manifest(self.contract, {**self.manifest, "incidents": [item]})
        self.assertTrue(any("success requires positive" in error for error in errors))

    def test_success_with_steps_is_pass(self) -> None:
        item = self.incident()
        item["conclusion"] = "success"
        item["steps_observed"] = 4
        item["logs_state"] = "AVAILABLE"
        self.assertEqual(classify_incident(item, self.contract), "WORKFLOW_PASS")

    def test_rll_positive_control_is_pass(self) -> None:
        item = next(
            incident
            for incident in self.manifest["incidents"]
            if incident["repository_full_name"] == "instituto-Rafael/relativity-living-light"
        )
        self.assertEqual(classify_incident(item, self.contract), "WORKFLOW_PASS")
        self.assertGreater(item["steps_observed"], 0)

    def test_cancelled_before_execution(self) -> None:
        item = self.incident()
        item["conclusion"] = "cancelled"
        self.assertEqual(classify_incident(item, self.contract), "CANCELLED_BEFORE_EXECUTION")

    def test_policy_requires_explicit_code(self) -> None:
        item = self.incident()
        item["evidence_code"] = "ACTIONS_DISABLED_BY_POLICY"
        self.assertEqual(classify_incident(item, self.contract), "POLICY_BLOCKED")

    def test_duplicate_incident_id_is_rejected(self) -> None:
        manifest = copy.deepcopy(self.manifest)
        manifest["incidents"].append(copy.deepcopy(manifest["incidents"][0]))
        errors = validate_manifest(self.contract, manifest)
        self.assertTrue(any("duplicate incident_id" in error for error in errors))

    def test_duplicate_repository_job_pair_is_rejected(self) -> None:
        manifest = copy.deepcopy(self.manifest)
        duplicate = copy.deepcopy(manifest["incidents"][0])
        duplicate["incident_id"] = "OTHER"
        manifest["incidents"].append(duplicate)
        errors = validate_manifest(self.contract, manifest)
        self.assertTrue(any("duplicate repository/job pair" in error for error in errors))

    def test_declared_classification_must_match_derivation(self) -> None:
        manifest = copy.deepcopy(self.manifest)
        manifest["incidents"][0]["declared_classification"] = "BILLING_BLOCKED"
        errors = validate_manifest(self.contract, manifest)
        self.assertTrue(any("declared_classification mismatch" in error for error in errors))

    def test_declared_scope_cannot_promote_claim(self) -> None:
        manifest = copy.deepcopy(self.manifest)
        manifest["scope_assertions"][0]["claim_allowed"] = True
        errors = validate_manifest(self.contract, manifest)
        self.assertTrue(any("cannot allow claim" in error for error in errors))

    def test_summary_is_deterministic(self) -> None:
        one = summarize(self.contract, self.manifest)
        two = summarize(self.contract, self.manifest)
        self.assertEqual(one, two)
        self.assertEqual(
            one["classification_counts"],
            {"WORKFLOW_PASS": 1, "ZERO_STEP_NO_LOGS": 4},
        )
        self.assertFalse(one["billing_inferred_from_zero_steps"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
