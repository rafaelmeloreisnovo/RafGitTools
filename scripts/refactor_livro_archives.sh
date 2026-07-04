#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

MODE="check"
REPORT=""

usage() {
  cat <<'EOF'
Uso: bash scripts/refactor_livro_archives.sh [--check] [--apply] [--report ARQUIVO]

--check   gera relatório sem alterar arquivos
--apply   cria diretórios e move apenas compactados conhecidos
--report  define caminho do relatório
EOF
}

while (($#)); do
  case "$1" in
    --check) MODE="check"; shift ;;
    --apply) MODE="apply"; shift ;;
    --report) REPORT="${2:?faltou caminho}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "opção desconhecida: $1" >&2; exit 2 ;;
  esac
done

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
LIVRO="$ROOT/Livro"
[[ -d "$LIVRO" ]] || { echo "Livro não encontrado" >&2; exit 1; }

if [[ -z "$REPORT" ]]; then
  REPORT="$LIVRO/_archives/reports/livro_refactor_report.md"
elif [[ "$REPORT" != /* ]]; then
  REPORT="$ROOT/$REPORT"
fi

category_for() {
  case "$(basename "$1")" in
    *.zip) echo archive_zip ;;
    *.tar.gz|*.tgz) echo archive_tarball ;;
    *.md|*.MD) echo document_markdown ;;
    *.txt|*.TXT) echo raw_text ;;
    *.sh) echo script_shell ;;
    *.py) echo prototype_python ;;
    *.c|*.h|*.S|*.asm|*.s) echo prototype_lowlevel ;;
    *.json|*.yml|*.yaml) echo metadata ;;
    *) echo other ;;
  esac
}

recommended_dest_for() {
  local base cat
  base="$(basename "$1")"
  cat="$(category_for "$1")"
  case "$cat" in
    archive_zip)
      case "$base" in *SESSION*|*session*) echo "Livro/_archives/sessions/$base" ;; *) echo "Livro/_archives/bundles/$base" ;; esac ;;
    archive_tarball) echo "Livro/_archives/bundles/$base" ;;
    script_shell|prototype_python|prototype_lowlevel) echo "Livro/03_compiladores/$base" ;;
    document_markdown) echo "Livro/02_pesquisa/$base" ;;
    raw_text) echo "Livro/04_raw/$base" ;;
    metadata) echo "Livro/_archives/reports/$base" ;;
    *) echo "Livro/04_raw/$base" ;;
  esac
}

known_moves() {
  cat <<'EOF'
Livro/RAFAELIA_SESSION_COMPLETE.zip|Livro/_archives/sessions/RAFAELIA_SESSION_COMPLETE.zip
Livro/rafaelia_bundle_v4.tar.gz|Livro/_archives/bundles/rafaelia_bundle_v4.tar.gz
Livro/rafaelia_bundle_v5.tar.gz|Livro/_archives/bundles/rafaelia_bundle_v5.tar.gz
Livro/rafaelia_bundle_v6.tar.gz|Livro/_archives/bundles/rafaelia_bundle_v6.tar.gz
EOF
}

mkdir -p "$(dirname "$REPORT")"
{
  echo '# Relatório de refatoração do Livro'
  echo
  echo "Modo: \`$MODE\`"
  echo
  echo '## Plano de compactados conhecidos'
  echo
  echo '| Origem | Destino | Status |'
  echo '| --- | --- | --- |'
  while IFS='|' read -r src dst; do
    [[ -n "$src" ]] || continue
    if [[ -e "$ROOT/$src" ]]; then status="pronto"; elif [[ -e "$ROOT/$dst" ]]; then status="ja_movido"; else status="ausente"; fi
    echo "| \`$src\` | \`$dst\` | $status |"
  done < <(known_moves)
  echo
  echo '## Inventário até 2 níveis'
  echo
  echo '| Arquivo | Classe | Destino recomendado |'
  echo '| --- | --- | --- |'
  while IFS= read -r file; do
    rel="${file#$ROOT/}"
    echo "| \`$rel\` | \`$(category_for "$file")\` | \`$(recommended_dest_for "$file")\` |"
  done < <(find "$LIVRO" -maxdepth 2 -type f | sort)
} > "$REPORT"

echo "relatório: ${REPORT#$ROOT/}"

if [[ "$MODE" == "apply" ]]; then
  mkdir -p "$LIVRO/_archives/sessions" "$LIVRO/_archives/bundles" "$LIVRO/_archives/reports" "$LIVRO/01_capitulos" "$LIVRO/02_pesquisa" "$LIVRO/03_compiladores" "$LIVRO/04_raw"
  while IFS='|' read -r src dst; do
    [[ -n "$src" ]] || continue
    [[ -e "$ROOT/$src" ]] || continue
    [[ ! -e "$ROOT/$dst" ]] || { echo "destino já existe: $dst" >&2; exit 1; }
    mkdir -p "$(dirname "$ROOT/$dst")"
    git -C "$ROOT" mv "$src" "$dst"
    echo "movido: $src -> $dst"
  done < <(known_moves)
fi
