from __future__ import annotations

import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CI_WORKFLOW = ROOT / ".github" / "workflows" / "ci.yml"


class WorkflowPullRequestBindingTest(unittest.TestCase):
    def setUp(self) -> None:
        self.text = CI_WORKFLOW.read_text(encoding="utf-8")

    def test_no_hardcoded_issue_or_pr_comment_target(self) -> None:
        matches = re.findall(r"issues/\d+/comments", self.text)
        self.assertEqual(
            matches,
            [],
            f"CI workflow must not publish to hardcoded issue/PR targets: {matches}",
        )

    def test_comment_target_is_bound_to_current_pull_request_event(self) -> None:
        self.assertIn("github.event_name == 'pull_request'", self.text)
        self.assertIn("PR_NUMBER: ${{ github.event.pull_request.number }}", self.text)
        self.assertIn('issues/${PR_NUMBER}/comments', self.text)

    def test_pr_number_is_validated_before_api_write(self) -> None:
        self.assertIn('test -n "$PR_NUMBER"', self.text)
        self.assertIn('invalid PR_NUMBER=$PR_NUMBER', self.text)


if __name__ == "__main__":
    unittest.main()
