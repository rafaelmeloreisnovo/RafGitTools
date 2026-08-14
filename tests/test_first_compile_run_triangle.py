import importlib.util
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
BUILD_SCRIPT = ROOT / "scripts/runtime/write_android_build_receipt.py"
TRIANGLE_SCRIPT = ROOT / "scripts/runtime/close_first_compile_run_triangle.py"

build_spec = importlib.util.spec_from_file_location("build_receipt", BUILD_SCRIPT)
build_mod = importlib.util.module_from_spec(build_spec)
assert build_spec.loader is not None
build_spec.loader.exec_module(build_mod)

triangle_spec = importlib.util.spec_from_file_location("triangle_receipt", TRIANGLE_SCRIPT)
triangle_mod = importlib.util.module_from_spec(triangle_spec)
assert triangle_spec.loader is not None
triangle_spec.loader.exec_module(triangle_mod)


class CompileRunTriangleTests(unittest.TestCase):
    def setUp(self):
        self.td = tempfile.TemporaryDirectory()
        self.root = Path(self.td.name)
        self.apk = self.root / "app.apk"
        with ZipFile(self.apk, "w") as zf:
            zf.writestr("AndroidManifest.xml", b"manifest")
            zf.writestr("lib/armeabi-v7a/libx.so", b"arm32")
            zf.writestr("lib/arm64-v8a/libx.so", b"arm64")
        self.commit = "1" * 40
        self.apk_sha = build_mod.sha256_file(self.apk)

    def tearDown(self):
        self.td.cleanup()

    def build_receipt(self):
        return {
            "schema": build_mod.SCHEMA,
            "commit": self.commit,
            "apk": {"sha256": self.apk_sha},
            "gates": {"artifact": build_mod.PASS},
        }

    def runtime_receipt(self, runtime_gate="PASS", commit=None, apk_sha=None):
        commit = commit or self.commit
        apk_sha = apk_sha or self.apk_sha
        return {
            "schema": triangle_mod.RUNTIME_SCHEMA,
            "repo": {
                "commit": commit,
                "expected_commit": self.commit,
            },
            "apk": {"sha256": apk_sha},
            "device": {"primary_abi": "armeabi-v7a"},
            "gates": {"runtime": runtime_gate},
        }

    def validate(self, build=None, runtime=None, repo_head=None, actual_sha=None):
        return triangle_mod.validate_triangle(
            build=build or self.build_receipt(),
            runtime=runtime or self.runtime_receipt(),
            actual_apk_sha256=actual_sha or self.apk_sha,
            repo_head=repo_head or self.commit,
            build_receipt_sha256="2" * 64,
            runtime_receipt_sha256="3" * 64,
        )

    def test_build_receipt_requires_dual_abi(self):
        args = type(
            "Args",
            (),
            {
                "apk": str(self.apk),
                "commit": self.commit,
                "variant": "devDebug",
                "workflow_run_id": "1",
                "workflow_run_attempt": "1",
            },
        )()
        receipt = build_mod.build_receipt(args)
        self.assertEqual(build_mod.PASS, receipt["gates"]["artifact"])
        self.assertEqual(self.apk_sha, receipt["apk"]["sha256"])
        self.assertFalse(receipt["claim_allowed"])

    def test_triangle_closes_only_when_all_vertices_match(self):
        receipt = self.validate()
        self.assertEqual(triangle_mod.PASS, receipt["gates"]["triangle_closure"])
        self.assertTrue(receipt["claim_allowed"])
        self.assertEqual(triangle_mod.PASS, receipt["edges"]["build_to_device"])

    def test_apk_hash_mismatch_keeps_triangle_open(self):
        receipt = self.validate(actual_sha="f" * 64)
        self.assertEqual(triangle_mod.FAIL, receipt["vertices"]["build"]["apk_hash_gate"])
        self.assertEqual(triangle_mod.BLOCKED, receipt["gates"]["triangle_closure"])
        self.assertFalse(receipt["claim_allowed"])

    def test_source_commit_mismatch_keeps_triangle_open(self):
        receipt = self.validate(repo_head="4" * 40)
        self.assertEqual(triangle_mod.FAIL, receipt["vertices"]["source"]["gate"])
        self.assertEqual(triangle_mod.BLOCKED, receipt["gates"]["triangle_closure"])

    def test_runtime_blocked_keeps_triangle_open(self):
        receipt = self.validate(runtime=self.runtime_receipt(runtime_gate="BLOCKED"))
        self.assertEqual(triangle_mod.FAIL, receipt["vertices"]["device"]["gate"])
        self.assertEqual(triangle_mod.BLOCKED, receipt["gates"]["triangle_closure"])
        self.assertIn("TOKEN_VAZIO_PHYSICAL_DEVICE_RUNTIME", receipt["F_gap"])


if __name__ == "__main__":
    unittest.main()
