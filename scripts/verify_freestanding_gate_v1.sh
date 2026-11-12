#!/bin/sh
# SPDX-License-Identifier: GPL-3.0-or-later
# Hosted verifier; these tools are NOT part of the freestanding C kernel.
set -eu
ROOT=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
for tool in clang gcc readelf nm; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        printf 'gate=BLOCKED missing_host_tool=%s\n' "$tool" >&2
        exit 127
    fi
done
WORK=$(mktemp -d)
trap 'rm -rf -- "$WORK"' EXIT HUP INT TERM
SRC="$ROOT/native/raf_gate_l0_v1.c"
TEST="$ROOT/tests/raf_gate_l0_v1_host_test.c"
for compiler in clang gcc; do
    "$compiler" -std=c11 -O2 -ffreestanding -fno-builtin \
        -fno-stack-protector -fno-unwind-tables \
        -fno-asynchronous-unwind-tables -Wall -Wextra -Werror \
        -pedantic -c "$SRC" -o "$WORK/$compiler.o"
    "$compiler" -std=c11 -O2 -Wall -Wextra -Werror -pedantic \
        "$TEST" "$WORK/$compiler.o" -o "$WORK/$compiler-test"
    "$WORK/$compiler-test"
done
for entry in 'armv7a-none-eabi armv7 ELF32 ARM' \
             'aarch64-none-elf aarch64 ELF64 AArch64' \
             'x86_64-unknown-none-elf x86_64 ELF64 X86-64'; do
    set -- $entry
    target=$1 abi=$2 elf_class=$3 expected_machine=$4
    obj="$WORK/$abi.o"
    clang -target "$target" -std=c11 -Oz -ffreestanding \
        -fno-builtin -fno-stack-protector -fno-unwind-tables \
        -fno-asynchronous-unwind-tables -Wall -Wextra -Werror \
        -pedantic -c "$SRC" -o "$obj"
    readelf -h "$obj" > "$WORK/header.txt"
    grep -Eq "Class:[[:space:]]*$elf_class" "$WORK/header.txt"
    grep -Eq 'Type:[[:space:]]*REL' "$WORK/header.txt"
    case "$expected_machine" in
        ARM) grep -Eq 'Machine:[[:space:]]*ARM' "$WORK/header.txt" ;;
        AArch64) grep -Eq 'Machine:[[:space:]]*AArch64' "$WORK/header.txt" ;;
        X86-64) grep -Eq 'Machine:.*X86-64' "$WORK/header.txt" ;;
    esac
    test -z "$(nm -u "$obj")"
done
printf 'scope=HOST_SYNTHETIC_AND_ET_REL_ONLY\n'
printf 'compilers=clang,gcc\n'
printf 'objects=armv7,aarch64,x86_64\n'
printf 'undefined_symbols=0\n'
printf 'binary_link=NOT_RUN\n'
printf 'device_e2e=NOT_RUN\n'
printf 'provider_enforcement=NOT_VERIFIED\n'
printf 'claim_allowed=false\n'
