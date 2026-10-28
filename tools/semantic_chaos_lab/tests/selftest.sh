#!/bin/sh
# Hosted CI wrapper; freestanding claim applies ONLY to omega_token_core.o.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf -- "$TMP"' EXIT HUP INT TERM
CC="${CC:-cc}"
command -v "$CC" >/dev/null 2>&1 || { echo "NO_CC" >&2; exit 2; }
command -v nm >/dev/null 2>&1 || { echo "NO_NM" >&2; exit 2; }
command -v python3 >/dev/null 2>&1 || { echo "NO_PYTHON" >&2; exit 2; }
"$CC" -std=c99 -O2 -Wall -Wextra -Werror -ffreestanding -fno-builtin -fno-stack-protector -c "$ROOT/omega_token_core.c" -o "$TMP/core.o"
if [ -n "$(nm -u "$TMP/core.o")" ]; then
 echo "FREESTANDING_LINK_IMPORTS_DETECTED" >&2
 exit 3
fi
"$CC" -std=c99 -O2 -Wall -Wextra -Werror "$ROOT/tests/test_omega_token_core.c" "$TMP/core.o" -o "$TMP/test"
"$TMP/test"
python3 "$ROOT/chaos_gate.py" selftest
python3 "$ROOT/chaos_gate.py" build "$ROOT/corpus/omega_adversarial_v1.jsonl" "$TMP/round" --seed 42
python3 "$ROOT/chaos_gate.py" restore "$TMP/round/chaos.jsonl" "$TMP/round/order.json" "$TMP/restored.jsonl"
cmp "$ROOT/corpus/omega_adversarial_v1.jsonl" "$TMP/restored.jsonl"
echo "SEMCHAOS_SCOPED_HARNESS_PASS"
