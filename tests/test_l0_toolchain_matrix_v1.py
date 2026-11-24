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
        values = GATE.source_values(ROOT)
        self.assertIn(values, (GATE.BASE, GATE.CI_TRIAL))
        self.assertIn(GATE.source_admission(values),
                      ("SCOPED_CI_BASELINE", "SCOPED_CI_KSP_2_3_12_TRIAL"))

    def test_exact_candidate_only(self):
        self.assertEqual(GATE.source_admission(GATE.BASE), "SCOPED_CI_BASELINE")
        self.assertEqual(GATE.source_admission(GATE.CI_TRIAL), "SCOPED_CI_KSP_2_3_12_TRIAL")
        self.assertEqual(list(GATE.matrix(GATE.CI_TRIAL)[24:30]), [2, 3, 12, 0, 0, 0])
        for changed in (dict(GATE.CI_TRIAL, ksp="2.3.99"),
                        dict(GATE.CI_TRIAL, agp="9.1.2"),
                        dict(GATE.CI_TRIAL, jdk="21")):
            with self.assertRaises(RuntimeError):
                GATE.source_admission(changed)

    def test_exact_size_and_offsets(self):
        # AGP9/Kotlin2/Compose2/KSP2 snapshot; independent KSP needs separate evidence.
        data = GATE.matrix(GATE.BASE)
        self.assertEqual(len(data), 64)
        self.assertEqual([data[0], data[8], data[16], data[24], data[32], data[40], data[48], data[56]],
                         [9, 9, 2, 2, 2, 17, 3, 0])
        self.assertEqual(list(data[24:30]), [2, 3, 10, 0, 0, 0])

    def test_fail_closed_on_tuple(self):
        # Reject malformed KSP2 legacy-suffix shape; do not infer compatibility.
        broken = dict(GATE.BASE, ksp="2.3.10-1.0")
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
