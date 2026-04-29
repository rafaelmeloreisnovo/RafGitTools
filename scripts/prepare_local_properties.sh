#!/usr/bin/env bash
set -euo pipefail

sdk_path="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

if [[ -z "$sdk_path" ]]; then
  echo "ANDROID_SDK_ROOT/ANDROID_HOME is not set" >&2
  exit 1
fi

if [[ ! -d "$sdk_path" ]]; then
  echo "Android SDK path does not exist: $sdk_path" >&2
  exit 1
fi

printf 'sdk.dir=%s\n' "$sdk_path" > local.properties
echo "Generated local.properties with sdk.dir=$sdk_path"
