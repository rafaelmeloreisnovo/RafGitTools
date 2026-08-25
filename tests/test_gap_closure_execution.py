from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "check_gap_closure_ledger.py"


def load_module():
    spec = importlib.util.spec_from_file_location("gap_closure_gate", SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class GapClosureExecutionTests(unittest.TestCase):
    def test_gap_closure_gate_passes_canonical_ledger(self) -> None:
        module = load_module()
        self.assertEqual(0, module.main())

    def test_contract_preserves_twelve_ordered_invariants(self) -> None:
        module = load_module()
        contract = module.json.loads(module.CONTRACT_PATH.read_text(encoding="utf-8"))
        ids = [item["id"] for item in contract["anti_regression_invariants"]]
        self.assertEqual([f"GC{i:02d}" for i in range(1, 13)], ids)
        self.assertIs(contract["claim_allowed"], False)
        self.assertIs(contract["automatic_promotion"], False)
        self.assertIs(contract["automatic_merge"], False)
        self.assertIs(contract["direct_main_mutation"], False)

    def test_token_vazio_is_unresolved_and_ready_to_test_is_not_resolution(self) -> None:
        module = load_module()
        contract = module.json.loads(module.CONTRACT_PATH.read_text(encoding="utf-8"))
        unresolved = set(contract["unresolved_states"])
        resolved = set(contract["resolution_states"])
        self.assertIn("TOKEN_VAZIO", unresolved)
        self.assertIn("READY_TO_TEST", unresolved)
        self.assertNotIn("TOKEN_VAZIO", resolved)
        self.assertNotIn("READY_TO_TEST", resolved)


if __name__ == "__main__":
    unittest.main()
