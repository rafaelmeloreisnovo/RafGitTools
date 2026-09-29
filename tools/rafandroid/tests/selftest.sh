#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAF="$ROOT/rafandroid"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

bash -n "$RAF"
test "$("$RAF" version)" = "1.0.0"
assert_output_contains() {
  local needle="$1"
  shift
  local output
  output="$("$@")"
  grep -Fq -- "$needle" <<<"$output"
}

assert_output_contains 'D8 -> DEX' "$RAF" graph
assert_output_contains 'silicon-gate' "$RAF" help
assert_output_contains 'network_implicit=false' "$RAF" provision-plan
assert_output_contains 'claim_allowed=false' "$RAF" probe
assert_output_contains 'RAFANDROID_SHADOW_REPORT_V1' "$RAF" shadow
assert_output_contains 'tail_state=INVENTORY_ONLY' "$RAF" tails
assert_output_contains 'numeric_score=TOKEN_VAZIO_UNCALIBRATED' "$RAF" friction
assert_output_contains 'boot_claim=TOKEN_VAZIO' "$RAF" qemu
"$RAF" receipt > "$TMP/receipt.json"
python3 -m json.tool "$TMP/receipt.json" >/dev/null

"$RAF" scaffold "$TMP/minimal" io.rafaelia.minimal
! grep -R -E '__PACKAGE__|__APPID__|__JNI_PACKAGE__' "$TMP/minimal"
test -f "$TMP/minimal/app/src/main/cpp/silicon_light/raf_silicon_light.c"
test -f "$TMP/minimal/app/src/main/cpp/silicon_light/raf_silicon_light.h"
grep -q 'silicon_light_l0=EMBEDDED_FROM_CANONICAL_SOURCE' <("$RAF" scaffold "$TMP/minimal2" io.rafaelia.minimal2)

clang -std=c11 -O2 -ffreestanding -fno-builtin -Wall -Wextra -Werror \
  -c "$TMP/minimal/app/src/main/cpp/core.c" -o "$TMP/core.o"
nm -u "$TMP/core.o" > "$TMP/core.undefined"
test ! -s "$TMP/core.undefined"

clang -nostdinc -I"$TMP/minimal/app/src/main/cpp/silicon_light" \
  -DRAF_SILICON_LIGHT_FREESTANDING=1 -std=c11 -Os -ffreestanding -fno-builtin \
  -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables \
  -fno-vectorize -fno-slp-vectorize -Wall -Wextra -Werror \
  -c "$TMP/minimal/app/src/main/cpp/silicon_light/raf_silicon_light.c" -o "$TMP/silicon.o"
nm -u "$TMP/silicon.o" > "$TMP/silicon.undefined"
test ! -s "$TMP/silicon.undefined"

grep -q 'android.useAndroidX=false' "$TMP/minimal/gradle.properties"
grep -q "abiFilters 'armeabi-v7a', 'arm64-v8a'" "$TMP/minimal/app/build.gradle"
grep -q 'minifyEnabled true' "$TMP/minimal/app/build.gradle"

echo "RAFANDROID_SELFTEST=PASS"
