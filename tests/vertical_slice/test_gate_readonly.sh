#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

INTENT_FORBIDDEN="$WORKDIR/intent_forbidden_cap.json"

cat > "$INTENT_FORBIDDEN" <<'JSON'
{
  "schema": "rafaelia.intent.v1",
  "intent_id": "intent-forbidden-01",
  "action": "compiled_plan.execute",
  "target": {"command_set": "readonly_git_status_diffstat"},
  "inputs": [],
  "constraints": [],
  "evidence_refs": ["chunk-002"],
  "requested_capabilities": ["git.read", "git.diff", "network.http"],
  "risk": "medium",
  "execution_gate": "allow"
}
JSON

if "$REPO_ROOT/scripts/vertical_slice/run_readonly_flow.sh" "$INTENT_FORBIDDEN" "$WORKDIR/out" >/tmp/gate_stdout.txt 2>/tmp/gate_stderr.txt; then
  echo "FAIL: gate deveria bloquear capability fora da allowlist"
  exit 1
fi

grep -q "fora da allowlist" /tmp/gate_stderr.txt

echo "PASS: test_gate_readonly"
