import copy
import importlib.util
import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "validate_authorial_omega_hypervisor_executor.py"
CONFIG = ROOT / "configs" / "authorial-omega-hypervisor-executor.v1.json"
spec = importlib.util.spec_from_file_location("omega_exec", SCRIPT)
M = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(M)

class OmegaHypervisorExecutorV1Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(CONFIG.read_text(encoding="utf-8"))

    def test_current_config(self):
        self.assertTrue(M.validate(copy.deepcopy(self.data)))

    def test_reject_mapa_pin_drift(self):
        bad = copy.deepcopy(self.data)
        bad["federated_authority"]["commit"] = "0" * 40
        with self.assertRaises(M.ValidationError):
            M.validate(bad)

    def test_reject_inherited_code_import(self):
        bad = copy.deepcopy(self.data)
        bad["inherited_code_imported"] = True
        with self.assertRaises(M.ValidationError):
            M.validate(bad)

    def test_reject_payload_blob_drift(self):
        bad = copy.deepcopy(self.data)
        bad["authorial_payloads"][0]["git_blob_sha1"] = "f" * 40
        with self.assertRaises(M.ValidationError):
            M.validate(bad)

    def test_reject_new_glue_authorship_promotion(self):
        bad = copy.deepcopy(self.data)
        bad["authorship_claim"] = "AUTHORIAL_PROVEN"
        with self.assertRaises(M.ValidationError):
            M.validate(bad)

if __name__ == "__main__":
    unittest.main()
