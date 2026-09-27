import copy
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE_PATH = ROOT / "scripts" / "validate_custody_taxonomy.py"
SPEC = importlib.util.spec_from_file_location("validate_custody_taxonomy", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)

def load_contract():
    return json.loads((ROOT / "contracts" / "custody-taxonomy-v1.json").read_text(encoding="utf-8"))

def load_schema():
    return json.loads((ROOT / "contracts" / "custody-event-v1.schema.json").read_text(encoding="utf-8"))

def test_canonical_contract_passes():
    assert MODULE.validate(load_contract(), load_schema()) == []

def test_duplicate_custody_class_fails_closed():
    contract = load_contract()
    contract["custody_classes"].append(copy.deepcopy(contract["custody_classes"][0]))
    errors = MODULE.validate(contract, load_schema())
    assert any("duplicate custody class id" in error for error in errors)

def test_assistant_cannot_become_authorization_authority():
    contract = load_contract()
    contract["actors"]["ASSISTANT_TOOL_OPERATOR"]["may_authorize"] = True
    errors = MODULE.validate(contract, load_schema())
    assert any("assistant must never become human authorization authority" in error for error in errors)

def test_claim_default_cannot_auto_promote():
    contract = load_contract()
    contract["claim_gate"]["claim_allowed_default"] = True
    errors = MODULE.validate(contract, load_schema())
    assert any("claim_allowed_default must remain false" in error for error in errors)

def test_event_schema_must_match_taxonomy_classes():
    schema = load_schema()
    schema["properties"]["custodyClass"]["enum"] = ["C01_SOURCE_IDENTITY"]
    errors = MODULE.validate(load_contract(), schema)
    assert any("event schema custodyClass enum differs from taxonomy" in error for error in errors)
