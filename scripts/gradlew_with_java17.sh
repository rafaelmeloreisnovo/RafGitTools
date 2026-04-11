#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set. Configure JDK 17 before running Gradle." >&2
  echo "Example: export JAVA_HOME=/path/to/jdk-17 && export PATH=\"$JAVA_HOME/bin:$PATH\"" >&2
  exit 1
fi

java_version_raw="$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1)"
if [[ ! "$java_version_raw" =~ \"17\.|\"17\" ]]; then
  echo "Gradle scripts in this repository require JDK 17 for stable execution." >&2
  echo "Detected: $java_version_raw" >&2
  exit 1
fi

exec ./gradlew "$@"
