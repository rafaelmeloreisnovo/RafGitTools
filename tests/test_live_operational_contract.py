import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "validate_live_operational_contract.py"
SPEC = importlib.util.spec_from_file_location("validate_live_operational_contract", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class LiveOperationalContractTests(unittest.TestCase):
    def load_contract(self):
        return json.loads(
            (ROOT / "contracts" / "live-operational-contract-v1.json").read_text(encoding="utf-8")
        )

    def load_schema(self):
        return json.loads(
            (ROOT / "contracts" / "live-operational-state-v1.schema.json").read_text(encoding="utf-8")
        )

    def test_canonical_contract_passes(self):
        self.assertEqual(MODULE.validate(self.load_contract(), self.load_schema()), [])

    def test_claim_default_fails_closed(self):
        contract = self.load_contract()
        contract["claim_gate"]["claim_allowed_default"] = True
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(
            any("claim_allowed_default must remain false" in error for error in errors)
        )

    def test_missing_pre_execution_gate_field_blocks_validation(self):
        contract = self.load_contract()
        contract["pre_execution_gate"]["required"].remove("EVIDENCE_RULE")
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(
            any("pre-execution gate field set mismatch" in error for error in errors)
        )

    def test_route_must_block_when_precondition_is_missing(self):
        contract = self.load_contract()
        contract["pre_execution_gate"]["on_missing"]["ROUTE_STATE"] = "PENDING"
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(any("missing precondition must block route" in error for error in errors))

    def test_token_vazio_must_remain_distinct(self):
        contract = self.load_contract()
        contract["semantic_sentinels"]["TOKEN_VAZIO"]["distinct_from"].remove("NOT_RUN")
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(
            any("TOKEN_VAZIO does not distinguish NOT_RUN" in error for error in errors)
        )

    def test_schema_must_require_claim_allowed(self):
        schema = self.load_schema()
        schema["properties"]["state"]["required"].remove("CLAIM_ALLOWED")
        errors = MODULE.validate(self.load_contract(), schema)
        self.assertTrue(any("state required fields mismatch" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
