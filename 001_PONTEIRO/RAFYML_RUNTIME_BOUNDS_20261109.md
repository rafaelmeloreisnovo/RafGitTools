# 001_PONTEIRO — RAFYML runtime bounded traversal · 2026-11-09

- Route: START HERE Ω V2.2 → RafGitTools producer → `src/rafyml_runtime.c` → `tests/rafyml_runtime_bounds_host_test.c`.
- Authority: contributor hotfix request; no modification to inherited sources, licensing documents, CI settings or global main.
- Source baseline main blob: `7fc0d9f01cd66d51132d2ffee1b50b7d9925f3e4` (verified by Git blob of tested baseline).
- Root cause: the original `rafyml_validate_document()` loop dereferenced `document->nodes[cursor]` before checking that the cursor remains less than `node_count`. A crafted sibling index made the original baseline fail with `AddressSanitizer: global-buffer-overflow` in a local host test.
- Delta: just one fail-closed check inside the loop, before the dereference: reject `cursor >= node_count` or `cursor == root_index`, returning `RAFYML_ERR_GRAPH`.
- Runtime source new SHA-256 (local exact bytes): `c7dbd7558543633e1252f9a09f90de0dd2963565f53d25c487c366f14c9b0487`.
- Test SHA-256 (local exact bytes): `1b1fe003c26fce8ca8642a45de7fe37ce111581201fdc66d490c955440d1a3e4`.
- Local host evidence: Clang 17, `-std=c11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra -Werror`; 1,040/1,040 checks PASS, exit 0 after fix.
- Cross-object evidence: Clang `-target armv7a-none-eabi`, `-target aarch64-none-elf`, `-target x86_64-unknown-none-elf` with `-Oz -ffreestanding -fno-builtin -fno-stack-protector`, warnings as errors; three objects created; ELF32 ARM / ELF64 AArch64 / ELF64 x86-64; `nm -u` empty.
- ARMv7 objects contain local relocations including `.ARM.exidx`. This is not runtime import evidence and does not prove fully linked freestanding ELF correctness.
- Scope: object compilation and host test only. Existing API/ABI signatures and Python-hosted RAFYML generation unchanged. `NULL` / zero / empty / four spaces remain distinguishable by type and representation; no phoneme-based coercion.
- Remaining gaps: CI exact-head; full parser corpus regression; buffer *allocation extent* proof; aliases/orphans and UTF-8 complete correctness; linked ELF; ARM physical E2E; full Android/Gradle build. `claim_allowed=false`.
- License: root GPLv3-or-later preserved; no separate grant invented or implied. Per-file origin and redistribution remain review-gated.
- Falsifier: any valid historical document with changed result, any ASan/UBSan failure, a newly undefined runtime symbol, or any unmet exact-head CI check blocks promotion.
- Commands:
  ```sh
  clang -std=c11 -O1 -g -fsanitize=address,undefined -fno-omit-frame-pointer -Wall -Wextra -Werror -Iinclude src/rafyml_runtime.c tests/rafyml_runtime_bounds_host_test.c -o /tmp/raf_bounds_host
  /tmp/raf_bounds_host
  clang -target armv7a-none-eabi -std=c11 -Oz -ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Werror -Iinclude -c src/rafyml_runtime.c -o /tmp/raf_bounds_armv7.o
  nm -u /tmp/raf_bounds_armv7.o
  ```
- Rollback: close draft PR without merge, preserving base `main` and parent SHA. If later promoted, revert only this bounded change with a successor receipt.
- R3: F_ok=memory-safety cause isolated+fixed locally; F_gap=CI exact-head/physical and corpus/integration not run; F_next=full bounded corpus regression plus exact-head CI and independent audit.
