#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "runtime_lock_refresh_candidate.py"
SPEC = importlib.util.spec_from_file_location("runtime_lock_refresh_candidate", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)
OBS = ROOT / "data" / "runtime-lock-observations" / "provider-heads-20260814T185818Z.json"
BASE = ROOT / "runtime-lock.json"


class RuntimeLockRefreshCandidateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.base = json.loads(BASE.read_text(encoding="utf-8"))
        self.obs = json.loads(OBS.read_text(encoding="utf-8"))

    def test_current_observation_builds_valid_candidate(self) -> None:
        candidate, changed = module.build_candidate(self.base, self.obs)
        module.contract.validate(candidate)
        self.assertEqual(len(changed), 6)
        self.assertEqual(candidate["generated_at"], self.obs["observed_at"])
        self.assertEqual(candidate["release_state"], "RAFCODEPHI_STACK_REFRESH_CANDIDATE_RUNTIME_PROOF_PENDING")

    def test_artifact_hashes_are_preserved_exactly(self) -> None:
        candidate, _ = module.build_candidate(self.base, self.obs)
        before = {r["name"]: r["expected_hashes"] for r in self.base["repositories"]}
        after = {r["name"]: r["expected_hashes"] for r in candidate["repositories"]}
        self.assertEqual(before, after)

    def test_missing_repository_is_rejected(self) -> None:
        broken = copy.deepcopy(self.obs)
        broken["repositories"].pop()
        with self.assertRaisesRegex(module.RefreshError, "missing observation repositories"):
            module.validate_observation(broken)

    def test_duplicate_repository_is_rejected(self) -> None:
        broken = copy.deepcopy(self.obs)
        broken["repositories"].append(copy.deepcopy(broken["repositories"][0]))
        with self.assertRaisesRegex(module.RefreshError, "duplicate observation repository"):
            module.validate_observation(broken)

    def test_wrong_branch_is_rejected(self) -> None:
        broken = copy.deepcopy(self.obs)
        broken["repositories"][0]["branch"] = "main"
        with self.assertRaisesRegex(module.RefreshError, "branch must be 'master'"):
            module.validate_observation(broken)

    def test_token_vazio_commit_is_rejected(self) -> None:
        broken = copy.deepcopy(self.obs)
        broken["repositories"][0]["commit"] = "TOKEN_VAZIO"
        with self.assertRaisesRegex(module.RefreshError, "concrete lowercase 40-hex SHA"):
            module.validate_observation(broken)

    def test_candidate_bytes_are_deterministic(self) -> None:
        c1, _ = module.build_candidate(self.base, self.obs)
        c2, _ = module.build_candidate(self.base, self.obs)
        self.assertEqual(module.canonical_bytes(c1), module.canonical_bytes(c2))
        self.assertEqual(module.sha256_bytes(module.canonical_bytes(c1)), module.sha256_bytes(module.canonical_bytes(c2)))

    def test_cli_refuses_to_overwrite_canonical_lock(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            receipt = Path(tmp) / "receipt.json"
            cp = subprocess.run(
                [sys.executable, str(SCRIPT), str(BASE), str(OBS), "--candidate", str(BASE), "--receipt", str(receipt)],
                cwd=ROOT,
                capture_output=True,
                text=True,
            )
        self.assertEqual(cp.returncode, 2)
        self.assertIn("refusing to overwrite canonical base lock", cp.stderr)

    def test_cli_is_byte_deterministic_and_receipt_blocks_promotion(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmpdir = Path(tmp)
            c1, r1 = tmpdir / "c1.json", tmpdir / "r1.json"
            c2, r2 = tmpdir / "c2.json", tmpdir / "r2.json"
            for candidate, receipt in ((c1, r1), (c2, r2)):
                cp = subprocess.run(
                    [sys.executable, str(SCRIPT), str(BASE), str(OBS), "--candidate", str(candidate), "--receipt", str(receipt)],
                    cwd=ROOT,
                    capture_output=True,
                    text=True,
                )
                self.assertEqual(cp.returncode, 0, cp.stderr)
            self.assertEqual(c1.read_bytes(), c2.read_bytes())
            self.assertEqual(r1.read_bytes(), r2.read_bytes())
            receipt = json.loads(r1.read_text(encoding="utf-8"))
            self.assertEqual(receipt["changed_count"], 6)
            self.assertFalse(receipt["promoted"])
            self.assertFalse(receipt["claim_allowed"])
            self.assertFalse(receipt["canonical_lock_mutated"])
            self.assertTrue(receipt["artifact_hashes_preserved"])
            self.assertEqual(receipt["candidate_sha256"], module.sha256_bytes(c1.read_bytes()))


if __name__ == "__main__":
    unittest.main()
