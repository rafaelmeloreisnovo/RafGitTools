#!/bin/sh
# Hosted hermetic bridge, NOT an ELF runtime dependency.
# Existing ApkC builder is unchanged; this produces a separate unsigned artifact.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
PRODUCER=${RAFB_PRODUCER_ROOT:-"$ROOT/../ninja_Rafcode"}
PIN=f9822388a2909aa54ce4d959a861686bd96aab97
OUT=${RAFB_OUTPUT_DIR:-"$ROOT/dist/rafb-auto"}
for cmd in git clang readelf sha256sum python3; do
  command -v "$cmd" >/dev/null 2>&1 || { echo "BLOCKED missing host tool: $cmd" >&2;exit 1; }
done
[ -d "$PRODUCER/.git" ] || { echo 'BLOCKED missing producer checkout' >&2;exit 1; }
[ "$(git -C "$PRODUCER" rev-parse HEAD)" = "$PIN" ] || {
  echo "BLOCKED producer HEAD mismatch; expected $PIN" >&2; exit 1; }
[ -z "$(git -C "$PRODUCER" status --porcelain -- rafcode/build)" ] || {
  echo 'BLOCKED producer worktree modified' >&2; exit 1; }
check_digest() {
  file=$1 expected=$2
  [ -f "$file" ] || { echo "BLOCKED missing $file" >&2;exit 1; }
  actual=$(sha256sum "$file"); actual=${actual%% *}
  [ "$actual" = "$expected" ] || { echo "BLOCKED producer source hash mismatch $file" >&2;exit 1; }
}
check_digest "$PRODUCER/rafcode/build/src/rafb.c" 3f701ff52a11271fb7b2fb3f2d4bd66058cc83737825a50579e590676ab31247
check_digest "$PRODUCER/rafcode/build/include/rafb.h" 4a6d198b4b94483cd7139becf8b7fdf47e95b715df109660712d556ddd9b508b
check_digest "$PRODUCER/rafcode/build/elf/rafb_elf.c" 4c047f846da4ca712dab0527b6fa8cdf7f4a95f820638cf5c8935fae431d99fb
check_digest "$PRODUCER/rafcode/build/elf/rafb_elf.h" ac2adfce0397dc1827359914eecef09b438898814c48791e41f6fe9ea89b7a89
mkdir -p "$OUT/native"
for spec in armeabi-v7a:armv7a-linux-androideabi24 arm64-v8a:aarch64-linux-android24; do
  abi=${spec%%:*};target=${spec#*:}
  clang --target="$target" -Oz -ffreestanding -fPIC -fno-builtin -fno-stack-protector \
    -nostdinc -I"$PRODUCER/rafcode/build/include" -I"$PRODUCER/rafcode/build/elf" \
    -c "$PRODUCER/rafcode/build/src/rafb.c" -o "$OUT/native/core-$abi.o"
  clang --target="$target" -Oz -ffreestanding -fPIC -fno-builtin -fno-stack-protector \
    -nostdinc -I"$PRODUCER/rafcode/build/include" -I"$PRODUCER/rafcode/build/elf" \
    -c "$PRODUCER/rafcode/build/elf/rafb_elf.c" -o "$OUT/native/elf-$abi.o"
  clang --target="$target" -nostdlib -fuse-ld=lld -shared \
    -Wl,--no-undefined -Wl,-soname,librafb.so -Wl,-z,relro,-z,now \
    "$OUT/native/core-$abi.o" "$OUT/native/elf-$abi.o" -o "$OUT/native/librafb-$abi.so"
  readelf -h "$OUT/native/librafb-$abi.so" | grep -F 'DYN (Shared object file)' >/dev/null || exit 1
  if readelf -d "$OUT/native/librafb-$abi.so" | grep 'NEEDED'; then
    echo "BLOCKED unexpected DT_NEEDED $abi" >&2;exit 1
  fi
done
# Delegate signed-APK-independent bootstrap generation to owner RafPolimata.
sh "$ROOT/scripts/termux/build_apkc_hermetic.sh" --abi both --unsigned --out "$OUT/bootstrap"
BASE="$OUT/bootstrap/rafgittools_hermetic-unsigned.apk"
[ -s "$BASE" ] || { echo 'BLOCKED missing bootstrap APK' >&2;exit 1; }
# Separate, new unsigned package. Requires human-controlled signing gate later.
python3 "$ROOT/scripts/termux/rafb_merge_unsigned.py" "$BASE" \
  "$OUT/native/librafb-armeabi-v7a.so" "$OUT/native/librafb-arm64-v8a.so" \
  "$OUT/rafgittools-rafb-dual-unsigned.apk"
printf 'PRODUCER_PIN=%s\nOUTPUT=%s\n' "$PIN" "$OUT/rafgittools-rafb-dual-unsigned.apk"
