from __future__ import annotations

import unittest
from pathlib import Path

from scripts.validate_documentation_claim_boundary import (
    find_forbidden_occurrences,
    validate,
)


class DocumentationClaimBoundaryTest(unittest.TestCase):
    def test_current_tree_passes(self) -> None:
        root = Path(__file__).resolve().parents[1]
        report = validate(root)
        self.assertEqual("PASS", report["status"], report["errors"])

    def test_positive_attainment_wording_is_rejected(self) -> None:
        errors = find_forbidden_occurrences(
            {"README.md": "The release is GDPR/CCPA compliant and production-ready."}
        )

        self.assertGreaterEqual(len(errors), 2)
        self.assertTrue(any("GDPR/CCPA attainment" in error for error in errors))
        self.assertTrue(any("production readiness" in error for error in errors))


if __name__ == "__main__":
    unittest.main()

