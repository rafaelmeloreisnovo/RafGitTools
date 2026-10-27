#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT HUP INT TERM
cc -std=c11 -Wall -Wextra -Werror -ffreestanding -fno-builtin -c app/src/main/cpp/atlas_core.c -o "$tmp/core.o"
if nm -u "$tmp/core.o" | grep .; then echo L0_UNDEFINED_SYMBOLS >&2; exit 1; fi
cc -std=c11 -Wall -Wextra -Werror tests/core_test.c "$tmp/core.o" -o "$tmp/test"
"$tmp/test"
echo L0_CORE_HOST_PASS
