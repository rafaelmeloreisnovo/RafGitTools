#!/usr/bin/env bash
# Build llamaRafaelia as an Android native library for a given ABI.
# Usage: build_llama_kernel.sh <ABI> <NDK_PATH> <SRC_DIR> <OUT_DIR>
# ABI: arm64-v8a | armeabi-v7a
set -euo pipefail

ABI="${1:?Usage: $0 <ABI> <NDK_PATH> <SRC_DIR> <OUT_DIR>}"
NDK_PATH="${2:?NDK_PATH required}"
SRC_DIR="${3:?SRC_DIR required}"
OUT_DIR="${4:?OUT_DIR required}"

BUILD_DIR="${OUT_DIR}/build-${ABI}"
mkdir -p "${BUILD_DIR}"

echo "[RAF] Configuring llamaRafaelia for ${ABI}"
cmake -B "${BUILD_DIR}" \
  -DCMAKE_TOOLCHAIN_FILE="${NDK_PATH}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="${ABI}" \
  -DANDROID_PLATFORM=android-24 \
  -DCMAKE_BUILD_TYPE=MinSizeRel \
  -DGGML_OPENMP=OFF \
  -DGGML_LLAMAFILE=OFF \
  -DLLAMA_BUILD_TESTS=OFF \
  -DLLAMA_BUILD_TOOLS=OFF \
  -DLLAMA_BUILD_EXAMPLES=OFF \
  -DLLAMA_BUILD_SERVER=OFF \
  -DLLAMA_BUILD_COMMON=OFF \
  -DLLAMA_CURL=OFF \
  -DBUILD_SHARED_LIBS=ON \
  "${SRC_DIR}"

echo "[RAF] Building llama + ggml targets for ${ABI}"
cmake --build "${BUILD_DIR}" \
  --target llama ggml ggml-base \
  --config MinSizeRel \
  -j"$(nproc 2>/dev/null || echo 4)"

echo "[RAF] .so artifacts for ${ABI}:"
find "${BUILD_DIR}" -name '*.so' | sort
