"""Synthetic adversarial lineage fixtures. Private corpus stays outside Git."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "scripts/verify_structural_vectors_v1.py"
spec = importlib.util.spec_from_file_location("lineage", SCRIPT)
lineage = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lineage)


def encode(value):
    return json.dumps(value, sort_keys=True).encode()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def fixture():
    source = [{"id": "SYNTHETIC_ONLY", "mapping": {
        "root": {"parent": None, "message": None},
        "a": {"parent": "root", "message": {
            "author": {"role": "user"}, "content": "DO_NOT_EMIT_ME"}},
        "b": {"parent": "a", "message": {"author": {"role": "assistant"}}},
    }}]
    raw = encode(source)
    artifact = {
        "schema": lineage.SCHEMA,
        "unit": "per_conversation_in_source_order",
        "scope": "ONE_RECOVERED_JSON_SHARD_ONLY",
        "feature_dictionary": lineage.FEATURES,
        "provenance": {"source_sha256": digest(raw), "source_size_bytes": len(raw)},
        "records": [{"ordinal": 0, "conversation_ref_sha256": "a" * 64,
                     "features_7d_integer": [3, 2, 1, 1, 0, 2, 0]}],
        "aggregate": {"conversations": 1, "nodes": 3, "message_objects": 2,
                      "user": 1, "assistant": 1, "other": 0,
                      "parent_links": 2, "dangling_parent_links": 0},
    }
    return raw, artifact


class LineageTests(unittest.TestCase):
    def setUp(self):
        self.source, self.artifact = fixture()

    def verify(self, artifact=None):
        raw = encode(self.artifact if artifact is None else artifact)
        return lineage.verify(self.source, raw, digest(self.source), digest(raw))

    def test_good_counts_preserve_unknown_reference_recipe(self):
        result = self.verify()
        self.assertEqual(result["feature_values_verified"], 7)
        self.assertEqual(result["state"], "VERIFIED_LIMITED")
        self.assertEqual(result["reference_derivation"], "TOKEN_VAZIO_DERIVATION_RECIPE")
        self.assertFalse(result["claim_allowed"])

    def test_source_bytes_must_match_external_pin(self):
        raw = encode(self.artifact)
        with self.assertRaisesRegex(lineage.LineageError, "BYTE_HASH_MISMATCH"):
            lineage.verify(self.source + b" ", raw, digest(self.source), digest(raw))

    def test_artifact_bytes_must_match_external_pin(self):
        raw = encode(self.artifact)
        with self.assertRaisesRegex(lineage.LineageError, "BYTE_HASH_MISMATCH"):
            lineage.verify(self.source, raw + b" ", digest(self.source), digest(raw))

    def test_wrong_vector_fails_even_with_recomputed_artifact_hash(self):
        self.artifact["records"][0]["features_7d_integer"][2] = 0
        with self.assertRaisesRegex(lineage.LineageError, "STRUCTURAL_VECTOR_MISMATCH"):
            self.verify()

    def test_boolean_is_rejected_as_integer(self):
        self.artifact["records"][0]["features_7d_integer"][2] = True
        with self.assertRaisesRegex(lineage.LineageError, "INVALID_FEATURE_VECTOR"):
            self.verify()

    def test_duplicate_record_is_rejected(self):
        self.artifact["records"].append(copy.deepcopy(self.artifact["records"][0]))
        with self.assertRaisesRegex(lineage.LineageError, "RECORD_COUNT_MISMATCH"):
            self.verify()

    def test_source_order_is_required(self):
        self.artifact["records"][0]["ordinal"] = 1
        with self.assertRaisesRegex(lineage.LineageError, "SOURCE_ORDER_MISMATCH"):
            self.verify()

    def test_aggregate_is_checked_independently(self):
        self.artifact["aggregate"]["user"] = 0
        with self.assertRaisesRegex(lineage.LineageError, "AGGREGATE_MISMATCH"):
            self.verify()

    def test_source_provenance_is_bound_to_bytes(self):
        self.artifact["provenance"]["source_sha256"] = "0" * 64
        with self.assertRaisesRegex(lineage.LineageError, "SOURCE_PROVENANCE_MISMATCH"):
            self.verify()

    def test_duplicate_json_key_is_rejected(self):
        raw = b'{"records":[],"records":[]}'
        with self.assertRaisesRegex(lineage.LineageError, "DUPLICATE_JSON_KEY"):
            lineage.verify(self.source, raw, digest(self.source), digest(raw))

    def test_dangling_parent_and_other_role_counts(self):
        conversation = {"mapping": {"a": {
            "parent": "missing", "message": {"author": {"role": "tool"}}}}}
        self.assertEqual(lineage.structural_counts(conversation), [1, 1, 0, 0, 1, 1, 1])

    def test_input_budget_is_enforced(self):
        with self.assertRaisesRegex(lineage.LineageError, "INPUT_LIMIT_REACHED"):
            lineage.pinned_json(b"[]", digest(b"[]"), 1)

    def test_cli_failure_emits_only_error_code(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "DO_NOT_EMIT_ME.json"
            artifact = Path(directory) / "artifact.json"
            source.write_bytes(self.source)
            artifact.write_bytes(encode(self.artifact))
            result = subprocess.run([
                sys.executable, str(SCRIPT), "--source", str(source),
                "--artifact", str(artifact), "--source-sha256", "0" * 64,
                "--artifact-sha256", digest(artifact.read_bytes()),
            ], capture_output=True, text=True, check=False)
        self.assertEqual(result.returncode, 3)
        self.assertEqual(result.stdout, "")
        self.assertEqual(json.loads(result.stderr)["error"], "BYTE_HASH_MISMATCH")
        self.assertNotIn("DO_NOT_EMIT_ME", result.stderr)


if __name__ == "__main__":
    unittest.main()
