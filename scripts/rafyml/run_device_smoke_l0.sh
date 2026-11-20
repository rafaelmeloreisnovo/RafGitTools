#!/bin/sh
# Manual Android/Termux execution, only after local review.
# No device IDs, secrets, network activity, storage traversal, or root required.
set -eu
cd "$(dirname "$0")"
command -v uname >/dev/null 2>&1 || { echo 'BLOCKED_UNAME_MISSING'; exit 2; }
command -v sha256sum >/dev/null 2>&1 || { echo 'BLOCKED_SHA256_TOOL_MISSING'; exit 2; }
test -f SHA256SUMS || { echo 'BLOCKED_MANIFEST_MISSING'; exit 2; }
if ! sha256sum -c SHA256SUMS >/dev/null 2>&1; then
  echo 'BLOCKED_BYTES_MISMATCH'; exit 4
fi
arch=$(uname -m)
case "$arch" in
  armv7l|armv7*|armv8l) binary=armv7-smoke.elf; abi=armv7 ;;
  aarch64|arm64) binary=arm64-smoke.elf; abi=aarch64 ;;
  *) printf 'BLOCKED_ARCH_UNSUPPORTED=%s\n' "$arch"; exit 3 ;;
esac
test -f "$binary" || { echo 'BLOCKED_ELF_MISSING'; exit 2; }
chmod 700 "$binary" || { echo 'BLOCKED_EXEC_PERMISSION'; exit 3; }
set +e
"./$binary"
result=$?
set -e
sha=$(sha256sum "$binary" | sed 's/ .*//')
if [ "$result" -eq 0 ]; then state=PASS_SCOPED_DEVICE_EXIT_ZERO; else state=FAIL_DEVICE_EXIT_NONZERO; fi
# Minimal private append-only receipt in test directory, no serial/GNSS/etc.
if command -v date >/dev/null 2>&1; then timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ); else timestamp=TOKEN_VAZIO; fi
printf 'utc=%s abi=%s binary=%s sha256=%s exit=%s state=%s claim_allowed=false\n' \
  "$timestamp" "$abi" "$binary" "$sha" "$result" "$state" >> device_receipts.log
printf 'DEVICE_RESULT=%s ABI=%s EXIT=%s SHA256=%s\n' "$state" "$abi" "$result" "$sha"
if [ "$result" -ne 0 ]; then exit 1; fi
