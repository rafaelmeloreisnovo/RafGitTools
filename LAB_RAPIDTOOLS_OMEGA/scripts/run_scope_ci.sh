#!/bin/sh
# This is a manually invocable scoped gate; no new repository-wide workflow.
set -eu
cd "$(dirname "$0")/.."
make test
if command -v clang >/dev/null 2>&1; then
    make arm32-obj arm64-obj
    echo "PASS_ARM32_ARM64_OBJECT_ONLY (not physical Android runtime)"
else
    echo "TOKEN_VAZIO_CROSS_COMPILER_CLANG"
fi
echo "HOLD: APK/DEVICE/GPU/SECRETS not tested"
