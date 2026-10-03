from __future__ import annotations

import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = ROOT / "scripts/validate_context_reconstruction_registry.py"


def _load_validator():
    spec = importlib.util.spec_from_file_location("context_reconstruction_validator", VALIDATOR_PATH)
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_context_reconstruction_registry_is_structurally_valid() -> None:
    validator = _load_validator()
    assert validator.validate_all() == []


def test_seed_is_reference_first_and_claim_gated() -> None:
    validator = _load_validator()
    seed = validator.load_json(validator.SEED)
    assert seed["claim_allowed"] is False
    assert 1 <= len(seed["source_refs"]) <= 3
    assert not (validator.walk_keys(seed) & validator.FORBIDDEN_PAYLOAD_KEYS)
