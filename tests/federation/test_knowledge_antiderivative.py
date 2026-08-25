from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "scripts" / "federation" / "knowledge_antiderivative.py"
PROFILE_PATH = ROOT / "configs" / "knowledge-antiderivative-v1.json"

spec = importlib.util.spec_from_file_location("knowledge_antiderivative", MODULE_PATH)
module = importlib.util.module_from_spec(spec)
assert spec and spec.loader
spec.loader.exec_module(module)


def load_profile():
    return json.loads(PROFILE_PATH.read_text(encoding="utf-8"))


def test_profile_valid():
    assert module.validate(load_profile()) == []


def test_finite_checks_pass_with_bounded_claim():
    checks = module.finite_checks()
    assert checks["status"] == "PASS"
    assert checks["hex_torus"]["degree_set"] == [6]
    assert checks["global_flux_conservation"]["sum_divergence"] == 0
    assert checks["single_vertex_removal_connectivity"]["boundary"].startswith("finite")


def test_typed_empty_is_preserved_and_not_numeric():
    assert module.typed_empty_roundtrip()
    profile = load_profile()
    assert profile["empty_state_model"]["token_vazio_is_numeric"] is False
    assert profile["empty_state_model"]["token_vazio_is_pass"] is False


def test_circular_shift_preserves_exact_energy():
    signal = [8, -5, 3, 0, 13, -21, 34]
    assert module.energy(signal) == module.energy(module.circular_shift(signal, 5))


def test_canonical_digest_ignores_admissible_order_only():
    records = [
        {"id": "z", "state": "BLOCKED"},
        {"id": "a", "state": "TOKEN_VAZIO"},
        {"id": "m", "state": "VERIFIED_LIMITED"},
    ]
    assert module.canonical_records_digest(records) == module.canonical_records_digest(list(reversed(records)))
    changed = json.loads(json.dumps(records))
    changed[1]["state"] = "CLOSED"
    assert module.canonical_records_digest(records) != module.canonical_records_digest(changed)


def test_hex_torus_has_six_neighbors():
    graph = module.hex_torus(5, 4)
    assert len(graph) == 20
    assert {len(neighbors) for neighbors in graph.values()} == {6}


def test_global_divergence_is_zero_for_closed_torus():
    divergence = module.deterministic_flux_divergence(7, 5)
    assert sum(divergence.values()) == 0


def test_secret_pattern_is_rejected_without_exposing_real_value():
    profile = load_profile()
    profile["term_boundaries"]["observed_terms"].append("github_pat_" + "A" * 30)
    errors = module.validate(profile)
    assert "profile contains a credential/private-key pattern" in errors


def test_unresolved_alias_cannot_be_silently_observed():
    profile = load_profile()
    profile["term_boundaries"]["observed_terms"].append("ZFR")
    errors = module.validate(profile)
    assert "a term cannot be both observed and unresolved" in errors


def test_physical_and_universal_claims_remain_empty():
    profile = load_profile()
    assert profile["toroidal_model"]["physical_universe_claim"] == "TOKEN_VAZIO"
    assert profile["hexagonal_longitudinal_model"]["universal_reconstruction_claim"] == "TOKEN_VAZIO"
    assert profile["hexagonal_longitudinal_model"]["three_edges_is_proof"] is False
