#!/usr/bin/env python3
import importlib.util
import json
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
VALIDATOR = HERE.parent / "scripts" / "rafaelia_receipt_validator.py"

spec = importlib.util.spec_from_file_location("receipt_validator", VALIDATOR)
rv = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(rv)


def base_receipt(receipt_id="R1", predecessor="TOKEN_VAZIO"):
    return {
        "receipt_id": receipt_id,
        "schema_version": rv.SCHEMA_VERSION,
        "repo": "owner/repo",
        "ref": "branch",
        "commit_sha": "a" * 40,
        "event_type": "CONTRACT_COMMIT",
        "source_pointer": "github:owner/repo@{}:docs/x.md".format("a" * 40),
        "artifact_hashes": ["git_blob_sha1:" + "b" * 40],
        "predecessor_receipt": predecessor,
        "evidence_level_before": "UNOBSERVED",
        "evidence_level_after": "SOURCE_OBSERVED",
        "claim_allowed": False,
        "token_vazio": [],
        "next_verifiable_step": "Run the next bounded gate.",
        "timestamp": "2026-09-06T10:10:05Z",
    }


class ReceiptValidatorTests(unittest.TestCase):
    def test_valid(self):
        out = rv.validate_bundle([base_receipt()])
        self.assertTrue(out["ok"])
        self.assertEqual(out["results"][0]["state"], "VALID")

    def test_valid_with_token_vazio(self):
        item = base_receipt()
        item["token_vazio"] = ["runtime_receipt"]
        out = rv.validate_bundle([item])
        self.assertTrue(out["ok"])
        self.assertEqual(out["results"][0]["state"], "VALID_WITH_TOKEN_VAZIO")

    def test_missing_field_is_invalid_schema(self):
        item = base_receipt()
        del item["commit_sha"]
        out = rv.validate_bundle([item])
        self.assertFalse(out["ok"])
        self.assertEqual(out["results"][0]["state"], "INVALID_SCHEMA")

    def test_broken_lineage(self):
        item = base_receipt(predecessor="DOES_NOT_EXIST")
        out = rv.validate_bundle([item])
        self.assertFalse(out["ok"])
        self.assertEqual(out["results"][0]["state"], "BROKEN_LINEAGE")

    def test_claim_true_with_token_vazio_is_rejected(self):
        item = base_receipt()
        item["claim_allowed"] = True
        item["token_vazio"] = ["device_receipt"]
        out = rv.validate_bundle([item])
        self.assertFalse(out["ok"])
        self.assertEqual(out["results"][0]["state"], "UNSUPPORTED_PROMOTION")

    def test_promotion_without_hash_is_rejected(self):
        item = base_receipt()
        item["artifact_hashes"] = []
        out = rv.validate_bundle([item])
        self.assertFalse(out["ok"])
        self.assertEqual(out["results"][0]["state"], "UNSUPPORTED_PROMOTION")

    def test_contract_commit_cannot_claim_runtime(self):
        item = base_receipt()
        item["evidence_level_after"] = "RUNTIME_PROVEN"
        out = rv.validate_bundle([item])
        self.assertFalse(out["ok"])
        self.assertEqual(out["results"][0]["state"], "UNSUPPORTED_PROMOTION")

    def test_fnext_bundle_has_seven_fail_closed_receipts(self):
        bundle = HERE.parent / "data" / "evidence" / "github" / "fnext8-20260906.receipts.v1.json"
        receipts = json.loads(bundle.read_text(encoding="utf-8"))["receipts"]
        out = rv.validate_bundle(receipts)
        self.assertTrue(out["ok"])
        self.assertEqual(out["receipt_count"], 7)
        self.assertEqual(out["accepted_count"], 7)
        self.assertEqual(out["rejected_count"], 0)
        self.assertTrue(all(
            row["state"] == "VALID_WITH_TOKEN_VAZIO"
            for row in out["results"]
        ))
        self.assertFalse(out["claim_promotion_performed"])


if __name__ == "__main__":
    unittest.main()
