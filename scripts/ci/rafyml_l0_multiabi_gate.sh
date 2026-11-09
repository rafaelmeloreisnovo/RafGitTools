#!/bin/sh
# RAFYML L0 scoped evidence gate. L1 hosted orchestration, L0 freestanding objects.
# Source authority: RafGitTools PR #807; no APK, signing, flashing or deployment.
set -eu
umask 077
LC_ALL=C; export LC_ALL
mode=dry
case "${1---dry-run}" in
  --dry-run) mode=dry ;;
  --run) mode=manual ;;
  --ci-run)
    [ "${CI-}" = true ] && [ "${GITHUB_EVENT_NAME-}" = pull_request ] &&
      [ "${GITHUB_REPOSITORY-}" = rafaelmeloreisnovo/RafGitTools ] || {
        echo 'BLOCKED: CI authority missing' >&2; exit 2;
      }
    mode=ci ;;
  *) echo 'Usage: sh script [--dry-run|--run|--ci-run]' >&2; exit 2 ;;
esac
printf '%s\n' 'RAFYML L0: hosted test + ARMv7/AArch64 relocatable objects ONLY'
printf '%s\n' 'SOURCE: src/rafyml_l0_graph.c | include/rafyml_l0_graph.h | tests/rafyml_l0_graph_test.c'
printf '%s\n' 'REQUIRED: clang, nm, readelf, sha256sum, mktemp, POSIX sh; no JDK/SDK/NDK/Gradle/JNI/R8'
printf '%s\n' 'RISK: compiler and runner are hosted dependencies; no linked executable or device validation'
printf '%s\n' 'FLAGS: -std=c11 -O2 -ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Werror'
printf '%s\n' 'PLANNED: hosted Clang 16 vectors; compile armv7a-none-eabi and aarch64-none-elf'
printf '%s\n' 'CHECKPOINT: source -> host result -> per-ABI ELF class/machine/type -> no undefined symbols -> digests'
printf '%s\n' 'ROLLBACK: close/revert draft PR; original parser and runtime remain untouched'
if [ "$mode" = dry ]; then
  echo 'DRY_RUN: no compiler executed; use --run for interactive review or --ci-run in scoped PR workflow'
  exit 0
fi
for tool in clang nm readelf sha256sum mktemp grep; do
  command -v "$tool" >/dev/null 2>&1 || { echo "BLOCKED: missing $tool" >&2; exit 2; }
done
for file in src/rafyml_l0_graph.c include/rafyml_l0_graph.h include/rafyml_runtime.h tests/rafyml_l0_graph_test.c; do
  [ -f "$file" ] || { echo "BLOCKED: missing $file" >&2; exit 2; }
done
if [ "$mode" = manual ]; then
  for stage in host armv7 aarch64; do
    printf 'Authorize %s compilation/test? Type YES: ' "$stage"
    IFS= read -r answer || exit 3
    [ "$answer" = YES ] || { echo 'HOLD: no consent' >&2; exit 3; }
  done
fi
scratch=$(mktemp -d "${TMPDIR:-/tmp}/rafyml-l0.XXXXXXXX")
trap 'rm -rf "$scratch"' EXIT HUP INT TERM
printf '%s\n' 'CHECK: host sanitized test (hosted, not freestanding runtime)'
clang -std=c11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra -Werror -Iinclude \
  src/rafyml_l0_graph.c tests/rafyml_l0_graph_test.c -o "$scratch/host_tests"
"$scratch/host_tests"
for pair in 'armv7:armv7a-none-eabi:ELF32:ARM' 'aarch64:aarch64-none-elf:ELF64:AArch64'; do
  old_ifs=$IFS; IFS=:; set -- $pair; IFS=$old_ifs
  abi=$1 target=$2 class=$3 machine=$4
  obj="$scratch/$abi.o"
  echo "CHECK: $abi object"
  clang -target "$target" -std=c11 -O2 -ffreestanding -fno-builtin \
    -fno-stack-protector -Wall -Wextra -Werror -Iinclude \
    -c src/rafyml_l0_graph.c -o "$obj"
  readelf -h "$obj" > "$scratch/$abi.elf.txt"
  grep -q "Class:.*$class" "$scratch/$abi.elf.txt" || { echo 'FAIL: class' >&2; exit 1; }
  grep -q "Machine:.*$machine" "$scratch/$abi.elf.txt" || { echo 'FAIL: machine' >&2; exit 1; }
  grep -q 'Type:.*REL' "$scratch/$abi.elf.txt" || { echo 'FAIL: not relocatable' >&2; exit 1; }
  if nm -u "$obj" > "$scratch/$abi.undefined"; then
    [ ! -s "$scratch/$abi.undefined" ] || { echo "FAIL: $abi undefined" >&2; cat "$scratch/$abi.undefined"; exit 1; }
  else
    echo "BLOCKED: nm cannot inspect $abi" >&2; exit 2
  fi
  sha256sum "$obj"
done
echo 'PASS_SCOPED: host vectors + two ISA objects only; E2E=TOKEN_VAZIO; claim_allowed=false'
