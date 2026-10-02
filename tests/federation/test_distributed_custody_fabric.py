from __future__ import annotations

import copy
import importlib.util
from pathlib import Path

import pytest


ROOT = Path(__file__).resolve().parents[2]
VALIDATOR_PATH = ROOT / "scripts" / "federation" / "validate_distributed_custody_fabric.py"
CONFIG_PATH = ROOT / "configs" / "distributed-custody-fabric.v1.json"

spec = importlib.util.spec_from_file_location("custody_validator", VALIDATOR_PATH)
assert spec and spec.loader
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


def test_custody_fabric_contract_is_structurally_valid() -> None:
    contract = validator.load_contract(CONFIG_PATH)
    validator.validate(contract)
    digest = validator.canonical_digest(contract)
    assert len(digest) == 64
    assert contract["claim_allowed"] is False


def test_secret_materialization_fails_closed() -> None:
    contract = validator.load_contract(CONFIG_PATH)
    broken = copy.deepcopy(contract)
    broken["restricted_fragment"]["materialize_in_logs"] = True
    with pytest.raises(validator.ContractError):
        validator.validate(broken)


def test_single_plane_reconstruction_fails_closed() -> None:
    contract = validator.load_contract(CONFIG_PATH)
    broken = copy.deepcopy(contract)
    broken["reconstruction"]["single_plane_reconstruction"] = True
    with pytest.raises(validator.ContractError):
        validator.validate(broken)
