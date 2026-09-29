import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULE = ROOT / "scripts" / "validate_receipt_envelope_v1.py"
SPEC = importlib.util.spec_from_file_location("receipt_validator", MODULE)
validator = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(validator)
FIX = ROOT / "tests" / "fixtures" / "receipt_envelope_v1"

def load(name):
    return json.loads((FIX / name).read_text(encoding="utf-8"))

def promoted_fixture():
    doc = load("pass_nonclaim.json")
    doc["receipt_id"] = "RCP-PROMOTED-001"
    doc["producer"]["commit"] = "a" * 40
    doc["source"] = {
        "source_id": "source-1",
        "source_ref": "source:opaque",
        "sha256": "b" * 64,
    }
    doc["execution"] = {
        "execution_id": "run-1",
        "environment": "ci",
        "architecture": "x86_64",
        "device_id": "TOKEN_VAZIO",
        "state": "PASS",
    }
    doc["evidence"] = [{
        "kind": "test-report",
        "ref": "artifact:test-report",
        "sha256": "c" * 64,
        "state": "PASS",
        "scope": "bounded-fixture",
    }]
    doc["result"] = {"state": "PASS", "summary": "Positive semantic fixture."}
    doc["gaps"] = []
    doc["receipt_sha256"] = "d" * 64
    doc["claim_allowed"] = True
    return doc

def test_nonclaim_fixture_passes():
    assert validator.validate_doc(load("pass_nonclaim.json")) == []

def test_positive_claim_semantics_pass():
    assert validator.validate_doc(promoted_fixture()) == []

def test_blocking_gap_prevents_claim():
    errors = validator.validate_doc(load("fail_blocking_claim.json"))
    assert any("blocking gaps" in e for e in errors)

def test_additional_property_fails_closed():
    doc = load("pass_nonclaim.json")
    doc["unexpected"] = 1
    errors = validator.validate_doc(doc)
    assert any("additionalProperties" in e for e in errors)

def test_token_vazio_source_cannot_promote_claim():
    doc = promoted_fixture()
    doc["source"]["sha256"] = "TOKEN_VAZIO"
    errors = validator.validate_doc(doc)
    assert any("source_ref and source.sha256" in e for e in errors)
