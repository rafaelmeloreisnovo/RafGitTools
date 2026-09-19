from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "validate_android_toolchain_matrix.py"
SPEC = importlib.util.spec_from_file_location("toolchain_gate", SCRIPT)
MOD = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MOD)


class AndroidToolchainMatrixTest(unittest.TestCase):
    def test_current_repository_tuple_is_approved(self):
        result = MOD.run()
        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["state"], "PASS_BASELINE")
        self.assertFalse(result["claim_allowed"])

    def test_isolated_kotlin_2420_bump_fails_closed(self):
        matrix = json.loads(
            (ROOT / "configs" / "android-toolchain-matrix.json").read_text(encoding="utf-8")
        )
        observed = {
            "kotlin": "2.4.20",
            "ksp": "1.9.24-1.0.20",
            "compose_mode": "legacy_extension",
            "compose_compiler": "1.5.14",
            "mockk": "1.13.10",
            "mockk_artifact": "mockk-jvm",
        }
        result = MOD.validate(observed, matrix)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn(
            "KOTLIN_2_PLUS_REQUIRES_COMPOSE_COMPILER_GRADLE_PLUGIN",
            result["reasons"],
        )
        self.assertIn("LEGACY_KSP_KOTLIN_PREFIX_MISMATCH", result["reasons"])

    def test_mockk_kotlin2_metadata_line_is_rejected_on_kotlin19(self):
        matrix = json.loads(
            (ROOT / "configs" / "android-toolchain-matrix.json").read_text(encoding="utf-8")
        )
        observed = {
            "kotlin": "1.9.24",
            "ksp": "1.9.24-1.0.20",
            "compose_mode": "legacy_extension",
            "compose_compiler": "1.5.14",
            "mockk": "1.14.11",
            "mockk_artifact": "mockk-jvm",
        }
        result = MOD.validate(observed, matrix)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("UNAPPROVED_ATOMIC_TOOLCHAIN_TUPLE", result["reasons"])

    def test_mockk_facade_artifact_is_rejected_for_governed_jvm_baseline(self):
        matrix = json.loads(
            (ROOT / "configs" / "android-toolchain-matrix.json").read_text(encoding="utf-8")
        )
        observed = {
            "kotlin": "1.9.24",
            "ksp": "1.9.24-1.0.20",
            "compose_mode": "legacy_extension",
            "compose_compiler": "1.5.14",
            "mockk": "1.13.10",
            "mockk_artifact": "mockk",
        }
        result = MOD.validate(observed, matrix)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("UNAPPROVED_ATOMIC_TOOLCHAIN_TUPLE", result["reasons"])

    def test_commented_mockk_declaration_is_not_observed(self):
        root_build = "classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24'\n" \
            "id 'com.google.devtools.ksp' version '1.9.24-1.0.20' apply false\n"
        app_build = (
            'kotlinCompilerExtensionVersion "1.5.14"\n'
            "// accidental literal escape \\n testImplementation 'io.mockk:mockk-jvm:1.13.10'\n"
        )
        with self.assertRaises(ValueError):
            MOD.observe(root_build, app_build)

    def test_unknown_tuple_is_never_promoted(self):
        matrix = json.loads(
            (ROOT / "configs" / "android-toolchain-matrix.json").read_text(encoding="utf-8")
        )
        observed = {
            "kotlin": "9.9.9",
            "ksp": "9.9.9",
            "compose_mode": "compose_plugin",
            "compose_compiler": "9.9.9",
            "mockk": "9.9.9",
            "mockk_artifact": "mockk-jvm",
        }
        result = MOD.validate(observed, matrix)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("UNAPPROVED_ATOMIC_TOOLCHAIN_TUPLE", result["reasons"])


if __name__ == "__main__":
    unittest.main()
