"""Hosted regression fixtures only; not a freestanding or device gate."""
import importlib.util
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
MODULE = ROOT / "scripts/verify_l0_toolchain_shell_v1.py"
SPEC = importlib.util.spec_from_file_location("rmr_l0_host", MODULE)
GATE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GATE)

class MatrixBoundaryTests(unittest.TestCase):
    def test_real_source_pins_and_start_route(self):
        self.assertEqual(GATE.BASE, GATE.source_values(ROOT))

    def test_exact_size_and_offsets(self):
        # AGP9 candidate changes byte 8 only; unsupported Kotlin1 remains BLOCKED.
        data = GATE.matrix(GATE.BASE)
        self.assertEqual(len(data), 64)
        self.assertEqual([data[0], data[8], data[16], data[24], data[32], data[40], data[48], data[56]],
                         [9, 9, 1, 1, 1, 17, 3, 0])
        self.assertEqual(list(data[24:30]), [1, 9, 24, 1, 0, 20])

    def test_fail_closed_on_tuple(self):
        broken = dict(GATE.BASE, ksp="1.9.24")
        with self.assertRaises(ValueError):
            GATE.matrix(broken)

    def test_explicit_evidence_boundary(self):
        source = (ROOT / "native/authorial_toolchain_l0/toolchain_l0.c").read_text()
        self.assertIn("void rmr_toolchain_l0", source)
        self.assertNotIn("#include", source)
        for forbidden in ("malloc(", "calloc(", "free(", "syscall(", "memcpy(",
                          "while (", "for ("):
            self.assertNotIn(forbidden, source)

if __name__ == "__main__":
    unittest.main()
