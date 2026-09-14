#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "configs/matrix-compose-federation.v1.json"
SCHEMA = ROOT / "schemas/rafaelia_matrix_compose_job.v1.schema.json"

def validate():
    errors = []
    cfg = json.loads(CONFIG.read_text())
    sch = json.loads(SCHEMA.read_text())
    if cfg.get("schema") != "rafaelia.matrix-compose-federation.v1":
        errors.append("bad federation schema")
    if cfg.get("claim_allowed") is not False:
        errors.append("claim_allowed must be false")
    roles = cfg.get("roles", {})
    expected = {
        "control_plane": "rafaelmeloreisnovo/RafGitTools",
        "producer": "rafaelmeloreisnovo/RafPolimata",
        "observer": "rafaelmeloreisnovo/frida-desktop"
    }
    for key, value in expected.items():
        if roles.get(key) != value:
            errors.append(f"role {key} mismatch")
    if cfg.get("contracts", {}).get("job_schema") != "schemas/rafaelia_matrix_compose_job.v1.schema.json":
        errors.append("job schema pointer mismatch")
    props = sch.get("properties", {})
    if props.get("operation", {}).get("const") != "compose_matrix_v1":
        errors.append("operation is not typed")
    policy = props.get("policy", {}).get("properties", {})
    for key, expected_value in [("read_only_sources", True), ("allow_network", False), ("claim_allowed", False)]:
        if policy.get(key, {}).get("const") is not expected_value:
            errors.append(f"policy {key} is not fail-closed")
    outputs = set(props.get("requested_outputs", {}).get("items", {}).get("enum", []))
    required = {"matrix.output.json", "compose.output.hex", "ifdex.v1.json", "receipt.v1.json"}
    if outputs != required:
        errors.append("requested output contract mismatch")
    return errors

def main():
    errors = validate()
    if errors:
        for error in errors:
            print("FAIL", error)
        return 1
    print("PASS matrix-compose federation contract")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
