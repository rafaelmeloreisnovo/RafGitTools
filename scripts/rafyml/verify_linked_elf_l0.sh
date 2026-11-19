#!/bin/sh
# Hosted linker gate only; NO runtime/device-E2E proof.
set -eu
LC_ALL=C
export LC_ALL
root=$(CDPATH= cd "$(dirname "$0")/../.." && pwd)
for tool in clang ld.lld readelf nm sha256sum mktemp grep sed; do
  command -v "$tool" >/dev/null 2>&1 || { echo "BLOCKED_MISSING_TOOL=$tool" >&2; exit 2; }
done
src="$root/src/rafyml_runtime.c"
header="$root/include/rafyml_runtime.h"
entry="$root/tests/rafyml_link_entry_l0.c"
test -f "$src" && test -f "$header" && test -f "$entry"
out=$(mktemp -d "/tmp/rafyml-link.XXXXXXXX")
printf 'extern void raf_missing_dependency(void);\nvoid _start(void) { raf_missing_dependency(); }\n' > "$out/mutant.c"
for abi in armv7 aarch64; do
  if [ "$abi" = armv7 ]; then
    target=armv7a-none-eabi
    elf_class=ELF32
    machine=ARM
  else
    target=aarch64-none-elf
    elf_class=ELF64
    machine=AArch64
  fi
  obj="$out/$abi-core.o"
  entry_obj="$out/$abi-entry.o"
  elf="$out/$abi.elf"
  flags="-std=c11 -Oz -ffreestanding -fno-builtin -fno-stack-protector -fno-unwind-tables -fno-asynchronous-unwind-tables -ffunction-sections -fdata-sections -Wall -Wextra -Werror"
  # shellcheck disable=SC2086
  clang --target="$target" $flags -I"$root/include" -c "$src" -o "$obj"
  # shellcheck disable=SC2086
  clang --target="$target" $flags -I"$root/include" -c "$entry" -o "$entry_obj"
  clang --target="$target" -nostdlib -fuse-ld=lld \
    -Wl,--no-undefined -Wl,--fatal-warnings -Wl,--gc-sections \
    -Wl,--build-id=none -Wl,-e,_start "$obj" "$entry_obj" -o "$elf"
  readelf -hW "$elf" > "$out/$abi.header"
  grep -Eq "Class:[[:space:]]+$elf_class" "$out/$abi.header" || exit 3
  grep -Eq "Machine:[[:space:]]+$machine([[:space:]]|$)" "$out/$abi.header" || exit 3
  grep -Eq 'Type:[[:space:]]+EXEC ' "$out/$abi.header" || exit 3
  grep -Eq 'Entry point address:[[:space:]]+0x[1-9a-fA-F][0-9a-fA-F]*' "$out/$abi.header" || exit 3
  nm -g --defined-only "$elf" | grep -Eq '[[:space:]]T[[:space:]]_start$' || exit 3
  nm -u "$elf" > "$out/$abi.undefined"
  if [ -s "$out/$abi.undefined" ]; then
    echo "FAIL_UNDEFINED_FINAL=$abi" >&2
    exit 4
  fi
  readelf -rW "$elf" > "$out/$abi.relocations"
  grep -q 'There are no relocations in this file' "$out/$abi.relocations" || exit 5
  readelf -dW "$elf" > "$out/$abi.dynamic"
  grep -q 'There is no dynamic section in this file' "$out/$abi.dynamic" || exit 5
  readelf -lW "$elf" > "$out/$abi.phdr"
  grep -Eq '^  LOAD' "$out/$abi.phdr" || exit 5
  if grep -Eq '(INTERP|DYNAMIC|[[:space:]]RWE[[:space:]])' "$out/$abi.phdr"; then
    echo "FAIL_INTERP_DYNAMIC_WX=$abi" >&2
    exit 5
  fi
  # Mutation in temporary test directory; original runtime untouched.
  # shellcheck disable=SC2086
  clang --target="$target" $flags -c "$out/mutant.c" -o "$out/$abi-mutant.o"
  if clang --target="$target" -nostdlib -fuse-ld=lld \
      -Wl,--no-undefined -Wl,--fatal-warnings -Wl,-e,_start \
      "$out/$abi-mutant.o" -o "$out/$abi-mutant.elf" > "$out/$abi-mutant.log" 2>&1; then
    echo "FAIL_MUTANT_ACCEPTED=$abi" >&2
    exit 6
  fi
  grep -q 'undefined symbol: raf_missing_dependency' "$out/$abi-mutant.log" || {
    cat "$out/$abi-mutant.log" >&2
    echo "FAIL_MUTANT_WRONG_CAUSE=$abi" >&2
    exit 6
  }
  printf 'LINKED_%s=PASS_SCOPED target=%s class=%s machine=%s type=ET_EXEC undefs=0 relocations=0 dynamic=0 interp=0 wx=0 mutant=REJECTED sha256=%s\n' \
    "$abi" "$target" "$elf_class" "$machine" "$(sha256sum "$elf" | sed 's/ .*//')"
done
commit=TOKEN_VAZIO
if command -v git >/dev/null 2>&1; then
  candidate=$(git -C "$root" rev-parse HEAD 2>/dev/null || true)
  case "$candidate" in
    ????????????????????????????????????????) commit="$candidate" ;;
  esac
fi
{
  printf 'scope=STATIC_LINK_ONLY_ARMV7_AARCH64\n'
  printf 'commit=%s\n' "$commit"
  printf 'source_sha256=%s\n' "$(sha256sum "$src" | sed 's/ .*//')"
  printf 'header_sha256=%s\n' "$(sha256sum "$header" | sed 's/ .*//')"
  printf 'entry_sha256=%s\n' "$(sha256sum "$entry" | sed 's/ .*//')"
  printf 'linked_elf=PASS_SCOPED_STATIC_LINK_ONLY\n'
  printf 'host_execution=NOT_RUN\n'
  printf 'physical_device_e2e=NOT_RUN\n'
  printf 'android_loader_compatibility=TOKEN_VAZIO\n'
  printf 'abi_stack_and_runtime=TOKEN_VAZIO\n'
  printf 'claim_allowed=false\n'
} > "$out/receipt.txt"
cat "$out/receipt.txt"
printf 'RECEIPT_PATH=%s\n' "$out/receipt.txt"
