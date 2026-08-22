from __future__ import annotations

import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_uncertainty_urgency_friction_v3.py"
SPEC = importlib.util.spec_from_file_location("friction_v3", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FrictionV3Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.v1 = MODULE.load_json(MODULE.DEFAULT_V1)
        cls.v2 = MODULE.load_json(MODULE.DEFAULT_V2)
        cls.v3 = MODULE.load_json(MODULE.DEFAULT_V3)
        cls.license_obj = MODULE.load_json(MODULE.DEFAULT_LICENSE)
        cls.uses = MODULE.load_json(MODULE.DEFAULT_USES)
        cls.relations = MODULE.load_json(MODULE.DEFAULT_RELATIONS)
        cls.provider = MODULE.load_json(MODULE.DEFAULT_PROVIDER)

    def test_canonical_bundle_passes(self) -> None:
        errors = MODULE.validate_all(
            self.v1, self.v2, self.v3, self.license_obj,
            self.uses, self.relations, self.provider,
        )
        self.assertEqual(errors, [])

    def test_analogy_cannot_gain_evidence_weight(self) -> None:
        relations = copy.deepcopy(self.relations)
        target = next(r for r in relations["relations"] if r["relation_type"] == "ANALOGY_OF")
        target["evidence_effect"] = "SUPPORT_LIMITED"
        errors = MODULE.validate_relation_graph(relations)
        self.assertTrue(any("analogy must have evidence_effect NONE" in e for e in errors))

    def test_unknown_license_permissions_fail_closed(self) -> None:
        matrix = copy.deepcopy(self.license_obj)
        target = next(u for u in matrix["units"] if u["compatibility_state"] == "TOKEN_VAZIO_LICENSE")
        target["training_allowed"] = True
        errors = MODULE.validate_license_matrix(matrix)
        self.assertTrue(any("unknown rights must fail closed" in e for e in errors))

    def test_provider_zero_steps_is_not_test_failure(self) -> None:
        provider = copy.deepcopy(self.provider)
        provider["target_workflow"]["classification"] = "TEST_EXECUTED_FAIL"
        errors = MODULE.validate_provider_evidence(provider)
        self.assertTrue(any("cannot be classified as test failure" in e for e in errors))

    def test_use_claim_promotion_fails(self) -> None:
        uses = copy.deepcopy(self.uses)
        uses["uses"][0]["claim_allowed"] = True
        errors = MODULE.validate_use_registry(uses)
        self.assertTrue(any("claim_allowed must remain false" in e for e in errors))

    def test_relation_without_source_fails(self) -> None:
        relations = copy.deepcopy(self.relations)
        relations["relations"][0]["source_ref"] = ""
        errors = MODULE.validate_relation_graph(relations)
        self.assertTrue(any("source_ref required" in e for e in errors))

    def test_zero_friction_never_implies_claim(self) -> None:
        v3 = copy.deepcopy(self.v3)
        v3["friction_measurement"]["zero_friction_does_not_imply_claim"] = False
        errors = MODULE.validate_v3(v3)
        self.assertTrue(any("zero friction must not imply claim" in e for e in errors))


if __name__ == "__main__":
    unittest.main()
