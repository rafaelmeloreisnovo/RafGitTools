#!/usr/bin/env bash
set -euo pipefail

./scripts/prepare_local_properties.sh
chmod +x ./gradlew ./scripts/gradlew_with_java17.sh

./scripts/gradlew_with_java17.sh testDevDebugUnitTest testProductionDebugUnitTest
