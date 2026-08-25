from __future__ import annotations

import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_friction_baseline_v1.py"
SPEC = importlib.util.spec_from_file_location("friction_baseline_v1", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FrictionBaselineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.baseline = MODULE.load(MODULE.BASELINE)
        cls.license_obj = MODULE.load(MODULE.LICENSE)
        cls.uses = MODULE.load(MODULE.USES)
        cls.relations = MODULE.load(MODULE.RELATIONS)
        cls.provider = MODULE.load(MODULE.PROVIDER)

    def errors(self, baseline=None, license_obj=None, uses=None, relations=None, provider=None):
        return MODULE.validate(
            baseline or self.baseline,
            license_obj or self.license_obj,
            uses or self.uses,
            relations or self.relations,
            provider or self.provider,
        )

    def test_canonical_baseline_passes(self) -> None:
        self.assertEqual(self.errors(), [])

    def test_unknown_manual_rework_forbids_numeric_total(self) -> None:
        baseline = copy.deepcopy(self.baseline)
        baseline["aggregate"]["numeric_total"] = 9
        self.assertTrue(any("numeric total must remain TOKEN_VAZIO_SCORE" in e for e in self.errors(baseline=baseline)))

    def test_provider_attempt_count_must_reconcile(self) -> None:
        baseline = copy.deepcopy(self.baseline)
        baseline["components"]["provider_pre_step_failure_count"]["value"] = 1
        self.assertTrue(any("provider_pre_step_failure_count does not reconcile" in e for e in self.errors(baseline=baseline)))

    def test_missing_rollback_reopens_component(self) -> None:
        uses = copy.deepcopy(self.uses)
        uses["uses"][0]["rollback_or_irreversible_boundary"] = ""
        self.assertTrue(any("missing_rollback_count does not reconcile" in e for e in self.errors(uses=uses)))

    def test_zero_vector_never_implies_claim(self) -> None:
        baseline = copy.deepcopy(self.baseline)
        baseline["aggregate"]["zero_vector_would_imply_claim"] = True
        self.assertTrue(any("zero friction vector must not imply claim" in e for e in self.errors(baseline=baseline)))


if __name__ == "__main__":
    unittest.main()
