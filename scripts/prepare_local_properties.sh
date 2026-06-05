#!/usr/bin/env bash
set -euo pipefail

is_termux_android_host() {
  [[ -n "${PREFIX:-}" && "$PREFIX" == /data/data/com.termux/files/usr* ]] || [[ -d /data/data/com.termux/files/usr ]]
}

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

  # `yes` exits with SIGPIPE after sdkmanager finishes reading answers.
  # Preserve pipefail for the script while checking sdkmanager's status, not the
  # expected SIGPIPE from yes.
  set +o pipefail
  yes | sdkmanager --licenses >/dev/null
  local license_status=${PIPESTATUS[1]}
  set -o pipefail
  if [[ "$license_status" -ne 0 ]]; then
    return "$license_status"
  fi

  sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;26.3.11579264" "cmake;3.22.1"
}

if ! sdk_path="$(resolve_sdk_path)"; then
  if is_termux_android_host; then
    echo "Termux/Android host detected without a valid Android SDK path." >&2
    echo "Refusing to bootstrap Linux desktop command-line tools in Termux." >&2
    echo "Use a preconfigured ANDROID_SDK_ROOT/ANDROID_HOME, run builds on desktop/CI," >&2
    echo "or run ./scripts/termux_arm32_runtime_check.sh for runtime/toolchain diagnostics." >&2
    exit 2
  fi

  install_sdk_if_missing
  sdk_path="$ANDROID_SDK_ROOT"
fi

export ANDROID_SDK_ROOT="$sdk_path"
export ANDROID_HOME="$sdk_path"
printf 'sdk.dir=%s\n' "$sdk_path" > local.properties
echo "Generated local.properties with sdk.dir=$sdk_path"
