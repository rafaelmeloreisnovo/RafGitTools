#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob

APK_DIR="app/build/outputs/apk"
REPORT_DIR="app/build/reports/apk"
mkdir -p "$REPORT_DIR"
SIZES_REPORT="$REPORT_DIR/apk_sizes.tsv"
NATIVE_REPORT="$REPORT_DIR/apk_native_lib_sizes.tsv"

UNSIGNED_APKS=("$APK_DIR"/*/release/*-unsigned.apk)
ALL_APKS=("$APK_DIR"/*/*/*.apk)
MAX_SIZE_DIFF_BYTES="${MAX_SIZE_DIFF_BYTES:-0}"
MAX_APK_SIZE_BYTES="${MAX_APK_SIZE_BYTES:-0}"

if [[ ${#UNSIGNED_APKS[@]} -eq 0 ]]; then
  echo "No unsigned APKs found under $APK_DIR (signed-only lane)"
fi

: > "$SIZES_REPORT"
: > "$NATIVE_REPORT"
printf 'apk\tvariant\tbuild_type\tsigned\tsize_bytes\n' >> "$SIZES_REPORT"
printf 'apk\tabi\tlib\tsize_bytes\n' >> "$NATIVE_REPORT"

echo "== APK inventory (with size) =="
for apk in "${ALL_APKS[@]}"; do
  [[ -f "$apk" ]] || continue
  variant="$(echo "$apk" | awk -F/ '{print $(NF-2)}')"
  build_type="$(echo "$apk" | awk -F/ '{print $(NF-1)}')"
  signed_state="signed"
  [[ "$apk" == *"-unsigned.apk" ]] && signed_state="unsigned"
  size=$(stat -c '%s' "$apk")
  printf '%s\t%s\t%s\t%s\t%s\n' "$apk" "$variant" "$build_type" "$signed_state" "$size" >> "$SIZES_REPORT"
  echo "$apk | ${size} bytes | ${signed_state}"

  if [[ "$MAX_APK_SIZE_BYTES" -gt 0 && "$size" -gt "$MAX_APK_SIZE_BYTES" ]]; then
    echo "APK exceeds MAX_APK_SIZE_BYTES=$MAX_APK_SIZE_BYTES: $apk ($size)" >&2
    exit 1
  fi

done

check_apk_abis() {
  local apk="$1"
  unzip -l "$apk" | awk '{print $4}' | rg '^lib/(armeabi-v7a|arm64-v8a)/.+\.so$' | cut -d/ -f2 | sort -u
}

echo "== ABI validation =="
for apk in "${ALL_APKS[@]}"; do
  [[ -f "$apk" ]] || continue
  abis="$(check_apk_abis "$apk" | xargs)"
  echo "$apk -> $abis"
  if ! check_apk_abis "$apk" | rg -q '^armeabi-v7a$'; then
    echo "Missing armeabi-v7a in $apk" >&2
    exit 1
  fi
  if ! check_apk_abis "$apk" | rg -q '^arm64-v8a$'; then
    echo "Missing arm64-v8a in $apk" >&2
    exit 1
  fi

  while IFS= read -r libpath; do
    [[ -n "$libpath" ]] || continue
    abi="$(echo "$libpath" | cut -d/ -f2)"
    lib="$(basename "$libpath")"
    lib_size="$(unzip -l "$apk" "$libpath" | awk 'NR==4 {print $1}')"
    printf '%s\t%s\t%s\t%s\n' "$apk" "$abi" "$lib" "$lib_size" >> "$NATIVE_REPORT"
  done < <(unzip -l "$apk" | awk '{print $4}' | rg '^lib/(armeabi-v7a|arm64-v8a)/.+\.so$')
done

echo "== Signature validation =="
APKSIGNER_BIN=""
if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/build-tools/34.0.0/apksigner" ]]; then
  APKSIGNER_BIN="${ANDROID_SDK_ROOT}/build-tools/34.0.0/apksigner"
elif command -v apksigner >/dev/null 2>&1; then
  APKSIGNER_BIN="$(command -v apksigner)"
fi

for apk in "${ALL_APKS[@]}"; do
  [[ -f "$apk" ]] || continue
  if [[ "$apk" == *"-unsigned.apk" ]]; then
    if [[ -n "$APKSIGNER_BIN" ]]; then
      if "$APKSIGNER_BIN" verify "$apk" >/dev/null 2>&1; then
        echo "Unexpected signed status for unsigned APK: $apk" >&2
        exit 1
      fi
      echo "$apk -> unsigned (expected)"
    else
      echo "$apk -> unsigned (apksigner unavailable; filename-based)"
    fi
  else
    if [[ -n "$APKSIGNER_BIN" ]]; then
      "$APKSIGNER_BIN" verify "$apk"
      echo "$apk -> signed"
    else
      echo "$apk -> signed/unknown (apksigner unavailable; filename-based)"
    fi
  fi
done

echo "== Signed vs unsigned size diff =="
if [[ ${#UNSIGNED_APKS[@]} -eq 0 ]]; then
  echo "Skipping size diff: no unsigned APKs present."
fi
for unsigned_apk in "${UNSIGNED_APKS[@]}"; do
  base="$(basename "$unsigned_apk" | sed 's/-unsigned\.apk$//')"
  signed_apk="$(dirname "$unsigned_apk")/${base}.apk"
  if [[ -f "$signed_apk" ]]; then
    u_size=$(stat -c '%s' "$unsigned_apk")
    s_size=$(stat -c '%s' "$signed_apk")
    diff=$((s_size - u_size))
    echo "$base | unsigned=$u_size signed=$s_size diff=$diff"
    if [[ "$MAX_SIZE_DIFF_BYTES" -gt 0 && "$diff" -gt "$MAX_SIZE_DIFF_BYTES" ]]; then
      echo "Signed/unsigned diff exceeds MAX_SIZE_DIFF_BYTES=$MAX_SIZE_DIFF_BYTES for $base" >&2
      exit 1
    fi
  fi
done

echo "Size report: $SIZES_REPORT"
echo "Native lib size report: $NATIVE_REPORT"
