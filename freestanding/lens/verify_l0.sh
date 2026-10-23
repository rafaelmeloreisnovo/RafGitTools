#!/bin/sh
# HOSTED verifier ONLY: never linked into L0 and never executes provider mutations.
set -eu
BASE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CC=${CC:-clang}
LD=${LD:-ld.lld}
NM=${NM:-nm}
READELF=${READELF:-readelf}
DIR=$(mktemp -d)
trap 'rm -rf "$DIR"' EXIT HUP INT TERM
CFLAGS="-std=c11 -Os -Wall -Wextra -Werror -ffreestanding -fno-builtin -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -ffunction-sections -fdata-sections"
for tool in "$CC" "$LD" "$NM" "$READELF"; do
  command -v "$tool" >/dev/null 2>&1 || { echo "MISSING_TOOL:$tool" >&2; exit 1; }
done
# The core cannot include system/third-party headers, including via its own header.
for source in rfl_lens.h rfl_sha256.c rfl_lens.c rfl_graph.c; do
  if grep -Eq '^#[[:space:]]*include[[:space:]]*<' "$BASE/$source"; then
    echo "DENY_EXTERNAL_HEADER:$source" >&2; exit 1
  fi
done
"$CC" -std=c11 -Os -Wall -Wextra -Werror -I"$BASE" \
  "$BASE/rfl_sha256.c" "$BASE/rfl_lens.c" "$BASE/rfl_graph.c" \
  "$BASE/tests/test_l0.c" -o "$DIR/kat"
"$DIR/kat"
echo 'KAT_AND_NEGATIVE_TESTS:PASS_HOST_ONLY'
for target in armv7a-none-eabi aarch64-none-elf x86_64-unknown-none; do
  for name in rfl_sha256 rfl_lens rfl_graph; do
    "$CC" --target="$target" $CFLAGS -nostdlib -c "$BASE/$name.c" -o "$DIR/${target}_${name}.o"
  done
  "$LD" -r "$DIR/${target}_rfl_sha256.o" "$DIR/${target}_rfl_lens.o" \
    "$DIR/${target}_rfl_graph.o" -o "$DIR/${target}_combined.o"
  combined="$DIR/${target}_combined.o"
  # A failed nm/readelf command must fail the job; a pipeline could mask its exit code.
  undefined=$("$NM" -u "$combined")
  if [ -n "$undefined" ]; then
    echo "FAIL_UNRESOLVED_SYMBOL:$target $undefined" >&2; exit 1
  fi
  "$READELF" -h "$combined" > "$DIR/elf_header"
  "$READELF" -S "$combined" > "$DIR/elf_sections"
  grep -Eq 'Type:[[:space:]]*REL[[:space:]]' "$DIR/elf_header" || {
    echo "FAIL_NOT_RELOCATABLE:$target" >&2; exit 1;
  }
  case "$target" in
    armv7a-none-eabi) machine='Machine:[[:space:]]*ARM' ;;
    aarch64-none-elf) machine='Machine:[[:space:]]*AArch64' ;;
    x86_64-unknown-none) machine='Machine:.*X86-64' ;;
  esac
  grep -Eq "$machine" "$DIR/elf_header" || {
    echo "FAIL_WRONG_MACHINE:$target" >&2; exit 1;
  }
  if grep -Eq '\.eh_frame|\.init_array|\.fini_array|\.interp|\.dynamic' "$DIR/elf_sections"; then
    echo "FAIL_UNEXPECTED_RUNTIME_SECTION:$target" >&2; exit 1
  fi
  echo "L0_RELOCATABLE_NO_UNDEFINED:$target:PASS"
done
echo 'RESULT:PASS_SCOPE=HOSTED_KAT_PLUS_CROSS_OBJECT_NOT_BARE_METAL_BOOT'
