#!/usr/bin/env python3
from __future__ import annotations
import copy, importlib.util, json, sys, unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "scripts/federation/validate_normative_reference_registry.py"
SPEC = importlib.util.spec_from_file_location("norm_registry_validator", MODULE)
assert SPEC and SPEC.loader
validator = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = validator
SPEC.loader.exec_module(validator)

class NormativeRegistryTests(unittest.TestCase):
    def setUp(self) -> None:
        self.registry = json.loads((ROOT / "configs/normative-reference-registry.v1.json").read_text(encoding="utf-8"))

    def test_registry_passes(self) -> None:
        self.assertEqual(validator.validate(self.registry), [])

    def test_conformance_claim_fails(self) -> None:
        broken = copy.deepcopy(self.registry)
        broken["standards"][0]["conformance_claim"] = True
        self.assertTrue(any("conformance_claim" in x for x in validator.validate(broken)))

    def test_unofficial_host_fails(self) -> None:
        broken = copy.deepcopy(self.registry)
        broken["standards"][0]["official_url"] = "https://example.com/iso"
        self.assertTrue(any("official authority host" in x for x in validator.validate(broken)))

    def test_supersession_is_explicit(self) -> None:
        broken = copy.deepcopy(self.registry)
        next(x for x in broken["standards"] if x["id"] == "IEEE-1012-2024")["supersedes"] = None
        self.assertTrue(any("1012-2016" in x for x in validator.validate(broken)))

    def test_iso_9001_future_is_not_promoted(self) -> None:
        broken = copy.deepcopy(self.registry)
        next(x for x in broken["standards"] if x["id"] == "ISO-9001-2015-AMD1-2024")["edition_state"] = "SUPERSEDED"
        self.assertTrue(any("current requirements" in x for x in validator.validate(broken)))

    def test_report_is_deterministic(self) -> None:
        self.assertEqual(validator.build_report(self.registry), validator.build_report(self.registry))

if __name__ == "__main__":
    unittest.main()
