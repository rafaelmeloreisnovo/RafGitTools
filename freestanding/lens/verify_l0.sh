#!/bin/sh
# HOSTED verification only; this script is not part of L0.
set -eu
BASE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CC=${CC:-clang}
LD=${LD:-ld.lld}
NM=${NM:-nm}
READELF=${READELF:-readelf}
DIR=$(mktemp -d)
trap 'rm -rf "$DIR"' EXIT HUP INT TERM
CFLAGS="-std=c11 -Os -Wall -Wextra -Werror -ffreestanding -fno-builtin -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -ffunction-sections -fdata-sections"
for source in rfl_sha256.c rfl_lens.c rfl_graph.c; do
  if grep -Eq '^#[[:space:]]*include[[:space:]]*<' "$BASE/$source"; then
    echo "DENY_EXTERNAL_HEADER:$source" >&2; exit 1
  fi
done
"$CC" -std=c11 -Os -Wall -Wextra -Werror -I"$BASE" \
  "$BASE/rfl_sha256.c" "$BASE/rfl_lens.c" "$BASE/rfl_graph.c" \
  "$BASE/tests/test_l0.c" -o "$DIR/kat"
"$DIR/kat"
echo "KAT_AND_FALSIFIERS:PASS_HOST_ONLY"
for target in armv7a-none-eabi aarch64-none-elf x86_64-unknown-none; do
  for name in rfl_sha256 rfl_lens rfl_graph; do
    "$CC" --target="$target" $CFLAGS -nostdlib -c "$BASE/$name.c" -o "$DIR/${target}_${name}.o"
  done
  "$LD" -r "$DIR/${target}_rfl_sha256.o" "$DIR/${target}_rfl_lens.o" \
     "$DIR/${target}_rfl_graph.o" -o "$DIR/${target}_combined.o"
  if "$NM" -u "$DIR/${target}_combined.o" | grep .; then
    echo "FAIL_UNRESOLVED_SYMBOL:$target" >&2; exit 1
  fi
  if "$READELF" -S "$DIR/${target}_combined.o" | grep -Eq '\.eh_frame|\.init_array|\.fini_array'; then
    echo "FAIL_RUNTIME_SECTION:$target" >&2; exit 1
  fi
  echo "L0_NO_UNDEFINED:$target:PASS"
done
echo "RESULT:PASS_SCOPE=HOSTED_KAT_PLUS_CROSS_OBJECT_NOT_BARE_METAL_BOOT"
