#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

INTENT_PATH="${1:-}"
OUTPUT_DIR="${2:-${REPO_ROOT}/_incoming/vertical_slice}"

if [[ -z "${INTENT_PATH}" ]]; then
  echo "Usage: $0 <intent_ir.json> [output_dir]" >&2
  exit 2
fi

python3 - "$REPO_ROOT" "$INTENT_PATH" "$OUTPUT_DIR" <<'PY'
import hashlib
import json
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

repo_root = Path(sys.argv[1])
intent_path = Path(sys.argv[2])
output_dir = Path(sys.argv[3])

schema_path = repo_root / "docs/contracts/intent_ir.schema.json"
capabilities_path = repo_root / "internal/governance/capabilities.json"
policy_path = repo_root / "internal/governance/policy.json"

output_dir.mkdir(parents=True, exist_ok=True)

intent = json.loads(intent_path.read_text(encoding="utf-8"))
schema = json.loads(schema_path.read_text(encoding="utf-8"))
capabilities = json.loads(capabilities_path.read_text(encoding="utf-8"))
policy = json.loads(policy_path.read_text(encoding="utf-8"))

required = schema["required"]
for key in required:
    if key not in intent:
        raise SystemExit(f"intent_ir inválido: campo obrigatório ausente '{key}'")

if intent.get("schema") != "rafaelia.intent.v1":
    raise SystemExit("intent_ir inválido: schema deve ser 'rafaelia.intent.v1'")

if intent.get("risk") not in ["low", "medium", "high", "critical"]:
    raise SystemExit("intent_ir inválido: risk fora do enum")

if intent.get("execution_gate") not in ["allow", "sandbox_only", "human_review", "blocked"]:
    raise SystemExit("intent_ir inválido: execution_gate fora do enum")

allowed_caps = capabilities.get("capabilities", {})
default_policy = capabilities.get("default_policy", "blocked")
requested_caps = intent.get("requested_capabilities", [])

for cap in requested_caps:
    decision = allowed_caps.get(cap, default_policy)
    if decision == "blocked":
        raise SystemExit(f"governance bloqueou capability fora da allowlist: {cap}")

for required_cap in policy.get("required_capabilities", []):
    if required_cap not in requested_caps:
        raise SystemExit(f"governance bloqueou intent sem capability obrigatória: {required_cap}")

if policy.get("forbid_free_text_shell", True):
    if intent.get("action") != "compiled_plan.execute":
        raise SystemExit("governance bloqueou action não compilada")

expected_target = {"command_set": "readonly_git_status_diffstat"}
if intent.get("target") != expected_target:
    raise SystemExit("governance bloqueou target fora do plano read-only explícito")

commands = [
    ["git", "status"],
    ["git", "diff", "--stat"],
]
allowed_commands = set(policy.get("allowlisted_commands", []))
for cmd in commands:
    cmd_text = " ".join(cmd)
    if cmd_text not in allowed_commands:
        raise SystemExit(f"governance bloqueou comando fora da policy: {cmd_text}")

plan = {
    "schema": "rafaelia.execution_plan.v1",
    "plan_id": f"plan-{intent['intent_id']}",
    "intent_id": intent["intent_id"],
    "compiled": True,
    "steps": [
        {"command": "git", "args": ["status"], "read_only": True},
        {"command": "git", "args": ["diff", "--stat"], "read_only": True},
    ],
}
plan_path = output_dir / "execution_plan.json"
plan_path.write_text(json.dumps(plan, indent=2) + "\n", encoding="utf-8")

started_at_dt = datetime.now(timezone.utc)
combined_stdout = []
combined_stderr = []
exit_code = 0

for cmd in commands:
    proc = subprocess.run(
        cmd,
        cwd=repo_root,
        capture_output=True,
        text=True,
        check=False,
    )
    cmd_text = " ".join(cmd)
    combined_stdout.append(f"$ {cmd_text}\n{proc.stdout}")
    combined_stderr.append(f"$ {cmd_text}\n{proc.stderr}")
    if proc.returncode != 0 and exit_code == 0:
        exit_code = proc.returncode

ended_at_dt = datetime.now(timezone.utc)
stdout_full = "\n".join(combined_stdout)
stderr_full = "\n".join(combined_stderr)

stdout_sha = hashlib.sha256(stdout_full.encode("utf-8")).hexdigest()
stderr_sha = hashlib.sha256(stderr_full.encode("utf-8")).hexdigest()

stdout_path = output_dir / "stdout.log"
stderr_path = output_dir / "stderr.log"
stdout_path.write_text(stdout_full, encoding="utf-8")
stderr_path.write_text(stderr_full, encoding="utf-8")

result = {
    "schema": "rafaelia.execution_result.v1",
    "intent_id": intent["intent_id"],
    "executed_command": "readonly_git_status_diffstat",
    "args": ["git status", "git diff --stat"],
    "working_directory": str(repo_root),
    "started_at": started_at_dt.isoformat().replace("+00:00", "Z"),
    "ended_at": ended_at_dt.isoformat().replace("+00:00", "Z"),
    "exit_code": exit_code,
    "stdout_truncated": False,
    "stderr_truncated": False,
    "stdout_sha256": stdout_sha,
    "stderr_sha256": stderr_sha,
    "artifacts": [
        str(plan_path),
        str(stdout_path),
        str(stderr_path)
    ],
    "final_state": "completed" if exit_code == 0 else "failed",
    "rollback_available": False,
    "source_chunk_refs": intent.get("evidence_refs", []),
}

result_path = output_dir / "execution_result.json"
result_path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(str(result_path))
PY
