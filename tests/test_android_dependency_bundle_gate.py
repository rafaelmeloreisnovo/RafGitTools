"""Source-level negative tests for Android Gradle/Kotlin/Hilt bundle checks."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools/validate_android_dependency_bundle.py"
spec = importlib.util.spec_from_file_location("android_bundle", SCRIPT)
assert spec and spec.loader
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class AndroidDependencyBundleGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.root = (ROOT / "build.gradle").read_text(encoding="utf-8")
        cls.app = (ROOT / "app/build.gradle").read_text(encoding="utf-8")

    def test_current_hilt_bundle_and_blocking_lint(self):
        result = module.validate(self.root, self.app)
        self.assertEqual(result["status"], "PASS", result)
        self.assertEqual(result["observed"]["hilt_gradle"], "2.48")
        self.assertEqual(result["observed"]["hilt_runtime"], "2.48")
        self.assertEqual(result["observed"]["hilt_ksp"], "2.48")
        self.assertEqual(result["observed"]["lint_abort_on_error"], "true")
        self.assertFalse(result["claim_allowed"])

    def test_split_runtime_and_plugin_fails(self):
        broken = self.app.replace(
            "implementation 'com.google.dagger:hilt-android:2.48'",
            "implementation 'com.google.dagger:hilt-android:2.60.1'",
        )
        result = module.validate(self.root, broken)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("HILT_GRADLE_RUNTIME_KSP_VERSION_SPLIT", result["reasons"])
        # Current Kotlin2 candidate still blocks Hilt version splitting.
        self.assertNotIn("KNOWN_HILT_2601_KOTLIN19_METADATA_FAILURE", result["reasons"])

    def test_hilt_2601_runtime_breaks_legacy_kotlin19_even_when_split(self):
        legacy_root = self.root.replace("kotlin-gradle-plugin:2.4.21", "kotlin-gradle-plugin:1.9.24")
        broken = self.app.replace("com.google.dagger:hilt-android:2.48", "com.google.dagger:hilt-android:2.60.1")
        result = module.validate(legacy_root, broken)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("HILT_GRADLE_RUNTIME_KSP_VERSION_SPLIT", result["reasons"])
        self.assertIn("KNOWN_HILT_2601_KOTLIN19_METADATA_FAILURE", result["reasons"])

    def test_unreviewed_full_hilt_bump_also_fails_with_kotlin19(self):
        root = self.root.replace("kotlin-gradle-plugin:2.4.21", "kotlin-gradle-plugin:1.9.24").replace(
            "hilt-android-gradle-plugin:2.48", "hilt-android-gradle-plugin:2.60.1"
        )
        app = self.app.replace(
            "com.google.dagger:hilt-android:2.48", "com.google.dagger:hilt-android:2.60.1"
        ).replace(
            "com.google.dagger:hilt-compiler:2.48", "com.google.dagger:hilt-compiler:2.60.1"
        )
        result = module.validate(root, app)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("KNOWN_HILT_2601_KOTLIN19_METADATA_FAILURE", result["reasons"])

    def test_lint_errors_cannot_be_silenced(self):
        result = module.validate(self.root,
                                 self.app.replace("abortOnError true", "abortOnError false"))
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("ANDROID_LINT_ERRORS_NOT_BLOCKING", result["reasons"])

    def test_android_apk_is_verified_before_strict_lint_and_no_error_waiver(self):
        workflow = (ROOT / ".github/workflows/START.yml").read_text(encoding="utf-8")
        android = workflow.split("\n  android:\n", 1)[1].split("\n  security:\n", 1)[0]
        positions = [
            android.index("      - name: " + label)
            for label in (
                "Unit tests",
                "Compile device route instrumentation APK",
                "Assemble devDebug",
                "Verify APK and SHA-256",
                "Upload APK",
                "Android lint",
            )
        ]
        self.assertEqual(positions, sorted(positions))
        self.assertIn("run: ./gradlew --no-daemon lintDevDebug --stacktrace", android)
        self.assertNotIn("continue-on-error: true", android)
        self.assertIn("abortOnError true", self.app)

    def test_missing_version_is_fail_closed(self):
        result = module.validate(self.root.replace("hilt-android-gradle-plugin:2.48",
                                                  "hilt-gradle-unknown:2.48"), self.app)
        self.assertEqual(result["status"], "FAIL")
        self.assertTrue(result["reasons"])


if __name__ == "__main__":
    unittest.main()
