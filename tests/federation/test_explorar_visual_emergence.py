#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / "scripts" / "federation" / "validate_explorar_visual_emergence.py"
CONTRACT = ROOT / "configs" / "explorar-visual-emergence-v1.json"

spec = importlib.util.spec_from_file_location("validator", VALIDATOR)
validator = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(validator)


class ExplorarVisualEmergenceTests(unittest.TestCase):
    def setUp(self) -> None:
        self.data = json.loads(CONTRACT.read_text(encoding="utf-8"))

    def test_canonical_contract_passes(self) -> None:
        report = validator.validate(copy.deepcopy(self.data))
        self.assertEqual(report["status"], "PASS")
        self.assertEqual(report["sources"], 10)
        self.assertEqual(report["directions"], 7)
        self.assertFalse(report["claim_allowed"])

    def test_symbolic_source_cannot_promote_claim(self) -> None:
        mutated = copy.deepcopy(self.data)
        mutated["sources"][1]["claim_allowed"] = True
        with self.assertRaises(validator.ContractError):
            validator.validate(mutated)

    def test_missing_direction_fails(self) -> None:
        mutated = copy.deepcopy(self.data)
        mutated["directions"].pop()
        with self.assertRaises(validator.ContractError):
            validator.validate(mutated)

    def test_token_vazio_cannot_equal_pass(self) -> None:
        mutated = copy.deepcopy(self.data)
        mutated["invariants"]["TOKEN_VAZIO_is_PASS"] = True
        with self.assertRaises(validator.ContractError):
            validator.validate(mutated)

    def test_correction_status_requires_flag(self) -> None:
        mutated = copy.deepcopy(self.data)
        source = next(item for item in mutated["sources"] if item["id"] == "IMG-08")
        source["requires_correction"] = False
        with self.assertRaises(validator.ContractError):
            validator.validate(mutated)


if __name__ == "__main__":
    unittest.main()
