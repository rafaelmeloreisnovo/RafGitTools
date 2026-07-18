import importlib.util
import json
import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).parents[2]
SPEC = importlib.util.spec_from_file_location(
    "capsule", ROOT / "scripts/federation/session_single_subtokenization.py"
)
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
assert SPEC.loader
SPEC.loader.exec_module(module)
CAPSULE = json.loads(
    (ROOT / "configs/session-single-subtokenization-v1.json").read_text()
)


class SessionSingleSubtokenizationTests(unittest.TestCase):
    def test_valid(self):
        self.assertEqual(module.validate_capsule(CAPSULE), [])

    def test_single_capsule_with_atomic_subtokens(self):
        self.assertEqual(
            CAPSULE["interaction_model"],
            "ONE_CAPSULE_ORDERED_ATOMIC_SUBTOKENS"
        )
        self.assertGreater(len(CAPSULE["tokens"]), 1)

    def test_sequence(self):
        self.assertEqual(
            [token["sequence"] for token in CAPSULE["tokens"]],
            list(range(1, len(CAPSULE["tokens"]) + 1))
        )

    def test_unique_ids(self):
        token_ids = [token["token_id"] for token in CAPSULE["tokens"]]
        self.assertEqual(len(token_ids), len(set(token_ids)))

    def test_units_and_numbers_preserved(self):
        self.assertEqual(module.validate_capsule(CAPSULE), [])

    def test_excluded_topic_not_materialized(self):
        self.assertTrue(
            all(token["domain"] != "digital_identity" for token in CAPSULE["tokens"])
        )

    def test_claim_gates_closed(self):
        self.assertFalse(CAPSULE["claim_allowed"])
        self.assertTrue(
            all(token["claim_gate"] != "OPEN" for token in CAPSULE["tokens"])
        )

    def test_private_payload_not_copied(self):
        self.assertFalse(CAPSULE["private_payload_copied"])

    def test_physics_route(self):
        self.assertEqual(
            CAPSULE["routes"]["physics"],
            "instituto-Rafael/relativity-living-light"
        )

    def test_deterministic_seal(self):
        self.assertEqual(
            module.seal_capsule(CAPSULE), module.seal_capsule(CAPSULE)
        )

    def test_semantic_mutation_changes_digest(self):
        first = module.seal_capsule(CAPSULE)
        changed = json.loads(json.dumps(CAPSULE))
        changed["tokens"][0]["statement"] += " changed"
        second = module.seal_capsule(changed)
        self.assertNotEqual(
            first["token_digest_sha256"], second["token_digest_sha256"]
        )

    def test_reorder_is_rejected(self):
        changed = json.loads(json.dumps(CAPSULE))
        changed["tokens"][0], changed["tokens"][1] = (
            changed["tokens"][1], changed["tokens"][0]
        )
        self.assertTrue(module.validate_capsule(changed))

    def test_no_automatic_cross_repository_mutation(self):
        self.assertFalse(CAPSULE["automatic_cross_repo_write"])
        self.assertFalse(CAPSULE["automatic_merge"])

    def test_seal_remains_claim_bounded(self):
        self.assertFalse(module.seal_capsule(CAPSULE)["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
