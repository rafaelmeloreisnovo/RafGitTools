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
        # AGP9/Kotlin2/Compose2/KSP2 snapshot; independent KSP needs separate evidence.
        data = GATE.matrix(GATE.BASE)
        self.assertEqual(len(data), 64)
        self.assertEqual([data[0], data[8], data[16], data[24], data[32], data[40], data[48], data[56]],
                         [9, 9, 2, 2, 2, 17, 3, 0])
        self.assertEqual(list(data[0:3]), [9, 9, 2])
        self.assertEqual(list(data[8:11]), [9, 5, 1])
        self.assertEqual(list(data[24:30]), [2, 3, 10, 0, 0, 0])

    def test_shared_wrapper_projects_use_same_agp_candidate(self):
        # START invokes the root wrapper for these three independent builds.
        for path in ("tools/rafandroid/templates/minimal-jni/build.gradle",
                     "native/authorial_keyboard_l0/android-demo/settings.gradle",
                     "atlas-apk/build.gradle"):
            source = (ROOT / path).read_text(encoding="utf-8")
            self.assertEqual(source.count("id 'com.android.application' version '9.5.1'"), 1, path)
        # Drive snapshot is separately provisioned; do not silently migrate it.
        standalone = (ROOT / "standalone/drive-snapshot/build.gradle").read_text(encoding="utf-8")
        route = (ROOT / ".github/workflows/drive-snapshot-solo-apk.yml").read_text(encoding="utf-8")
        self.assertIn("version '8.6.1'", standalone)
        self.assertIn("gradle-version: '8.7'", route)
        self.assertIn("run: gradle --no-daemon", route)

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
