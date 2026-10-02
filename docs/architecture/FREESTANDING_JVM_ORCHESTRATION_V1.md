# Freestanding L0 + Optional JVM Orchestration V1

Status: `IMPLEMENTED_UNTESTED`  
Claim boundary: `claim_allowed=false`

## Authority

The orchestration authority is now the freestanding L0 core:

```text
freestanding/orchestration/raf_orchestrator_l0.c
```

Runtime dependency set for that core:

```text
{}
```

The L0 core requires no JVM, JNI, NDK, libc, libstdc++, POSIX, allocator, syscall, filesystem, thread runtime, reflection, exception runtime, or external library. Memory and mutable state are caller-owned.

The Kotlin/JVM implementation remains an **optional hosted adapter/reference surface**. It is not required for the freestanding core to compile or execute.

```text
SOURCE + AUTHORITY
        |
        v
+-----------------------------+
| FREESTANDING L0 CONTROL     |
| C11 subset / no libc        |
| caller-owned memory         |
| typed module descriptors    |
| bit flags + warning mask    |
| iterative fail-closed loop  |
+-----------------------------+
        |
        +-------------------+-------------------+------------------+
        v                   v                   v                  v
   RafPolimata L0        Audio DSP          BLAKE3/rmr         Vectra kernel
   freestanding          freestanding       rmr-only           freestanding
        \                   |                   |                  /
         +------------------+-------------------+-----------------+
                                |
                     OPTIONAL HOSTED ADAPTERS
                     JVM / JNI / Android / POSIX
                                |
                                v
                       PLATFORM / DEVICE GATE
                                |
                                v
                             RECEIPT
```

Hosted adapters may expose L0 to Android/JVM/provider environments, but they never become the authority of the orchestration state machine.

## L0 invariants

- no heap allocation;
- no global mutable state;
- no syscall;
- no libc or C++ runtime;
- no external unresolved symbol in the compiled L0 object;
- no recursion or tail chaining;
- no reflection or string dispatch;
- caller-owned payload/state;
- O(1) duplicate-stage membership using a fixed 64-bit mask for module IDs 1..63;
- required flags checked before stage execution;
- `TOKEN_VAZIO`, `BLOCKED`, and `FAILED` stop downstream promotion;
- `claim_allowed=0` in every L0 receipt.

## Gates

The CI separates core proof from adapters:

1. compile L0 object with `-ffreestanding -fno-builtin -fno-stack-protector -Wall -Wextra -Werror -pedantic`;
2. compile the same source for host, ARMv7 none-eabi, and AArch64 none-elf;
3. run `nm -u` on the host object and require **zero undefined external symbols**;
4. execute a hosted behavioral harness proving `TOKEN_VAZIO` stops downstream execution;
5. validate the cross-repository manifest;
6. run JVM adapter tests independently. JVM failure does not redefine the freestanding L0 dependency surface.

A compiler/toolchain is a **build-time instrument**, not a runtime dependency of the produced L0 object.

## Repository authority

- **RafGitTools** — owns the freestanding orchestration L0 and the optional hosted adapter contract.
- **RafPolimata** — owns its L0/compiler/freestanding implementation and gates.
- **Est-dio-de-udio** — owns audio DSP/fixed-point freestanding cores.
- **BLAKE3** — upstream cryptographic core remains untouched; authorial integration stays under `rmr/`.
- **Vectras-VM-Android** — owns VM/kernel behavior; Android/JNI remains hosted.
- **termux-app-rafacodephi** — owns Android/provider/platform runtime and remains a platform gate.

## Evidence boundary

Compilation without undefined symbols proves only the scoped binary-dependency property of the L0 object. It does **not** prove device behavior, performance, cryptographic equivalence, acoustic quality, VM boot, or provider enforcement.

Until those gates execute on exact artifacts, they remain `TOKEN_VAZIO`.

## Rollback

Revert only the L0/manifest/workflow commits or close the feature PR. No force push or cross-repository history rewrite is required.

## R3

`F_ok` = zero-runtime-dependency L0 source, fixed-state contract and CI gates materialized.  
`F_gap` = current-head L0 CI and physical/provider/conformance gates remain unobserved.  
`F_next` = close L0 compile/symbol gate first; then bind specialized modules directly to L0; keep JVM/JNI strictly optional.
