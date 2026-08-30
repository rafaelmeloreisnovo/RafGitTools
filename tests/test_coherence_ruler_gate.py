#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("gate", ROOT / "tools/validate_coherence_ruler_gate.py")
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MOD)


def load(path: str):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


class CoherenceRulerGateTests(unittest.TestCase):
    def setUp(self):
        self.config = load("configs/coherence-ruler-gate.v1.json")
        self.packet = load("fixtures/coherence_ruler_gate/positive.v1.json")

    def test_positive_fixture_authorizes_limited_route(self):
        result = MOD.validate(self.config, self.packet)
        self.assertEqual(result["status"], "EXECUTION_ROUTE_AUTHORIZED_LIMITED")
        self.assertFalse(result["claim_allowed"])
        self.assertEqual(result["live_cross_repo_receipt"], "TOKEN_VAZIO")

    def test_hold_status_cannot_execute(self):
        packet = copy.deepcopy(self.packet)
        packet["receipt"]["status"] = "FAILSAFE_HOLD"
        result = MOD.validate(self.config, packet)
        self.assertEqual(result["status"], "HOLD")
        self.assertIn("SOURCE_STATUS_NOT_AUTHORIZED", result["failures"])

    def test_claim_promotion_is_blocked(self):
        packet = copy.deepcopy(self.packet)
        packet["receipt"]["claim_allowed"] = True
        result = MOD.validate(self.config, packet)
        self.assertEqual(result["status"], "HOLD")
        self.assertIn("CLAIM_BOUNDARY_BROKEN", result["failures"])

    def test_missing_watchdog_is_blocked(self):
        packet = copy.deepcopy(self.packet)
        del packet["receipt"]["watchdog_budget"]
        result = MOD.validate(self.config, packet)
        self.assertEqual(result["status"], "HOLD")
        self.assertIn("WATCHDOG_BUDGET_MISSING", result["failures"])

    def test_total_permutation_sweep_is_blocked(self):
        packet = copy.deepcopy(self.packet)
        packet["receipt"]["random_total_permutation_sweep_required"] = True
        result = MOD.validate(self.config, packet)
        self.assertEqual(result["status"], "HOLD")
        self.assertIn("PERMUTATION_SWEEP_NOT_RESTRICTED", result["failures"])


if __name__ == "__main__":
    unittest.main()
