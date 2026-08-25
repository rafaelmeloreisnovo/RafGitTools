#!/bin/sh
set -eu

binary=${1:-build/host/rafcode-federation}
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT HUP INT TERM

emit_valid()
{
    printf '\122\101\106\061\001\000\007\017\001\000\000\000\000\000\000\000\001\000\000\000\000\000\000\000\003\000\000\000\020\000\000\000\001\000\000\000\002\000\000\000\003\000\000\000\004\000\000\000\005\000\000\000\006\000\000\000\007\000\000\000\010\000\000\000'
}

emit_claim_allowed()
{
    printf '\122\101\106\061\001\000\007\017\001\000\000\000\000\000\000\000\001\000\000\000\000\000\000\000\003\000\000\000\021\000\000\000\001\000\000\000\002\000\000\000\003\000\000\000\004\000\000\000\005\000\000\000\006\000\000\000\007\000\000\000\010\000\000\000'
}

emit_valid | "$binary" > "$scratch/valid-a.bin"
emit_valid | "$binary" > "$scratch/valid-b.bin"
test "$(wc -c < "$scratch/valid-a.bin")" -eq 64
cmp "$scratch/valid-a.bin" "$scratch/valid-b.bin"
test "$(od -An -tu2 -j6 -N2 "$scratch/valid-a.bin" | tr -d ' ')" -eq 0

set +e
emit_claim_allowed | "$binary" > "$scratch/claim.bin"
claim_exit=$?
printf '\122\101' | "$binary" > "$scratch/short.bin"
short_exit=$?
set -e

test "$claim_exit" -eq 2
test "$short_exit" -eq 2
test "$(wc -c < "$scratch/claim.bin")" -eq 64
test "$(wc -c < "$scratch/short.bin")" -eq 64
test "$(od -An -tu2 -j6 -N2 "$scratch/claim.bin" | tr -d ' ')" -eq 1
test "$(od -An -tu4 -j8 -N4 "$scratch/claim.bin" | tr -d ' ')" -ge 256
test "$(od -An -tu4 -j8 -N4 "$scratch/short.bin" | tr -d ' ')" -ge 1

printf '%s\n' 'PASS rafcode-federation smoke: deterministic valid receipt + fail-closed claim/short-frame paths'
