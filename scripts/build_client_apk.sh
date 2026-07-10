#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DIST_DIR="${DIST_DIR:-$ROOT_DIR/dist/rafgittools}"
GRADLE_RUNNER="$ROOT_DIR/scripts/gradlew_with_java17.sh"

log() {
  printf '[RafGitTools] %s\n' "$*"
}

fail() {
  printf '[RafGitTools] ERROR: %s\n' "$*" >&2
  exit 1
}

[[ -f "$ROOT_DIR/gradlew" ]] || fail "gradlew não encontrado em $ROOT_DIR"
[[ -f "$GRADLE_RUNNER" ]] || fail "scripts/gradlew_with_java17.sh não encontrado"

if [[ ! -f "$ROOT_DIR/local.properties" && -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  fail "Android SDK não configurado. Defina ANDROID_HOME/ANDROID_SDK_ROOT ou crie local.properties com sdk.dir=/caminho/do/sdk"
fi

mkdir -p "$DIST_DIR"
rm -f "$DIST_DIR"/*.apk "$DIST_DIR"/SHA256SUMS.txt "$DIST_DIR"/BUILD_INFO.txt

log "Java/Gradle"
bash "$GRADLE_RUNNER" --version

log "Testes de autenticação"
bash "$GRADLE_RUNNER" --no-daemon \
  testDevDebugUnitTest \
  --tests 'com.rafgittools.ui.screens.auth.*' \
  --stacktrace

log "Testes unitários devDebug"
bash "$GRADLE_RUNNER" --no-daemon testDevDebugUnitTest --stacktrace

log "Lint devDebug"
bash "$GRADLE_RUNNER" --no-daemon lintDevDebug --stacktrace

log "Montagem do APK devDebug"
bash "$GRADLE_RUNNER" --no-daemon assembleDevDebug --stacktrace

APK="$(find "$ROOT_DIR/app/build/outputs/apk/dev/debug" -type f -name '*.apk' -print -quit 2>/dev/null || true)"
[[ -n "$APK" ]] || fail "assembleDevDebug terminou sem localizar APK"
[[ -s "$APK" ]] || fail "APK localizado, porém vazio: $APK"

cp -f "$APK" "$DIST_DIR/RafGitTools-devDebug.apk"
(
  cd "$DIST_DIR"
  sha256sum RafGitTools-devDebug.apk > SHA256SUMS.txt
)

{
  printf 'commit=%s\n' "$(git rev-parse HEAD 2>/dev/null || printf 'TOKEN_VAZIO')"
  printf 'built_at_utc=%s\n' "$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  printf 'variant=devDebug\n'
  printf 'oauth_dev_client_configured=%s\n' "$([[ -n "${GITHUB_CLIENT_ID_DEV:-}" ]] && printf true || printf false)"
  printf 'apk=%s\n' "$DIST_DIR/RafGitTools-devDebug.apk"
} > "$DIST_DIR/BUILD_INFO.txt"

log "Build concluído"
ls -lh "$DIST_DIR/RafGitTools-devDebug.apk" "$DIST_DIR/SHA256SUMS.txt" "$DIST_DIR/BUILD_INFO.txt"
cat "$DIST_DIR/SHA256SUMS.txt"
