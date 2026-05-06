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

install_sdk_if_missing() {
  local sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
  local cmdline_zip="/tmp/android-commandlinetools.zip"
  local tools_dir="${sdk_root}/cmdline-tools/latest"

  mkdir -p "$sdk_root"

  if [[ ! -x "${tools_dir}/bin/sdkmanager" ]]; then
    echo "Android SDK not found locally. Bootstrapping command-line tools into $sdk_root"
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$cmdline_zip"
    rm -rf "${sdk_root}/cmdline-tools/latest"
    mkdir -p "${sdk_root}/cmdline-tools/latest"
    unzip -q "$cmdline_zip" -d "${sdk_root}/cmdline-tools/latest"
    if [[ -d "${sdk_root}/cmdline-tools/latest/cmdline-tools" ]]; then
      mv "${sdk_root}/cmdline-tools/latest/cmdline-tools"/* "${sdk_root}/cmdline-tools/latest/"
      rmdir "${sdk_root}/cmdline-tools/latest/cmdline-tools"
    fi
  fi

  export ANDROID_SDK_ROOT="$sdk_root"
  export ANDROID_HOME="$sdk_root"
  export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

  yes | sdkmanager --licenses >/dev/null
  sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;26.3.11579264" "cmake;3.22.1"
}

if ! sdk_path="$(resolve_sdk_path)"; then
  install_sdk_if_missing
  sdk_path="$ANDROID_SDK_ROOT"
fi

export ANDROID_SDK_ROOT="$sdk_path"
export ANDROID_HOME="$sdk_path"
printf 'sdk.dir=%s\n' "$sdk_path" > local.properties
echo "Generated local.properties with sdk.dir=$sdk_path"
