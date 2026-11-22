#!/usr/bin/env python3
"""Negative-control and valid fixture checks: no network, no credentials."""
import copy
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from federated_field_lab import validate, receipt  # noqa: E402

CONTRACT = ROOT / "configs" / "federated-field-lab.v1.json"


class FieldLabTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.good = json.loads(CONTRACT.read_text(encoding="utf-8"))

    def reject(self, mutate):
        bad = copy.deepcopy(self.good)
        mutate(bad)
        self.assertTrue(validate(bad), "negative-control mutation unexpectedly PASS")
        self.assertEqual(receipt(bad, validate(bad))["result"], "FAIL")

    def test_valid_readonly_plan(self):
        self.assertEqual(validate(self.good), [])
        r = receipt(self.good, [])
        self.assertEqual(r["result"], "PASS_SCOPED_CONTRACT_ONLY")
        self.assertEqual(r["private_access"], "TOKEN_VAZIO_NOT_RUN")
        self.assertFalse(r["claim_allowed"])
        self.assertFalse(r["device_executed"])
        self.assertNotIn("PAT_", json.dumps(r))

    def test_deterministic_receipt(self):
        self.assertEqual(receipt(self.good, []), receipt(copy.deepcopy(self.good), []))

    def test_refuse_merge(self):
        self.reject(lambda d: d.update(auto_merge=True))

    def test_refuse_release(self):
        self.reject(lambda d: d.update(auto_release=True))

    def test_refuse_dispatch(self):
        self.reject(lambda d: d.update(auto_dispatch=True))

    def test_refuse_muted_child_protection(self):
        self.reject(lambda d: d["privacy"].update(child_protection=False))

    def test_refuse_dignity_override(self):
        self.reject(lambda d: d["privacy"].update(human_dignity=False))

    def test_refuse_private_payload_export(self):
        self.reject(lambda d: d["privacy"].update(raw_private_export=True))

    def test_refuse_pat_fallback(self):
        self.reject(lambda d: d["credentials"].update(PAT_AGENTS="AUTOMATED_ADMIN"))

    def test_refuse_dynamic_public_ref(self):
        self.reject(lambda d: d["members"][0].update(source_sha="main"))

    def test_refuse_public_unsafe_exec(self):
        self.reject(lambda d: d["members"][2].update(execution="RUN_REMOTE_SHELL"))

    def test_refuse_private_repository_path(self):
        self.reject(lambda d: d["members"][-1].update(repo="owner/private"))

    def test_refuse_private_sha_leak(self):
        self.reject(lambda d: d["members"][-1].update(source_sha="a" * 40))

    def test_refuse_private_auto_execution(self):
        self.reject(lambda d: d["members"][-1].update(execution="RUN"))

    def test_refuse_duplicate(self):
        self.reject(lambda d: d["members"][1].update(id="router"))

    def test_refuse_missing_license_gate(self):
        self.reject(lambda d: d["gates"].remove("SOURCE_LICENSE"))

    def test_refuse_unknown_member_field(self):
        self.reject(lambda d: d["members"][0].update(exfiltration_endpoint="http://invalid"))

    def test_refuse_missing_mapa(self):
        self.reject(lambda d: d["members"].__setitem__(0, {**d["members"][0], "repo": "rafaelmeloreisnovo/other"}))


if __name__ == "__main__":
    unittest.main()
