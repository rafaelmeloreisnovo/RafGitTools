#!/usr/bin/env bash
set -euo pipefail

resolve_sdk_path() {
  local candidates=(
    "${ANDROID_SDK_ROOT:-}"
    "${ANDROID_HOME:-}"
    "/opt/android-sdk"
    "/usr/local/lib/android/sdk"
    "$HOME/Android/Sdk"
  )

  local c
  for c in "${candidates[@]}"; do
    if [[ -n "$c" && -d "$c" ]]; then
      printf '%s\n' "$c"
      return 0
    fi
  done

  return 1
}

if ! sdk_path="$(resolve_sdk_path)"; then
  echo "ANDROID_SDK_ROOT/ANDROID_HOME is not set and no default SDK path was found." >&2
  echo "Checked: /opt/android-sdk, /usr/local/lib/android/sdk, ~/Android/Sdk" >&2
  exit 1
fi

export ANDROID_SDK_ROOT="$sdk_path"
export ANDROID_HOME="$sdk_path"
printf 'sdk.dir=%s\n' "$sdk_path" > local.properties
echo "Generated local.properties with sdk.dir=$sdk_path"
