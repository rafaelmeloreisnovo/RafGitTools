from __future__ import annotations

import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_rafgitfs_foundation.py"
SPEC = importlib.util.spec_from_file_location("validate_rafgitfs_foundation", SCRIPT)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RafGitFsFoundationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.profile = MODULE.load_json(ROOT / "configs/rafgitfs/default-readonly-profile.json")
        self.receipt = MODULE.load_json(ROOT / "examples/rafgitfs/operation-receipt.readonly.json")

    def test_canonical_foundation_passes(self) -> None:
        report = MODULE.validate(ROOT)
        self.assertEqual("PASS", report["status"])
        self.assertEqual("READ_ONLY", report["profile_mode"])
        self.assertEqual("BLOCKED", report["write_policy"])
        self.assertFalse(report["claim_allowed"])

    def test_claim_promotion_is_rejected(self) -> None:
        profile = copy.deepcopy(self.profile)
        profile["claim_allowed"] = True
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_profile(profile)

    def test_read_only_profile_cannot_enable_writes(self) -> None:
        profile = copy.deepcopy(self.profile)
        profile["write_policy"] = "BRANCH_AND_PULL_REQUEST"
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_profile(profile)

    def test_receipt_is_mandatory(self) -> None:
        profile = copy.deepcopy(self.profile)
        profile["receipt_required"] = False
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_profile(profile)

    def test_protected_branches_cannot_be_empty(self) -> None:
        profile = copy.deepcopy(self.profile)
        profile["protected_branch_patterns"] = []
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_profile(profile)

    def test_success_requires_result_hash(self) -> None:
        receipt = copy.deepcopy(self.receipt)
        receipt["result_sha256"] = None
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_receipt(receipt)

    def test_invalid_request_hash_is_rejected(self) -> None:
        receipt = copy.deepcopy(self.receipt)
        receipt["request_sha256"] = "not-a-hash"
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_receipt(receipt)

    def test_prompt_one_receipt_cannot_claim_write_execution(self) -> None:
        receipt = copy.deepcopy(self.receipt)
        receipt["operation"] = "PUSH"
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_receipt(receipt)

    def test_token_vazio_requires_gap_and_next(self) -> None:
        receipt = copy.deepcopy(self.receipt)
        receipt["success"] = False
        receipt["result_sha256"] = None
        receipt["epistemic_state"] = "TOKEN_VAZIO"
        receipt["f_gap"] = []
        receipt["f_next"] = []
        with self.assertRaises(MODULE.ValidationError):
            MODULE.validate_receipt(receipt)

    def test_missing_contract_file_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            for relative in (
                "contracts/rafgitfs-storage-profile-v1.schema.json",
                "contracts/rafgitfs-operation-receipt-v1.schema.json",
                "configs/rafgitfs/default-readonly-profile.json",
            ):
                source = ROOT / relative
                target = root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)


if __name__ == "__main__":
    unittest.main()
