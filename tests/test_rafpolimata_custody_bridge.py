#!/usr/bin/env python3
from pathlib import Path
import importlib.util

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "emit_rafpolimata_custody_bridge.py"
spec = importlib.util.spec_from_file_location("bridge", SCRIPT)
bridge = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(bridge)

assert bridge.canonical_label("pat_eNvir") == "Pat_envir"
assert bridge.canonical_label("PAT_ACTIONS") == "Pat_actions"
assert bridge.canonical_label("Pat_codespace") == "Pat_codespace"

for forbidden in ("ghp_example", "github_pat_example", "sk-example"):
    try:
        bridge.canonical_label(forbidden)
    except ValueError:
        pass
    else:
        raise AssertionError("secret-looking value was accepted")

print("PASS rafpolimata-custody-bridge-v1")
