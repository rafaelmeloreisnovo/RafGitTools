#!/usr/bin/env python3
from __future__ import annotations

import importlib.util
import io
import json
import sys
import unittest
import urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "private_provider_access_gate.py"
SPEC = importlib.util.spec_from_file_location("private_provider_access_gate", SCRIPT)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)

REPO = "rafaelmeloreisnovo/llamaRafaelia"
COMMIT = "a" * 40


class FakeResponse:
    def __init__(self, status: int, payload: object):
        self.status = status
        self._data = json.dumps(payload).encode("utf-8")

    def getcode(self) -> int:
        return self.status

    def read(self) -> bytes:
        return self._data

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


class PrivateProviderAccessGateTest(unittest.TestCase):
    def test_missing_token_is_explicit_access_gap_without_network(self) -> None:
        called = False

        def opener(*args, **kwargs):
            nonlocal called
            called = True
            raise AssertionError("network must not be called without a token")

        result = module.evaluate(REPO, COMMIT, "", opener=opener)
        self.assertFalse(called)
        self.assertEqual(result["result"], "BLOCKED")
        self.assertEqual(result["f_gap"], [module.TOKEN_VAZIO_ACCESS])
        self.assertFalse(result["access_proven"])
        self.assertFalse(result["claim_allowed"])

    def test_exact_commit_200_proves_access_only(self) -> None:
        def opener(request, timeout=20):
            self.assertEqual(request.get_header("Authorization"), "Bearer secret")
            return FakeResponse(200, {"sha": COMMIT})

        result = module.evaluate(REPO, COMMIT, "secret", opener=opener)
        self.assertEqual(result["result"], "PASS")
        self.assertTrue(result["access_proven"])
        self.assertEqual(result["observed_commit"], COMMIT)
        self.assertEqual(result["f_gap"], [])
        self.assertFalse(result["runtime_proven"])
        self.assertFalse(result["device_proven"])
        self.assertFalse(result["claim_allowed"])

    def test_404_is_authority_gap_not_source_absence_claim(self) -> None:
        def opener(request, timeout=20):
            raise urllib.error.HTTPError(request.full_url, 404, "Not Found", {}, io.BytesIO())

        result = module.evaluate(REPO, COMMIT, "secret", opener=opener)
        self.assertEqual(result["http_status"], 404)
        self.assertEqual(result["f_gap"], [module.TOKEN_VAZIO_ACCESS])
        self.assertFalse(result["access_proven"])

    def test_401_and_403_are_authority_gaps(self) -> None:
        for status in (401, 403):
            with self.subTest(status=status):
                def opener(request, timeout=20, status=status):
                    raise urllib.error.HTTPError(request.full_url, status, "Denied", {}, io.BytesIO())

                result = module.evaluate(REPO, COMMIT, "secret", opener=opener)
                self.assertEqual(result["http_status"], status)
                self.assertEqual(result["f_gap"], [module.TOKEN_VAZIO_ACCESS])

    def test_commit_identity_mismatch_blocks(self) -> None:
        result = module.evaluate(
            REPO,
            COMMIT,
            "secret",
            opener=lambda request, timeout=20: FakeResponse(200, {"sha": "b" * 40}),
        )
        self.assertEqual(result["result"], "BLOCKED")
        self.assertEqual(result["f_gap"], [module.TOKEN_VAZIO_COMMIT_IDENTITY])
        self.assertFalse(result["access_proven"])

    def test_network_failure_is_distinct_token_vazio(self) -> None:
        def opener(request, timeout=20):
            raise urllib.error.URLError("offline")

        result = module.evaluate(REPO, COMMIT, "secret", opener=opener)
        self.assertEqual(result["result"], "BLOCKED")
        self.assertEqual(result["f_gap"], [module.TOKEN_VAZIO_NETWORK])

    def test_invalid_commit_is_rejected_before_network(self) -> None:
        with self.assertRaisesRegex(ValueError, "40-hex"):
            module.evaluate(REPO, "short", "secret")


if __name__ == "__main__":
    unittest.main()
