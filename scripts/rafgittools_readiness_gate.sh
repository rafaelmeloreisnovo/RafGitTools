#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

STAMP="$(date -u '+%Y%m%dT%H%M%SZ')"
RECEIPT_DIR="${RAFGITTOOLS_RECEIPTS_DIR:-$ROOT/.rafgittools/receipts}"
RECEIPT="$RECEIPT_DIR/readiness-$STAMP.tsv"
mkdir -p "$RECEIPT_DIR"

PASS_COUNT=0
FAIL_COUNT=0
TV_COUNT=0
SKIP_COUNT=0

record() {
  local id="$1" state="$2" detail="$3"
  detail="${detail//$'\t'/ }"
  detail="${detail//$'\n'/ }"
  printf '%s\t%s\t%s\n' "$id" "$state" "$detail" | tee -a "$RECEIPT"
  case "$state" in
    PASS) PASS_COUNT=$((PASS_COUNT + 1)) ;;
    FAIL) FAIL_COUNT=$((FAIL_COUNT + 1)) ;;
    TOKEN_VAZIO) TV_COUNT=$((TV_COUNT + 1)) ;;
    SKIP) SKIP_COUNT=$((SKIP_COUNT + 1)) ;;
  esac
}

has() { command -v "$1" >/dev/null 2>&1; }

printf 'gate\tstate\tdetail\n' > "$RECEIPT"
record META PASS "schema=RAFGITTOOLS_READINESS_RECEIPT_V1 observed_at=$STAMP host=$(uname -m 2>/dev/null || echo unknown)"

# G0 — repository integrity / required anchors.
missing=""
for path in \
  app/src/main/AndroidManifest.xml \
  app/src/main/kotlin/com/rafgittools/data/auth/AuthRepository.kt \
  app/src/main/kotlin/com/rafgittools/data/auth/OAuthDeviceFlowManager.kt \
  app/src/main/kotlin/com/rafgittools/data/auth/GhCliAuthImporter.kt \
  app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt \
  contracts/job-v1.schema.json \
  ECOSYSTEM_RUNTIME_STATE.json \
  scripts/validate_runtime_truth.py \
  scripts/gradlew_with_java17.sh \
  gradlew
  do
    [[ -e "$path" ]] || missing+="$path;"
  done
if [[ -z "$missing" ]]; then
  record G0_REPO_ANCHORS PASS "required source/contracts/build anchors present"
else
  record G0_REPO_ANCHORS FAIL "missing=$missing"
fi

# G1 — repository truth validator; no network and no Android SDK required.
if has python3; then
  if out="$(python3 scripts/validate_runtime_truth.py 2>&1)"; then
    record G1_RUNTIME_TRUTH PASS "$out"
  else
    record G1_RUNTIME_TRUTH FAIL "$out"
  fi
else
  record G1_RUNTIME_TRUTH TOKEN_VAZIO "python3 unavailable; validator not executed"
fi

# G2 — authentication paths are source-present. OAuth client id is deliberately
# not required because PAT and gh-import remain valid bootstrap paths.
auth_missing=""
for path in \
  app/src/main/kotlin/com/rafgittools/data/auth/AuthRepository.kt \
  app/src/main/kotlin/com/rafgittools/data/auth/OAuthDeviceFlowManager.kt \
  app/src/main/kotlin/com/rafgittools/data/auth/GhCliAuthImporter.kt
  do
    [[ -s "$path" ]] || auth_missing+="$path;"
  done
if [[ -z "$auth_missing" ]]; then
  record G2_AUTH_SURFACE PASS "PAT + OAuth Device Flow + gh CLI import sources present; OAuth Client ID remains runtime configuration"
else
  record G2_AUTH_SURFACE FAIL "missing=$auth_missing"
fi

# G3 — Termux/device host diagnostics. This is diagnostic, not a claim that the
# Android app itself compiled or executed.
if [[ -n "${PREFIX:-}" && "$PREFIX" == /data/data/com.termux/files/usr* ]] || [[ -d /data/data/com.termux/files/usr ]]; then
  if out="$(bash scripts/termux_arm32_runtime_check.sh 2>&1)"; then
    record G3_TERMUX_HOST PASS "$out"
  else
    record G3_TERMUX_HOST FAIL "$out"
  fi
else
  record G3_TERMUX_HOST SKIP "not running on a detected Termux/Android host"
fi

# G4 — JDK 17 evidence.
java_bin=""
if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  java_bin="${JAVA_HOME}/bin/java"
elif has java; then
  java_bin="$(command -v java)"
fi
if [[ -n "$java_bin" ]]; then
  java_version="$($java_bin -version 2>&1 | head -n 1)"
  if [[ "$java_version" =~ \"17\.|\"17\" ]]; then
    record G4_JDK17 PASS "$java_version"
  else
    record G4_JDK17 TOKEN_VAZIO "JDK 17 not active; detected=$java_version"
  fi
else
  record G4_JDK17 TOKEN_VAZIO "java unavailable"
fi

# G5 — Android SDK evidence without downloading anything.
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -n "$sdk" && -d "$sdk" ]]; then
  record G5_ANDROID_SDK PASS "sdk_root=$sdk"
else
  record G5_ANDROID_SDK TOKEN_VAZIO "ANDROID_SDK_ROOT/ANDROID_HOME unavailable; no automatic network bootstrap performed"
fi

# G6 — Unit tests. Execute only when JDK17 + SDK are already available locally.
if grep -q $'^G4_JDK17\tPASS\t' "$RECEIPT" && grep -q $'^G5_ANDROID_SDK\tPASS\t' "$RECEIPT"; then
  if out="$(./scripts/gradlew_with_java17.sh testDevDebugUnitTest testProductionDebugUnitTest --no-daemon 2>&1)"; then
    record G6_UNIT_TESTS PASS "$out"
  else
    record G6_UNIT_TESTS FAIL "$out"
  fi
else
  record G6_UNIT_TESTS TOKEN_VAZIO "not executed because JDK17 and/or Android SDK evidence is missing"
fi

# G7 — Debug APK build. Same rule: use only the already configured local toolchain.
if grep -q $'^G6_UNIT_TESTS\tPASS\t' "$RECEIPT"; then
  if out="$(./scripts/gradlew_with_java17.sh assembleDevDebug --no-daemon 2>&1)"; then
    record G7_APK_BUILD PASS "$out"
  else
    record G7_APK_BUILD FAIL "$out"
  fi
else
  record G7_APK_BUILD TOKEN_VAZIO "not executed until unit-test gate passes"
fi

# G8 — artifact hash. Never invent a path; search only Gradle's canonical output.
apk="$(find app/build/outputs/apk/dev/debug -maxdepth 1 -type f -name '*.apk' 2>/dev/null | sort | head -n 1 || true)"
if [[ -n "$apk" && -f "$apk" ]]; then
  if has sha256sum; then
    apk_sha="$(sha256sum "$apk" | awk '{print $1}')"
    apk_bytes="$(wc -c < "$apk" | tr -d ' ')"
    record G8_APK_SHA256 PASS "path=$apk bytes=$apk_bytes sha256=$apk_sha"
  else
    record G8_APK_SHA256 TOKEN_VAZIO "APK exists but sha256sum is unavailable"
  fi
else
  record G8_APK_SHA256 TOKEN_VAZIO "no devDebug APK found in canonical Gradle output"
fi

# G9 — connected device presence only. Installation/smoke is deliberately opt-in.
if has adb; then
  serial="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')"
  if [[ -n "$serial" ]]; then
    abi="$(adb -s "$serial" shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')"
    sdk_device="$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
    record G9_DEVICE_PRESENT PASS "serial=$serial abi=$abi sdk=$sdk_device"
  else
    record G9_DEVICE_PRESENT TOKEN_VAZIO "adb available but no authorized device is connected"
  fi
else
  record G9_DEVICE_PRESENT TOKEN_VAZIO "adb unavailable"
fi

# G10 — optional physical smoke. It is never run by surprise.
if [[ "${RAFGITTOOLS_DEVICE_SMOKE:-0}" == "1" ]]; then
  if [[ -n "${serial:-}" && -n "$apk" && -f "$apk" ]]; then
    if install_out="$(adb -s "$serial" install -r "$apk" 2>&1)" && \
       start_out="$(adb -s "$serial" shell am start -W -n com.rafgittools.dev/com.rafgittools.MainActivity 2>&1)"; then
      record G10_DEVICE_SMOKE PASS "install=$install_out start=$start_out"
    else
      record G10_DEVICE_SMOKE FAIL "install=${install_out:-not-run} start=${start_out:-not-run}"
    fi
  else
    record G10_DEVICE_SMOKE FAIL "requested but authorized device and/or APK is missing"
  fi
else
  record G10_DEVICE_SMOKE TOKEN_VAZIO "not requested; set RAFGITTOOLS_DEVICE_SMOKE=1 for explicit install/start smoke"
fi

# Final claim boundary: source readiness can be PASS without claiming APK/device.
if [[ "$FAIL_COUNT" -gt 0 ]]; then
  final="FAIL"
  claim="false"
elif grep -q $'^G7_APK_BUILD\tPASS\t' "$RECEIPT" && \
     grep -q $'^G8_APK_SHA256\tPASS\t' "$RECEIPT" && \
     grep -q $'^G10_DEVICE_SMOKE\tPASS\t' "$RECEIPT"; then
  final="VERIFIED_DEVICE"
  claim="true"
else
  final="VERIFIED_LIMITED"
  claim="false"
fi

record FINAL PASS "state=$final claim_allowed=$claim pass=$PASS_COUNT fail=$FAIL_COUNT token_vazio=$TV_COUNT skip=$SKIP_COUNT"

if has sha256sum; then
  sha256sum "$RECEIPT" > "$RECEIPT.sha256"
  printf 'receipt=%s\nreceipt_sha256=%s\n' "$RECEIPT" "$(awk '{print $1}' "$RECEIPT.sha256")"
else
  printf 'receipt=%s\nreceipt_sha256=TOKEN_VAZIO\n' "$RECEIPT"
fi

[[ "$FAIL_COUNT" -eq 0 ]]
