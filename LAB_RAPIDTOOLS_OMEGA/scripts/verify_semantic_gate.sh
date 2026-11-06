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
printf '%s\n' 'DEVICE_EXECUTION=TOKEN_VAZIO RELEASE_ALLOWED=false CLAIM_ALLOWED=false'
