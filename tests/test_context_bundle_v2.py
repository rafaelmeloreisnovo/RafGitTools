import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "context_bundle_v2.py"
SPEC = importlib.util.spec_from_file_location("context_bundle_v2", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class ContextBundleV2Tests(unittest.TestCase):
    def test_detects_rafgittools_v1(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b1",
            "chunks": ["c1"],
            "created_at": "2026-09-23T04:00:00Z",
            "metadata": {},
        }
        self.assertEqual(MODULE.detect_v1_variant(doc), MODULE.VARIANT_RAFGITTOOLS)

    def test_detects_llama_v1(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b1",
            "conversation_chunks": ["c1"],
            "generated_at": "2026-09-23T04:00:00Z",
        }
        self.assertEqual(MODULE.detect_v1_variant(doc), MODULE.VARIANT_LLAMA)

    def test_detects_private_v1(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b1",
            "chunk_refs": ["c1"],
            "intent_candidates": [],
        }
        self.assertEqual(MODULE.detect_v1_variant(doc), MODULE.VARIANT_PRIVATE)

    def test_ambiguous_v1_is_rejected(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b1",
            "chunks": ["c1"],
            "conversation_chunks": ["c2"],
        }
        with self.assertRaises(MODULE.ContextBundleError):
            MODULE.detect_v1_variant(doc)

    def test_rafgittools_adaptation_preserves_refs_and_explicit_metadata(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b1",
            "chunks": ["c1", "c2"],
            "created_at": "2026-09-23T04:00:00Z",
            "metadata": {
                "intent": "Compare two source files",
                "request_id": "REQ-1",
                "privacy_class": "PRIVATE",
                "x": 7,
            },
        }
        out = MODULE.adapt_v1(doc)
        self.assertEqual(out["chunk_refs"], ["c1", "c2"])
        self.assertEqual(out["intent"]["objective"], "Compare two source files")
        self.assertEqual(out["intent"]["state"], "OBSERVED")
        self.assertEqual(out["privacy_class"], "PRIVATE")
        self.assertEqual(out["annotations"]["legacy_metadata"]["x"], 7)
        self.assertEqual(out["compatibility"]["unmapped_keys"], [])
        MODULE.validate_v2(out)

    def test_llama_adaptation_preserves_evidence_and_constraints_without_inventing_intent(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b2",
            "conversation_chunks": ["c1"],
            "generated_at": "2026-09-23T04:00:00Z",
            "evidence_refs": ["sha256:abc"],
            "constraints": ["read-only"],
        }
        out = MODULE.adapt_v1(doc)
        self.assertEqual(out["chunk_refs"], ["c1"])
        self.assertEqual(out["evidence_refs"], ["sha256:abc"])
        self.assertEqual(out["constraints"], ["read-only"])
        self.assertEqual(out["intent"]["state"], "TOKEN_VAZIO")
        self.assertIn("intent", out["compatibility"]["unresolved_fields"])
        MODULE.validate_v2(out)

    def test_private_adaptation_preserves_candidates_and_marks_missing_time(self):
        doc = {
            "schema": MODULE.SCHEMA_V1,
            "bundle_id": "b3",
            "chunk_refs": ["source#1"],
            "intent_candidates": [{"label": "candidate"}],
            "annotations": {"route": "NOVO"},
            "future_key": "not-copied",
        }
        out = MODULE.adapt_v1(doc)
        self.assertEqual(out["created_at"], "TOKEN_VAZIO")
        self.assertEqual(out["intent"]["state"], "TOKEN_VAZIO")
        self.assertEqual(out["annotations"]["legacy_intent_candidates"], [{"label": "candidate"}])
        self.assertEqual(out["annotations"]["legacy_annotations"], {"route": "NOVO"})
        self.assertEqual(out["compatibility"]["unmapped_keys"], ["future_key"])
        self.assertNotIn("future_key", out["annotations"])
        MODULE.validate_v2(out)

    def test_invalid_sha_is_rejected(self):
        doc = native_v2()
        doc["resources"] = [{
            "resource_id": "r1",
            "provider": "GitHub",
            "source": "owner/repo",
            "locator": "README.md",
            "object_id": None,
            "ref": "main",
            "sha256": "not-a-hash",
            "visibility": "PUBLIC",
            "mime_type": "text/markdown",
            "epistemic_state": "SOURCE_OBSERVED",
        }]
        with self.assertRaises(MODULE.ContextBundleError):
            MODULE.validate_v2(doc)

    def test_materialized_segment_is_bounded_and_hash_typed(self):
        doc = native_v2()
        text = "bounded context"
        import hashlib
        doc["segments"] = [{
            "segment_id": "s1",
            "source_ref": "r1#L1",
            "text": text,
            "text_sha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
            "privacy_class": "PRIVATE",
        }]
        MODULE.validate_v2(doc)

    def test_cli_validate_example(self):
        example = ROOT / "examples" / "context-bundle-v2" / "native.example.json"
        self.assertEqual(MODULE.main(["validate", str(example)]), 0)


def native_v2():
    return {
        "schema": MODULE.SCHEMA_V2,
        "bundle_id": "native",
        "created_at": "2026-09-23T04:00:00Z",
        "intent": {
            "objective": "Read selected context",
            "request_id": None,
            "state": "DECLARED",
        },
        "privacy_class": "PRIVATE",
        "source_generation": None,
        "resources": [],
        "chunk_refs": [],
        "segments": [],
        "evidence_refs": [],
        "constraints": ["read-only"],
        "annotations": {},
        "compatibility": {
            "source_schema": MODULE.SCHEMA_V2,
            "source_variant": MODULE.VARIANT_NATIVE,
            "adapter": "native",
            "unresolved_fields": [],
            "unmapped_keys": [],
        },
    }


if __name__ == "__main__":
    unittest.main()
