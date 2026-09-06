#!/usr/bin/env python3
from __future__ import annotations

import copy
import importlib.util
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "runtime_lock_promotion_gate.py"
SPEC = importlib.util.spec_from_file_location("runtime_lock_promotion_gate", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)

CANDIDATE = ROOT / "data" / "runtime-lock-candidates" / "runtime-lock-candidate-20260906T220143Z.json"
REFRESH = ROOT / "data" / "runtime-lock-candidates" / "runtime-lock-refresh-receipt-20260906T220143Z.json"


def concrete_candidate() -> tuple[bytes, dict]:
    candidate = json.loads(CANDIDATE.read_text(encoding="utf-8"))
    for index, entry in enumerate(candidate["repositories"]):
        entry["expected_hashes"]["manifest_sha256"] = f"{index + 1:064x}"
        entry["expected_hashes"]["bundle_sha256"] = f"{index + 101:064x}"
    data = module.canonical_bytes(candidate)
    return data, candidate


def valid_refresh(candidate_sha: str) -> dict:
    refresh = json.loads(REFRESH.read_text(encoding="utf-8"))
    refresh["candidate_sha256"] = candidate_sha
    return refresh


def valid_integration(candidate_sha: str) -> dict:
    return {
        "schema": module.INTEGRATION_SCHEMA,
        "candidate_sha256": candidate_sha,
        "result": "PASS",
        "checks": [
            {"id": "source-lock", "status": "PASS", "evidence_sha256": "1" * 64},
            {"id": "cross-repo", "status": "PASS", "evidence_sha256": "2" * 64},
        ],
        "claim_allowed": False,
    }


def valid_authorization(candidate_sha: str) -> dict:
    return {
        "schema": module.AUTH_SCHEMA,
        "candidate_sha256": candidate_sha,
        "authorized": True,
        "scope": "CANONICAL_RUNTIME_LOCK_PROMOTION_AFTER_ALL_TECHNICAL_GATES_PASS",
        "claim_allowed": False,
    }


class RuntimeLockPromotionGateTest(unittest.TestCase):
    def test_fully_bound_concrete_evidence_is_promotable(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        result = module.assess(
            candidate_bytes,
            candidate,
            valid_refresh(sha),
            valid_integration(sha),
            valid_authorization(sha),
        )
        self.assertTrue(result["promotable"])
        self.assertEqual(result["evaluation"], "PASS")
        self.assertEqual(result["f_gap"], [])
        self.assertFalse(result["canonical_lock_mutated"])
        self.assertFalse(result["claim_allowed"])

    def test_current_candidate_stays_blocked_on_token_vazio_artifact_hashes(self) -> None:
        candidate_bytes = CANDIDATE.read_bytes()
        candidate = json.loads(candidate_bytes.decode("utf-8"))
        sha = module.sha256_bytes(candidate_bytes)
        refresh = json.loads(REFRESH.read_text(encoding="utf-8"))
        result = module.assess(
            candidate_bytes,
            candidate,
            refresh,
            valid_integration(sha),
            valid_authorization(sha),
        )
        self.assertFalse(result["promotable"])
        self.assertEqual(result["evaluation"], "BLOCKED")
        self.assertTrue(any(item.startswith("TOKEN_VAZIO_ARTIFACT_HASH:") for item in result["f_gap"]))

    def test_refresh_receipt_must_bind_exact_candidate_bytes(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        refresh = valid_refresh("0" * 64)
        with self.assertRaisesRegex(module.PromotionGateError, "does not match candidate bytes"):
            module.assess(candidate_bytes, candidate, refresh, valid_integration(sha), valid_authorization(sha))

    def test_integration_candidate_mismatch_blocks(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        integration = valid_integration("0" * 64)
        result = module.assess(candidate_bytes, candidate, valid_refresh(sha), integration, valid_authorization(sha))
        self.assertFalse(result["promotable"])
        self.assertIn("TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_CANDIDATE_MISMATCH", result["f_gap"])

    def test_failed_integration_check_blocks(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        integration = valid_integration(sha)
        integration["checks"][1]["status"] = "FAIL"
        result = module.assess(candidate_bytes, candidate, valid_refresh(sha), integration, valid_authorization(sha))
        self.assertFalse(result["promotable"])
        self.assertIn("TOKEN_VAZIO_INTEGRATION_CHECK_1_PASS", result["f_gap"])

    def test_missing_evidence_digest_blocks(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        integration = valid_integration(sha)
        integration["checks"][0]["evidence_sha256"] = "TOKEN_VAZIO"
        result = module.assess(candidate_bytes, candidate, valid_refresh(sha), integration, valid_authorization(sha))
        self.assertFalse(result["promotable"])
        self.assertIn("TOKEN_VAZIO_INTEGRATION_CHECK_0_EVIDENCE_SHA256", result["f_gap"])

    def test_authorization_cannot_be_reused_for_another_candidate(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        authorization = valid_authorization("f" * 64)
        result = module.assess(candidate_bytes, candidate, valid_refresh(sha), valid_integration(sha), authorization)
        self.assertFalse(result["promotable"])
        self.assertIn("TOKEN_VAZIO_RUNTIME_LOCK_PROMOTION_AUTHORIZATION_CANDIDATE_MISMATCH", result["f_gap"])

    def test_authorization_never_enables_claim_by_itself(self) -> None:
        candidate_bytes, candidate = concrete_candidate()
        sha = module.sha256_bytes(candidate_bytes)
        integration = valid_integration(sha)
        integration["result"] = "TOKEN_VAZIO"
        result = module.assess(candidate_bytes, candidate, valid_refresh(sha), integration, valid_authorization(sha))
        self.assertFalse(result["promotable"])
        self.assertFalse(result["claim_allowed"])
        self.assertIn("TOKEN_VAZIO_CROSS_REPO_CANDIDATE_INTEGRATION_EXECUTION", result["f_gap"])


if __name__ == "__main__":
    unittest.main()
