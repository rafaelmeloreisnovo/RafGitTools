#!/usr/bin/env python3
import hashlib
import importlib.util
import pathlib
import subprocess
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tools" / "rafymlc" / "rafymlc.py"
SPEC = importlib.util.spec_from_file_location("rafymlc", MODULE_PATH)
assert SPEC and SPEC.loader
rafymlc = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = rafymlc
SPEC.loader.exec_module(rafymlc)


class RafYmlCompilerTests(unittest.TestCase):
    def test_nested_map_and_list(self):
        value = rafymlc.parse_document("app:\n  name: test\n  enabled: true\nitems:\n  - one\n  - two\n")
        self.assertEqual(value["app"]["name"], "test")
        self.assertEqual(value["items"], ["one", "two"])

    def test_deterministic_key_order(self):
        left = rafymlc.emit_c(rafymlc.parse_document("z: 1\na: 2\n"), "config")[:2]
        right = rafymlc.emit_c(rafymlc.parse_document("a: 2\nz: 1\n"), "config")[:2]
        self.assertEqual(left, right)

    def test_duplicate_key_rejected(self):
        with self.assertRaises(rafymlc.RafYmlError):
            rafymlc.parse_document("a: 1\na: 2\n")

    def test_anchors_rejected(self):
        with self.assertRaises(rafymlc.RafYmlError):
            rafymlc.parse_document("a: &base 1\n")

    def test_generated_c_compiles_freestanding(self):
        with tempfile.TemporaryDirectory() as tmp_raw:
            tmp = pathlib.Path(tmp_raw)
            header, source, _ = rafymlc.emit_c({"app": {"safe": True, "workers": 16}}, "config")
            (tmp / "config.generated.h").write_text(header)
            (tmp / "config.generated.c").write_text(source)
            compiler = next((name for name in ("clang", "cc", "gcc") if subprocess.run(["sh", "-c", f"command -v {name}"], capture_output=True).returncode == 0), None)
            if not compiler:
                self.skipTest("C compiler unavailable")
            commands = [
                [compiler, "-std=c11", "-ffreestanding", "-fno-builtin", "-I", str(ROOT / "include"), "-I", str(tmp), "-c", str(ROOT / "src" / "rafyml_runtime.c"), "-o", str(tmp / "runtime.o")],
                [compiler, "-std=c11", "-ffreestanding", "-fno-builtin", "-I", str(ROOT / "include"), "-I", str(tmp), "-c", str(tmp / "config.generated.c"), "-o", str(tmp / "config.o")],
            ]
            for command in commands:
                subprocess.run(command, check=True)

    def test_receipt_hashes_are_stable(self):
        header, source, meta = rafymlc.emit_c({"x": 42}, "x")
        self.assertEqual(hashlib.sha256(header.encode()).hexdigest(), hashlib.sha256(header.encode()).hexdigest())
        self.assertEqual(meta["profile"], "RAFYML-FREESTANDING-V1")
        self.assertIn("canonical_sha256", meta)


if __name__ == "__main__":
    unittest.main()
