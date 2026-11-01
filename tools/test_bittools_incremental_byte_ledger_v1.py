"""Read/append/resume falsifiers for BitTools. All fixtures synthetic and offline."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
import zipfile

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("bittools", HERE/"bittools_incremental_byte_ledger_v1.py")
sut = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sut)


class IncrementalByteLedgerTests(unittest.TestCase):
    def setUp(self):
        self.t = tempfile.TemporaryDirectory()
        self.addCleanup(self.t.cleanup)
        self.base = Path(self.t.name)
        self.root = self.base/"source"
        self.root.mkdir()
        self.ledger = self.base/"receipts"/"events.jsonl"

    def cycle(self, **kw):
        return sut.run(self.root, self.ledger, chunk_bytes=kw.get("chunk", 3),
                       max_bytes=kw.get("budget", 5),
                       max_events=kw.get("events", 50),
                       zip_index=kw.get("zip_index", True))

    def records(self):
        return [json.loads(x) for x in self.ledger.read_text().splitlines()]

    def test_byte_exacts_spaces_crossing_chunk_boundary_and_resume(self):
        (self.root/"a.txt").write_bytes(b"AB  C\x00D\r\nZ")
        r1 = self.cycle(chunk=3, budget=3)
        self.assertEqual(r1["state"], "PARTIAL_CHECKPOINT")
        self.assertEqual(r1["bytes_committed"], 3)
        r2 = self.cycle(chunk=3, budget=3)
        self.assertEqual(r2["state"], "PARTIAL_CHECKPOINT")
        for _ in range(8):
            last = self.cycle(chunk=3, budget=3)
            if last["state"] == "DONE_SCOPED":
                break
        self.assertEqual(last["state"], "DONE_SCOPED")
        self.assertEqual(last["files_completed"], 1)
        self.assertEqual(last["bytes_committed"], 10)
        chunks = [v for v in self.records() if v["kind"] == "chunk"]
        self.assertEqual([v["offset"] for v in chunks], [0, 3, 6, 9])
        self.assertEqual(sum(v["anomalies"]["ascii_double_space_pairs"] for v in chunks), 1)
        self.assertEqual(sum(v["anomalies"]["nul_bytes"] for v in chunks), 1)
        self.assertEqual(sum(v["anomalies"]["crlf_pairs"] for v in chunks), 1)
        self.assertEqual(chunks[0]["sha256"], sut.h256(b"AB "))
        self.assertEqual(sut.Ledger(self.ledger, self.root).files["a.txt"]["offset"], 10)
        self.assertEqual(last["whole_file_sha256"], "TOKEN_VAZIO_NOT_COMPUTED")

    def test_zip_central_directory_incremental_no_member_decompression(self):
        with zipfile.ZipFile(self.root/"bundle.zip", "w") as z:
            z.writestr("letters/a  b.txt", b"some  data")
            z.writestr("../unsafe.txt", b"do not extract")
        final = None
        for _ in range(60):
            final = self.cycle(chunk=64, budget=80, events=6)
            if final["state"] == "DONE_SCOPED":
                break
        self.assertEqual(final["state"], "DONE_SCOPED")
        entries = [x for x in self.records() if x["kind"] == "zip_member"]
        self.assertEqual(len(entries), 2)
        self.assertEqual(entries[0]["member_name"], "letters/a  b.txt")
        self.assertTrue(entries[0]["entry_name_safe"])
        self.assertFalse(entries[1]["entry_name_safe"])
        self.assertFalse(entries[0]["member_content_read"])
        self.assertEqual(entries[0]["member_content_sha256"], "TOKEN_VAZIO_NOT_READ")
        self.assertEqual(final["zip_indexes_completed"], 1)

    def test_snapshot_mutation_denies_resume_without_rewrite(self):
        p = self.root/"a.bin"
        p.write_bytes(b"0123456789")
        result = self.cycle(chunk=2, budget=2)
        head = result["ledger_event_sha256"]
        p.write_bytes(b"0000000000")
        with self.assertRaisesRegex(ValueError, "SOURCE_MUTATED"):
            self.cycle(chunk=2, budget=2)
        self.assertEqual(sut.Ledger(self.ledger, self.root).head, head)

    def test_ledger_tamper_blocks_and_never_reinitializes(self):
        (self.root/"a.txt").write_bytes(b"alpha")
        self.cycle(chunk=5, budget=5)
        text = self.ledger.read_text()
        self.ledger.write_text(text.replace("file_done", "file_open", 1))
        with self.assertRaisesRegex(ValueError, "LEDGER_INVALID"):
            self.cycle(chunk=5, budget=5)

    def test_symlink_is_not_followed(self):
        outside = self.base/"secret.txt"
        outside.write_text("PRIVATE CONTENT")
        (self.root/"leak").symlink_to(outside)
        result = self.cycle(chunk=10, budget=10)
        self.assertEqual(result["state"], "DONE_SCOPED")
        self.assertEqual(result["bytes_committed"], 0)
        self.assertEqual([x["kind"] for x in self.records()].count("skip"), 1)

    def test_no_self_index_and_stale_source_and_wrong_root(self):
        (self.root/"hi.txt").write_text("hi")
        with self.assertRaisesRegex(ValueError, "LEDGER_INSIDE_ROOT"):
            sut.run(self.root, self.root/"self.jsonl",
                    chunk_bytes=2, max_bytes=2, max_events=10)
        self.cycle(chunk=8, budget=100)
        with self.assertRaisesRegex(ValueError, "DENY_LEDGER_INVALID"):
            sut.Ledger(self.ledger, self.base)
        (self.root/"hi.txt").unlink()
        with self.assertRaisesRegex(ValueError, "PREVIOUS_SOURCE_DISAPPEARED"):
            self.cycle(chunk=8, budget=100)

    def test_bounded_repeated_incremental_progress_over_many_files(self):
        for i in range(160):
            (self.root/f"file_{i:04}.txt").write_bytes(b"A  B")
        states = []
        for _ in range(70):
            result = self.cycle(chunk=4, budget=100, events=17)
            states.append(result["state"])
            if result["state"] == "DONE_SCOPED":
                break
        self.assertEqual(states[-1], "DONE_SCOPED")
        self.assertEqual(result["files_observed"], 160)
        self.assertEqual(result["files_completed"], 160)
        self.assertEqual(result["bytes_committed"], 640)
        self.assertIn("PARTIAL_CHECKPOINT", states)
        self.assertFalse(result["claim_allowed"])

    def test_budgets_and_nonexistent_ledger_verify_are_fail_closed(self):
        (self.root/"one").write_text("hi")
        with self.assertRaisesRegex(ValueError, "DENY_BUDGET"):
            self.cycle(chunk=0)
        self.assertFalse(self.ledger.exists())
        self.assertEqual(sut.chain_step(sut.ZERO, 0, 2, sut.h256(b"hi")).__len__(), 64)


if __name__ == "__main__":
    unittest.main()
