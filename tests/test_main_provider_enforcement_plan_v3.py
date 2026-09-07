import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "validate_main_provider_enforcement_plan_v3",
    ROOT / "scripts" / "validate_main_provider_enforcement_plan_v3.py",
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class MainProviderEnforcementPlanV3Tests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(
            (ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v3.json").read_text(encoding="utf-8")
        )
        self.start = ROOT / ".github" / "workflows" / "START.yml"

    def test_canonical_plan_passes(self):
        self.assertEqual([], MODULE.validate(self.data))

    def test_single_active_yaml_root_is_start(self):
        self.assertEqual([self.start], MODULE._active_yaml_paths())

    def test_start_pull_request_scope_is_unfiltered_and_targets_main(self):
        scope = MODULE._pull_request_properties(self.start.read_text(encoding="utf-8"))
        self.assertTrue(scope["present"])
        self.assertFalse(scope["path_filtered"])
        self.assertTrue(scope["targets_main"])

    def test_global_required_jobs_are_only_start_core(self):
        expected = {"plan", "topology", "coherence", "receipt"}
        actual = {item["job_id"] for item in self.data["required_status_checks_global"]}
        self.assertEqual(expected, actual)
        text = self.start.read_text(encoding="utf-8")
        for item in self.data["required_status_checks_global"]:
            self.assertEqual(".github/workflows/START.yml", item["workflow_path"])
            self.assertTrue(MODULE._job_declared(text, item["job_id"]))

    def test_receipt_is_unconditional_closure_surface(self):
        text = self.start.read_text(encoding="utf-8")
        receipt = MODULE._block(text, "receipt:", 2)
        self.assertIn("if: always()", receipt)

    def test_conditional_lanes_cannot_be_provider_global(self):
        for item in self.data["specialized_conditional_lanes"]:
            self.assertFalse(item["provider_required_globally"])
            self.assertEqual("TOKEN_VAZIO_NOT_SELECTED", item["skip_state"])

        broken = copy.deepcopy(self.data)
        broken["specialized_conditional_lanes"][0]["provider_required_globally"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("conditional lane cannot be provider-global" in error for error in errors))

    def test_archived_legacy_workflow_cannot_become_global_required(self):
        broken = copy.deepcopy(self.data)
        broken["required_status_checks_global"][0]["workflow_path"] = ".github/workflows-legacy-20260907/ci.yml"
        errors = MODULE.validate(broken)
        self.assertTrue(any("must bind to START.yml" in error for error in errors))

    def test_provider_authority_cannot_be_fabricated(self):
        broken = copy.deepcopy(self.data)
        broken["provider_enforcement_active"] = True
        broken["application_boundary"]["administration_write_proven"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("provider_enforcement_active" in error for error in errors))
        self.assertTrue(any("administration_write_proven" in error for error in errors))

    def test_v2_and_v1_remain_preserved_history(self):
        self.assertEqual(
            "contracts/MAIN_PROVIDER_ENFORCEMENT_PLAN_20260907.v2.json",
            self.data["supersedes"],
        )
        self.assertTrue((ROOT / self.data["supersedes"]).is_file())
        self.assertTrue((ROOT / "contracts" / "MAIN_PROVIDER_ENFORCEMENT_PLAN_20260830.v1.json").is_file())


if __name__ == "__main__":
    unittest.main()
