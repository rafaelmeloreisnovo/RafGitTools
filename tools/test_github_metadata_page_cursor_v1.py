"""Synthetic negative fixtures: no provider, private corpus or network in CI."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

MODULE = Path(__file__).resolve().parent / "github_metadata_page_cursor_v1.py"
spec = importlib.util.spec_from_file_location("github_cursor", MODULE)
sut = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sut)
OWNER = "rafaelmeloreisnovo"
HEAD = "a" * 40


class CursorGateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.page = Path(self.tmp.name) / "page.json"
        self.ledger = Path(self.tmp.name) / "ledger.jsonl"

    def row(self, name="alpha", ident=1, head=HEAD):
        branch = "main"
        full = OWNER + "/" + name
        return {"id": ident, "full_name": full, "default_branch": branch,
                "head_sha": head,
                "head_source_url": ("https://api.github.com/repos/" + full +
                                    "/branches/main") if head != sut.TOKEN else None}

    def write(self, number=0, incoming=None, outgoing=None, repos=None):
        data = {"schema": sut.PAGE_SCHEMA, "provider": "github",
                "source_owner": OWNER, "page_index": number,
                "cursor_in": incoming, "cursor_out": outgoing,
                "repositories": repos if repos is not None else [self.row()]}
        self.page.write_text(json.dumps(data, sort_keys=True))
        return data

    def call(self):
        return sut.process(str(self.page), str(self.ledger), OWNER)

    def test_checkpoint_head_and_exact_idempotent_replay(self):
        self.write(outgoing="next")
        first = self.call()
        self.assertEqual(first["state"], "PAGE_CHECKPOINTED")
        self.assertEqual(first["heads_observed"], 1)
        bytes_before = self.ledger.read_bytes()
        self.assertEqual(self.call()["state"], "ALREADY_COMMITTED")
        self.assertEqual(bytes_before, self.ledger.read_bytes())
        self.assertEqual(len(sut.replay(bytes_before)), 1)

    def test_cursor_resume_and_terminal(self):
        self.write(outgoing="p2")
        self.call()
        self.write(1, "p2", None, [self.row("beta", 2, sut.TOKEN)])
        result = self.call()
        self.assertEqual(result["heads_token_vazio"], 1)
        self.assertEqual(result["next_cursor"], sut.TOKEN)
        self.write(2, "p3", None, [self.row("gamma", 3)])
        with self.assertRaisesRegex(ValueError, "CURSOR_MISMATCH"):
            self.call()

    def test_missing_page_skipped(self):
        self.write(number=2, incoming="p2")
        with self.assertRaisesRegex(ValueError, "CURSOR_GAP"):
            self.call()

    def test_changed_page_rejected_without_rewrite(self):
        self.write()
        self.call()
        original = self.ledger.read_bytes()
        self.write(repos=[self.row("edited", 3)])
        with self.assertRaisesRegex(ValueError, "HISTORICAL_REPLAY_DRIFT"):
            self.call()
        self.assertEqual(self.ledger.read_bytes(), original)

    def test_duplicate_identity_cross_page(self):
        self.write(outgoing="p")
        self.call()
        self.write(1, "p", None, [self.row("other", 1)])
        with self.assertRaisesRegex(ValueError, "REPEATED_REPOSITORY"):
            self.call()

    def test_cross_page_cursor_mismatch(self):
        self.write(outgoing="expected")
        self.call()
        self.write(1, "wrong", None, [self.row("second", 2)])
        with self.assertRaisesRegex(ValueError, "CURSOR_MISMATCH"):
            self.call()

    def test_corrupted_chain_is_not_reset(self):
        self.write()
        self.call()
        self.ledger.write_bytes(self.ledger.read_bytes().replace(b"alpha", b"omega"))
        with self.assertRaisesRegex(ValueError, "LEDGER_CHAIN"):
            self.call()

    def test_truncated_ledger_is_denied(self):
        self.write()
        self.call()
        self.ledger.write_bytes(self.ledger.read_bytes()[:-1])
        with self.assertRaisesRegex(ValueError, "TRUNCATED_LEDGER"):
            self.call()

    def test_untyped_unknown_not_converted_to_zero(self):
        self.write(repos=[self.row(head=None)])
        with self.assertRaisesRegex(ValueError, "UNTYPED_UNKNOWN_HEAD"):
            self.call()
        self.assertFalse(self.ledger.exists())

    def test_fake_head_without_exact_branch_ref_rejected(self):
        bad = self.row()
        bad["head_source_url"] = "https://example.com/fake"
        self.write(repos=[bad])
        with self.assertRaisesRegex(ValueError, "BRANCH_SOURCE_REF"):
            self.call()

    def test_foreign_owner_and_duplicate_ids_rejected(self):
        self.write(repos=[self.row(), self.row("beta", 1)])
        with self.assertRaisesRegex(ValueError, "DUPLICATE_REPOSITORY"):
            self.call()
        foreign = self.row()
        foreign["full_name"] = "other/alpha"
        self.write(repos=[foreign])
        with self.assertRaisesRegex(ValueError, "REPO_OWNER_BRANCH"):
            self.call()

    def test_self_ledger_and_symlink_denied(self):
        self.write()
        with self.assertRaisesRegex(ValueError, "SELF_LEDGER"):
            sut.process(str(self.page), str(self.page), OWNER)
        link = Path(self.tmp.name) / "alias.json"
        link.symlink_to(self.page)
        with self.assertRaises(OSError):
            sut.process(str(link), str(self.ledger), OWNER)


if __name__ == "__main__":
    unittest.main()
