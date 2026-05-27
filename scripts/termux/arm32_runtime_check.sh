#!/usr/bin/env bash
set -euo pipefail

echo "== RafGitTools Termux ARM32 runtime check =="
echo "uname -m: $(uname -m 2>/dev/null || echo unknown)"
echo "PREFIX: ${PREFIX:-unset}"
echo "Android release: $(getprop ro.build.version.release 2>/dev/null || echo unknown)"
echo "SDK: $(getprop ro.build.version.sdk 2>/dev/null || echo unknown)"

case "$(uname -m 2>/dev/null || true)" in
  armv7*|armv8l) echo "ABI host: ARM32 compatible" ;;
  aarch64) echo "ABI host: ARM64; verify 32-bit userspace before installing armeabi-v7a-only APKs" ;;
  *) echo "WARN: unknown/non-ARM Android host" ;;
esac

for cmd in sh bash git clang unzip grep awk sed; do
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "OK: $cmd -> $(command -v "$cmd")"
  else
    echo "MISS: $cmd"
  fi
done

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  "$JAVA_HOME/bin/java" -version 2>&1 | head -n 1
elif [[ -x "${PREFIX:-/data/data/com.termux/files/usr}/lib/jvm/java-17-openjdk/bin/java" ]]; then
  "${PREFIX:-/data/data/com.termux/files/usr}/lib/jvm/java-17-openjdk/bin/java" -version 2>&1 | head -n 1
else
  echo "MISS: JDK 17. Install with: pkg install openjdk-17"
fi

cat <<'NEXT'
NEXT:
- Build APK on CI/desktop:
  ./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
- Validate APK ABI:
  ./scripts/native/verify_apks.sh
- Use Termux ARM32 for runtime/device validation, not default SDK bootstrap.
NEXT
