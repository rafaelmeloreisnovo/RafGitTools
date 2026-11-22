#!/usr/bin/env python3
"""Synthetic tests for private interaction genealogy. NO user corpus stored."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools" / "rafaelia_navigator"))
from interaction_token_ledger_v1 import Blocked, emit_shard, tokens_of


def node(mid, role, text=None, parent=None, children=None):
    msg = None if mid is None else {
        "id": mid, "author": {"role": role},
        "content": {"content_type": "text", "parts": [text or ""]}}
    return {"parent": parent, "children": children or [], "message": msg}


class InteractionLedgerTests(unittest.TestCase):
    def test_tokens_are_lexical_and_not_model_ids(self):
        result = tokens_of(["Olá, μΠΩ⁷!"], emit_tokens=True)
        self.assertEqual(result["model_token_count"], "TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED")
        self.assertEqual(result["lexical_tokens"][0]["surface"], "Olá")
        self.assertGreater(result["lexical_count"], 2)

    def test_ancestry_branches_and_duplicate_native_ids(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            src = root / "conversations-000.json"
            data = [
                {"id": "c1", "mapping": {
                    "n0": node(None, None),
                    "n1": node("reused", "user", "Pergunta μ", "n0", ["n2", "n3"]),
                    "n2": node("a2", "assistant", "Resposta A", "n1"),
                    "n3": node("a3", "assistant", "Resposta B", "n1"),
                    "n4": node("m4", "user", "Continua", "n2"),
                    "n5": node("a5", "assistant", "Outra interação", "n4")}},
                {"id": "c2", "mapping": {
                    "x1": node("reused", "user", "Outra pergunta"),
                    "x2": node("z2", "assistant", "Resposta independente", "x1")}},
            ]
            src.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")
            out = root / "private"
            receipt = emit_shard(src, out, emit_tokens=True)
            rows = [json.loads(line) for line in (out / "INTERACTIONS_PRIVATE.jsonl").read_text().splitlines()]
            self.assertEqual(receipt["counts"]["mapping_nodes"], 8)
            turns = [json.loads(line) for line in
                     (out / "INTERACTION_INDEX_PRIVATE.jsonl").read_text().splitlines()]
            self.assertEqual(len(turns), 3)
            self.assertEqual(len(set(t["interaction_id"] for t in turns)), 3)
            self.assertTrue(any(len(t["assistant_source_pointers"]) == 2 for t in turns))
            self.assertTrue(all(t["model_token_count"] ==
                "TOKEN_VAZIO_MODEL_TOKENIZER_UNPINNED" for t in turns))
            self.assertIn("interaction_index_sha256", receipt)
            self.assertEqual(receipt["counts"]["interactions_anchored"], 3)
            self.assertEqual(rows[1]["interaction_id"], rows[2]["interaction_id"])
            self.assertEqual(rows[1]["interaction_id"], rows[3]["interaction_id"])
            self.assertNotEqual(rows[2]["interaction_id"], rows[4]["interaction_id"])
            self.assertNotEqual(rows[1]["stable_node_key_sha256"], rows[6]["stable_node_key_sha256"])
            self.assertEqual(rows[1]["content"]["text_parts"], ["Pergunta μ"])
            self.assertFalse(rows[1]["claim_allowed"])
            with self.assertRaises(Blocked):
                emit_shard(src, out)

    def test_missing_parent_and_cycle_are_typed(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            src = root / "conversations-007.json"
            src.write_text(json.dumps([{"id": "c", "mapping": {
                "a": node("m1", "assistant", "no parent", "missing"),
                "b": node("m2", "assistant", "loop", "b")}}]), encoding="utf-8")
            receipt = emit_shard(src, root / "OUT")
            self.assertEqual(receipt["status"], "PARTIAL_TOPOLOGY_GAPS_NOT_PROMOTABLE")
            self.assertEqual(receipt["counts"]["topology_gaps"], 2)

    def test_multimodal_part_does_not_become_fake_text(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            src = root / "conversations-002.json"
            src.write_text(json.dumps([{"id": "c", "mapping": {"n": {
                "parent": None, "message": {"id": "m", "author": {"role": "user"},
                "content": {"content_type": "multimodal",
                            "parts": ["oi", {"image": "opaque_asset"}]}}}}}]), encoding="utf-8")
            emit_shard(src, root / "OUT")
            row = json.loads((root / "OUT" / "INTERACTIONS_PRIVATE.jsonl").read_text().strip())
            self.assertEqual(row["content"]["text_parts"], ["oi"])
            self.assertEqual(len(row["content"]["opaque_part_descriptors"]), 1)
            self.assertEqual(row["content"]["lexical_tokenization"]["lexical_count"], 1)

    def test_idempotent_source_immutability_scope(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); src = root / "conversations-011.json"
            src.write_text(json.dumps([{"id": "c", "mapping": {"n": node("u", "user", "x")}}]))
            first = emit_shard(src, root / "OUTPUT")
            self.assertEqual(first["source_sha256_before"], first["source_sha256_after"])
            self.assertEqual(first["status"], "PASS_SCOPED_STRUCTURAL_NOT_MODEL_TOKENS")


if __name__ == "__main__":
    unittest.main()
