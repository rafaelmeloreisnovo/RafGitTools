from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
VALIDATOR_PATH = ROOT / "scripts/validate_context_reconstruction_registry.py"


def _load_validator():
    spec = importlib.util.spec_from_file_location("context_reconstruction_validator", VALIDATOR_PATH)
    if spec is None or spec.loader is None:
        raise RuntimeError("unable to load context reconstruction validator")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ContextReconstructionRegistryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.validator = _load_validator()

    def test_registry_is_structurally_valid(self) -> None:
        self.assertEqual(self.validator.validate_all(), [])

    def test_seed_is_reference_first_and_claim_gated(self) -> None:
        seed = self.validator.load_json(self.validator.SEED)
        self.assertIs(seed["claim_allowed"], False)
        self.assertGreaterEqual(len(seed["source_refs"]), 1)
        self.assertLessEqual(len(seed["source_refs"]), 3)
        self.assertFalse(
            self.validator.walk_keys(seed) & self.validator.FORBIDDEN_PAYLOAD_KEYS
        )

    def test_next_best_gate_reuses_existing_priority_authorities(self) -> None:
        registry = self.validator.load_json(self.validator.REGISTRY)
        routes = {route["route_id"]: route for route in registry["routes"]}
        self.assertIn("NEXT_BEST_GATE", routes)
        route = routes["NEXT_BEST_GATE"]
        self.assertEqual(
            route["source_min"],
            [
                "configs/agent-entry-kernel.v1.json",
                "docs/UNCERTAINTY_URGENCY_FRICTION_ETHICS_LICENSE_BY_DESIGN_V3.md",
            ],
        )
        self.assertEqual(
            route["claim_boundary"],
            "priority-selection!=gap-closure!=claim-promotion",
        )
        self.assertTrue(route["next_on_missing"].startswith("TOKEN_VAZIO"))


if __name__ == "__main__":
    unittest.main()
