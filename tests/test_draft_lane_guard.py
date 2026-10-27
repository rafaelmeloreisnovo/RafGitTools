"""Adversarial, stdlib-only draft CI route tests (no network/provider mutations)."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "scripts/ci/draft_lane_guard.py"
spec = importlib.util.spec_from_file_location("draft_lane_guard", SOURCE)
assert spec and spec.loader
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class DraftLaneGuardTests(unittest.TestCase):
    def test_docs_fast(self):
        self.assertEqual(module.select_lane(["docs/README.md"]), "fast")

    def test_android_and_privacy_changes_force_android(self):
        for filename in (
            "app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt",
            "app/src/test/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacySelectionTest.kt",
            "gradle/libs.versions.toml",
            "gradle/wrapper/gradle-wrapper.properties",
            "build.gradle",
            "settings.gradle.kts",
            "src/main/kotlin/OtherModule.kt",
        ):
            with self.subTest(filename=filename):
                self.assertEqual(module.select_lane([filename]), "android")

    def test_workflows_and_dependabot_force_full(self):
        for filename in (
            ".github/workflows/START.yml",
            ".github/dependabot.yml",
            ".github/actions/workflow-graph-audit/action.yml",
            "scripts/ci/draft_lane_guard.py",
            "tests/ci/test_workflow.py",
        ):
            with self.subTest(filename=filename):
                self.assertEqual(module.select_lane([filename]), "full")

    def test_order_independence_and_strictest_lane_wins(self):
        for filenames in (
            ["docs/a.md", "app/src/main/kotlin/Foo.kt"],
            ["app/src/main/kotlin/Foo.kt", "docs/a.md"],
            ["docs/a.md", ".github/dependabot.yml", "app/src/a.kt"],
        ):
            with self.subTest(filenames=filenames):
                self.assertEqual(
                    module.select_lane(filenames),
                    "full" if ".github/dependabot.yml" in filenames else "android",
                )

    def test_missing_diff_runs_full(self):
        self.assertEqual(module.select_lane([]), "full")

    def test_reject_unsafe_paths(self):
        for filename in ("../secret", "/etc/passwd", "docs//name", "docs/./name",
                         "docs/line\nname", "docs\\windows"):
            with self.subTest(filename=filename):
                with self.assertRaises(ValueError):
                    module.select_lane([filename])

    def test_cli_requires_complete_null_delimited_diff(self):
        bad = subprocess.run(
            [sys.executable, str(SOURCE)],
            input=b"app/build.gradle",
            capture_output=True,
            check=False,
        )
        self.assertEqual(bad.returncode, 2)
        self.assertIn(b"refused", bad.stderr)

        good = subprocess.run(
            [sys.executable, str(SOURCE)],
            input=b"docs/notes.md\0app/src/main/kotlin/Privacy.kt\0",
            capture_output=True,
            check=False,
        )
        self.assertEqual(good.returncode, 0)
        self.assertEqual(good.stdout.strip(), b"android")


if __name__ == "__main__":
    unittest.main()
