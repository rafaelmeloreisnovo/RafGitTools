#!/bin/sh
# Hosted verification adapter; never part of the L0 runtime.
set -eu
LC_ALL=C
export LC_ALL

root=$(CDPATH= cd "$(dirname "$0")/../.." && pwd)
: "${CLANG:=clang}"
for tool in "$CLANG" readelf nm sha256sum mktemp grep sed; do
    command -v "$tool" >/dev/null 2>&1 || {
        printf 'BLOCKED_MISSING_TOOL=%s\n' "$tool" >&2
        exit 2
    }
done

out=$(mktemp -d "${RUNNER_TEMP:-${TMPDIR:-/tmp}}/rafyml-l0.XXXXXXXX")
source_file="$root/src/rafyml_runtime.c"
header_file="$root/include/rafyml_runtime.h"
test_file="$root/tests/rafyml_runtime_bounds_host_test.c"
test -f "$source_file" && test -f "$header_file" && test -f "$test_file"

"$CLANG" -std=c11 -O1 -g -fsanitize=address,undefined \
    -fno-omit-frame-pointer -Wall -Wextra -Werror \
    -I"$root/include" "$source_file" "$test_file" -o "$out/rafyml_host_falsifier"
if ! ASAN_OPTIONS=detect_leaks=1:halt_on_error=1 \
     UBSAN_OPTIONS=halt_on_error=1 "$out/rafyml_host_falsifier" > "$out/host.log" 2>&1; then
    cat "$out/host.log" >&2
    printf 'FAIL_HOST_FALSIFIER\n' >&2
    exit 1
fi
cat "$out/host.log"
case_line=$(grep -E '^cases=[0-9]+ failures=0$' "$out/host.log" || true)
case "$case_line" in
    cases=*) case_count=${case_line#cases=}; case_count=${case_count%% *} ;;
    *) printf 'FAIL_HOST_RECEIPT\n' >&2; exit 1 ;;
esac
if [ "$case_count" -lt 1040 ]; then
    printf 'FAIL_INSUFFICIENT_FALSIFIERS=%s\n' "$case_count" >&2
    exit 1
fi

for spec in 'armv7a-none-eabi:ELF32:ARM:armv7' 'aarch64-none-elf:ELF64:AArch64:aarch64'; do
    target=${spec%%:*}
    fields=${spec#*:}
    elf_class=${fields%%:*}
    fields=${fields#*:}
    machine=${fields%%:*}
    abi=${fields#*:}
    obj="$out/rafyml_${abi}.o"
    "$CLANG" --target="$target" -std=c11 -Oz -ffreestanding \
        -fno-builtin -fno-stack-protector -fno-unwind-tables \
        -fno-asynchronous-unwind-tables -ffunction-sections -fdata-sections \
        -Wall -Wextra -Werror -I"$root/include" \
        -c "$source_file" -o "$obj"
    readelf -h "$obj" > "$out/${abi}.elf_header"
    grep -Eq "Class:[[:space:]]+$elf_class" "$out/${abi}.elf_header" || exit 3
    grep -Eq "Machine:[[:space:]]+$machine([[:space:]]|$)" "$out/${abi}.elf_header" || exit 3
    grep -Eq 'Type:[[:space:]]+REL' "$out/${abi}.elf_header" || exit 3
    nm -u "$obj" > "$out/${abi}.undefined"
    if [ -s "$out/${abi}.undefined" ]; then
        cat "$out/${abi}.undefined" >&2
        printf 'FAIL_UNDEFINED_SYMBOLS=%s\n' "$abi" >&2
        exit 4
    fi
    printf 'OBJECT_%s=PASS_SCOPED target=%s class=%s machine=%s sha256=%s\n' \
        "$abi" "$target" "$elf_class" "$machine" "$(sha256sum "$obj" | sed 's/ .*//')"
done

commit=TOKEN_VAZIO
if command -v git >/dev/null 2>&1; then
    candidate=$(git -C "$root" rev-parse HEAD 2>/dev/null || true)
    case "$candidate" in
        ????????????????????????????????????????) commit=$candidate ;;
    esac
fi
{
    printf 'scope=HOST_ASAN_UBSAN_AND_CROSS_TARGET_OBJECTS\n'
    printf 'source_repository=rafaelmeloreisnovo/RafGitTools\n'
    printf 'commit=%s\n' "$commit"
    printf 'source_sha256=%s\n' "$(sha256sum "$source_file" | sed 's/ .*//')"
    printf 'header_sha256=%s\n' "$(sha256sum "$header_file" | sed 's/ .*//')"
    printf 'test_sha256=%s\n' "$(sha256sum "$test_file" | sed 's/ .*//')"
    printf 'host_falsifiers=%s failures=0\n' "$case_count"
    printf 'targets=armv7a-none-eabi,aarch64-none-elf\n'
    printf 'undefined_symbols=0\n'
    printf 'linkable_elf=NOT_RUN\n'
    printf 'device_e2e=NOT_RUN\n'
    printf 'apk_build=NOT_RUN\n'
    printf 'claim_allowed=false\n'
    printf 'evidence_state=PASS_SCOPED\n'
} > "$out/receipt.txt"
cat "$out/receipt.txt"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        printf '### RAFYML L0 · evidence only\n\n```text\n'
        cat "$out/receipt.txt"
        printf '```\n'
    } >> "$GITHUB_STEP_SUMMARY"
fi
printf 'RECEIPT_PATH=%s\n' "$out/receipt.txt"
