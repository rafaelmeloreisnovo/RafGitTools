from __future__ import annotations

import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKFLOW = ROOT / ".github" / "workflows" / "START.yml"
CONFIG = ROOT / "configs" / "provider-actions-preflight.once.json"


class MasterCoverageProviderRoutingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.workflow = WORKFLOW.read_text(encoding="utf-8")
        cls.config = json.loads(CONFIG.read_text(encoding="utf-8"))

    def test_one_shot_targets_exact_private_coverage_commit(self) -> None:
        self.assertEqual(
            self.config["target_repository"],
            "rafaelmeloreisnovo/Rafaelia_Private",
        )
        self.assertRegex(self.config["target_commit"], r"^[0-9a-f]{40}$")
        self.assertEqual(self.config["task"], "master_coverage_import")
        self.assertFalse(self.config["write_allowed"])
        self.assertFalse(self.config["claim_allowed"])
        self.assertTrue(self.config["exact_target_sha_required"])

    def test_workflow_keeps_closed_task_to_repository_pairs(self) -> None:
        self.assertIn(
            'river7_offline requires rafaelmeloreisnovo/ChipQuantum',
            self.workflow,
        )
        self.assertIn(
            'master_coverage_import requires rafaelmeloreisnovo/Rafaelia_Private',
            self.workflow,
        )
        self.assertIn('case "$target_task" in', self.workflow)
        self.assertIn('case "$TARGET_TASK" in', self.workflow)

    def test_master_import_runs_against_checked_out_target_only(self) -> None:
        self.assertIn("python3 scripts/import_master_coverage.py", self.workflow)
        self.assertIn("--source-root target", self.workflow)
        self.assertIn('--source-repo "$TARGET_REPOSITORY"', self.workflow)
        self.assertIn('--source-sha "$TARGET_SHA"', self.workflow)
        self.assertIn(
            "artifacts/MASTER_COVERAGE_IMPORT_RECEIPT.json",
            self.workflow,
        )

    def test_private_checkout_remains_read_only_and_exact(self) -> None:
        self.assertIn("token: ${{ secrets.PAT_ACTIONS }}", self.workflow)
        self.assertIn("persist-credentials: false", self.workflow)
        self.assertIn('observed="$(git -C target rev-parse HEAD)"', self.workflow)
        self.assertIn('[[ "$observed" == "$TARGET_SHA" ]]', self.workflow)

    def test_no_generic_repository_execution_pair_is_introduced(self) -> None:
        allowed_tasks = set(
            re.findall(
                r"^\s{16}(river7_offline|master_coverage_import)\)$",
                self.workflow,
                flags=re.MULTILINE,
            )
        )
        self.assertEqual(
            allowed_tasks,
            {"river7_offline", "master_coverage_import"},
        )
        provider_lane = self.workflow.split("  provider_actions:", 1)[1].split(
            "  provider_environments:", 1
        )[0]
        self.assertNotIn("eval ", provider_lane)
        self.assertNotIn("TARGET_TASK_CMD", provider_lane)

    def test_single_root_workflow_file_still_exists(self) -> None:
        workflows = sorted(
            p.name for p in (ROOT / ".github" / "workflows").glob("*.y*ml")
        )
        self.assertEqual(workflows, ["START.yml"])


if __name__ == "__main__":
    unittest.main()
