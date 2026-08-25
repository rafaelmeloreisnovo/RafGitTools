from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts/validate_rafgitfs_ui.py"
SPEC = importlib.util.spec_from_file_location("validate_rafgitfs_ui", SCRIPT)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class RafGitFsUiGateTest(unittest.TestCase):
    def test_canonical_ui_passes(self) -> None:
        report = MODULE.validate(ROOT)
        self.assertEqual("PASS", report["status"])
        self.assertEqual("4/8", report["prompt"])
        self.assertEqual(5, report["screens"])
        self.assertEqual(5, report["view_models"])
        self.assertFalse(report["remote_write_enabled"])
        self.assertFalse(report["claim_allowed"])

    def test_remote_mutation_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[8],
            lambda value: value + '\nfun push() = Unit\n',
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_claim_promotion_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[11],
            lambda value: value.replace("claimAllowed = false", "claimAllowed = true"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_token_vazio_visibility_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[1],
            lambda value: value.replace("TOKEN_VAZIO", "UNKNOWN_EVIDENCE"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_missing_breadcrumb_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[2],
            lambda value: value.replace("RafGitFsBreadcrumbBar", "MissingBreadcrumbBar"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def test_unregistered_activity_is_rejected(self) -> None:
        with self.mutated_root(
            MODULE.FILES[13],
            lambda value: value.replace(".RafGitFsActivity", ".MissingRafGitFsActivity"),
        ) as root:
            with self.assertRaises(MODULE.ValidationError):
                MODULE.validate(root)

    def mutated_root(self, relative: str, mutate):
        class TempRoot:
            def __init__(self) -> None:
                self.temp = tempfile.TemporaryDirectory()
                self.path = Path(self.temp.name)

            def __enter__(self) -> Path:
                for item in MODULE.FILES:
                    source = ROOT / item
                    target = self.path / item
                    target.parent.mkdir(parents=True, exist_ok=True)
                    text = source.read_text(encoding="utf-8")
                    if item == relative:
                        text = mutate(text)
                    target.write_text(text, encoding="utf-8")
                return self.path

            def __exit__(self, exc_type, exc, tb) -> None:
                self.temp.cleanup()

        return TempRoot()


if __name__ == "__main__":
    unittest.main()
