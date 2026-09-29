from __future__ import annotations

import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = ROOT / "scripts" / "validate_private_processing_seed_v1.py"
EXAMPLE_PATH = ROOT / "examples" / "private-processing-seed.example.json"

SPEC = importlib.util.spec_from_file_location("private_processing_seed_v1", VALIDATOR_PATH)
assert SPEC and SPEC.loader
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)


def load_example() -> dict:
    return json.loads(EXAMPLE_PATH.read_text(encoding="utf-8"))


def test_example_is_valid_and_claim_blocked() -> None:
    data = load_example()
    assert validator.validate(data) == []
    assert data["architecture"]["state"] == "DETERMINED"
    assert data["execution"]["state"] == "NOT_RUN"
    assert data["evidence"]["state"] == "TOKEN_VAZIO"
    assert data["claim"]["claim_allowed"] is False


def test_token_vazio_is_not_null_zero_or_false() -> None:
    for bad in (None, "", 0, False):
        data = load_example()
        data["execution"]["execution_id"] = bad
        errors = validator.validate(data)
        assert any("execution.execution_id" in error for error in errors)


def test_unresolved_execution_cannot_promote_claim() -> None:
    data = load_example()
    data["claim"]["claim_allowed"] = True
    data["claim"]["state"] = "ALLOWED"
    errors = validator.validate(data)
    assert any("execution.state must be PASS" in error for error in errors)
    assert any("evidence.state must be PASS" in error for error in errors)


def test_claim_requires_concrete_hashes_and_ids() -> None:
    data = load_example()
    concrete = "a" * 64
    data["source"]["sha256"] = concrete
    data["artifact"]["state"] = "PASS"
    data["artifact"]["id"] = "ART-001"
    data["artifact"]["output_sha256"] = concrete
    data["execution"]["state"] = "PASS"
    data["execution"]["execution_id"] = "EXEC-001"
    data["execution"]["device_ref"] = "DEVICE-OPAQUE-001"
    data["execution"]["processor_sha256"] = concrete
    data["evidence"]["state"] = "PASS"
    data["evidence"]["receipt_id"] = "REC-001"
    data["evidence"]["receipt_sha256"] = concrete
    data["claim"]["claim_allowed"] = True
    data["claim"]["state"] = "ALLOWED"
    data["claim"]["reason"] = "Synthetic unit-test fixture satisfies local gate semantics."
    assert validator.validate(data) == []


def test_claim_state_allowed_is_invalid_when_claim_allowed_false() -> None:
    data = load_example()
    data["claim"]["state"] = "ALLOWED"
    errors = validator.validate(data)
    assert any("claim.state cannot be ALLOWED" in error for error in errors)


def test_source_raw_mutation_is_forbidden() -> None:
    data = load_example()
    data["source"]["raw_mutated"] = True
    errors = validator.validate(data)
    assert any("source.raw_mutated" in error for error in errors)
