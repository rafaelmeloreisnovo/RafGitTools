#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "semantic_context_exam.py"
SPEC = importlib.util.spec_from_file_location("semantic_context_exam", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)


class SemanticContextExamTests(unittest.TestCase):
    def setUp(self) -> None:
        self.contract = json.loads((ROOT / "configs" / "semantic_context_exam_contract.v1.json").read_text(encoding="utf-8"))
        self.manifest = json.loads((ROOT / "examples" / "semantic-context-exam" / "bridge-that-refused-to-lie.example.json").read_text(encoding="utf-8"))

    def test_contract_has_all_seven_parable_gates(self) -> None:
        compiled = module.validate_contract(self.contract)
        self.assertEqual(list(compiled["gates"]), module.EXPECTED_GATES)

    def test_reference_example_passes_and_blocks_unsafe_operation(self) -> None:
        report = module.evaluate_manifest(self.contract, self.manifest)
        states = {item["id"]: item["state"] for item in report["operations"]}
        self.assertEqual(states["OP_ENERGY_ADD"], "EXECUTABLE")
        self.assertEqual(states["OP_MIXED_BLOCK"], "BLOCKED")
        self.assertFalse(report["claim_allowed"])
        self.assertEqual(report["gates"]["G07_DOOR"], "BLOCKED_CLAIM_GATE")

    def test_dimension_mismatch_cannot_be_declared_executable(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["operations"][1]["expected_state"] = "EXECUTABLE"
        with self.assertRaisesRegex(module.ExamError, "expected_state=EXECUTABLE"):
            module.evaluate_manifest(self.contract, broken)

    def test_silent_unit_conversion_is_blocked(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["operations"][0]["input_bindings"][0]["transform_ref"] = None
        broken["operations"][0]["expected_state"] = "BLOCKED"
        report = module.evaluate_manifest(self.contract, broken)
        op = next(item for item in report["operations"] if item["id"] == "OP_ENERGY_ADD")
        self.assertEqual(op["reason"], "UNIT_MISMATCH_NO_COMMON_REPRESENTATION")

    def test_transform_requires_invariant(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["transforms"][0]["invariant"] = "TOKEN_VAZIO"
        with self.assertRaisesRegex(module.ExamError, "requires explicit invariant"):
            module.evaluate_manifest(self.contract, broken)

    def test_tested_requires_test_evidence(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["execution_state"]["test_evidence_refs"] = []
        with self.assertRaisesRegex(module.ExamError, "tested=true requires test evidence"):
            module.evaluate_manifest(self.contract, broken)

    def test_physical_claim_remains_blocked_with_token_vazio_authority(self) -> None:
        report = module.evaluate_manifest(self.contract, self.manifest)
        self.assertEqual(report["execution_state"], "TESTED_NOT_PHYSICALLY_PROVEN")
        self.assertEqual(report["physical_representation_selection"], module.TOKEN_VAZIO)
        self.assertFalse(report["claim_allowed"])

    def test_claim_allowed_must_match_evaluated_gate(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["claim_allowed"] = True
        with self.assertRaisesRegex(module.ExamError, "claim_allowed must equal evaluated"):
            module.evaluate_manifest(self.contract, broken)

    def test_delivery_requires_next_executable_action(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["delivery"]["next_executable_action"] = ""
        with self.assertRaisesRegex(module.ExamError, "next_executable_action"):
            module.evaluate_manifest(self.contract, broken)


if __name__ == "__main__":
    unittest.main()
