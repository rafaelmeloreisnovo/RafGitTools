from __future__ import annotations

import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "check_gap_closure_ledger.py"


def load_module():
    spec = importlib.util.spec_from_file_location("gap_closure_gate", SCRIPT)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_gap_closure_gate_passes_canonical_ledger() -> None:
    module = load_module()
    assert module.main() == 0


def test_contract_preserves_twelve_ordered_invariants() -> None:
    module = load_module()
    contract = module.json.loads(module.CONTRACT_PATH.read_text(encoding="utf-8"))
    ids = [item["id"] for item in contract["anti_regression_invariants"]]
    assert ids == [f"GC{i:02d}" for i in range(1, 13)]
    assert contract["claim_allowed"] is False
    assert contract["automatic_promotion"] is False
    assert contract["automatic_merge"] is False
    assert contract["direct_main_mutation"] is False


def test_token_vazio_is_unresolved_and_ready_to_test_is_not_resolution() -> None:
    module = load_module()
    contract = module.json.loads(module.CONTRACT_PATH.read_text(encoding="utf-8"))
    unresolved = set(contract["unresolved_states"])
    resolved = set(contract["resolution_states"])
    assert "TOKEN_VAZIO" in unresolved
    assert "READY_TO_TEST" in unresolved
    assert "TOKEN_VAZIO" not in resolved
    assert "READY_TO_TEST" not in resolved
