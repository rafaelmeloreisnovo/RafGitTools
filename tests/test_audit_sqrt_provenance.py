from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "scripts" / "audit_sqrt_provenance.py"
SPEC = importlib.util.spec_from_file_location("audit_sqrt_provenance", SCRIPT)
assert SPEC and SPEC.loader
AUDIT = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = AUDIT
SPEC.loader.exec_module(AUDIT)


class SqrtProvenanceAuditTest(unittest.TestCase):
    def make_repo(
        self, rules: list[dict], source: str, path: str = "core.c"
    ) -> tuple[Path, dict]:
        root = Path(tempfile.mkdtemp(prefix="raf-sqrt-audit-"))
        (root / path).parent.mkdir(parents=True, exist_ok=True)
        (root / path).write_text(source, encoding="utf-8")
        manifest = {
            "academic_reference_id": "RJ-RPM107-2023",
            "allowed_classifications": [
                "rafaelia_original",
                "standard_math",
                "regression_julia_reference",
                "needs_review",
            ],
            "strict_forbidden_classifications": ["needs_review"],
            "ignore_directories": [".git"],
            "rules": rules,
        }
        return root, manifest

    def test_unclassified_sqrt_is_error(self) -> None:
        root, manifest = self.make_repo([], "double x = sqrt(9.0);\n")
        findings, summary = AUDIT.audit(root, manifest, strict=False)
        self.assertEqual(summary["errors"], 1)
        self.assertEqual(findings[0].classification, "unclassified")

    def test_standard_math_is_classified(self) -> None:
        rules = [
            {
                "pattern": "core.c",
                "classification": "standard_math",
                "origin": "independent Newton implementation",
            }
        ]
        root, manifest = self.make_repo(rules, "double x = sqrt(9.0);\n")
        _, summary = AUDIT.audit(root, manifest, strict=True)
        self.assertEqual(summary["errors"], 0)
        self.assertEqual(summary["classified"], 1)

    def test_named_academic_use_requires_reference(self) -> None:
        rules = [
            {
                "pattern": "paper.md",
                "classification": "regression_julia_reference",
                "origin": "academic note",
            }
        ]
        root, manifest = self.make_repo(rules, "Formula: sqrt(81).\n", "paper.md")
        _, summary = AUDIT.audit(root, manifest, strict=True)
        self.assertEqual(summary["errors"], 1)

        (root / "paper.md").write_text(
            "[RJ-RPM107-2023]\nFormula: sqrt(81).\n", encoding="utf-8"
        )
        _, summary = AUDIT.audit(root, manifest, strict=True)
        self.assertEqual(summary["errors"], 0)

    def test_needs_review_fails_only_in_strict_mode(self) -> None:
        rules = [
            {
                "pattern": "core.c",
                "classification": "needs_review",
                "origin": "TOKEN_VAZIO",
            }
        ]
        root, manifest = self.make_repo(rules, "int x = isqrt(81);\n")
        _, relaxed = AUDIT.audit(root, manifest, strict=False)
        _, strict = AUDIT.audit(root, manifest, strict=True)
        self.assertEqual(relaxed["errors"], 0)
        self.assertEqual(relaxed["warnings"], 1)
        self.assertEqual(strict["errors"], 1)

    def test_policy_language_does_not_create_false_author_claim(self) -> None:
        rules = [
            {
                "pattern": "policy.md",
                "classification": "regression_julia_reference",
                "origin": "governance",
                "reference": "RJ-RPM107-2023",
            }
        ]
        source = "[RJ-RPM107-2023]\nExample: sqrt(4).\nGate: sole_author=true.\n"
        root, manifest = self.make_repo(rules, source, "policy.md")
        _, summary = AUDIT.audit(root, manifest, strict=True)
        self.assertEqual(summary["errors"], 0)

    def test_manifest_round_trip(self) -> None:
        root, manifest = self.make_repo([], "plain text\n")
        manifest_path = root / "config.json"
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        loaded = AUDIT.load_manifest(manifest_path)
        self.assertEqual(loaded["academic_reference_id"], "RJ-RPM107-2023")


if __name__ == "__main__":
    unittest.main()
