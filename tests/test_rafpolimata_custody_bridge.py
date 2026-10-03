#!/usr/bin/env python3
from pathlib import Path
import hashlib
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

assert bridge.canonical_observed_at("2026-10-03T00:00:00Z") == "2026-10-03T00:00:00Z"
assert bridge.canonical_observed_at("2026-10-02T21:00:00-03:00") == "2026-10-03T00:00:00Z"
for invalid_time in ("", "2026-10-03T00:00:00", "not-a-time"):
    try:
        bridge.canonical_observed_at(invalid_time)
    except ValueError:
        pass
    else:
        raise AssertionError(f"invalid observed-at was accepted: {invalid_time!r}")

argv = [
    "--bridge-id", "REPLAY-001",
    "--source-ref", "github:RafGitTools@d44fb2560d4ee92a08ab2d7a7d3f3be6ebe8c96d",
    "--artifact-ref", "sha256:example",
    "--execution-ref", "run:example",
    "--evidence-ref", "receipt:example",
    "--state", "OBSERVED_UNPROMOTED",
    "--predecessor", "receipt:previous",
    "--supersedes", "TOKEN_VAZIO",
    "--capability-name", "PAT_ACTIONS",
    "--capability-name", "pat_eNvir",
    "--observed-at", "2026-10-03T00:00:00Z",
]
first = bridge.canonical_json(bridge.build_envelope(bridge.parser().parse_args(argv)))
second = bridge.canonical_json(bridge.build_envelope(bridge.parser().parse_args(argv)))
assert first == second
assert hashlib.sha256(first.encode("utf-8")).hexdigest() == hashlib.sha256(second.encode("utf-8")).hexdigest()
assert '"observedAt":"2026-10-03T00:00:00Z"' in first
assert '"claimAllowed":false' in first

print("PASS rafpolimata-custody-bridge-v1 deterministic-replay")
