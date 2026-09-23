#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-artifacts/device-route}"
mkdir -p "${OUT_DIR}"

if ! command -v adb >/dev/null 2>&1; then
  echo "FAIL: adb not found" >&2
  exit 2
fi

mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
if [[ "${#devices[@]}" -ne 1 ]]; then
  echo "FAIL: expected exactly one authorized Android device/emulator, got ${#devices[@]}" >&2
  exit 3
fi

device="${devices[0]}"
abi="$(adb -s "${device}" shell getprop ro.product.cpu.abi | tr -d '\r')"
sdk="$(adb -s "${device}" shell getprop ro.build.version.sdk | tr -d '\r')"
manufacturer="$(adb -s "${device}" shell getprop ro.product.manufacturer | tr -d '\r')"
model="$(adb -s "${device}" shell getprop ro.product.model | tr -d '\r')"
fingerprint="$(adb -s "${device}" shell getprop ro.build.fingerprint | tr -d '\r')"
fingerprint_sha256="$(printf '%s' "${fingerprint}" | sha256sum | awk '{print $1}')"
head="$(git rev-parse HEAD)"
ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

set +e
./gradlew :app:connectedDevDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.rafgittools.workspace.ManifoldRouteNativeBridgeInstrumentedTest
status=$?
set -e

cat > "${OUT_DIR}/route-device-receipt.txt" <<EOF
receipt_version=1
timestamp_utc=${ts}
git_head=${head}
test_class=com.rafgittools.workspace.ManifoldRouteNativeBridgeInstrumentedTest
abi=${abi}
sdk=${sdk}
manufacturer=${manufacturer}
model=${model}
build_fingerprint_sha256=${fingerprint_sha256}
gradle_exit=${status}
claim_allowed=false
physical_route_device_pass=$([[ "${status}" -eq 0 ]] && echo true || echo false)
EOF

sha256sum "${OUT_DIR}/route-device-receipt.txt" > "${OUT_DIR}/route-device-receipt.txt.sha256"

if [[ "${status}" -ne 0 ]]; then
  echo "FAIL: device route instrumentation failed; receipt preserved at ${OUT_DIR}" >&2
  exit "${status}"
fi

echo "PASS: physical route instrumentation receipt at ${OUT_DIR}"
