"""Fail-open/fail-closed adversarial fixtures for the scoped lint ratchet."""
from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "scripts/ci/privacy_lint_gate.py"
spec = importlib.util.spec_from_file_location("privacy_lint_gate", PATH)
assert spec and spec.loader
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def lint(issues: str) -> str:
    return '<issues format="6">' + issues + '</issues>'

def issue(level: str, location: str, code: str = "Example") -> str:
    return '<issue id="' + code + '" severity="' + level + '"><location file="' + location + '" line="1"/></issue>'


class PrivacyLintRatchetTests(unittest.TestCase):
    def test_legacy_observations_remain_visible_and_scoped(self):
        xml = lint(
            issue("Error", "app/src/main/kotlin/com/rafgittools/core/haptics/Legacy.kt", "MissingPermission")
            + issue("Warning", "app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt")
            + issue("Information", "app/src/main/kotlin/com/rafgittools/core/Other.kt")
        )
        result = module.inspect_lint_xml(xml, maximum=1)
        self.assertEqual(result["status"], "PASS_SCOPED_LEGACY_DEBT")
        self.assertEqual(result["legacy_global_errors_observed"], 1)
        self.assertFalse(result["global_errors_resolved"])
        self.assertEqual(result["warnings_observed"], 1)
        self.assertEqual(result["hints_observed"], 1)
        self.assertFalse(result["claim_allowed"])

    def test_any_privacy_error_is_blocked_despite_global_allowance(self):
        for path in (
            "/app/src/main/kotlin/com/rafgittools/ui/screens/privacy/RepositoryPrivacyScreen.kt",
            "/app/src/main/kotlin/com/rafgittools/data/privacy/RepositoryPrivacyManager.kt",
        ):
            with self.subTest(path=path):
                result = module.inspect_lint_xml(lint(issue("Error", path)), maximum=68)
                self.assertEqual(result["status"], "FAIL")
                self.assertIn("PRIVACY_SOURCE_LINT_ERRORS", result["reasons"])

    def test_legacy_error_ceiling_never_grows(self):
        xml = lint(issue("Error", "/app/core/Old.kt") * 69)
        result = module.inspect_lint_xml(xml)
        self.assertEqual(result["status"], "FAIL")
        self.assertIn("ANDROID_LINT_ERROR_COUNT_REGRESSION", result["reasons"])

    def test_unexpected_or_missing_xml_is_not_a_pass(self):
        for xml in ("", "<invalid/>", "<issues>"):
            with self.subTest(xml=xml):
                with self.assertRaises(Exception):
                    module.inspect_lint_xml(xml)

    def test_zero_errors_is_identified_separately(self):
        result = module.inspect_lint_xml(lint(issue("Warning", "/app/Other.kt")))
        self.assertEqual(result["status"], "PASS_SCOPED_LEGACY_DEBT")
        self.assertTrue(result["global_errors_resolved"])


if __name__ == "__main__":
    unittest.main()
