#!/usr/bin/env bash
set -euo pipefail

detect_java17_home() {
  if command -v mise >/dev/null 2>&1; then
    local mise_path
    mise_path="$(mise where java@17 2>/dev/null || true)"
    if [[ -n "$mise_path" && -x "$mise_path/bin/java" ]]; then
      printf '%s\n' "$mise_path"
      return 0
    fi
  fi

  local candidate
  for candidate in \
    /usr/lib/jvm/java-17-openjdk-amd64 \
    /usr/lib/jvm/temurin-17-jdk-amd64 \
    /usr/lib/jvm/zulu-17-amd64 \
    "$HOME/.sdkman/candidates/java/current" \
    "$HOME/.sdkman/candidates/java/17.0.*/" \
    "$HOME/.local/share/mise/installs/java/17" \
    "$HOME/.local/share/mise/installs/java/17.0.2"
  do
    # shellcheck disable=SC2086
    for expanded in $candidate; do
      if [[ -x "$expanded/bin/java" ]]; then
        printf '%s\n' "$expanded"
        return 0
      fi
    done
  done

  return 1
}

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version >/dev/null 2>&1; then
  if detected_java_home="$(detect_java17_home)"; then
    export JAVA_HOME="$detected_java_home"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "Using detected JDK 17 at JAVA_HOME=$JAVA_HOME" >&2
  else
    echo "JAVA_HOME is not set (or invalid) and no JDK 17 was auto-detected." >&2
    echo "Configure JDK 17 before running Gradle." >&2
    echo "Example: export JAVA_HOME=/path/to/jdk-17 && export PATH=\"$JAVA_HOME/bin:$PATH\"" >&2
    exit 1
  fi
fi

java_version_raw="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)"
if [[ ! "$java_version_raw" =~ \"17\.|\"17\" ]]; then
  echo "Gradle scripts in this repository require JDK 17 for stable execution." >&2
  echo "Detected: $java_version_raw" >&2
  exit 1
fi

exec ./gradlew "$@"
