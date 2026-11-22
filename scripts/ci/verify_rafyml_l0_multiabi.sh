#!/usr/bin/env bash
# HOSTED evidence gate for a freestanding L0 C object; not a freestanding shell/CI.
set -euo pipefail
cd "$(dirname "$0")/../.."
for tool in clang readelf nm cmp sha256sum mktemp; do
  command -v "$tool" >/dev/null 2>&1 || { echo "RAFYML_L0=BLOCKED_MISSING_HOST_TOOL:$tool"; exit 2; }
done
workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

# The hosted harness is allowed to use stdio; the L0 runtime is not.
clang -std=c11 -O1 -g -Wall -Wextra -Werror \
  -fsanitize=address,undefined -fno-omit-frame-pointer -Iinclude \
  src/rafyml_l0_graph.c tests/rafyml_l0_graph_test.c -o "$workdir/host-test"
test_output="$("$workdir/host-test")"
printf '%s\n' "$test_output"
[[ "$test_output" == 'checks=16 failures=0' ]] || { echo 'RAFYML_L0=FAIL_HOST_VECTORS'; exit 1; }

for target in armv7a-none-eabi aarch64-none-elf; do
  case "$target" in
    armv7a-none-eabi) elf_class=ELF32; elf_machine=ARM ;;
    aarch64-none-elf) elf_class=ELF64; elf_machine=AArch64 ;;
  esac
  for attempt in 1 2; do
    clang --target="$target" -std=c11 -O2 -Wall -Wextra -Werror \
      -ffreestanding -fno-builtin -fno-stack-protector -Iinclude \
      -c src/rafyml_l0_graph.c -o "$workdir/$target-$attempt.o"
  done
  obj="$workdir/$target-1.o"
  cmp "$obj" "$workdir/$target-2.o" || { echo "RAFYML_L0=FAIL_NONDETERMINISTIC_OBJECT:$target"; exit 1; }
  elf_info="$(LC_ALL=C readelf -h "$obj")"
  grep -Eq "Class:[[:space:]]+$elf_class([[:space:]]|$)" <<<"$elf_info" || { echo "RAFYML_L0=FAIL_ELF_CLASS:$target"; exit 1; }
  grep -Eq "Machine:[[:space:]]+$elf_machine([[:space:]]|$)" <<<"$elf_info" || { echo "RAFYML_L0=FAIL_ELF_MACHINE:$target"; exit 1; }
  grep -Eq 'Data:.*little endian' <<<"$elf_info" || { echo "RAFYML_L0=FAIL_ELF_ENDIAN:$target"; exit 1; }
  undefined="$(nm -u "$obj")"
  [[ -z "$undefined" ]] || { echo "RAFYML_L0=FAIL_UNDEFINED_SYMBOLS:$target"; printf '%s\n' "$undefined"; exit 1; }
  printf 'OBJECT=%s CLASS=%s MACHINE=%s SIZE=%s SHA256=%s\n' \
    "$target" "$elf_class" "$elf_machine" "$(wc -c < "$obj" | tr -d ' ')" "$(sha256sum "$obj" | cut -d' ' -f1)"
done
printf 'RAFYML_L0=PASS_SCOPED_HOST_AND_CROSS_OBJECTS; ARM_PHYSICAL=NOT_RUN; LINKED_TARGET=NOT_RUN; CLAIM_ALLOWED=false\n'
