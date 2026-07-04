#!/usr/bin/env bash
# Refatoração conservadora dos arquivos compactados e textos do diretório Livro.
# Modo padrão: --check, sem mover nada.

set -Eeuo pipefail
IFS=$'\n\t'

MODE="check"
REPORT=""
ROOT=""

usage() {
  cat <<'EOF'
Uso: bash scripts/refactor_livro_archives.sh [opções]

Opções:
  --check              inventaria e gera relatório sem mover arquivos (padrão)
  --apply              cria diretórios e move apenas arquivos históricos conhecidos
  --report ARQUIVO     caminho do relatório Markdown gerado
  -h, --help           mostra esta ajuda

Exemplos:
  bash scripts/refactor_livro_archives.sh --check
  bash scripts/refactor_livro_archives.sh --apply
  bash scripts/refactor_livro_archives.sh --check --report Livro/_archives/reports/livro_refactor_report.md
EOF
}

die() {
  printf '[livro-refactor:erro] %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[livro-refactor] %s\n' "$*" >&2
}

parse_args() {
  while (($#)); do
    case "$1" in
      --check) MODE="check"; shift ;;
      --apply) MODE="apply"; shift ;;
      --report)
        [[ $# -ge 2 ]] || die "faltou valor para --report"
        REPORT="$2"; shift 2 ;;
      -h|--help) usage; exit 0 ;;
      *) die "opção desconhecida: $1" ;;
    esac
  done
}

resolve_root() {
  if git rev-parse --show-toplevel >/dev/null 2>&1; then
    ROOT="$(git rev-parse --show-toplevel)"
  else
    ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  fi
  [[ -d "$ROOT/Livro" ]] || die "diretório Livro não encontrado em $ROOT"
  if [[ -z "$REPORT" ]]; then
    REPORT="$ROOT/Livro/_archives/reports/livro_refactor_report.md"
  elif [[ "$REPORT" != /* ]]; then
    REPORT="$ROOT/$REPORT"
  fi
}

category_for() {
  local path="$1" base
  base="$(basename "$path")"
  case "$base" in
    *.zip) printf 'archive_zip' ;;
    *.tar.gz|*.tgz) printf 'archive_tarball' ;;
    *.md|*.MD) printf 'document_markdown' ;;
    *.txt|*.TXT) printf 'raw_text' ;;
    *.sh) printf 'script_shell' ;;
    *.py) printf 'prototype_python' ;;
    *.c|*.h|*.S|*.asm|*.s) printf 'prototype_lowlevel' ;;
    *.json|*.yml|*.yaml) printf 'metadata' ;;
    *) printf 'other' ;;
  esac
}

recommended_dest_for() {
  local path="$1" category
  category="$(category_for "$path")"
  case "$category" in
    archive_zip)
      case "$(basename "$path")" in
        *SESSION*|*session*) printf 'Livro/_archives/sessions/%s' "$(basename "$path")" ;;
        *) printf 'Livro/_archives/bundles/%s' "$(basename "$path")" ;;
      esac ;;
    archive_tarball) printf 'Livro/_archives/bundles/%s' "$(basename "$path")" ;;
    script_shell|prototype_python|prototype_lowlevel) printf 'Livro/03_compiladores/%s' "$(basename "$path")" ;;
    document_markdown) printf 'Livro/02_pesquisa/%s' "$(basename "$path")" ;;
    raw_text) printf 'Livro/04_raw/%s' "$(basename "$path")" ;;
    metadata) printf 'Livro/_archives/reports/%s' "$(basename "$path")" ;;
    *) printf 'Livro/04_raw/%s' "$(basename "$path")" ;;
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

safe_move() {
  local src_rel="$1" dst_rel="$2" src="$ROOT/$src_rel" dst="$ROOT/$dst_rel"
  [[ -e "$src" ]] || { log "ignorado, não existe: $src_rel"; return 0; }
  [[ ! -e "$dst" ]] || die "destino já existe: $dst_rel"
  mkdir -p "$(dirname "$dst")"
  if git -C "$ROOT" ls-files --error-unmatch "$src_rel" >/dev/null 2>&1; then
    git -C "$ROOT" mv "$src_rel" "$dst_rel"
  else
    mv "$src" "$dst"
  fi
  log "movido: $src_rel -> $dst_rel"
}

write_report() {
  local livro="$ROOT/Livro"
  mkdir -p "$(dirname "$REPORT")"
  {
    printf '# Relatório de refatoração do Livro\n\n'
    printf 'Modo: `%s`\n\n' "$MODE"
    printf 'Raiz: `%s`\n\n' "$ROOT"
    printf '## Plano de movimentos seguros\n\n'
    printf '| Origem | Destino | Status |\n'
    printf '| --- | --- | --- |\n'
    while IFS='|' read -r src dst; do
      [[ -n "$src" ]] || continue
      if [[ -e "$ROOT/$src" ]]; then
        printf '| `%s` | `%s` | pronto_para_mover |\n' "$src" "$dst"
      elif [[ -e "$ROOT/$dst" ]]; then
        printf '| `%s` | `%s` | já_movido |\n' "$src" "$dst"
      else
        printf '| `%s` | `%s` | ausente |\n' "$src" "$dst"
      fi
    done < <(known_moves)

    printf '\n## Inventário até 2 níveis\n\n'
    printf '| Arquivo | Classe | Destino recomendado |\n'
    printf '| --- | --- | --- |\n'
    while IFS= read -r file; do
      rel="${file#$ROOT/}"
      printf '| `%s` | `%s` | `%s` |\n' "$rel" "$(category_for "$file")" "$(recommended_dest_for "$file")"
    done < <(find "$livro" -maxdepth 2 -type f | sort)

    printf '\n## Regras aplicadas\n\n'
    printf -- '- Pacotes compactados ficam como arquivo histórico.\n'
    printf -- '- O script não extrai zip nem tarball.\n'
    printf -- '- Arquivos ativos com referência conhecida não são movidos automaticamente.\n'
    printf -- '- Qualquer promoção futura precisa de comando de validação e diff revisado.\n'
  } > "$REPORT"
  log "relatório: ${REPORT#$ROOT/}"
}

apply_moves() {
  mkdir -p \
    "$ROOT/Livro/_archives/sessions" \
    "$ROOT/Livro/_archives/bundles" \
    "$ROOT/Livro/_archives/reports" \
    "$ROOT/Livro/01_capitulos" \
    "$ROOT/Livro/02_pesquisa" \
    "$ROOT/Livro/03_compiladores" \
    "$ROOT/Livro/04_raw"

  while IFS='|' read -r src dst; do
    [[ -n "$src" ]] || continue
    safe_move "$src" "$dst"
  done < <(known_moves)
}

main() {
  parse_args "$@"
  resolve_root
  case "$MODE" in
    check)
      write_report
      log "checagem concluída sem mover arquivos"
      ;;
    apply)
      apply_moves
      write_report
      log "aplicação concluída; revise git status e git diff"
      ;;
    *) die "modo inválido: $MODE" ;;
  esac
}

main "$@"
