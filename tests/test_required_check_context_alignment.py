import json
import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260927.v4.json"
START = ROOT / ".github" / "workflows" / "START.yml"


class RequiredCheckContextAlignmentTests(unittest.TestCase):
    def test_required_contexts_are_exact_emitted_job_names(self):
        plan = json.loads(PLAN.read_text(encoding="utf-8"))
        workflow = START.read_text(encoding="utf-8")

        # Job display names are indented by exactly four spaces in START.yml.
        # Step names are deeper and therefore excluded from this extraction.
        emitted_job_names = set(
            re.findall(r"^    name:\s*['\"]([^'\"]+)['\"]\s*$", workflow, re.MULTILINE)
        )
        required = {
            str(item["context"])
            for item in plan["required_status_checks_global"]
        }

        self.assertEqual(len(required), 4)
        self.assertTrue(required.issubset(emitted_job_names), (
            "branch-protection contexts must equal check-run job names; "
            f"missing={sorted(required - emitted_job_names)}"
        ))

    def test_required_contexts_do_not_prefix_workflow_name(self):
        plan = json.loads(PLAN.read_text(encoding="utf-8"))
        prefix = "START · RAFAELIA Orchestrated Pipeline / "
        contexts = [
            str(item["context"])
            for item in plan["required_status_checks_global"]
        ]
        self.assertTrue(all(not value.startswith(prefix) for value in contexts))

    def test_required_job_ids_remain_the_stage1_gate_set(self):
        plan = json.loads(PLAN.read_text(encoding="utf-8"))
        job_ids = {
            str(item["job_id"])
            for item in plan["required_status_checks_global"]
        }
        self.assertEqual(job_ids, {"plan", "topology", "coherence", "receipt"})


if __name__ == "__main__":
    unittest.main()
