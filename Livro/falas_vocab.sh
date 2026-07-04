#!/usr/bin/env bash
# =============================================================================
# falas_vocab.sh — executa tudo: monólito + vocabulário + scan + scheduler
# =============================================================================
set -Eeuo pipefail
IFS=$'\n\t'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${1:-compiladorlowFala.txt}"
MANIFEST="${2:-compiladorlowFala.manifest.json}"
VOCAB_JSON="${3:-semantic_vocab.export.json}"
SMOKE_TEXT="FALA TOKEN AST BYTECODE ASM VM COERENCIA SCHEDULER AGENTE BIBLIA_CORPUS TERMUX ANDROID QEMU TCG CACHE"

need() {
  command -v "$1" >/dev/null 2>&1 || {
    printf '[falas_vocab:erro] comando ausente: %s\n' "$1" >&2
    exit 1
  }
}

need bash
need python3

[[ -x "$ROOT_DIR/falas.sh" ]] || chmod +x "$ROOT_DIR/falas.sh" 2>/dev/null || true
[[ -f "$ROOT_DIR/falas.sh" ]] || { echo '[falas_vocab:erro] Livro/falas.sh ausente' >&2; exit 1; }
[[ -f "$ROOT_DIR/semantic_vocab.py" ]] || { echo '[falas_vocab:erro] Livro/semantic_vocab.py ausente' >&2; exit 1; }

"$ROOT_DIR/falas.sh" -o "$OUT" -m "$MANIFEST"
python3 "$ROOT_DIR/semantic_vocab.py" --root "$ROOT_DIR" --scan-livro --export-vocab --pretty > "$VOCAB_JSON"
python3 "$ROOT_DIR/semantic_vocab.py" --root "$ROOT_DIR" --scan-livro --pretty "$SMOKE_TEXT" > "${VOCAB_JSON%.json}.smoke.json"
python3 "$ROOT_DIR/semantic_vocab.py" --root "$ROOT_DIR" --scan-livro --schedule --pretty "$SMOKE_TEXT" > "${VOCAB_JSON%.json}.schedule.json"
python3 "$ROOT_DIR/semantic_vocab.py" --explain-methods --pretty > "${VOCAB_JSON%.json}.methods.json"

seed_count=$(grep -Ec '^# S[0-9]{2}_V[1-5]' "$OUT")
vocab_clusters=$(python3 - "$VOCAB_JSON" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
print(len(data.get('semantic_clusters', {})))
PY
)
learned_terms=$(python3 - "$VOCAB_JSON" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
scan = data.get('livro_scan', {})
print(len(scan.get('learned_terms_top', [])))
PY
)
next_action=$(python3 - "${VOCAB_JSON%.json}.schedule.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
print(data.get('action', 'audit_phi'))
PY
)

printf 'artifact=%s\n' "$OUT"
printf 'manifest=%s\n' "$MANIFEST"
printf 'vocab=%s\n' "$VOCAB_JSON"
printf 'smoke=%s\n' "${VOCAB_JSON%.json}.smoke.json"
printf 'schedule=%s\n' "${VOCAB_JSON%.json}.schedule.json"
printf 'methods=%s\n' "${VOCAB_JSON%.json}.methods.json"
printf 'seed_count=%s\n' "$seed_count"
printf 'vocab_clusters=%s\n' "$vocab_clusters"
printf 'learned_terms=%s\n' "$learned_terms"
printf 'next_action=%s\n' "$next_action"
printf 'status=sealed\n'
