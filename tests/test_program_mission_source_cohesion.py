import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "check_program_mission_source_cohesion",
    ROOT / "scripts" / "check_program_mission_source_cohesion.py",
)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


class ProgramMissionSourceCohesionTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads(
            (ROOT / "configs" / "program-mission-source-cohesion.v1.json").read_text(encoding="utf-8")
        )

    def test_canonical_contract_passes(self):
        self.assertEqual([], MODULE.validate(self.data))

    def test_dataset_cannot_become_mission_authority(self):
        broken = json.loads(json.dumps(self.data))
        broken["planes"]["DATASET"]["may_define_or_rewrite_mission"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("DATASET" in error and "mission" in error.lower() for error in errors))

    def test_model_cannot_self_authorize_execution(self):
        broken = json.loads(json.dumps(self.data))
        broken["planes"]["MODEL_PROPOSAL"]["may_self_authorize_execution"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("MODEL_PROPOSAL" in error and "self_authorize" in error for error in errors))

    def test_learn_is_append_only_and_not_execution_authority(self):
        broken = json.loads(json.dumps(self.data))
        broken["planes"]["LEARN"]["append_only"] = False
        broken["planes"]["LEARN"]["may_grant_execution"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("append-only" in error for error in errors))
        self.assertTrue(any("LEARN" in error and "may_grant_execution" in error for error in errors))

    def test_training_and_weight_updates_are_not_authorized(self):
        broken = json.loads(json.dumps(self.data))
        broken["training_boundary"]["this_contract_authorizes_training"] = True
        broken["training_boundary"]["this_contract_authorizes_weight_updates"] = True
        errors = MODULE.validate(broken)
        self.assertTrue(any("authorize training" in error for error in errors))
        self.assertTrue(any("authorize weight updates" in error for error in errors))

    def test_unknown_mission_binding_blocks_mutation(self):
        binding = self.data["mission_binding"]
        self.assertEqual("TOKEN_VAZIO_MISSION_BINDING", binding["unknown_state"])
        self.assertEqual("BLOCK_MUTATION_ROUTE_ONLY", binding["unknown_behavior"])

        broken = json.loads(json.dumps(self.data))
        broken["mission_binding"]["unknown_behavior"] = "ALLOW_MUTATION"
        errors = MODULE.validate(broken)
        self.assertTrue(any("fail closed" in error for error in errors))

    def test_execution_gate_requires_authority_and_rollback(self):
        required = set(self.data["planes"]["EXECUTION_GATE"]["requires"])
        self.assertIn("EXPLICIT_MISSION_BINDING", required)
        self.assertIn("AUTHORITY", required)
        self.assertIn("ROLLBACK_WHEN_MUTATING", required)
        self.assertIn("GOVERNANCE_DATA_PRIVACY_SECURITY_GATES", required)

        broken = json.loads(json.dumps(self.data))
        broken["planes"]["EXECUTION_GATE"]["requires"].remove("AUTHORITY")
        self.assertTrue(any("required inputs mismatch" in error for error in MODULE.validate(broken)))

    def test_forbidden_promotions_include_learn_to_weight_update(self):
        self.assertIn("LEARN -> WEIGHT_UPDATE", self.data["forbidden_promotions"])
        self.assertIn("TOKEN_VAZIO -> PASS", self.data["forbidden_promotions"])


if __name__ == "__main__":
    unittest.main()
