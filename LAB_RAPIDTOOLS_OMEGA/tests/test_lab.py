import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

LAB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(LAB / "tools"))
import receipts
import atlas_graph


class LabGovernanceTests(unittest.TestCase):
    def test_manifest_has_no_claim(self):
        m = json.loads((LAB / "lab_manifest.json").read_text())
        self.assertFalse(m["properties"]["claim_allowed"])
        self.assertFalse(m["properties"]["release_allowed"])
        self.assertTrue(m["properties"]["freestanding_core"])
        self.assertFalse(m["properties"]["core_malloc"])
        self.assertFalse(m["properties"]["no_android_manifest_changes"] is False)
        self.assertEqual(len(m["suites"]), 9)

    def test_historical_negative_and_missing_abi(self):
        rows = json.loads((LAB / "cases/benchmark_history.json").read_text())["records"]
        self.assertLess(rows[1]["metrics"][0]["value"], rows[1]["metrics"][1]["value"])
        self.assertGreater(rows[2]["metrics"][0]["value"], rows[2]["metrics"][1]["value"])
        self.assertGreater(rows[2]["metrics"][0]["value"], rows[2]["metrics"][2]["value"])
        self.assertEqual(rows[-1]["state"], "TOKEN_VAZIO_DEVICE_ARM64")

    def test_atlas_keeps_provenance(self):
        g = atlas_graph.load()
        self.assertEqual(len(g["nodes"]), 15)
        self.assertIsNotNone(atlas_graph.route(g, "ARM32", "TLB"))
        self.assertIsNone(atlas_graph.route(g, "Q16", "SHADER"))
        uncertain = atlas_graph.route(g, "Q16", "SHADER", True)
        self.assertEqual(uncertain[0]["state"], "HYPOTHESIS")

    def test_receipt_linked_and_tamper_detected(self):
        with tempfile.TemporaryDirectory() as d:
            a = receipts.append(d, {"status": "HOST_SCOPED", "claim_allowed": False})
            receipts.append(d, {"status": "LOCAL_RETEST", "claim_allowed": False})
            count, _ = receipts.verify(d)
            self.assertEqual(count, 2)
            record = json.loads(a.read_text())
            record["payload"]["claim_allowed"] = True
            a.write_text(json.dumps(record))
            with self.assertRaises(AssertionError):
                receipts.verify(d)

    def test_shader_is_only_source(self):
        shader = (LAB / "shaders/raf_geometry.frag").read_text()
        self.assertIn("#version 450", shader)
        self.assertIn("void main()", shader)
        policy = json.loads((LAB / "contracts/claim_policy.json").read_text())
        self.assertFalse(policy["policy"]["global_claim_allowed"])
        self.assertIn("SHADER_GLSL != GPU_RUN", policy["non_equivalences"])


if __name__ == "__main__":
    unittest.main()
