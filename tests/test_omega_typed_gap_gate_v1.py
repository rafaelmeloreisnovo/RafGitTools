"""Scoped adversarial tests: semantics and cross-provider non-promotion."""
import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "scripts" / "omega_typed_gap_gate_v1.py"
SPEC = importlib.util.spec_from_file_location("omega_typed_gap_gate_v1", PATH)
GATE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(GATE)
MANIFEST = ROOT / "configs" / "omega-cross-provider-gap-bridge.v1.json"


class OmegaTypedGapGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.original = json.loads(MANIFEST.read_text(encoding="utf-8"))

    def setUp(self):
        self.doc = copy.deepcopy(self.original)

    def errors(self):
        return GATE.validate(self.doc)

    def test_scoped_manifest_accepted_without_claim_promotion(self):
        self.assertEqual([], self.errors())
        self.assertFalse(self.doc["claim_allowed"])

    def test_null_empty_space_zero_and_false_are_distinct(self):
        values = [
            (None, "NULL"), ("", "EMPTY_STRING"), ("    ", "WHITESPACE_ONLY"),
            (0, "NUMBER_ZERO"), (False, "BOOLEAN_FALSE"),
            ("∅", "SYMBOLIC_LITERAL_UNVERIFIED"),
            ("0x00", "TEXT_UNVERIFIED"), ("molho", "TEXT_UNVERIFIED"),
        ]
        for value, expected in values:
            with self.subTest(value=repr(value)):
                self.assertEqual(expected, GATE.literal_kind(value))
        self.assertEqual("ABSENT", GATE.literal_kind(None, present=False))

    def test_user_symbols_remain_source_not_provenance(self):
        self.assertEqual({f"Ω{i:02d}" for i in range(16)},
                         {s["id"] for s in self.doc["symbolic_labels"]})
        self.assertTrue(all(s["proof"] == "NOT_EVIDENCE"
                            for s in self.doc["symbolic_labels"]))

    def test_claim_promotion_is_denied(self):
        self.doc["claim_allowed"] = True
        self.assertTrue(any("claim_allowed" in e for e in self.errors()))

    def test_edge_must_have_both_producer_and_consumer_receipts(self):
        edge = self.doc["edges"][0]
        edge["state"] = "VERIFIED_SCOPED"
        edge["producer_receipt"] = "GITHUB_RUN_1"
        edge["producer_sha"] = "a" * 40
        edge["consumer_sha"] = "b" * 40
        self.assertTrue(any("consumer_receipt" in e for e in self.errors()))

    def test_verified_edge_requires_exact_commit_sha(self):
        edge = self.doc["edges"][0]
        edge.update(state="VERIFIED_SCOPED", producer_receipt="P", consumer_receipt="C",
                    producer_sha="TOKEN_VAZIO", consumer_sha="b" * 40)
        self.assertTrue(any("producer_sha" in e for e in self.errors()))

    def test_verified_scoped_does_not_promote_global_claim(self):
        edge = self.doc["edges"][0]
        edge.update(state="VERIFIED_SCOPED", producer_receipt="P", consumer_receipt="C",
                    producer_sha="a" * 40, consumer_sha="b" * 40)
        self.assertEqual([], self.errors())
        self.assertFalse(self.doc["claim_allowed"])

    def test_missing_graph_node_denied(self):
        self.doc["edges"][0]["to"] = "inexistent"
        self.assertTrue(any("missing graph node" in e for e in self.errors()))

    def test_duplicate_node_id_denied(self):
        self.doc["nodes"].append(copy.deepcopy(self.doc["nodes"][0]))
        self.assertTrue(any("duplicated" in e for e in self.errors()))

    def test_untyped_gap_reason_denied(self):
        self.doc["gaps"][0]["reason"] = "    "
        self.assertTrue(any(".reason" in e for e in self.errors()))

    def test_evidenced_gap_requires_real_evidence_pointer(self):
        self.doc["gaps"][0]["state"] = "EVIDENCED_SCOPED"
        self.assertTrue(any(".evidence_ref" in e for e in self.errors()))

    def test_secret_values_are_not_part_of_manifest(self):
        body = MANIFEST.read_text(encoding="utf-8")
        for forbidden in ("ghp_", "github_pat_", "sb_secret_", "authorization_header", "secret_value"):
            with self.subTest(forbidden=forbidden):
                self.assertNotIn(forbidden, body)

    def test_invalid_literal_reinterpretation_denied(self):
        for sample in self.doc["literal_cases"]:
            if sample["id"] == "spaces4":
                sample["expected_kind"] = "EMPTY_STRING"
        self.assertTrue(any("literal_cases" in e for e in self.errors()))

    def test_symbol_is_not_translation_or_proof(self):
        self.doc["symbolic_labels"][14]["proof"] = "PROVEN_ORIGIN"
        self.assertTrue(any("must not assert provenance" in e for e in self.errors()))

    def test_cross_provider_payload_export_denied(self):
        self.doc["data_transfer"] = "RAW_PRIVATE_FILES"
        self.assertTrue(any("raw data transfer forbidden" in e for e in self.errors()))


if __name__ == "__main__":
    unittest.main()
