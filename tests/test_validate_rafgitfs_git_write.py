from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from scripts.validate_rafgitfs_git_write import FILES, ValidationError, validate

class GitWriteGateTest(unittest.TestCase):
    def setUp(self):
        self.source = Path(__file__).resolve().parents[1]
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for relative in FILES:
            target = self.root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(self.source / relative, target)

    def tearDown(self): self.temp.cleanup()

    def mutate(self, relative, old, new):
        path = self.root / relative
        text = path.read_text(encoding="utf-8")
        self.assertIn(old, text)
        path.write_text(text.replace(old, new, 1), encoding="utf-8")

    def test_current_tree_passes(self):
        self.assertEqual("PASS", validate(self.root)["status"])

    def test_force_push_default_is_rejected(self):
        self.mutate(FILES[0], "val force: Boolean = false", "val force: Boolean = true")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_non_draft_pull_request_is_rejected(self):
        self.mutate(FILES[0], "val draft: Boolean = true", "val draft: Boolean = false")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_delete_endpoint_is_rejected(self):
        path = self.root / FILES[0]
        path.write_text(path.read_text(encoding="utf-8") + '\n@retrofit2.http.DELETE("repos/x/y")\n', encoding="utf-8")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_workspace_path_guard_is_required(self):
        self.mutate(FILES[1], "WORKSPACE_PATH_ESCAPE", "UNSAFE_WORKSPACE_PATH")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_git_internal_path_guard_is_required(self):
        self.mutate(FILES[1], "GIT_INTERNAL_PATH_BLOCKED", "GIT_INTERNAL_PATH_ALLOWED")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_exact_approval_is_required(self):
        self.mutate(FILES[10], "APPROVAL_CONFIRMATION_MISMATCH", "APPROVAL_OPTIONAL")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_workspace_must_remain_in_plan_hash(self):
        self.mutate(FILES[6], 'append("workspace=")', 'append("workspace-ignored=")')
        with self.assertRaises(ValidationError): validate(self.root)

    def test_claim_promotion_is_rejected(self):
        self.mutate(FILES[1], "claimAllowed = false", "claimAllowed = true")
        with self.assertRaises(ValidationError): validate(self.root)

if __name__ == "__main__": unittest.main()
