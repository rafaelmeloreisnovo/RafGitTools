#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob

APK_DIR="app/build/outputs/apk"
UNSIGNED_APKS=("$APK_DIR"/*/release/*-unsigned.apk)
SIGNED_APKS=("$APK_DIR"/*/release/*.apk)

if [[ ${#UNSIGNED_APKS[@]} -eq 0 ]]; then
  echo "No unsigned APKs found under $APK_DIR"
  exit 1
fi

echo "== APK inventory =="
for apk in "$APK_DIR"/*/*/*.apk; do
  [[ -f "$apk" ]] || continue
  size=$(stat -c '%s' "$apk")
  echo "$apk | ${size} bytes"
done

check_apk_abis() {
  local apk="$1"
  unzip -l "$apk" | awk '{print $4}' | rg '^lib/(armeabi-v7a|arm64-v8a)/.+\.so$' | cut -d/ -f2 | sort -u
}

echo "== ABI validation =="
for apk in "$APK_DIR"/*/*/*.apk; do
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
done

echo "== Signature validation =="
APKSIGNER_BIN=""
if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "${ANDROID_SDK_ROOT}/build-tools/34.0.0/apksigner" ]]; then
  APKSIGNER_BIN="${ANDROID_SDK_ROOT}/build-tools/34.0.0/apksigner"
elif command -v apksigner >/dev/null 2>&1; then
  APKSIGNER_BIN="$(command -v apksigner)"
fi

for apk in "$APK_DIR"/*/*/*.apk; do
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

if [[ ${#SIGNED_APKS[@]} -gt 0 ]]; then
  echo "== Signed vs unsigned size diff =="
  for unsigned_apk in "${UNSIGNED_APKS[@]}"; do
    base="$(basename "$unsigned_apk" | sed 's/-unsigned\.apk$//')"
    signed_apk="$(dirname "$unsigned_apk")/${base}.apk"
    if [[ -f "$signed_apk" ]]; then
      u_size=$(stat -c '%s' "$unsigned_apk")
      s_size=$(stat -c '%s' "$signed_apk")
      diff=$((s_size - u_size))
      echo "$base | unsigned=$u_size signed=$s_size diff=$diff"
    fi
  done
fi
