from __future__ import annotations

import copy
import importlib.util
import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "tools" / "validate_ecosystem_runtime_map.py"
MAP_PATH = ROOT / "configs" / "ecosystem-runtime-map.phase1.json"
SPEC = importlib.util.spec_from_file_location("validate_ecosystem_runtime_map", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class EcosystemRuntimeMapTests(unittest.TestCase):
    def load_map(self) -> dict:
        return json.loads(MAP_PATH.read_text(encoding="utf-8"))

    def test_phase1_map_passes_canonical_invariants(self) -> None:
        result = MODULE.validate_map(self.load_map())

        self.assertEqual(result["status"], "PASS")
        self.assertEqual(result["repositories"], 7)
        self.assertEqual(result["capabilities"], 13)
        self.assertEqual(result["edges"], 7)
        self.assertFalse(result["claim_allowed"])
        self.assertEqual(
            result["capability_states"],
            {
                "pass": 1,
                "partial": 7,
                "design": 1,
                "token_vazio": 2,
                "blocked": 2,
                "quarantine": 0,
            },
        )

    def test_summary_tampering_is_rejected(self) -> None:
        data = self.load_map()
        data["summary"]["capabilities_total"] += 1

        with self.assertRaisesRegex(MODULE.ValidationError, "capabilities_total"):
            MODULE.validate_map(data)

    def test_unknown_repository_reference_is_rejected(self) -> None:
        data = copy.deepcopy(self.load_map())
        data["capabilities"][0]["repositories"].append("unknown/not-installed")

        with self.assertRaisesRegex(MODULE.ValidationError, "unknown nodes"):
            MODULE.validate_map(data)

    def test_policy_cannot_promote_unknown_to_success(self) -> None:
        data = self.load_map()
        data["policy"]["unknown_is_success"] = True

        with self.assertRaisesRegex(MODULE.ValidationError, "unknown_is_success"):
            MODULE.validate_map(data)

    def test_pass_capability_requires_evidence(self) -> None:
        data = self.load_map()
        passing = next(item for item in data["capabilities"] if item["state"] == "PASS")
        passing["evidence"] = []

        with self.assertRaisesRegex(MODULE.ValidationError, "PASS requires evidence"):
            MODULE.validate_map(data)


if __name__ == "__main__":
    unittest.main()
