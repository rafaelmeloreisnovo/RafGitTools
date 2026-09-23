#!/bin/sh
set -eu

binary=${1:?usage: device_smoke.sh <rafcode-route-elf> [out-dir]}
out_dir=${2:-artifacts/route-freestanding-device}
mkdir -p "$out_dir"
test -x "$binary"

emit_valid()
{
    printf '\122\122\121\061\001\000\004\000\001\000\000\000\001\000\000\000\002\000\000\000\004\000\000\000\010\000\000\000\000\000\000\000'
}

emit_ambiguous()
{
    printf '\122\122\121\061\001\000\007\000\003\000\000\000\001\000\000\000\002\000\000\000\004\000\000\000\010\000\000\000\000\000\000\000'
}

valid_receipt="$out_dir/valid.receipt.bin"
ambiguous_receipt="$out_dir/ambiguous.receipt.bin"

set +e
emit_valid | "$binary" > "$valid_receipt"
valid_exit=$?
emit_ambiguous | "$binary" > "$ambiguous_receipt"
ambiguous_exit=$?
set -e

test "$valid_exit" -eq 0
test "$ambiguous_exit" -eq 2
test "$(wc -c < "$valid_receipt")" -eq 32
test "$(wc -c < "$ambiguous_receipt")" -eq 32
test "$(od -An -tu2 -j6 -N2 "$valid_receipt" | tr -d ' ')" -eq 0
test "$(od -An -tu4 -j16 -N4 "$valid_receipt" | tr -d ' ')" -eq 4
test "$(od -An -tu2 -j6 -N2 "$ambiguous_receipt" | tr -d ' ')" -eq 1
ambiguous_mask=$(od -An -tu4 -j8 -N4 "$ambiguous_receipt" | tr -d ' ')
test "$((ambiguous_mask & 16))" -eq 16

abi=$(getprop ro.product.cpu.abi 2>/dev/null || uname -m)
sdk=$(getprop ro.build.version.sdk 2>/dev/null || printf TOKEN_VAZIO)
model=$(getprop ro.product.model 2>/dev/null || printf TOKEN_VAZIO)
kernel=$(uname -srmo 2>/dev/null || uname -a)
elf_sha256=$(sha256sum "$binary" | awk '{print $1}')
valid_sha256=$(sha256sum "$valid_receipt" | awk '{print $1}')
ambiguous_sha256=$(sha256sum "$ambiguous_receipt" | awk '{print $1}')
timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)

cat > "$out_dir/device-receipt.txt" <<EOF
receipt_version=1
timestamp_utc=$timestamp
abi=$abi
sdk=$sdk
model=$model
kernel=$kernel
elf_sha256=$elf_sha256
valid_exit=$valid_exit
valid_receipt_sha256=$valid_sha256
ambiguous_exit=$ambiguous_exit
ambiguous_receipt_sha256=$ambiguous_sha256
route_valid=R0004
ambiguous_error_bit=0x10
claim_allowed=false
EOF

sha256sum "$out_dir/device-receipt.txt" > "$out_dir/device-receipt.txt.sha256"
printf 'PASS device route executable smoke abi=%s receipt=%s\n' "$abi" "$out_dir/device-receipt.txt"
