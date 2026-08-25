#!/bin/sh
set -eu

assembly=${1:?usage: audit_hotpath_assembly.sh <core.s> <x86_64|armv7|aarch64>}
architecture=${2:?usage: audit_hotpath_assembly.sh <core.s> <x86_64|armv7|aarch64>}
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT HUP INT TERM

sed -n '/^raf_fed_validate:/,/^[[:space:]]*\.size[[:space:]]*raf_fed_validate/p' \
    "$assembly" > "$scratch/hotpath.s"
test -s "$scratch/hotpath.s"

case "$architecture" in
    x86_64)
        branch_pattern='[[:space:]]j[a-z0-9]*[[:space:]]'
        call_pattern='[[:space:]]call[q]?[[:space:]]'
        ;;
    armv7)
        branch_pattern='[[:space:]](b|b(eq|ne|cs|hs|cc|lo|mi|pl|vs|vc|hi|ls|ge|lt|gt|le|al))(\.w)?[[:space:]]|[[:space:]](cbz|cbnz|tbb|tbh)[[:space:]]'
        call_pattern='[[:space:]](bl|blx)[[:space:]]'
        ;;
    aarch64)
        branch_pattern='[[:space:]]b(\.[a-z]+)?[[:space:]]|[[:space:]](cbz|cbnz|tbz|tbnz|br)[[:space:]]'
        call_pattern='[[:space:]](bl|blr)[[:space:]]'
        ;;
    *)
        printf 'FAIL: unsupported architecture=%s\n' "$architecture" >&2
        exit 1
        ;;
esac

branches=$(grep -Ec "$branch_pattern" "$scratch/hotpath.s" || true)
calls=$(grep -Ec "$call_pattern" "$scratch/hotpath.s" || true)

if test "$branches" -ne 0 || test "$calls" -ne 0; then
    printf 'FAIL: generated assembly architecture=%s branches=%s calls=%s\n' \
        "$architecture" "$branches" "$calls" >&2
    exit 1
fi

printf 'PASS generated_assembly=%s hotpath=raf_fed_validate branches=0 calls=0\n' "$architecture"
