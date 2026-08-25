#!/bin/sh
set -eu

binary=${1:?usage: audit_elf.sh <elf> [expected-machine]}
expected_machine=${2:-}
maximum_bytes=${MAXIMUM_BYTES:-8192}

test -f "$binary"
test "$(wc -c < "$binary")" -le "$maximum_bytes"

machine=$(readelf -hW "$binary" | sed -n 's/^[[:space:]]*Machine:[[:space:]]*//p')
if test -n "$expected_machine" && test "$machine" != "$expected_machine"; then
    printf 'FAIL: machine expected=%s observed=%s\n' "$expected_machine" "$machine" >&2
    exit 1
fi

if readelf -lW "$binary" | grep -q ' INTERP '; then
    printf '%s\n' 'FAIL: PT_INTERP present' >&2
    exit 1
fi

if readelf -dW "$binary" 2>&1 | grep -q 'NEEDED'; then
    printf '%s\n' 'FAIL: DT_NEEDED present' >&2
    exit 1
fi

if readelf -rW "$binary" 2>&1 | grep -Eq 'R_[A-Z0-9_]+'; then
    printf '%s\n' 'FAIL: relocation present' >&2
    exit 1
fi

if readelf -Ws "$binary" 2>&1 | grep -Eq '[[:space:]]UND[[:space:]]+[A-Za-z_]'; then
    printf '%s\n' 'FAIL: undefined symbol present' >&2
    exit 1
fi

if readelf -SW "$binary" | grep -Eq '\.(dynsym|symtab|dynamic|got|plt|eh_frame)([.[:space:]]|$)'; then
    printf '%s\n' 'FAIL: symbol/runtime metadata section present' >&2
    exit 1
fi

load_count=$(readelf -lW "$binary" | grep -c ' LOAD ')
test "$load_count" -eq 1
readelf -lW "$binary" | grep -q ' LOAD .*R E '
readelf -lW "$binary" | grep -q 'GNU_STACK.*RW '

if grep -En '(^|[^[:alnum:]_])(for|while)[[:space:]]*\(|(^|[^[:alnum:]_])do[[:space:]]*\{' src/*.c >/dev/null; then
    printf '%s\n' 'FAIL: source loop present in runtime C' >&2
    exit 1
fi

if grep -En '(^|[^[:alnum:]_])(malloc|calloc|realloc|free|memcpy|memmove|memset)[[:space:]]*\(' src/*.c >/dev/null; then
    printf '%s\n' 'FAIL: allocator or hosted-memory primitive present' >&2
    exit 1
fi

printf 'PASS elf=%s machine=%s bytes=%s load_segments=%s interp=0 needed=0 relocations=0 symbols=0 undefined=0 source_loops=0 heap_primitives=0\n' \
    "$binary" "$machine" "$(wc -c < "$binary")" "$load_count"
