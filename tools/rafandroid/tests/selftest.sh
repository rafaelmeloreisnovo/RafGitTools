#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RAF="$ROOT/rafandroid"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

bash -n "$RAF"
test "$("$RAF" version)" = "1.0.0"
"$RAF" graph | grep -q 'D8 -> DEX'
"$RAF" provision-plan | grep -q 'network_implicit=false'
"$RAF" probe | grep -q 'claim_allowed=false'
"$RAF" shadow | grep -q 'RAFANDROID_SHADOW_REPORT_V1'
"$RAF" tails | grep -q 'tail_state=INVENTORY_ONLY'
"$RAF" friction | grep -q 'numeric_score=TOKEN_VAZIO_UNCALIBRATED'
"$RAF" qemu | grep -q 'boot_claim=TOKEN_VAZIO'
"$RAF" receipt > "$TMP/receipt.json"
python3 -m json.tool "$TMP/receipt.json" >/dev/null

"$RAF" scaffold "$TMP/minimal" io.rafaelia.minimal
! grep -R -E '__PACKAGE__|__APPID__|__JNI_PACKAGE__' "$TMP/minimal"

clang -std=c11 -O2 -ffreestanding -fno-builtin -Wall -Wextra -Werror \
  -c "$TMP/minimal/app/src/main/cpp/core.c" -o "$TMP/core.o"
nm -u "$TMP/core.o" > "$TMP/core.undefined"
test ! -s "$TMP/core.undefined"

grep -q 'android.useAndroidX=false' "$TMP/minimal/gradle.properties"
grep -q "abiFilters 'armeabi-v7a', 'arm64-v8a'" "$TMP/minimal/app/build.gradle"
grep -q 'minifyEnabled true' "$TMP/minimal/app/build.gradle"

echo "RAFANDROID_SELFTEST=PASS"
