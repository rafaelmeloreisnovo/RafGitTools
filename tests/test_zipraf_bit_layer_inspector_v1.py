import copy
import hashlib
import importlib.util
import json
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

ROOT = Path(__file__).resolve().parents[1]
TOOL = ROOT / "tools" / "zipraf_bit_layer_inspector_v1.py"
CONFIG = ROOT / "configs" / "zipraf-bit-layer-inspector-v1.json"

SPEC = importlib.util.spec_from_file_location("zipraf_bit_layer_inspector_v1", TOOL)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(MODULE)


def vectors() -> dict:
    return {
        "contract": "ZIPRAF-BIT-LAYER-REFERENCE-VECTORS-V1",
        "parent_contract": "ZIPRAF-BIT-LAYER-PROGRESSIVE-RASTER-V1",
        "version": 1,
        "phase": "A_BITPLANES_ONLY",
        "claim_allowed": False,
        "governance_closure": "CLOSURE_L1",
        "geometry": {
            "M": "TOKEN_VAZIO_NOT_BOUND",
            "G(M)": "TOKEN_VAZIO_NOT_BOUND",
            "T-BL-010": "TOKEN_VAZIO",
        },
        "widths": [30, 60],
        "q_values": [1, 2, 4, 8],
        "samples": [
            {
                "id": "BLV-A5",
                "byte": 165,
                "planes_lsb_to_msb": [1,0,1,0,0,1,0,1],
                "q_reconstruction": {"1":128,"2":128,"4":160,"8":165},
            }
        ],
    }


class ZiprafBitLayerInspectorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.config = json.loads(CONFIG.read_text(encoding="utf-8"))

    def test_positive_read_only_inspection(self) -> None:
        result = MODULE.inspect(self.config, vectors(), source_sha256="abc", sample_id="BLV-A5")
        self.assertEqual("PASS_INSPECTION", result["status"])
        self.assertFalse(result["claim_allowed"])
        self.assertFalse(result["mutations_performed"])
        self.assertEqual(165, result["selected_sample"]["byte"])
        self.assertTrue(result["geometry"]["M"].startswith("TOKEN_VAZIO"))
        self.assertEqual([], result["failures"])

    def test_geometry_promotion_fails_closed(self) -> None:
        data = vectors()
        data["geometry"]["G(M)"] = {"state": "invented"}
        result = MODULE.inspect(self.config, data)
        self.assertEqual("HOLD_FAIL_CLOSED", result["status"])
        self.assertIn("GAP_PROMOTED_OR_MISSING:G(M)", result["failures"])

    def test_mutating_mode_is_rejected(self) -> None:
        config = copy.deepcopy(self.config)
        config["mode"] = "READ_WRITE"
        config["mutations_allowed"] = True
        result = MODULE.inspect(config, vectors())
        self.assertEqual("HOLD_FAIL_CLOSED", result["status"])
        self.assertIn("MODE_NOT_READ_ONLY", result["failures"])
        self.assertIn("MUTATION_BOUNDARY_BROKEN", result["failures"])

    def test_wrong_contract_is_rejected(self) -> None:
        data = vectors()
        data["contract"] = "OTHER"
        result = MODULE.inspect(self.config, data)
        self.assertIn("CONTRACT_MISMATCH", result["failures"])

    def test_plane_mutation_is_rejected(self) -> None:
        data = vectors()
        data["samples"][0]["planes_lsb_to_msb"][0] = 0
        result = MODULE.inspect(self.config, data)
        self.assertIn("PLANES_MISMATCH:BLV-A5", result["failures"])

    def test_q_mutation_is_rejected(self) -> None:
        data = vectors()
        data["samples"][0]["q_reconstruction"]["4"] = 161
        result = MODULE.inspect(self.config, data)
        self.assertIn("Q_RECONSTRUCTION_MISMATCH:BLV-A5:q4", result["failures"])

    def test_source_file_hash_is_reported_not_promoted(self) -> None:
        raw = json.dumps(vectors(), sort_keys=True).encode()
        digest = hashlib.sha256(raw).hexdigest()
        result = MODULE.inspect(self.config, vectors(), source_sha256=digest)
        self.assertEqual(digest, result["source_sha256"])
        self.assertFalse(result["claim_allowed"])


if __name__ == "__main__":
    unittest.main()
