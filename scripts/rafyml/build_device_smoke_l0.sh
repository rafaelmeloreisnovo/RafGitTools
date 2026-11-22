#!/bin/sh
# Hosted CI adapter. Core C remains freestanding, independent of the test syscall.
set -eu
LC_ALL=C; export LC_ALL
root=$(CDPATH= cd "$(dirname "$0")/../.." && pwd)
out=${RAFYML_SMOKE_OUT:-"$root/.rafyml-smoke-output"}
for tool in clang readelf nm sha256sum grep sed; do
  command -v "$tool" >/dev/null 2>&1 || { echo "BLOCKED_MISSING_TOOL=$tool" >&2; exit 2; }
done
linker=lld
if ! command -v ld.lld >/dev/null 2>&1; then
  linker=
  for c in /usr/local/swift/usr/bin/ld.lld /usr/share/swift/usr/bin/ld.lld /usr/lib/llvm-18/bin/ld.lld /usr/lib/llvm-17/bin/ld.lld /usr/bin/ld.lld-18 /usr/bin/ld.lld-17; do
    if [ -x "$c" ]; then linker=$c; break; fi
  done
  test -n "$linker" || { echo 'BLOCKED_LLD_UNAVAILABLE' >&2; exit 2; }
fi
mkdir -p "$out"
clang -std=c11 -O1 -g -Wall -Wextra -Werror -fsanitize=address,undefined -fno-omit-frame-pointer \
  -DRAFYML_HOSTED_TEST -I"$root/include" "$root/src/rafyml_runtime.c" "$root/tests/rafyml_device_smoke_l0.c" -o "$out/host-smoke"
ASAN_OPTIONS=detect_leaks=1:halt_on_error=1 UBSAN_OPTIONS=halt_on_error=1 "$out/host-smoke"
echo 'HOST_SMOKE=PASS_SCOPED'
for spec in 'armv7a-none-eabi:armv7:ELF32:ARM' 'aarch64-none-elf:arm64:ELF64:AArch64'; do
  target=${spec%%:*}; remain=${spec#*:}
  abi=${remain%%:*}; remain=${remain#*:}
  cls=${remain%%:*}; mach=${remain#*:}
  core="$out/$abi-core.o"; adapter="$out/$abi-adapter.o"; elf="$out/$abi-smoke.elf"
  flags='-std=c11 -Oz -ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Werror -ffunction-sections -fdata-sections'
  # shellcheck disable=SC2086
  clang --target="$target" $flags -I"$root/include" -c "$root/src/rafyml_runtime.c" -o "$core"
  # shellcheck disable=SC2086
  clang --target="$target" $flags -I"$root/include" -c "$root/tests/rafyml_device_smoke_l0.c" -o "$adapter"
  clang --target="$target" -nostdlib -fuse-ld="$linker" -Wl,-e,_start -Wl,--no-undefined \
    -Wl,--fatal-warnings -Wl,--build-id=none "$core" "$adapter" -o "$elf"
  readelf -hW "$elf" > "$out/$abi.header"
  grep -Eq "Class:[[:space:]]+$cls" "$out/$abi.header" || exit 3
  grep -Eq "Machine:[[:space:]]+$mach([[:space:]]|$)" "$out/$abi.header" || exit 3
  grep -Eq 'Type:[[:space:]]+EXEC ' "$out/$abi.header" || exit 3
  test -z "$(nm -u "$elf")" || { echo "FAIL_UNDEFINED=$abi" >&2; exit 4; }
  readelf -dW "$elf" | grep -q 'There is no dynamic section in this file' || exit 5
  readelf -rW "$elf" | grep -q 'There are no relocations in this file' || exit 5
  readelf -lW "$elf" > "$out/$abi.phdr"
  grep -Eq '^  LOAD' "$out/$abi.phdr" || exit 5
  ! grep -Eq '(INTERP|DYNAMIC|[[:space:]]RWE[[:space:]])' "$out/$abi.phdr" || exit 5
  chmod 755 "$elf"
  printf 'ELF_%s=PASS_STATIC_PREPARE_ONLY sha256=%s\n' "$abi" "$(sha256sum "$elf" | sed 's/ .*//')"
done
mkdir -p "$out/package"
cp "$out/armv7-smoke.elf" "$out/arm64-smoke.elf" "$out/package/"
cp "$root/scripts/rafyml/run_device_smoke_l0.sh" "$out/package/run_device_smoke_l0.sh"
chmod 755 "$out/package/run_device_smoke_l0.sh"
(
  cd "$out/package"
  sha256sum armv7-smoke.elf arm64-smoke.elf > SHA256SUMS
  echo 'Scope: Linux EABI smoke adapter with synthetic fixtures only. 0 exit = scoped checks passed. No secrets or network; run sh run_device_smoke_l0.sh inside Termux HOME (not Downloads). SHA256 checks detect byte changes, not prove publisher identity. No physical success until device receipt observed.' > README.txt
)
# Host-only negative gate: x86_64 must be unsupported, never silently execute ARM.
if [ "$(uname -m)" = x86_64 ]; then
  set +e
  (cd "$out/package" && sh ./run_device_smoke_l0.sh) > "$out/unsupported.log" 2>&1
  result=$?
  set -e
  test "$result" -eq 3 && grep -q '^BLOCKED_ARCH_UNSUPPORTED=' "$out/unsupported.log" || {
    cat "$out/unsupported.log" >&2
    echo 'FAIL_ARCH_GUARD' >&2
    exit 6
  }
  echo 'HOST_UNSUPPORTED_ARCH=REJECTED'
fi
echo 'DEVICE_HARDWARE=NOT_RUN; claim_allowed=false'
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  { echo '### RAFYML Linux EABI smoke kit prepared'; echo 'No physical Android device executed; one exit-status test adapter per ABI. Device E2E remains NOT_RUN.'; } >> "$GITHUB_STEP_SUMMARY"
fi
