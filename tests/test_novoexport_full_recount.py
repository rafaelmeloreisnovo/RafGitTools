import importlib.util
import json
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "evidence" / "novoexport" / "full-recount-000-050.safe.v1.json"
SPEC = importlib.util.spec_from_file_location(
    "rafaelia_navigator_full_recount_v1",
    ROOT / "tools" / "rafaelia_navigator" / "rafaelia_navigator_full_recount_v1.py",
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def message(mid, role="user", text="PRIVATE_BODY_SENTINEL"):
    value = {
        "author": {"role": role},
        "content": {"content_type": "text", "parts": [text]},
    }
    if mid is not None:
        value["id"] = mid
    return value


def conversation(cid, nodes):
    return {
        "id": cid,
        "mapping": {
            node_id: {"parent": None, "message": msg}
            for node_id, msg in nodes
        },
    }


def write_shard(root, number, conversations):
    path = root / f"conversations-{number:03d}.json"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(conversations), encoding="utf-8")
    return path


class NovoexportFullRecountTests(unittest.TestCase):
    def test_counts_observations_unique_ids_distinct_duplicates_and_excess(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(
                source,
                0,
                [
                    conversation("c0", [("n0", message("m-shared")), ("n-null", None)]),
                    conversation("c1", [("n1", message("m-shared", "assistant"))]),
                ],
            )
            write_shard(
                source,
                1,
                [
                    conversation("c2", [("n2", message("m-shared")), ("n3", message("m-unique"))]),
                ],
            )

            report = MODULE.recount(source, 0, 1, expected_roots=3)

            self.assertEqual("PASS_SCOPED_STRUCTURAL_RECOUNT", report["status"])
            self.assertEqual(2, report["source_shards"])
            self.assertEqual(3, report["root_conversations_observed"])
            self.assertEqual(5, report["mapping_nodes_observed"])
            self.assertEqual(4, report["message_bearing_nodes_observed"])
            self.assertEqual(1, report["null_or_nonmessage_nodes"])
            self.assertEqual(2, report["unique_effective_message_ids"])
            self.assertEqual(1, report["distinct_duplicated_effective_message_ids"])
            self.assertEqual(2, report["duplicate_effective_message_id_observations"])
            self.assertEqual(0, report["duplicates_same_conversation_excess"])
            self.assertEqual(2, report["duplicates_cross_conversation_excess"])
            self.assertEqual(1, report["duplicates_same_shard_excess"])
            self.assertEqual(1, report["duplicates_cross_shard_excess"])

    def test_receipt_never_contains_message_body_and_never_grants_authority(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(
                source,
                0,
                [conversation("c0", [("n0", message("m0", text="DO_NOT_PUBLISH_THIS_BODY"))])],
            )
            report = MODULE.recount(source, 0, 0)
            serialized = json.dumps(report, sort_keys=True)

            self.assertNotIn("DO_NOT_PUBLISH_THIS_BODY", serialized)
            self.assertFalse(report["training_executed"])
            self.assertFalse(report["weight_update_executed"])
            self.assertFalse(report["mission_authority_from_dataset"])
            self.assertFalse(report["claim_allowed"])
            self.assertIn("DATASET_INFORMS != MISSION_AUTHORITY", report["invariants"])
            self.assertIn("RECOUNT != TRAINING", report["invariants"])

    def test_safe_evidence_binds_exact_full_recount_without_semantic_promotion(self):
        evidence = json.loads(EVIDENCE.read_text(encoding="utf-8"))

        self.assertEqual("conversations-000..050", evidence["scope"])
        self.assertEqual(51, evidence["source_shards"])
        self.assertEqual(1107289897, evidence["source_bytes"])
        self.assertEqual(5054, evidence["root_conversations_observed"])
        self.assertEqual(307043, evidence["mapping_nodes_observed"])
        self.assertEqual(301991, evidence["message_bearing_nodes_observed"])
        self.assertEqual(296794, evidence["unique_effective_message_ids"])
        self.assertEqual(2988, evidence["distinct_duplicated_effective_message_ids"])
        self.assertEqual(5197, evidence["duplicate_effective_message_id_observations"])
        self.assertEqual(0, evidence["duplicates_same_conversation_excess"])
        self.assertEqual(5197, evidence["duplicates_cross_conversation_excess"])
        self.assertEqual(
            "ba27012377c5ec7d3603a8c0eb42f12c83ac700c85b9d2b2ef9ff514860b122d",
            evidence["source_set_digest_sha256"],
        )
        self.assertEqual(
            "TV-MESSAGE-ID-CROSS-CONVERSATION-REUSE-CAUSE-20260907",
            evidence["residual_token_vazio"],
        )
        self.assertFalse(evidence["semantic_full_ingestion_proven"])
        self.assertFalse(evidence["training_executed"])
        self.assertFalse(evidence["weight_update_executed"])
        self.assertFalse(evidence["mission_authority_from_dataset"])
        self.assertFalse(evidence["claim_allowed"])
        self.assertIn("STRUCTURAL_CARDINALITY != SEMANTIC_EXHAUSTIVITY", evidence["invariants"])

    def test_missing_shard_blocks_contiguous_scope(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(source, 0, [])
            write_shard(source, 2, [])
            with self.assertRaisesRegex(MODULE.RecountBlocked, "missing shards: 001"):
                MODULE.recount(source, 0, 2)

    def test_duplicate_shard_filename_across_recursive_tree_blocks(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(source / "a", 0, [])
            write_shard(source / "b", 0, [])
            with self.assertRaisesRegex(MODULE.RecountBlocked, "duplicate shard filenames"):
                MODULE.recount(source, 0, 0)

    def test_non_array_source_blocks_parse(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            path = source / "conversations-000.json"
            path.write_text(json.dumps({"id": "not-an-array"}), encoding="utf-8")
            with self.assertRaisesRegex(MODULE.RecountBlocked, "top-level JSON is not an array"):
                MODULE.recount(source, 0, 0)

    def test_duplicate_conversation_identity_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(source, 0, [conversation("same", []), conversation("same", [])])
            with self.assertRaisesRegex(MODULE.RecountBlocked, "duplicate conversation ids"):
                MODULE.recount(source, 0, 0)

    def test_expected_root_mismatch_blocks_promotion(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            write_shard(source, 0, [conversation("c0", [])])
            with self.assertRaisesRegex(MODULE.RecountBlocked, "root conversation count mismatch"):
                MODULE.recount(source, 0, 0, expected_roots=2)

    def test_invalid_range_blocks_before_discovery(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(MODULE.RecountBlocked, "invalid shard range"):
                MODULE.recount(Path(directory), 5, 4)

    def test_cli_blocked_receipt_remains_fail_closed(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory)
            output = StringIO()
            with redirect_stdout(output):
                code = MODULE.main([str(source), "--first", "0", "--last", "0"])
            report = json.loads(output.getvalue())

            self.assertEqual(3, code)
            self.assertEqual("BLOCKED", report["status"])
            self.assertEqual("TOKEN_VAZIO_FULL_RECOUNT_SOURCE_OR_PARSE_GATE", report["token_vazio"])
            self.assertFalse(report["training_executed"])
            self.assertFalse(report["weight_update_executed"])
            self.assertFalse(report["mission_authority_from_dataset"])
            self.assertFalse(report["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
