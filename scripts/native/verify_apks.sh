#!/usr/bin/env bash
set -euo pipefail
shopt -s nullglob

APK_DIR="app/build/outputs/apk"
REPORT_DIR="app/build/reports/apk"
mkdir -p "$REPORT_DIR"
SIZES_REPORT="$REPORT_DIR/apk_sizes.tsv"
NATIVE_REPORT="$REPORT_DIR/apk_native_lib_sizes.tsv"
ABI_MD_REPORT="$REPORT_DIR/apk_native_abi_report.md"

REQUIRE_APKS="${REQUIRE_APKS:-false}"
VERIFY_STRICT_ABI="${VERIFY_STRICT_ABI:-false}"

ALL_APKS=("$APK_DIR"/*/*/*.apk)
UNSIGNED_APKS=("$APK_DIR"/*/release/*-unsigned.apk)

if [[ ${#ALL_APKS[@]} -eq 0 ]]; then
  echo "No APKs found. Run ./gradlew assembleDevDebug assembleProductionDebug first."
  if [[ "$REQUIRE_APKS" == "true" ]]; then
    exit 1
  fi
  exit 0
fi

: > "$SIZES_REPORT"
: > "$NATIVE_REPORT"
: > "$ABI_MD_REPORT"
printf 'apk\tvariant\tbuild_type\tsigned\tsize_bytes\n' >> "$SIZES_REPORT"
printf 'apk\tabi\tlib\tsize_bytes\n' >> "$NATIVE_REPORT"

{
  echo "# APK Native ABI Report"
  echo
} >> "$ABI_MD_REPORT"

check_apk_abis() {
  local apk="$1"
  unzip -l "$apk" | awk '{print $4}' | grep -E '^lib/(armeabi-v7a|arm64-v8a)/.+\.so$' | cut -d/ -f2 | sort -u
}

for apk in "${ALL_APKS[@]}"; do
  [[ -f "$apk" ]] || continue
  size=$(stat -c "%s" "$apk" 2>/dev/null || wc -c < "$apk")
  variant="$(echo "$apk" | awk -F/ '{print $(NF-2)}')"
  build_type="$(echo "$apk" | awk -F/ '{print $(NF-1)}')"
  signed_state="signed"
  [[ "$apk" == *"-unsigned.apk" ]] && signed_state="unsigned"

  printf '%s\t%s\t%s\t%s\t%s\n' "$apk" "$variant" "$build_type" "$signed_state" "$size" >> "$SIZES_REPORT"

  mapfile -t abis < <(check_apk_abis "$apk")
  libs=$(unzip -l "$apk" | awk '{print $4}' | grep -E '^lib/(armeabi-v7a|arm64-v8a)/.+\.so$' || true)

  missing=()
  printf '%s\n' "${abis[@]}" | grep -q '^armeabi-v7a$' || missing+=("armeabi-v7a")
  printf '%s\n' "${abis[@]}" | grep -q '^arm64-v8a$' || missing+=("arm64-v8a")

  {
    echo "## $apk"
    echo "- APK path: $apk"
    echo "- size: $size"
    echo "- signed/unsigned: $signed_state"
    echo "- ABIs found: ${abis[*]:-none}"
    echo "- native libs found:"
    if [[ -n "$libs" ]]; then
      while IFS= read -r libpath; do
        [[ -n "$libpath" ]] || continue
        echo "  - $libpath"
        abi="$(echo "$libpath" | cut -d/ -f2)"
        lib="$(basename "$libpath")"
        lib_size="$(unzip -l "$apk" "$libpath" | awk 'NR==4 {print $1}')"
        printf '%s\t%s\t%s\t%s\n' "$apk" "$abi" "$lib" "$lib_size" >> "$NATIVE_REPORT"
      done <<< "$libs"
    else
      echo "  - none"
    fi

    if [[ ${#missing[@]} -gt 0 ]]; then
      echo "- missing ABI warnings: ${missing[*]}"
    else
      echo "- missing ABI warnings: none"
    fi
    echo
  } >> "$ABI_MD_REPORT"

  if [[ "$VERIFY_STRICT_ABI" == "true" && ${#missing[@]} -gt 0 ]]; then
    echo "Strict ABI verification failed for $apk: missing ${missing[*]}" >&2
    exit 1
  fi

done

echo "Size report: $SIZES_REPORT"
echo "Native lib size report: $NATIVE_REPORT"
echo "ABI markdown report: $ABI_MD_REPORT"
