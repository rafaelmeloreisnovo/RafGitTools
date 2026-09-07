import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_main_provider_enforcement_plan_v2",
    ROOT / "scripts" / "validate_main_provider_enforcement_plan_v2.py",
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class MainProviderEnforcementPlanV2Tests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(
            (ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v2.json").read_text(encoding="utf-8")
        )

    def test_canonical_plan_passes(self):
        self.assertEqual([], MODULE.validate(self.data))

    def test_global_required_workflows_are_unfiltered_for_pull_requests(self):
        for item in self.data["required_status_checks_global"]:
            scope = MODULE.workflow_pull_request_scope(ROOT / item["workflow_path"])
            self.assertTrue(scope["present"], item["context"])
            self.assertFalse(scope["path_filtered"], item["context"])
            self.assertTrue(scope["targets_main"], item["context"])

    def test_historical_path_filtered_checks_are_not_global_required(self):
        global_contexts = {
            item["context"] for item in self.data["required_status_checks_global"]
        }
        self.assertNotIn("Auditor Closure Gate V1 / auditor-closure", global_contexts)
        self.assertNotIn("Documentation / Validate Documentation", global_contexts)

        for item in self.data["specialized_conditional_checks"]:
            self.assertFalse(item["provider_required_globally"])
            self.assertEqual("PATH_FILTERED", item["pull_request_scope"])
            scope = MODULE.workflow_pull_request_scope(ROOT / item["workflow_path"])
            self.assertTrue(scope["path_filtered"], item["context"])

    def test_path_filtered_workflow_cannot_be_promoted_to_global(self):
        broken = copy.deepcopy(self.data)
        first = broken["required_status_checks_global"][0]
        first["workflow_path"] = ".github/workflows/docs.yml"
        first["job_id"] = "validate-docs"
        errors = MODULE.validate(broken)
        self.assertTrue(any("path-filtered workflow cannot be provider-global" in error for error in errors))

    def test_provider_authority_cannot_be_inferred(self):
        broken = copy.deepcopy(self.data)
        broken["provider_enforcement_active"] = True
        broken["application_boundary"]["administration_write_proven"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("provider_enforcement_active" in error for error in errors))
        self.assertTrue(any("administration_write_proven" in error for error in errors))

    def test_pr_validation_stays_ready_only_while_draft_conditional(self):
        ready = self.data["candidate_ready_checks"][0]
        self.assertEqual("PR Validation / Validate Pull Request", ready["context"])
        self.assertFalse(ready["provider_required_globally"])
        workflow = (ROOT / ready["workflow_path"]).read_text(encoding="utf-8")
        self.assertIn("pull_request.draft == false", workflow)

    def test_v1_remains_preserved_as_superseded_history(self):
        self.assertEqual(
            "contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260830.v1.json",
            self.data["supersedes"],
        )
        self.assertTrue((ROOT / self.data["supersedes"]).exists())


if __name__ == "__main__":
    unittest.main()
