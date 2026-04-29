#!/usr/bin/env bash
set -euo pipefail

./scripts/prepare_local_properties.sh
chmod +x ./gradlew ./scripts/gradlew_with_java17.sh

./scripts/gradlew_with_java17.sh assembleDevDebug assembleProductionDebug
ALLOW_UNSIGNED_RELEASE=true ./scripts/gradlew_with_java17.sh assembleDevRelease assembleProductionRelease

if [[ -n "${RELEASE_STORE_FILE:-}" && -n "${RELEASE_STORE_PASSWORD:-}" && -n "${RELEASE_KEY_ALIAS:-}" && -n "${RELEASE_KEY_PASSWORD:-}" ]]; then
  ./scripts/gradlew_with_java17.sh assembleProductionRelease
fi
