from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from scripts.validate_rafgitfs_cache_offline import FILES, ValidationError, validate


class RafGitFsCacheOfflineGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.source_root = Path(__file__).resolve().parents[1]
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for relative in FILES:
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.source_root / relative, target)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def mutate(self, relative: str, old: str, new: str) -> None:
        path = self.root / relative
        content = path.read_text(encoding="utf-8")
        self.assertIn(old, content)
        path.write_text(content.replace(old, new, 1), encoding="utf-8")

    def test_current_tree_passes(self) -> None:
        self.assertEqual("PASS", validate(self.root)["status"])

    def test_external_storage_is_rejected(self) -> None:
        self.mutate(
            FILES[3],
            'File(context.filesDir, "rafgitfs-cache-v1")',
            'File(context.getExternalStorageDirectory(), "rafgitfs-cache-v1")',
        )
        with self.assertRaises(ValidationError):
            validate(self.root)

    def test_removing_git_blob_verification_is_rejected(self) -> None:
        self.mutate(FILES[5], "verifyGitBlob", "verifyNothing")
        with self.assertRaises(ValidationError):
            validate(self.root)

    def test_pinned_eviction_regression_is_rejected(self) -> None:
        self.mutate(FILES[4], "if (entry.pinned) return false", "if (false) return false")
        with self.assertRaises(ValidationError):
            validate(self.root)

    def test_remote_mutation_is_rejected(self) -> None:
        path = self.root / FILES[6]
        path.write_text(path.read_text(encoding="utf-8") + "\n@retrofit2.http.DELETE(\"repos/x/y\")\n", encoding="utf-8")
        with self.assertRaises(ValidationError):
            validate(self.root)

    def test_claim_promotion_is_rejected(self) -> None:
        self.mutate(FILES[6], "claimAllowed = false", "claimAllowed = true")
        with self.assertRaises(ValidationError):
            validate(self.root)


if __name__ == "__main__":
    unittest.main()
