#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

INTENT_OK="$WORKDIR/intent_ok.json"

cat > "$INTENT_OK" <<'JSON'
{
  "schema": "rafaelia.intent.v1",
  "intent_id": "intent-result-01",
  "action": "compiled_plan.execute",
  "target": {"command_set": "readonly_git_status_diffstat"},
  "inputs": [],
  "constraints": [],
  "evidence_refs": ["chunk-003", "chunk-004"],
  "requested_capabilities": ["git.read", "git.diff"],
  "risk": "low",
  "execution_gate": "allow"
}
JSON

"$REPO_ROOT/scripts/vertical_slice/run_readonly_flow.sh" "$INTENT_OK" "$WORKDIR/out" >/dev/null

python3 - "$WORKDIR/out/execution_result.json" <<'PY'
import hashlib
import json
import sys
from datetime import datetime
from pathlib import Path

result_path = Path(sys.argv[1])
res = json.loads(result_path.read_text(encoding="utf-8"))
required = [
    "intent_id",
    "executed_command",
    "args",
    "working_directory",
    "started_at",
    "ended_at",
    "exit_code",
    "stdout_truncated",
    "stderr_truncated",
    "stdout_sha256",
    "stderr_sha256",
    "artifacts",
    "final_state",
    "rollback_available",
    "source_chunk_refs",
]
for key in required:
    assert key in res, f"missing field: {key}"

start = datetime.fromisoformat(res["started_at"].replace("Z", "+00:00"))
end = datetime.fromisoformat(res["ended_at"].replace("Z", "+00:00"))
assert end >= start, "ended_at must be >= started_at"

artifacts = [Path(p) for p in res["artifacts"]]
stdout_path = next(p for p in artifacts if p.name == "stdout.log")
stderr_path = next(p for p in artifacts if p.name == "stderr.log")

stdout_hash = hashlib.sha256(stdout_path.read_text(encoding="utf-8").encode("utf-8")).hexdigest()
stderr_hash = hashlib.sha256(stderr_path.read_text(encoding="utf-8").encode("utf-8")).hexdigest()

assert stdout_hash == res["stdout_sha256"], "stdout hash mismatch"
assert stderr_hash == res["stderr_sha256"], "stderr hash mismatch"
print("PASS: test_execution_result_schema")
PY
