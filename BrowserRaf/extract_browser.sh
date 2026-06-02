#!/usr/bin/env bash
set -euo pipefail

SRC="${1:-BrowserRaf/Browser.sh}"
OUT="${2:-BrowserRaf/Browser_extracted.sh}"

if [[ ! -f "$SRC" ]]; then
  echo "erro: arquivo fonte nao encontrado: $SRC" >&2
  exit 1
fi

awk 'BEGIN{on=0} /^#!\/usr\/bin\/env bash/{on=1} on{print} /^OUTER_SCRIPT$/{exit}' "$SRC" | sed '$d' > "$OUT"
chmod +x "$OUT"

echo "ok: extraido para $OUT"
