import importlib.util
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "federation" / "validate_federated_runtime_v1.py"
SPEC = importlib.util.spec_from_file_location("validate_federated_runtime_v1", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class FederatedRuntimeContractTest(unittest.TestCase):
    def load_contract(self):
        return json.loads((ROOT / "contracts" / "rafaelia-federated-runtime-v1.json").read_text())

    def test_valid_contract(self):
        report = MODULE.validate_contract(self.load_contract())
        self.assertEqual("PASS", report["status"])
        self.assertFalse(report["claim_allowed"])
        self.assertEqual(4, report["participants"])

    def test_private_path_exposure_is_rejected(self):
        data = self.load_contract()
        data["android_ipc"]["private_paths_exposed"] = True
        with self.assertRaises(MODULE.ContractError):
            MODULE.validate_contract(data)

    def test_false_claim_promotion_is_rejected(self):
        data = self.load_contract()
        data["claim_allowed"] = True
        with self.assertRaises(MODULE.ContractError):
            MODULE.validate_contract(data)

    def test_vm_gate_is_required(self):
        data = self.load_contract()
        data["promotion_gates"]["vm_start_requires_vm_required_true"] = False
        with self.assertRaises(MODULE.ContractError):
            MODULE.validate_contract(data)

    def test_receipt_field_removal_is_rejected(self):
        data = self.load_contract()
        data["required_receipt_fields"].remove("output_sha256")
        with self.assertRaises(MODULE.ContractError):
            MODULE.validate_contract(data)


if __name__ == "__main__":
    unittest.main()
