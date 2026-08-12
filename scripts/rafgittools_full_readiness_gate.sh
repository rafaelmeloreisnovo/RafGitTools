#!/usr/bin/env bash
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PYTHON_BIN="${PYTHON_BIN:-python3}"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  printf 'FULL_GATE\tFAIL\tpython3 unavailable; source-gap audit cannot run\n' >&2
  exit 2
fi

printf 'FULL_GATE\tSTART\tsource-gap self-test\n'
"$PYTHON_BIN" scripts/audit_source_gaps.py --self-test || {
  rc=$?
  printf 'FULL_GATE\tFAIL\tsource-gap self-test rc=%s\n' "$rc" >&2
  exit "$rc"
}

printf 'FULL_GATE\tSTART\tsource-gap scan\n'
"$PYTHON_BIN" scripts/audit_source_gaps.py || {
  rc=$?
  printf 'FULL_GATE\tFAIL\tsource-gap scan rc=%s\n' "$rc" >&2
  exit "$rc"
}

printf 'FULL_GATE\tPASS\tsource-gap scan\n'

if [[ ! -x scripts/rafgittools_readiness_gate.sh ]]; then
  chmod +x scripts/rafgittools_readiness_gate.sh 2>/dev/null || true
fi

printf 'FULL_GATE\tSTART\tcanonical readiness gate\n'
bash scripts/rafgittools_readiness_gate.sh
rc=$?
if [[ "$rc" -ne 0 ]]; then
  printf 'FULL_GATE\tFAIL\tcanonical readiness gate rc=%s\n' "$rc" >&2
  exit "$rc"
fi

printf 'FULL_GATE\tPASS\tall local source/readiness gates completed\n'
