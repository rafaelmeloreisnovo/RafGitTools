#!/usr/bin/env python3
"""Synthetic falsifiers for local file/ZIP/line navigation."""
import io
import sqlite3
import tempfile
import unittest
import zipfile
from pathlib import Path

import archive_line_index_v1 as atlas


class AtlasTests(unittest.TestCase):
    def scan(self, source, out, **overrides):
        opts = dict(max_files=100, max_bytes=1 << 20, max_entry_bytes=1 << 18,
                    max_ratio=200, max_depth=3, private_text=False)
        opts.update(overrides)
        x = atlas.Indexer(source, out, **opts)
        x.visit()
        return x.finish()

    def test_line_locator_and_text_denied(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "x.sh").write_bytes(b"#!/bin/sh\ngit status\nsecret\n")
            report = self.scan(root, Path(d) / "out")
            self.assertTrue(report["complete"])
            self.assertEqual(report["line_fragments"], 3)
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select count(*) from lines where kind='COMMAND_CANDIDATE'").fetchone()[0], 2)
            self.assertEqual(db.execute("select count(*) from lines where private_text is not null").fetchone()[0], 0)
            self.assertEqual(db.execute("select offset_bytes from lines where line_no=2").fetchone()[0], 10)
            db.close()

    def test_zip_nested_and_unsafe(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            nested = io.BytesIO()
            with zipfile.ZipFile(nested, "w") as z:
                z.writestr("inner.py", "def hello():\n    pass\n")
            with zipfile.ZipFile(root / "outer.zip", "w") as z:
                z.writestr("sub.zip", nested.getvalue())
                z.writestr("../escape.txt", "x")
            report = self.scan(root, Path(d) / "out")
            self.assertFalse(report["complete"])
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select status from entries where locator='outer.zip!../escape.txt'").fetchone()[0], "UNSAFE_MEMBER_PATH")
            self.assertEqual(db.execute("select count(*) from entries where locator='outer.zip!sub.zip!inner.py'").fetchone()[0], 1)
            db.close()

    def test_symlink_file_and_directory_are_explicit_gaps(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "target.txt").write_text("safe\n")
            (root / "link.txt").symlink_to(root / "target.txt")
            (root / "subdir").mkdir()
            (root / "linkdir").symlink_to(root / "subdir", target_is_directory=True)
            report = self.scan(root, Path(d) / "out")
            self.assertFalse(report["complete"])
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select count(*) from entries where status='SYMLINK_BLOCKED'").fetchone()[0], 2)
            db.close()

    def test_three_duplicate_zip_names_are_typed_not_crash(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            with zipfile.ZipFile(root / "repeated.zip", "w") as z:
                for _ in range(3):
                    z.writestr("repeated.txt", "ok")
            report = self.scan(root, Path(d) / "out")
            self.assertFalse(report["complete"])
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select count(*) from entries where status='DUPLICATE_NAME_BLOCKED'").fetchone()[0], 2)
            db.close()

    def test_zip_member_count_is_bounded(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            with zipfile.ZipFile(root / "many.zip", "w") as z:
                for i in range(10001):
                    z.writestr(f"e{i}.txt", b"")
            report = self.scan(root, Path(d) / "out")
            self.assertFalse(report["complete"])
            self.assertIn("many.zip:ZIP_MEMBER_COUNT_LIMIT", report["gaps"])

    def test_partial_budget_never_claimed_complete(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "big.txt").write_bytes(b"x" * 200)
            report = self.scan(root, Path(d) / "out", max_bytes=40)
            self.assertFalse(report["complete"])
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertIsNone(db.execute("select sha256 from entries where locator='big.txt'").fetchone()[0])
            db.close()

    def test_disjoint_and_append_only(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            with self.assertRaisesRegex(ValueError, "DISJOINT"):
                self.scan(root, root / "out")
            self.scan(root, Path(d) / "out")
            with self.assertRaisesRegex(ValueError, "MUST_NOT_EXIST"):
                self.scan(root, Path(d) / "out")

    def test_zero_is_not_unknown(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "empty.txt").touch()
            report = self.scan(root, Path(d) / "out")
            self.assertTrue(report["complete"])
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select size_bytes from entries where locator='empty.txt'").fetchone()[0], 0)
            self.assertEqual(db.execute("select sha256 from entries where locator='empty.txt'").fetchone()[0], atlas.digest(b""))
            db.close()

    def test_long_logical_line_keeps_number(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "x.txt").write_bytes(b"a" * 140000 + b"\nend\n")
            report = self.scan(root, Path(d) / "out")
            self.assertEqual(report["line_fragments"], 4)
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select distinct line_no from lines order by line_no").fetchall(), [(1,), (2,)])
            db.close()

    def test_private_text_explicit(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"; root.mkdir()
            (root / "x.txt").write_text("known content\n")
            self.scan(root, Path(d) / "out", private_text=True)
            db = sqlite3.connect(Path(d) / "out" / "ATLAS.sqlite3")
            self.assertEqual(db.execute("select private_text from lines").fetchone()[0], "known content")
            db.close()


if __name__ == "__main__":
    unittest.main()
