#!/bin/sh
# RafGitTools authorial independent receipt validator. Host tools ONLY.
set -eu
D=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
: "${CC:=cc}" "${CLANG:=clang}" "${CXX:=c++}" "${NM:=nm}"
for T in "$CC" "$CLANG" "$CXX" "$NM"; do
 command -v "$T" >/dev/null 2>&1 || { echo "TOKEN_VAZIO_TOOLCHAIN_UNAVAILABLE=$T";exit 77; }
done
TMP=$(mktemp -d "${TMPDIR:-/tmp}/rafgit-g4.XXXXXXXX")
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
"$CC" -std=c11 -O2 -Wall -Wextra -Werror -pedantic -I"$D" "$D/test_g4_self.c" -o "$TMP/self"
"$TMP/self"
printf '#include "rafgit_ci_g4_verify.h"\nint main(){return RAFGIT_CI_G4_BYTES==32u?0:1;}\n' > "$TMP/cpp.cpp"
"$CXX" -std=c++17 -Wall -Wextra -Werror -pedantic -I"$D" "$TMP/cpp.cpp" -o "$TMP/cpp"
"$TMP/cpp"
for TARGET in x86_64-unknown-none i686-unknown-none armv7a-none-eabi aarch64-none-elf riscv32-unknown-elf riscv64-unknown-elf; do
 "$CLANG" -target "$TARGET" -std=c11 -Os -Wall -Wextra -Werror -ffreestanding -fno-builtin -fno-stack-protector -nostdinc -I"$D" -c "$D/test_g4_self.c" -o "$TMP/$TARGET.o"
 [ -z "$("$NM" -u "$TMP/$TARGET.o")" ] || { echo "FAIL_UNDEFINED=$TARGET";exit 1; }
 echo "PASS_RECEIPT_NO_UNDEFINED=$TARGET"
done
echo "PASS_LOCAL_SCOPED SOURCE=UNVERIFIED PROVIDER_CI=NOT_RUN CLAIM_ALLOWED=false PROMOTION=HOLD"
