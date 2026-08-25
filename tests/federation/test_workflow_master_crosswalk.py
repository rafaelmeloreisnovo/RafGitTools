#!/usr/bin/env python3
from __future__ import annotations
import copy, importlib.util, json, sys, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "scripts/federation/validate_workflow_master_crosswalk.py"
SPEC = importlib.util.spec_from_file_location("crosswalk_validator", MODULE)
assert SPEC and SPEC.loader
validator = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = validator
SPEC.loader.exec_module(validator)

class CrosswalkTests(unittest.TestCase):
    def setUp(self) -> None:
        self.semantic = json.loads((ROOT / "workflow-master-index.json").read_text(encoding="utf-8"))
        self.control = json.loads((ROOT / "configs/workflow-master-index.json").read_text(encoding="utf-8"))
        self.crosswalk = json.loads((ROOT / "configs/workflow-master-index.crosswalk.v1.json").read_text(encoding="utf-8"))

    def test_current_indices_and_crosswalk_pass(self) -> None:
        self.assertEqual(validator.validate(self.semantic, self.control, self.crosswalk), [])

    def test_all_thirty_layers_are_covered_once(self) -> None:
        covered = [layer for route in self.crosswalk["cycle_routes"] for layer in route["semantic_layers"]]
        self.assertEqual(len(covered), 30)
        self.assertEqual(set(covered), validator.EXPECTED_LAYERS)

    def test_unknown_control_path_fails(self) -> None:
        broken = copy.deepcopy(self.crosswalk)
        broken["cycle_routes"][0]["control_paths"][0] = "policies.missing"
        self.assertTrue(any("unknown control path" in x for x in validator.validate(self.semantic, self.control, broken)))

    def test_silent_precedence_fails(self) -> None:
        broken = copy.deepcopy(self.crosswalk)
        broken["conflict_policy"]["silent_precedence"] = True
        self.assertTrue(any("silent precedence" in x for x in validator.validate(self.semantic, self.control, broken)))

    def test_promotion_effect_fails(self) -> None:
        broken = copy.deepcopy(self.crosswalk)
        broken["directive_policy"]["allowed_effects"].append("PROMOTE")
        self.assertTrue(any("PROMOTE cannot" in x for x in validator.validate(self.semantic, self.control, broken)))

    def test_cross_source_requires_both_profiles(self) -> None:
        broken = copy.deepcopy(self.crosswalk)
        next(x for x in broken["selection_rules"] if x["intent"] == "CROSS_SOURCE")["required_profiles"] = ["semantic_v1"]
        self.assertTrue(any("CROSS_SOURCE must require both" in x for x in validator.validate(self.semantic, self.control, broken)))

    def test_report_is_deterministic(self) -> None:
        self.assertEqual(
            validator.build_report(self.semantic, self.control, self.crosswalk),
            validator.build_report(self.semantic, self.control, self.crosswalk),
        )

if __name__ == "__main__":
    unittest.main()
