#!/usr/bin/env bash
# RAFAELIA · raf_scan_repo.sh
# Deterministic repository scanner for priority, latent files, hotfix candidates and evidence gaps.
# Usage:
#   bash tools/raf_scan_repo.sh /path/to/repo
#   bash tools/raf_scan_repo.sh .

set -euo pipefail

ROOT="${1:-$PWD}"
ROOT="$(cd "$ROOT" && pwd)"
REPO_NAME="$(basename "$ROOT")"
STAMP="$(date -Iseconds 2>/dev/null || date '+%Y-%m-%dT%H:%M:%S')"
OUT_DIR="$ROOT/.rafaelia/reports"
mkdir -p "$OUT_DIR"

INVENTORY="$OUT_DIR/RAFAELIA_REPO_INVENTORY.md"
PRIORITY="$OUT_DIR/RAFAELIA_PRIORITY_REPORT.md"
HOTFIX="$OUT_DIR/RAFAELIA_HOTFIX_CANDIDATES.md"
ORPHANS="$OUT_DIR/RAFAELIA_ORPHAN_FILES.md"
SUMMARY="$OUT_DIR/RAFAELIA_SCAN_SUMMARY.md"
TMP_FILES="$OUT_DIR/.raf_files.tmp"
TMP_FINDINGS="$OUT_DIR/.raf_findings.tmp"

cd "$ROOT"

if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git ls-files > "$TMP_FILES"
else
  find . -type f \
    -not -path './.git/*' \
    -not -path './build/*' \
    -not -path './.gradle/*' \
    -not -path './node_modules/*' \
    | sed 's#^./##' | sort > "$TMP_FILES"
fi

is_text_ext() {
  case "$1" in
    *.md|*.txt|*.c|*.h|*.cpp|*.hpp|*.cc|*.kt|*.kts|*.java|*.py|*.sh|*.bash|*.zsh|*.js|*.ts|*.tsx|*.jsx|*.json|*.yml|*.yaml|*.xml|*.toml|*.ini|*.gradle|*.properties|*.cmake|CMakeLists.txt|Makefile|Dockerfile) return 0 ;;
    *) return 1 ;;
  esac
}

kind_of() {
  local f="$1"
  case "$f" in
    *.c|*.h|*.cpp|*.hpp|*.cc|*.S|*.s) echo "native-code" ;;
    *.kt|*.kts|*.java) echo "android-jvm" ;;
    *.py|*.sh|*.bash|*.zsh|Makefile|CMakeLists.txt|*.cmake) echo "script-build" ;;
    *.md|*.txt) echo "doc-text" ;;
    *.json|*.yml|*.yaml|*.toml|*.xml|*.properties|*.gradle) echo "config" ;;
    *.png|*.jpg|*.jpeg|*.webp|*.gif|*.svg) echo "image" ;;
    *.zip|*.tar|*.gz|*.xz|*.7z|*.apk|*.jar|*.aar) echo "archive-binary" ;;
    *.*) echo "other" ;;
    *) echo "orphan-no-extension" ;;
  esac
}

score_file() {
  local f="$1"
  local score=0
  local reason=""

  case "$(kind_of "$f")" in
    native-code|android-jvm|script-build) score=$((score+18)); reason="$reason code/build;" ;;
    config) score=$((score+10)); reason="$reason config;" ;;
    orphan-no-extension) score=$((score+14)); reason="$reason no-extension-latent;" ;;
    doc-text) score=$((score+8)); reason="$reason doc;" ;;
  esac

  case "$f" in
    *build.gradle*|*settings.gradle*|*AndroidManifest.xml|*CMakeLists.txt|*Makefile*) score=$((score+16)); reason="$reason build-critical;" ;;
    *MainActivity*|*Application*|*Service*|*Terminal*|*Session*|*Shell*|*Pty*|*PTY*|*Bootstrap*|*bootstrap*) score=$((score+18)); reason="$reason runtime-critical;" ;;
    *test*|*Test*|*bench*|*Bench*|*kat*|*KAT*) score=$((score+20)); reason="$reason evidence-test;" ;;
    *README*|*INDEX*|*BUILD*|*docs/*) score=$((score+8)); reason="$reason obvious-doc;" ;;
  esac

  if is_text_ext "$f" && [ -f "$f" ]; then
    if grep -InE 'TODO|FIXME|XXX|HACK|BUG|panic|crash|exception|fail|failed|error|SDK 29|sdk 29|logcat|permission|denied|segfault|SIGSEGV|malloc|garbage|leak|overflow|undefined|UB|race|deadlock' "$f" >/dev/null 2>&1; then
      score=$((score+20)); reason="$reason hotfix-signal;"
    fi
  fi

  printf '%03d|%s|%s\n' "$score" "$f" "$reason"
}

: > "$TMP_FINDINGS"
while IFS= read -r f; do
  [ -z "$f" ] && continue
  score_file "$f" >> "$TMP_FINDINGS"
done < "$TMP_FILES"

TOTAL_FILES="$(wc -l < "$TMP_FILES" | tr -d ' ')"
NO_EXT="$(awk -F'|' '$2 !~ /\./ {c++} END{print c+0}' "$TMP_FINDINGS")"
CODE_FILES="$(awk -F'|' '$3 ~ /code|build|runtime/ {c++} END{print c+0}' "$TMP_FINDINGS")"
HOTFIX_SIGNALS="$(awk -F'|' '$3 ~ /hotfix-signal/ {c++} END{print c+0}' "$TMP_FINDINGS")"
TOP_SCORE="$(sort -t'|' -k1,1nr "$TMP_FINDINGS" | head -n 1 | cut -d'|' -f1)"

cat > "$SUMMARY" <<EOF
# RAFAELIA · scan summary

Repository: \\`$REPO_NAME\\`  
Root: \\`$ROOT\\`  
Timestamp: \\`$STAMP\\`

## Metrics

| Metric | Value |
|---|---:|
| Total files | $TOTAL_FILES |
| Files without extension | $NO_EXT |
| Code/build/runtime candidates | $CODE_FILES |
| Hotfix signal files | $HOTFIX_SIGNALS |
| Top score | ${TOP_SCORE:-0} |

## Reports

- \\`.rafaelia/reports/RAFAELIA_REPO_INVENTORY.md\\`
- \\`.rafaelia/reports/RAFAELIA_PRIORITY_REPORT.md\\`
- \\`.rafaelia/reports/RAFAELIA_HOTFIX_CANDIDATES.md\\`
- \\`.rafaelia/reports/RAFAELIA_ORPHAN_FILES.md\\`
EOF

cat > "$INVENTORY" <<EOF
# RAFAELIA · repository inventory

Repository: \\`$REPO_NAME\\`  
Timestamp: \\`$STAMP\\`

| Kind | File |
|---|---|
EOF

while IFS= read -r f; do
  printf '| %s | `%s` |\n' "$(kind_of "$f")" "$f" >> "$INVENTORY"
done < "$TMP_FILES"

cat > "$PRIORITY" <<'EOF'
# RAFAELIA · priority report

Score is a practical approximation of:

```text
valor = 0.20*evidencia + 0.18*urgencia + 0.16*memoria + 0.14*transmutacao + 0.12*latencia + 0.08*obviedade + 0.06*intencao + 0.04*ruido + 0.02*coerencia
```

| Score | File | Signals |
|---:|---|---|
EOF

sort -t'|' -k1,1nr "$TMP_FINDINGS" | head -n 80 | while IFS='|' read -r score file reason; do
  printf '| %s | `%s` | %s |\n' "$score" "$file" "$reason" >> "$PRIORITY"
done

cat > "$HOTFIX" <<EOF
# RAFAELIA · hotfix candidates

Repository: \\`$REPO_NAME\\`  
Timestamp: \\`$STAMP\\`

These files contain signals such as TODO/FIXME/BUG/crash/error/SDK/logcat/permission/overflow.

| File | Matching lines |
|---|---|
EOF

while IFS= read -r f; do
  if is_text_ext "$f" && [ -f "$f" ]; then
    matches="$(grep -InE 'TODO|FIXME|XXX|HACK|BUG|panic|crash|exception|fail|failed|error|SDK 29|sdk 29|logcat|permission|denied|segfault|SIGSEGV|malloc|garbage|leak|overflow|undefined|UB|race|deadlock' "$f" 2>/dev/null | head -n 5 | sed 's/|/\\|/g' | tr '\n' '<' | sed 's/</<br>/g')"
    if [ -n "$matches" ]; then
      printf '| `%s` | %s |\n' "$f" "$matches" >> "$HOTFIX"
    fi
  fi
done < "$TMP_FILES"

cat > "$ORPHANS" <<EOF
# RAFAELIA · orphan and latent files

Repository: \\`$REPO_NAME\\`  
Timestamp: \\`$STAMP\\`

Files without extension or with unclear classification. Do not delete automatically; inspect first.

| File | Kind | Suggested action |
|---|---|---|
EOF

awk -F'|' '$2 !~ /\./ {print $2}' "$TMP_FINDINGS" | while IFS= read -r f; do
  [ -z "$f" ] && continue
  printf '| `%s` | `%s` | inspect text/binary, name purpose, index or archive |\n' "$f" "$(kind_of "$f")" >> "$ORPHANS"
done

rm -f "$TMP_FILES" "$TMP_FINDINGS"

printf '[RAFAELIA] scan complete: %s\n' "$ROOT"
printf '[RAFAELIA] reports: %s\n' "$OUT_DIR"
printf '[RAFAELIA] summary: %s\n' "$SUMMARY"
