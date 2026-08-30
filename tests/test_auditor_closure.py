#!/usr/bin/env python3
from __future__ import annotations

import copy
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
POLICY = json.loads((ROOT / "contracts/auditor-closure-policy.v1.json").read_text(encoding="utf-8"))
RECORD = json.loads((ROOT / "audits/AUDITOR_CLOSURE_PR390_20260830.v1.json").read_text(encoding="utf-8"))


class AuditorClosureTests(unittest.TestCase):
    def test_all_real_closures_are_limited_and_evidenced(self):
        for item in RECORD["closures"]:
            self.assertEqual(item["decision"], "CLOSED_LIMITED")
            self.assertTrue(item["scope"])
            self.assertTrue(item["evidence_refs"])
            self.assertTrue(item["claim_boundary"])

    def test_token_vazio_cannot_be_silently_closed(self):
        item = copy.deepcopy(RECORD["open_items"][0])
        self.assertEqual(item["decision"], "TOKEN_VAZIO")
        forbidden = POLICY["forbidden_promotions"]
        self.assertTrue(forbidden["token_vazio_to_closed_without_new_evidence"])

    def test_failed_documentation_preserves_merge_hold(self):
        evidence = {x["name"]: x["conclusion"] for x in RECORD["observed_evidence"]}
        self.assertEqual(evidence["Documentation"], "failure")
        self.assertFalse(RECORD["merge_authorized"])
        merge_item = next(x for x in RECORD["open_items"] if x["id"] == "OP-004")
        self.assertEqual(merge_item["decision"], "HOLD")

    def test_external_compliance_remains_unclaimed(self):
        item = next(x for x in RECORD["open_items"] if x["id"] == "OP-002")
        self.assertEqual(item["decision"], "TOKEN_VAZIO")
        self.assertFalse(RECORD["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
