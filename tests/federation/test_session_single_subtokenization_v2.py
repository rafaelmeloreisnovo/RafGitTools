import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).parents[2]
SCRIPT = ROOT / "scripts/federation/session_single_subtokenization.py"
CONFIG = ROOT / "configs/session-single-subtokenization-v2.json"
spec = importlib.util.spec_from_file_location("capsule", SCRIPT)
m = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(m)


class TestSessionSingleSubtokenizationV2(unittest.TestCase):
    def setUp(self):
        self.capsule = json.loads(CONFIG.read_text())

    def test_capsule_valid(self):
        self.assertEqual(m.validate_capsule(self.capsule), [])

    def test_twenty_two_atomic_tokens(self):
        self.assertEqual(len(self.capsule["tokens"]), 22)

    def test_contiguous_order(self):
        self.assertEqual(
            [item["sequence"] for item in self.capsule["tokens"]],
            list(range(1, 23)),
        )

    def test_new_domains_present(self):
        domains = {item["domain"] for item in self.capsule["tokens"]}
        self.assertTrue({
            "microscopic_collision",
            "microphysics_thresholds",
            "pauli_spin_degeneracy",
            "stress_energy_gravity",
            "magnetic_field_evolution",
            "photon_thrust",
        }.issubset(domains))

    def test_compression_does_not_promote_subparticles(self):
        token = next(item for item in self.capsule["tokens"] if item["token_id"] == "S18")
        self.assertTrue(token["negation"])
        self.assertEqual(token["claim_gate"], "CLOSED")

    def test_stress_energy_proxy_not_gr_solution(self):
        token = next(item for item in self.capsule["tokens"] if item["token_id"] == "S20")
        self.assertTrue(token["negation"])
        self.assertIn("not an Einstein-equation", token["statement"])

    def test_seal_is_deterministic(self):
        self.assertEqual(m.seal_capsule(self.capsule), m.seal_capsule(self.capsule))

    def test_semantic_mutation_changes_digest(self):
        altered = json.loads(json.dumps(self.capsule))
        before = m.seal_capsule(altered)["capsule_digest_sha256"]
        altered["tokens"][17]["statement"] += " altered"
        after = m.seal_capsule(altered)["capsule_digest_sha256"]
        self.assertNotEqual(before, after)

    def test_reordering_rejected(self):
        altered = json.loads(json.dumps(self.capsule))
        altered["tokens"][0], altered["tokens"][1] = altered["tokens"][1], altered["tokens"][0]
        self.assertIn("sequence must be contiguous", m.validate_capsule(altered))

    def test_global_claim_closed(self):
        self.assertFalse(self.capsule["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
