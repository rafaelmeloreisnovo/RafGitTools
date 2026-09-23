#!/bin/sh
set -eu

binary=${1:-build/host/rafcode-route}
scratch=$(mktemp -d)
trap 'rm -rf "$scratch"' EXIT HUP INT TERM

emit_valid()
{
    printf '\122\122\121\061\001\000\004\000\001\000\000\000\001\000\000\000\002\000\000\000\004\000\000\000\010\000\000\000\000\000\000\000'
}

emit_ambiguous()
{
    printf '\122\122\121\061\001\000\007\000\003\000\000\000\001\000\000\000\002\000\000\000\004\000\000\000\010\000\000\000\000\000\000\000'
}

emit_valid | "$binary" > "$scratch/valid-a.bin"
emit_valid | "$binary" > "$scratch/valid-b.bin"
test "$(wc -c < "$scratch/valid-a.bin")" -eq 32
cmp "$scratch/valid-a.bin" "$scratch/valid-b.bin"
test "$(od -An -tu2 -j6 -N2 "$scratch/valid-a.bin" | tr -d ' ')" -eq 0
test "$(od -An -tu4 -j12 -N4 "$scratch/valid-a.bin" | tr -d ' ')" -eq 4
test "$(od -An -tu4 -j16 -N4 "$scratch/valid-a.bin" | tr -d ' ')" -eq 4
test "$(od -An -tu4 -j20 -N4 "$scratch/valid-a.bin" | tr -d ' ')" -ne 0

set +e
emit_ambiguous | "$binary" > "$scratch/ambiguous.bin"
ambiguous_exit=$?
printf '\122\122' | "$binary" > "$scratch/short.bin"
short_exit=$?
set -e

test "$ambiguous_exit" -eq 2
test "$short_exit" -eq 2
test "$(wc -c < "$scratch/ambiguous.bin")" -eq 32
test "$(wc -c < "$scratch/short.bin")" -eq 32
test "$(od -An -tu2 -j6 -N2 "$scratch/ambiguous.bin" | tr -d ' ')" -eq 1
ambiguous_mask=$(od -An -tu4 -j8 -N4 "$scratch/ambiguous.bin" | tr -d ' ')
test "$((ambiguous_mask & 16))" -eq 16
test "$(od -An -tu2 -j6 -N2 "$scratch/short.bin" | tr -d ' ')" -eq 1

printf '%s\n' 'PASS rafcode-route executable smoke: deterministic valid receipt + fail-closed ambiguity/short-frame paths'
