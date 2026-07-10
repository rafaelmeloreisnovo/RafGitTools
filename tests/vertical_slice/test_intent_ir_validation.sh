#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

VALID_INTENT="$WORKDIR/intent_valid.json"
INVALID_INTENT="$WORKDIR/intent_invalid.json"

cat > "$VALID_INTENT" <<'JSON'
{
  "schema": "rafaelia.intent.v1",
  "intent_id": "intent-valid-01",
  "action": "compiled_plan.execute",
  "target": {"command_set": "readonly_git_status_diffstat"},
  "inputs": [],
  "constraints": [],
  "evidence_refs": ["chunk-001"],
  "requested_capabilities": ["git.read", "git.diff"],
  "risk": "low",
  "execution_gate": "allow"
}
JSON

cat > "$INVALID_INTENT" <<'JSON'
{
  "schema": "rafaelia.intent.v1",
  "intent_id": "intent-invalid-01",
  "action": "compiled_plan.execute",
  "target": {"command_set": "readonly_git_status_diffstat"},
  "inputs": [],
  "constraints": [],
  "evidence_refs": [],
  "requested_capabilities": ["git.read", "git.diff"],
  "risk": "extreme",
  "execution_gate": "allow"
}
JSON

"$REPO_ROOT/scripts/vertical_slice/run_readonly_flow.sh" "$VALID_INTENT" "$WORKDIR/out-valid" >/dev/null

if "$REPO_ROOT/scripts/vertical_slice/run_readonly_flow.sh" "$INVALID_INTENT" "$WORKDIR/out-invalid" >/tmp/intent_invalid_stdout.txt 2>/tmp/intent_invalid_stderr.txt; then
  echo "FAIL: intent inválido deveria falhar"
  exit 1
fi

grep -q "risk fora do enum" /tmp/intent_invalid_stderr.txt

echo "PASS: test_intent_ir_validation"
