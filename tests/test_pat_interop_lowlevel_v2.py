"""Host-side decision falsifiers for the pure C freestanding intent gate."""
from __future__ import annotations
import ctypes
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "native/pat_interop_l0_v2/raf_pat_interop_v2.c"

class PATLowlevelV2Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tmp = tempfile.TemporaryDirectory(prefix="rafpat-l0-")
        output = Path(cls.tmp.name) / "librafpat.so"
        subprocess.run([
            "clang", "-std=c11", "-O2", "-Wall", "-Wextra", "-Werror",
            "-ffreestanding", "-fno-builtin", "-fPIC", "-shared",
            str(SOURCE), "-o", str(output)
        ], check=True, capture_output=True)
        lib = ctypes.CDLL(str(output))
        cls.plan = lib.raf_pat_candidate_v2
        cls.plan.argtypes = [ctypes.c_uint32] * 3
        cls.plan.restype = ctypes.c_uint32
        cls.abi = lib.raf_pat_target_abi_v2
        cls.abi.argtypes = []
        cls.abi.restype = ctypes.c_uint32

    @classmethod
    def tearDownClass(cls):
        cls.tmp.cleanup()

    def test_only_exact_scoped_read_is_candidate(self):
        self.assertEqual(1, self.plan(0, 0, 0xF))
        self.assertEqual(1, self.plan(0, 0, 0xFFFFFFFF))
        for cap in (1, 2, 3, 4, 5, 6, 0xFFFFFFFF):
            self.assertEqual(0, self.plan(cap, 0, 0xF), f"capability {cap}")
        for operation in (1, 2, 3, 9, 0xFFFFFFFF):
            self.assertEqual(0, self.plan(0, operation, 0xF), f"operation {operation}")

    def test_missing_each_evidence_bit_denies(self):
        for bit in (1, 2, 4, 8):
            self.assertEqual(0, self.plan(0, 0, 0xF & ~bit))
        self.assertEqual(0, self.plan(0, 0, 0))

    def test_host_target_is_diagnostic_not_device_proof(self):
        descriptor = self.abi()
        self.assertTrue(descriptor & (8 | 16))
        self.assertFalse(descriptor & 3)

if __name__ == "__main__":
    unittest.main()
