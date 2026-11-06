#!/bin/sh
# Hosted build/test driver. Only core/raf_semantic_gate.c is freestanding.
set -eu
cd "$(dirname "$0")/.."
CC=${CC:-cc}
CLANG=${CLANG:-clang}
OUT=${TMPDIR:-/tmp}/raf_semantic_gate_$$
trap 'rm -f "$OUT" "$OUT".*' EXIT HUP INT TERM
"$CC" -std=c11 -O2 -Wall -Wextra -Werror -pedantic -ffreestanding -fno-builtin \
  -Icore core/raf_semantic_gate.c tests/test_semantic_gate.c -o "$OUT"
"$OUT"
printf '%s\n' 'HOST_TEST=PASS_SCOPED'
if command -v "$CLANG" >/dev/null 2>&1; then
  for target in x86_64-unknown-none armv7a-none-eabi aarch64-none-elf; do
    "$CLANG" --target="$target" -std=c11 -O2 -Wall -Wextra -Werror \
      -ffreestanding -fno-builtin -fno-stack-protector \
      -c core/raf_semantic_gate.c -o "$OUT.$target.o"
    if command -v nm >/dev/null 2>&1; then
      if nm -u "$OUT.$target.o" | grep '[^[:space:]]' >/dev/null; then
        printf 'UNDEFINED_EXTERNALS=%s\n' "$target" >&2
        exit 1
      fi
    else
      printf '%s\n' 'UNDEFINED_SYMBOL_CHECK=NOT_RUN (nm absent)'
    fi
    printf 'CROSS_OBJECT=%s PASS_SCOPED\n' "$target"
  done
else
  printf '%s\n' 'CROSS_OBJECT=NOT_RUN (clang absent)'
fi
# Strict CI-only ELF evidence. Local smoke checks remain compatible.
# No missing tool, wrong ELF, unresolved externals, or nondeterminism may pass CI.
REQUIRE_CROSS=${RAF_GATE_REQUIRE_CROSS:-0}
# GitHub-hosted CI must never report green when a cross-ABI check is missing.
if [ "${GITHUB_ACTIONS:-false}" = true ]; then REQUIRE_CROSS=1; fi
case "$REQUIRE_CROSS" in
  0) ;;
  1)
    READELF=${READELF:-readelf}
    for tool in "$CLANG" "$READELF" cmp sha256sum; do
      if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'STRICT_CROSS_ABI=FAIL_MISSING_TOOL tool=%s\n' "$tool" >&2
        exit 2
      fi
    done
    for target in x86_64-unknown-none armv7a-none-eabi aarch64-none-elf; do
      case "$target" in
        x86_64-unknown-none) machine='Advanced Micro Devices X86-64' ;;
        armv7a-none-eabi) machine='ARM' ;;
        aarch64-none-elf) machine='AArch64' ;;
      esac
      obj="$OUT.$target.o"
      if [ ! -s "$obj" ] ||
         ! "$READELF" -h "$obj" > "$OUT.header" ||
         ! grep -E 'Type:.*REL' "$OUT.header" >/dev/null ||
         ! grep -F 'Machine:' "$OUT.header" | grep -F "$machine" >/dev/null; then
        printf 'STRICT_CROSS_ABI=FAIL_ELF_IDENTITY target=%s\n' "$target" >&2
        exit 3
      fi
      if ! "$READELF" -Ws "$obj" > "$OUT.symbols"; then
        printf 'STRICT_CROSS_ABI=FAIL_SYMBOL_INSPECTION target=%s\n' "$target" >&2
        exit 3
      fi
      if awk '$7 == "UND" && NF >= 8 { print; found=1 } END { exit found ? 0 : 1 }' "$OUT.symbols"; then
        printf 'STRICT_CROSS_ABI=FAIL_UNDEFINED_EXTERNAL target=%s\n' "$target" >&2
        exit 4
      fi
      "$CLANG" --target="$target" -std=c11 -O2 -Wall -Wextra -Werror \
        -ffreestanding -fno-builtin -fno-stack-protector \
        -c core/raf_semantic_gate.c -o "$OUT.$target.again.o"
      if ! cmp -s "$obj" "$OUT.$target.again.o"; then
        printf 'STRICT_CROSS_ABI=FAIL_OBJECT_REPRODUCIBILITY target=%s\n' "$target" >&2
        exit 5
      fi
      digest=$(sha256sum "$obj")
      printf 'STRICT_CROSS_ABI=%s REL=PASS UNDEFINED=NONE REPRODUCIBLE=YES SHA256=%s\n' "$target" "${digest%% *}"
    done
    printf '%s\n' 'STRICT_CROSS_ABI=PASS_SCOPED OBJECT_ONLY'
    ;;
  *) printf '%s\n' 'STRICT_CROSS_ABI=FAIL_INVALID_MODE' >&2; exit 2 ;;
esac
printf '%s\n' 'DEVICE_EXECUTION=TOKEN_VAZIO RELEASE_ALLOWED=false CLAIM_ALLOWED=false'
