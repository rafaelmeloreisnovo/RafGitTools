# Raf Lens L0 — C11 freestanding / no hosted runtime

Status: SOURCE_CANDIDATE · HOSTED_KAT_PASS · CROSS_OBJECT_PASS · DEVICE_NOT_RUN · NO_MERGE · claim_allowed=false.

## Boundary

- Freestanding core only: `rfl_lens.h`, `rfl_sha256.c`, `rfl_lens.c`, `rfl_graph.c`. No libc, malloc/free, heap, syscalls, IO, Android/JNI, threading, external include or third-party code.
- `void *` is limited to explicit-length SHA-256 byte input. Other payloads have typed pointer+length spans and caller-owned output buffers.
- No unbounded recursion, training, autonomous execution, implicit external login or background processing.
- `TOKEN_VAZIO` = explicit enum 1; enum 0 is invalid, not a numeric unknown. All failures return typed states; absent source is never PASS.
- Bounded constants: 64 dimensions, 16 tokens per node, 64 nodes, 64 edges, 3 hops, 40 bytes/token; lexical candidates are NOT semantic embeddings, scientific validity or causal avalanches.
- Internal SHA-256 is NIST-compatible for tested ASCII/byte inputs. Node digest derives from sorted source/gap/tokens under domain separation compatible with the Kotlin v1 structural contract.

## Build matrix (separate from Android)

Run from the repository root:

```sh
sh freestanding/lens/verify_l0.sh
```

The shell, compiler, temporary directory, `nm`, `readelf` and hosted `main` test are **test harness only**. They are not linked into L0. The harness checks NIST SHA-256 empty/abc, independent canonical node digest, sort invariance, malformed/duplicate refusals, graph bounds, plus relocatable code compilation for ARMv7, AArch64 and x86-64. After combining L0 objects using `ld.lld -r`, `nm -u` must show zero undefined symbols.

One multi-target compiler can generate three different CPU objects. **One compiled ELF is not automatically portable across CPUs**. CPU ABI/machine code remains target-specific. Booting without an OS additionally needs reset/startup code, board memory map, linker placement and (for real I/O) platform-specific drivers. No bare-metal boot, real device or hosted Android parity is claimed by these object tests.

## Source versus adapter

```text
Private Drive / GitHub / Mapa (provider ACL, consent, custody; NOT part of L0)
   -> L1 hosted SAF/JNI/NDK wrapper, separately reviewed (NOT WIRED YET)
   -> verified bounded byte spans -> L0 SHA + vectors + lexical graph
   -> L1 presentation / append-only provider receipt, separately gated
```

RafGitTools PR #682 remains the Android/Kotlin prototype; it is deliberately not replaced or silently coupled. This successor branch holds the independently testable L0. Importing a private source, performing OAuth, reading physical ACL, emitting a durable Drive receipt, building APK or booting hardware are independent future gates. Do not promote implemented source into a runtime claim without exact-head/physical evidence.

## Anti-regression gates and next actions

1. Verify exact Git blobs, run `verify_l0.sh` at the exact PR head; exercise malformed inputs, duplicate IDs/tokens and capacity overflow.
2. Use a separate L1 adapter to demonstrate Kotlin-to-L0 byte equality for canonical metadata, then execute Android ARM32 and ARM64 builds and physical device tests.
3. Resolve provider permission and custody independently; no PAT value or private Drive ID belongs in a public repo. Readback, signed receipt and owner approval before any live mutation.

Rollback = close this draft stacked PR. Neither RafGitTools `main` nor private Drive data were changed by L0 development. SOURCE ≠ ARTIFACT ≠ EXECUTION ≠ EVIDENCE ≠ CLAIM.
