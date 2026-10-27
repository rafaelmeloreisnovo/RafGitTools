"""Scoped structural checks for Dependabot risk controls; not a YAML schema verifier.

Dependabot itself and GitHub provider policies remain final authorities.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / ".github/dependabot.yml"


class DependabotPolicyGateTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.config = CONFIG.read_text(encoding="utf-8")
        cls.gradle, cls.actions = cls.config.split('  - package-ecosystem: "github-actions"', 1)

    def test_both_ecosystems_are_active_review_only(self):
        self.assertIn('package-ecosystem: "gradle"', self.gradle)
        self.assertIn('directory: "/"', self.gradle)
        self.assertIn('directory: "/"', self.actions)
        self.assertIn('reviewers:', self.gradle)
        self.assertIn('reviewers:', self.actions)
        self.assertIn('open-pull-requests-limit: 10', self.gradle)
        self.assertIn('open-pull-requests-limit: 5', self.actions)
        self.assertNotIn("pull_request_target", self.config)
        self.assertIsNone(re.search(r"(?m)^\\s*(?:auto-merge|automerge):", self.config))

    def test_specific_gradle_groups_precede_broad_android(self):
        specific = ("compose", "hilt", "kotlin", "testing", "networking")
        broad = self.gradle.index("      android:\n")
        for group in specific:
            with self.subTest(group=group):
                self.assertLess(self.gradle.index("      " + group + ":\n"), broad)

    def test_all_version_groups_limit_minor_patch_and_do_not_hide_major(self):
        matches = re.findall(r"(?m)^      ([a-z][a-z-]*):\n(.*?)(?=^      [a-z][a-z-]*:\n|^  - package-ecosystem:|\Z)", self.config, re.S | re.M)
        groups = dict(matches)
        for group in ("compose", "hilt", "kotlin", "testing", "networking",
                      "android", "github-core-actions", "codeql-actions"):
            with self.subTest(group=group):
                body = groups[group]
                self.assertIn('applies-to: "version-updates"', body)
                self.assertIn('update-types: ["minor", "patch"]', body)
        self.assertNotIn("\n    ignore:\n", self.gradle)
        self.assertNotIn("applies-to: security-updates", self.config)

    def test_codeql_init_and_analyze_share_one_immutable_digest(self):
        # PR #717 demonstrated a real 4.38.2 vs 4.39.1 CodeQL failure.
        workflow = (ROOT / ".github/workflows/START.yml").read_text(encoding="utf-8")

        def pins(source):
            return {
                phase: re.findall(
                    rf"(?m)^\\s*uses:\\s*github/codeql-action/{phase}@([0-9a-f]{{40}})\\s",
                    source,
                )
                for phase in ("init", "analyze")
            }

        current = pins(workflow)
        self.assertEqual(len(current["init"]), 1)
        self.assertEqual(len(current["analyze"]), 1)
        self.assertEqual(current["init"][0], current["analyze"][0])

        altered = workflow.replace(
            "github/codeql-action/analyze@" + current["analyze"][0],
            "github/codeql-action/analyze@" + "0" * 40,
            1,
        )
        self.assertNotEqual(pins(altered)["init"][0], pins(altered)["analyze"][0])

    def test_compatibility_and_source_guard_remain(self):
        for expected in (
            'com.android.*', 'androidx.compose.*', 'org.jetbrains.kotlin.*',
            'com.squareup.okhttp3.*', 'actions/*', 'github/codeql-action*',
            'version: 2', 'timezone: "America/Sao_Paulo"',
        ):
            with self.subTest(expected=expected):
                self.assertIn(expected, self.config)


if __name__ == "__main__":
    unittest.main()
