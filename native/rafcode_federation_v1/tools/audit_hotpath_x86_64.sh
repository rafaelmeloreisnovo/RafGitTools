#!/bin/sh
set -eu

object=${1:?usage: audit_hotpath_x86_64.sh <core.o>}
objdump_tool=${OBJDUMP:-objdump}
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT HUP INT TERM

"$objdump_tool" -drw "$object" |
    sed -n '/<raf_fed_validate>:/,/^$/p' > "$scratch/hotpath.asm"

test -s "$scratch/hotpath.asm"

branches=$(grep -Ec '[[:space:]]j[a-z0-9]*[[:space:]]' "$scratch/hotpath.asm" || true)
calls=$(grep -Ec '[[:space:]]call[q]?[[:space:]]' "$scratch/hotpath.asm" || true)

if test "$branches" -ne 0 || test "$calls" -ne 0; then
    printf 'FAIL: x86_64 hotpath branches=%s calls=%s\n' "$branches" "$calls" >&2
    exit 1
fi

printf 'PASS x86_64_hotpath=raf_fed_validate conditional_or_unconditional_branches=0 calls=0\n'
