from __future__ import annotations

import shutil
import tempfile
import unittest
from pathlib import Path

from scripts.validate_rafgitfs_governed_sync import FILES, ValidationError, validate

class GovernedSyncGateTest(unittest.TestCase):
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

    def test_claim_promotion_is_rejected(self):
        self.mutate(FILES[0], "claimAllowed: Boolean = false", "claimAllowed: Boolean = true")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_approval_gate_removal_is_rejected(self):
        self.mutate(FILES[5], "APPROVAL_REQUIRED", "APPROVAL_OPTIONAL")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_remote_writer_binding_is_required(self):
        module = self.root / FILES[6]
        text = module.read_text(encoding="utf-8")
        marker = "RafGitFsGithubBranchWriter" if "RafGitFsGithubBranchWriter" in text else "RafGitFsBlockedRemoteWriteCapability"
        self.assertIn(marker, text)
        module.write_text(text.replace(marker, "UnsafeRemoteWriter"), encoding="utf-8")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_receipt_append_only_is_required(self):
        self.mutate(FILES[7], "OnConflictStrategy.ABORT", "OnConflictStrategy.REPLACE")
        with self.assertRaises(ValidationError): validate(self.root)

    def test_secret_redaction_is_required(self):
        self.mutate(FILES[1], "REDACTED_GITHUB_TOKEN", "VISIBLE_GITHUB_TOKEN")
        with self.assertRaises(ValidationError): validate(self.root)

if __name__ == "__main__": unittest.main()
