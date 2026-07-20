from __future__ import annotations

import importlib.util
import shutil
import tempfile
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tools" / "verify_browserraf_entropy_cross_abi.py"
SPEC = importlib.util.spec_from_file_location("browserraf_cross_abi", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class BrowserEntropyCrossAbiVerifierTests(unittest.TestCase):
    def test_target_matrix_is_explicit(self) -> None:
        targets = {
            item["abi"]: (item["triple"], item["elf_class"], item["machine"])
            for item in MODULE.TARGETS
        }
        self.assertEqual(
            targets,
            {
                "armeabi-v7a": ("armv7a-linux-androideabi21", 1, 40),
                "arm64-v8a": ("aarch64-linux-android21", 2, 183),
                "x86_64": ("x86_64-linux-android21", 2, 62),
            },
        )

    def test_elf_inspector_rejects_wrong_machine(self) -> None:
        data = bytearray(20)
        data[:4] = b"\x7fELF"
        data[4] = 2
        data[5] = 1
        data[18:20] = (62).to_bytes(2, "little")
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "probe.o"
            path.write_bytes(data)
            with self.assertRaises(ValueError):
                MODULE.inspect_elf(path, expected_class=2, expected_machine=183)

    @unittest.skipUnless(shutil.which("clang"), "clang is required")
    def test_exact_repository_headers_compile_for_supported_abis(self) -> None:
        manifest = MODULE.verify(str(shutil.which("clang")))
        self.assertEqual(manifest["status"], "PASS")
        self.assertFalse(manifest["claim_allowed"])
        self.assertFalse(manifest["runtime_proved"])
        self.assertFalse(manifest["https_enabled"])
        self.assertTrue(manifest["source_hashes"])
        self.assertEqual(
            {item["abi"] for item in manifest["results"]},
            {"armeabi-v7a", "arm64-v8a", "x86_64"},
        )
        self.assertTrue(all(item["status"] == "PASS" for item in manifest["results"]))


if __name__ == "__main__":
    unittest.main()
