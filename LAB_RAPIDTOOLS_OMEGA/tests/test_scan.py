import json
from pathlib import Path
import sys
import tempfile
import unittest

LAB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(LAB / "tools"))
from nov_export_scan import classify, scan


class NovoexportScanTests(unittest.TestCase):
    def test_detect_and_redact_names_and_content(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "nov_export"
            root.mkdir()
            (root / "conversao.jsum").write_text("PRIVATE_MATERIAL_NOT_FOR_GITHUB", encoding="utf-8")
            (root / "leader.jsonl").write_text('{"private":"TOKEN_SECRET"}\n', encoding="utf-8")
            (root / "shader.glsl").write_text("void main() {}", encoding="utf-8")
            (root / "ignored.csv").write_text("1,2,3", encoding="utf-8")
            result = scan(root, max_depth=1)
            self.assertEqual(len(result["records"]), 3)
            data = json.dumps(result)
            for secret in ("PRIVATE_MATERIAL_NOT_FOR_GITHUB", "TOKEN_SECRET",
                           "conversao.jsum", "leader.jsonl", "shader.glsl"):
                self.assertNotIn(secret, data)
            self.assertFalse(result["raw_corpus_uploaded"])
            self.assertFalse(result["claim_allowed"])
            jsum = [r for r in result["records"] if r["format"] == ".jsum"][0]
            self.assertEqual(jsum["parser"], "OPAQUE_NOT_PARSED")

    def test_hash_budget_does_not_read_large_file(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "sources"
            root.mkdir()
            (root / "bench_large.json").write_bytes(b"abc" * 100)
            result = scan(root, hash_max_bytes=1)
            self.assertEqual(result["records"][0]["state"],
                             "TOKEN_VAZIO_HASH_SIZE_GATED")
            self.assertIsNone(result["records"][0]["sha256"])

    def test_scan_denies_unscoped_root_and_invalid_depth(self):
        with self.assertRaises(ValueError):
            scan("/")
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError):
                scan(d, max_depth=10)

    def test_classification_and_depth(self):
        self.assertEqual(classify("MapaAtlas.json"), "atlas")
        self.assertEqual(classify("NeonBench.csv"), "benchmark")
        with tempfile.TemporaryDirectory() as d:
            root = Path(d) / "src"
            sub = root / "sub"
            sub.mkdir(parents=True)
            (sub / "rota.json").write_text("{}")
            self.assertEqual(len(scan(root, max_depth=0)["records"]), 0)
            self.assertEqual(len(scan(root, max_depth=1)["records"]), 1)


if __name__ == "__main__":
    unittest.main()
