import copy
import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "validate_custody_taxonomy.py"
SPEC = importlib.util.spec_from_file_location("validate_custody_taxonomy", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class CustodyTaxonomyTests(unittest.TestCase):
    def load_contract(self):
        return json.loads(
            (ROOT / "contracts" / "custody-taxonomy-v1.json").read_text(encoding="utf-8")
        )

    def load_schema(self):
        return json.loads(
            (ROOT / "contracts" / "custody-event-v1.schema.json").read_text(encoding="utf-8")
        )

    def test_canonical_contract_passes(self):
        self.assertEqual(MODULE.validate(self.load_contract(), self.load_schema()), [])

    def test_duplicate_custody_class_fails_closed(self):
        contract = self.load_contract()
        contract["custody_classes"].append(copy.deepcopy(contract["custody_classes"][0]))
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(any("duplicate custody class id" in error for error in errors))

    def test_assistant_cannot_become_authorization_authority(self):
        contract = self.load_contract()
        contract["actors"]["ASSISTANT_TOOL_OPERATOR"]["may_authorize"] = True
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(
            any(
                "assistant must never become human authorization authority" in error
                for error in errors
            )
        )

    def test_claim_default_cannot_auto_promote(self):
        contract = self.load_contract()
        contract["claim_gate"]["claim_allowed_default"] = True
        errors = MODULE.validate(contract, self.load_schema())
        self.assertTrue(
            any("claim_allowed_default must remain false" in error for error in errors)
        )

    def test_event_schema_must_match_taxonomy_classes(self):
        schema = self.load_schema()
        schema["properties"]["custodyClass"]["enum"] = ["C01_SOURCE_IDENTITY"]
        errors = MODULE.validate(self.load_contract(), schema)
        self.assertTrue(
            any("event schema custodyClass enum differs from taxonomy" in error for error in errors)
        )


if __name__ == "__main__":
    unittest.main()
