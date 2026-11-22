#!/bin/sh
# Termux Android ARMv7 or AArch64, no root, Java, SDK or NDK installation.
# POSIX adapter uses Bionic libc/pthreads; core itself is freestanding.
set -eu
cd "$(dirname "$0")/.."
mode="${1:-smoke}"
case "$mode" in
 smoke)
    command -v clang >/dev/null || { echo 'BLOCKED_CLANG_REQUIRED_IN_TERMUX'; exit 77; }
    make CC=clang all
    ./build/test_core
    if command -v python3 >/dev/null 2>&1; then
        python3 tools/run_lab.py --smoke
    else
        ./build/raf_lab_host stream 1 0.25
        ./build/raf_lab_host stream 2 0.25
        echo "TOKEN_VAZIO_RECEIPT_PYTHON"
    fi
    ;;
 large)
    printf '64/150 MiB mmap may exhaust phone RAM or overheat. Type EXACTLY ALLOW_LARGE: '
    IFS= read -r answer
    [ "$answer" = "ALLOW_LARGE" ] || { echo "BLOCKED_LARGE_BUFFER_CONSENT"; exit 77; }
    command -v clang >/dev/null || { echo 'BLOCKED_CLANG_REQUIRED_IN_TERMUX'; exit 77; }
    make CC=clang all
    ./build/raf_lab_host stream 1 64 --allow-large
    ./build/raf_lab_host reuse 1 64 --allow-large
    # 150 MiB is separate and intentionally not automatic even under large.
    printf 'For 150 MiB, type EXACTLY ALLOW_150: '
    IFS= read -r answer
    [ "$answer" = "ALLOW_150" ] || { echo "SKIP_150_MIB"; exit 0; }
    ./build/raf_lab_host stream 1 150 --allow-large
    ;;
 *) echo "Usage: sh scripts/run_termux.sh smoke|large"; exit 64;;
esac
