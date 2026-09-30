#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs" / "authorial-omega-hypervisor-executor.v1.json"
EXPECTED_MAPA_HEAD = "f23304cc5a2b77805df85ab1a0586c8521b87756"
EXPECTED_MAPA_BLOB = "d223400c4bd57d6e4c31cd2de0db4c882b55a4bb"
EXPECTED_BL0_BLOB = "132f948d199f6679fcae4f33912e0a3ad69691a3"

class ValidationError(ValueError):
    pass

def require(value, message):
    if not value:
        raise ValidationError(message)

def load():
    return json.loads(CONFIG.read_text(encoding="utf-8"))

def validate(data):
    require(data.get("schema") == "rafgittools.authorial-omega-hypervisor-executor.v1", "schema")
    require(data.get("claim_allowed") is False, "claim_allowed must remain false")
    require(data.get("inherited_code_imported") is False, "inherited code import forbidden")
    require(data.get("model_references_imported_as_code") is False, "model code import forbidden")

    fed = data.get("federated_authority", {})
    require(fed.get("repository") == "rafaelmeloreisnovo/Mapa", "Mapa authority")
    require(fed.get("commit") == EXPECTED_MAPA_HEAD, "Mapa exact head")
    require(fed.get("manifest_blob_sha1") == EXPECTED_MAPA_BLOB, "Mapa manifest blob")
    require(fed.get("manifold_ci_state") == "PASS", "Mapa manifold structural PASS")
    require(fed.get("promotion_state") == "NOT_AUTHORIZED", "promotion boundary")

    contract = data.get("execution_contract", {})
    for key in ("heap", "libc", "system_headers", "syscalls", "network", "filesystem", "floating_point", "external_runtime_symbols"):
        require(contract.get(key) is False, f"freestanding boundary: {key}")

    payloads = data.get("authorial_payloads", [])
    require(len(payloads) == 1, "V1 requires one bounded authorial payload")
    payload = payloads[0]
    require(payload.get("artifact_id") == "RAF_BL0_V0", "RAF_BL0 artifact")
    require(payload.get("git_blob_sha1") == EXPECTED_BL0_BLOB, "RAF_BL0 exact blob")
    require(payload.get("copied_into_state_engine") is False, "state engine must not silently copy RAF_BL0")

    gaps = data.get("gaps", [])
    ids = [g.get("id") for g in gaps]
    require(len(ids) == len(set(ids)), "duplicate gaps")
    require(any(g.get("state") == "TOKEN_VAZIO" for g in gaps), "unknowns must remain explicit")
    require(data.get("authorship_claim") == "TOKEN_VAZIO_LEGAL_AUTHORSHIP_NOT_PROMOTED", "new glue authorship boundary")
    require(bool(data.get("F_next")), "F_next")
    return True

def main():
    data = load()
    validate(data)
    print(json.dumps({
        "schema": data["schema"],
        "state": "PASS_STRUCTURAL",
        "mapa_head": EXPECTED_MAPA_HEAD,
        "mapa_manifest_blob": EXPECTED_MAPA_BLOB,
        "authorial_payload_blob": EXPECTED_BL0_BLOB,
        "claim_allowed": False
    }, sort_keys=True))

if __name__ == "__main__":
    main()
