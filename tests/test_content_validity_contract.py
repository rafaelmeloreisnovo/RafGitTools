#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "content_validity_contract.py"
SPEC = importlib.util.spec_from_file_location("content_validity_contract", MODULE_PATH)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)


class ContentValidityContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.contract = json.loads(
            (ROOT / "configs" / "content_validity_contract.json").read_text(encoding="utf-8")
        )
        self.manifest = json.loads(
            (ROOT / "examples" / "content_validity.example.json").read_text(encoding="utf-8")
        )

    def test_contract_is_valid(self) -> None:
        compiled = module.validate_contract(self.contract)
        self.assertEqual(sorted(compiled["dimensions"]), module.EXPECTED_DIMENSIONS)

    def test_example_manifest_is_valid(self) -> None:
        result = module.validate_manifest(self.contract, self.manifest)
        self.assertEqual(result["tokens"], 2)
        self.assertEqual(result["token_vazio"], 2)
        self.assertEqual(result["qualities"]["T001"]["quality_floor"], 0.8)
        self.assertEqual(result["qualities"]["T002"]["quality_floor"], module.TOKEN_VAZIO)

    def test_token_vazio_is_not_numeric(self) -> None:
        broken = copy.deepcopy(self.contract)
        broken["weight_domain"]["token_vazio_is_numeric"] = True
        with self.assertRaisesRegex(module.ContractError, "must not be numeric"):
            module.validate_contract(broken)

    def test_token_vazio_requires_ledger(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["token_vazio_ledger"] = [
            item for item in broken["token_vazio_ledger"] if item["field"] != "tokens.T002"
        ]
        with self.assertRaisesRegex(module.ContractError, "TOKEN_VAZIO requires a ledger entry"):
            module.validate_manifest(self.contract, broken)

    def test_valid_token_requires_evidence(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["tokens"][0]["evidence_refs"] = []
        with self.assertRaisesRegex(module.ContractError, "VALID requires evidence"):
            module.validate_manifest(self.contract, broken)

    def test_missing_required_dimension_blocks_valid(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["tensor_cells"] = [
            cell
            for cell in broken["tensor_cells"]
            if not (cell["token_ref"] == "T001" and cell["dimension_ref"] == "D08")
        ]
        with self.assertRaisesRegex(module.ContractError, "requires all dimensions"):
            module.validate_manifest(self.contract, broken)

    def test_numeric_weight_requires_evidence(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["tensor_cells"][0]["evidence_refs"] = []
        with self.assertRaisesRegex(module.ContractError, "numeric weight requires evidence"):
            module.validate_manifest(self.contract, broken)

    def test_token_vazio_weight_rejects_positive_evidence(self) -> None:
        broken = copy.deepcopy(self.manifest)
        for cell in broken["tensor_cells"]:
            if cell["weight"] == module.TOKEN_VAZIO:
                cell["evidence_refs"] = ["E001"]
        with self.assertRaisesRegex(module.ContractError, "cannot cite positive evidence"):
            module.validate_manifest(self.contract, broken)

    def test_overlap_must_be_less_than_window(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["windows"][0]["overlap_tokens"] = broken["windows"][0]["max_tokens"]
        with self.assertRaisesRegex(module.ContractError, "overlap must be less"):
            module.validate_manifest(self.contract, broken)

    def test_cross_source_window_requires_bridge(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["sources"].append(
            {
                "id": "SRC002",
                "repository": "rafaelmeloreisnovo/Mapa",
                "ref": "main",
                "path": "README.md",
                "source_state": "SOURCE_DECLARED",
            }
        )
        broken["windows"][0]["source_refs"].append("SRC002")
        with self.assertRaisesRegex(module.ContractError, "cross-source window requires"):
            module.validate_manifest(self.contract, broken)

    def test_promoted_token_rejects_open_contradiction(self) -> None:
        broken = copy.deepcopy(self.manifest)
        broken["contradictions"] = [
            {"id": "C001", "token_refs": ["T001"], "status": "OPEN"}
        ]
        with self.assertRaisesRegex(module.ContractError, "unresolved contradiction"):
            module.validate_manifest(self.contract, broken)

    def test_semantic_features_are_mandatory(self) -> None:
        broken = copy.deepcopy(self.manifest)
        del broken["tokens"][0]["semantic_features"]["negation"]
        with self.assertRaisesRegex(module.ContractError, "must contain exactly"):
            module.validate_manifest(self.contract, broken)

    def test_summary_is_stable(self) -> None:
        first = module.summarize(self.contract, self.manifest)
        second = module.summarize(self.contract, self.manifest)
        self.assertEqual(first, second)
        self.assertIn("TOKEN_VAZIO:1", first)
        self.assertIn("VALID:1", first)


if __name__ == "__main__":
    unittest.main()
