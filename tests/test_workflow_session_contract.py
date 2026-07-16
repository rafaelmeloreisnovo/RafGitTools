#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "workflow_session_contract.py"
SPEC = importlib.util.spec_from_file_location("workflow_session_contract", MODULE_PATH)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)


class WorkflowSessionContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.index = json.loads(
            (ROOT / "workflow-master-index.json").read_text(encoding="utf-8")
        )
        self.session = json.loads(
            (ROOT / "examples" / "workflow_session.example.json").read_text(
                encoding="utf-8"
            )
        )

    def test_master_index_has_exact_coverage(self) -> None:
        layers = module.validate_index(self.index)
        self.assertEqual(list(sorted(layers)), module.EXPECTED_LAYER_IDS)
        self.assertEqual(len(self.index["modules"]), 13)

    def test_example_session_is_valid(self) -> None:
        counts = module.validate_session(self.index, self.session)
        self.assertEqual(counts["support_layers"], 30)
        self.assertEqual(counts["claims"], 5)

    def test_duplicate_master_layer_is_rejected(self) -> None:
        broken = copy.deepcopy(self.index)
        broken["support_layers"].append(copy.deepcopy(broken["support_layers"][0]))
        with self.assertRaisesRegex(module.ContractError, "duplicate support_layers"):
            module.validate_index(broken)

    def test_missing_session_layer_is_rejected(self) -> None:
        broken = copy.deepcopy(self.session)
        broken["support_layers"].pop()
        with self.assertRaisesRegex(module.ContractError, "exactly S01..S30"):
            module.validate_session(self.index, broken)

    def test_verified_test_claim_requires_passing_test(self) -> None:
        broken = copy.deepcopy(self.session)
        broken["tests"][0]["result"] = "FAIL"
        with self.assertRaisesRegex(module.ContractError, "requires referenced PASS tests"):
            module.validate_session(self.index, broken)

    def test_token_vazio_claim_requires_ledger_entry(self) -> None:
        broken = copy.deepcopy(self.session)
        broken["token_vazio"] = [
            item for item in broken["token_vazio"] if item["field"] != "claims.C005"
        ]
        with self.assertRaisesRegex(module.ContractError, "requires a token_vazio ledger entry"):
            module.validate_session(self.index, broken)

    def test_unknown_evidence_reference_is_rejected(self) -> None:
        broken = copy.deepcopy(self.session)
        broken["claims"][0]["evidence_refs"].append("E999")
        with self.assertRaisesRegex(module.ContractError, "unknown id: E999"):
            module.validate_session(self.index, broken)

    def test_pass_support_layer_requires_evidence(self) -> None:
        broken = copy.deepcopy(self.session)
        broken["support_layers"][0]["evidence_refs"] = []
        with self.assertRaisesRegex(module.ContractError, "PASS requires evidence"):
            module.validate_session(self.index, broken)

    def test_summary_is_stable_and_contains_next_action(self) -> None:
        first = module.summarize(self.index, self.session)
        second = module.summarize(self.index, self.session)
        self.assertEqual(first, second)
        self.assertIn("support_layers:30", first)
        self.assertIn("next=READY:", first)


if __name__ == "__main__":
    unittest.main()
