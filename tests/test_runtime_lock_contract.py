#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "runtime_lock_contract.py"
SPEC = importlib.util.spec_from_file_location("runtime_lock_contract", MODULE_PATH)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)


class RuntimeLockContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.data = json.loads((ROOT / "runtime-lock.json").read_text(encoding="utf-8"))

    def test_current_lock_is_valid_for_source_build(self) -> None:
        mapped = module.validate(self.data)
        self.assertEqual(set(mapped), set(module.REQUIRED_REPOSITORIES))

    def test_duplicate_repository_is_rejected(self) -> None:
        broken = copy.deepcopy(self.data)
        broken["repositories"].append(copy.deepcopy(broken["repositories"][0]))
        with self.assertRaisesRegex(module.ContractError, "duplicate repository"):
            module.validate(broken)

    def test_token_vazio_commit_is_rejected(self) -> None:
        broken = copy.deepcopy(self.data)
        broken["repositories"][0]["commit"] = module.TOKEN_VAZIO
        with self.assertRaisesRegex(module.ContractError, "concrete lowercase 40-hex SHA"):
            module.validate(broken)

    def test_wrong_branch_is_rejected(self) -> None:
        broken = copy.deepcopy(self.data)
        broken["repositories"][0]["branch"] = "main"
        with self.assertRaisesRegex(module.ContractError, "branch must be 'master'"):
            module.validate(broken)

    def test_release_gate_rejects_pending_artifact_hashes(self) -> None:
        with self.assertRaisesRegex(module.ContractError, "concrete value"):
            module.validate(self.data, require_artifact_hashes=True)


if __name__ == "__main__":
    unittest.main()
