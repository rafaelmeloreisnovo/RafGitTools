#!/usr/bin/env bash
# =============================================================================
# falas_vocab.sh — wrapper para gerar monólito + vocabulário semântico RAFCODE-Φ
# =============================================================================
set -Eeuo pipefail
IFS=$'\n\t'

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${1:-compiladorlowFala.txt}"
MANIFEST="${2:-compiladorlowFala.manifest.json}"
VOCAB_JSON="${3:-semantic_vocab.export.json}"
SMOKE_TEXT="FALA TOKEN AST BYTECODE VM COERENCIA SCHEDULER AGENTE BIBLIA_CORPUS"

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
python3 "$ROOT_DIR/semantic_vocab.py" --export-vocab --pretty > "$VOCAB_JSON"
python3 "$ROOT_DIR/semantic_vocab.py" --pretty "$SMOKE_TEXT" > "${VOCAB_JSON%.json}.smoke.json"

seed_count=$(grep -Ec '^# S[0-9]{2}_V[1-5]' "$OUT")
vocab_clusters=$(python3 - "$VOCAB_JSON" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)
print(len(data.get('semantic_clusters', {})))
PY
)

printf 'artifact=%s\n' "$OUT"
printf 'manifest=%s\n' "$MANIFEST"
printf 'vocab=%s\n' "$VOCAB_JSON"
printf 'smoke=%s\n' "${VOCAB_JSON%.json}.smoke.json"
printf 'seed_count=%s\n' "$seed_count"
printf 'vocab_clusters=%s\n' "$vocab_clusters"
printf 'status=sealed\n'
