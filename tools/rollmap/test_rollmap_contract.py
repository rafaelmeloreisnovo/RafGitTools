"""Roll-map public, synthetic regression checks. No private Drive corpus."""
import contextlib
import io
import json
import pathlib
import sqlite3
import tempfile
import unittest
import warnings
import zipfile
from types import SimpleNamespace
import importlib.util

spec = importlib.util.spec_from_file_location(
    "rollmap", str(pathlib.Path(__file__).with_name("rafaelia_rollmap_v2.py"))
)
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)

class RollmapContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.root = pathlib.Path(cls.temp.name) / "RAFAELIA_Ω"
        (cls.root / "ALFA" / "BETA" / "Γ").mkdir(parents=True)
        (cls.root / "ALFA" / "BETA" / "Γ" / "README_ψ.txt").write_text(
            "Ω: NODE-REF", encoding="utf-8"
        )
        (cls.root / "ALFA" / "BETA" / "Γ" / "ZERO").write_bytes(b"")
        (cls.root / "ALFA" / "receipts").mkdir()
        (cls.root / "ALFA" / "receipts" / "receipt.json").write_text("{}")
        (cls.root / "ALFA" / "manifests").mkdir()
        (cls.root / "ALFA" / "manifests" / "manifest.json").write_text("{}")
        cls.target = cls.root / "ALFA" / "BETA" / "Γ" / "README_ψ.txt"
        cls.digest = r.bsha(cls.target)
        (cls.root / cls.digest[:10]).mkdir()
        (cls.root / cls.digest[:10] / "fragment.txt").write_text(cls.digest)
        b = io.BytesIO()
        with zipfile.ZipFile(b, "w") as z:
            z.writestr("NEST/Δ/x.txt", "sample")
            z.writestr("../invalid.txt", "reject")
        (cls.root / "package.zip").write_bytes(b.getvalue())
        cls.db = str(pathlib.Path(cls.temp.name) / "index.db")
        with contextlib.redirect_stdout(io.StringIO()):
            cls.build = r.build(SimpleNamespace(
                out=cls.db, replace=False, graph=None, root=[str(cls.root)],
                header_bytes=256, archive_depth=2, archive_budget=1024*1024,
                hash_content=True
            ))
        cls.c = sqlite3.connect(cls.db)
    @classmethod
    def tearDownClass(cls):
        cls.c.close()
        cls.temp.cleanup()
    def ids(self, expr):
        return r.Q(r.lex(expr), self.c).parse()
    def names(self, expr):
        return {self.c.execute("SELECT name FROM nodes WHERE id=?", (i,)).fetchone()[0]
                for i in self.ids(expr)}
    def test_multi_level_ancestry(self):
        self.assertIn("ALFA", self.names("HAS_DESC(name:README_ψ.txt)"))
    def test_cross_sibling_and(self):
        self.assertIn("ALFA", self.names(
            "HAS_DESC(name:manifest.json) AND HAS_DESC(name:receipt.json)"))
    def test_or(self):
        self.assertIn("README_ψ.txt", self.names(
            "name:README_ψ.txt OR name:receipt.json"))
    def test_not(self):
        self.assertNotIn("ZERO", self.names("NOT size=0 AND path:*BETA*"))
    def test_zero_byte_distinct_unknown(self):
        self.assertIn("ZERO", self.names("size=0"))
        self.assertEqual(self.c.execute(
            "SELECT size FROM nodes WHERE name='ALFA'").fetchone()[0], None)
    def test_casefold(self):
        self.assertIn("README_ψ.txt", self.names("name:readme_ψ.txt"))
    def test_virtual_directory(self):
        self.assertIn("Δ", self.names("HAS_CHILD(name:x.txt)"))
    def test_embedded_zip_node(self):
        self.assertIn("x.txt", self.names("path:*NEST/Δ*"))
    def test_archive_path_escape_rejected(self):
        self.assertFalse((cls_root := self.root / "invalid.txt").exists())
        self.assertGreater(self.c.execute(
            "SELECT count(*) FROM edges WHERE rel='UNSAFE_MEMBER_REJECTED'"
        ).fetchone()[0], 0)
    def test_hash_fragment_is_candidate(self):
        self.assertGreater(self.c.execute(
            "SELECT count(*) FROM edges WHERE rel='HASH_FRAGMENT_CANDIDATE'"
        ).fetchone()[0], 0)
    def test_hash_fragment_query(self):
        self.assertTrue(self.ids("hashfrag=" + self.digest[:10]))
    def test_no_candidate_promotion(self):
        self.assertEqual(self.c.execute(
            "SELECT count(*) FROM edges WHERE rel='HASH_FRAGMENT_CANDIDATE' "
            "AND status != 'CANDIDATE'").fetchone()[0], 0)
    def test_bad_predicate_rejected(self):
        with self.assertRaises(r.QueryError):
            self.ids("unknown_field:anything")
    def test_short_hash_rejected(self):
        with self.assertRaises(r.QueryError):
            self.ids("hashfrag=ab")
    def test_graph_expansion_both(self):
        found, edges = r.expansion(self.c, self.ids("name:README_ψ.txt"), 2, set(), "both")
        self.assertGreater(len(found), 1)
        self.assertGreater(len(edges), 0)
    def test_directory_manifest_hash_not_native(self):
        m = json.loads(self.c.execute(
            "SELECT meta FROM nodes WHERE name='ALFA'").fetchone()[0])
        self.assertEqual(len(m["derived_directory_snapshot_sha256"]), 64)
    def test_root_source_not_mutated(self):
        self.assertEqual(r.bsha(self.target), self.digest)
    def test_nonexistent_returns_empty(self):
        self.assertEqual(self.ids("name:*NO_EXAMPLE_MATCH*"), set())

if __name__ == "__main__":
    unittest.main()
