# Architecture — Silicon Light Low-Level V1

Status: `VERIFIED_LIMITED / MERGED` for the revision-bound L0 and generated APK-fixture scope.  
Authority: RafGitTools local source.  
Evidence: PR #526 head `40d90c93bf7bfe6d3299a89585688c0324d25245`; START #263 / run `36385528436` = `SUCCESS`; merge `0fdfc8a01e9a513facc3873d9002b3bac79af8ae`.  
`claim_allowed=false`.

## Principle

Move every operation that can be expressed as a deterministic memory/register transformation into L0. Keep everything that requires an operating system, runtime, permission model, filesystem, network, package manager or UI **outside** L0.

This produces a one-way dependency graph:

```text
L3 orchestration
   ↓
L2 Android shell
   ↓
L1 ABI adapter
   ↓
L0 Silicon Light
```

L0 must never point upward.

## What “no external dependency” means here

For the produced L0 object:

- no external undefined symbol;
- no libc or compiler runtime call;
- no system include;
- no allocator;
- no syscall;
- no Android/JNI type;
- no dynamic loader;
- no filesystem/network call.

The compiler/linker used to produce the object are build tools, not runtime dependencies. The Android application still depends on Android to be an Android application.

## Anti-shadow rule

A capability that can live in L0 must not be reimplemented separately in JNI, Kotlin and shell. One low-level implementation is exposed upward through narrow adapters.

A capability that inherently belongs to Android remains in L2; it is not copied into L0 as a fake abstraction.

## “Light” rule

The light metaphor maps to observability:

```text
source identity
→ compiler flags
→ object identity
→ undefined-symbol gate
→ adapter identity
→ package identity
→ runtime receipt
```

Every boundary should make more provenance visible, not hide it.

## Next migration candidates

Candidates are not automatically migrated. Each needs equivalence tests before replacement:

1. small byte transforms duplicated across JNI/native code;
2. fixed-point arithmetic currently duplicated by domain modules;
3. structural tags/checksums that are explicitly non-cryptographic;
4. bounded state-machine helpers;
5. memory zero/copy/xor primitives in freestanding cores.

Cryptography, Android permission logic, storage, networking, JGit, Compose, Room, Gradle and QEMU runtime are **not** silently absorbed into this L0.


## Verified boundary

The final PR head passed both the host L0 gate and NDK-bound ARMv7/AArch64 gate. The canonical Android lane also generated a fresh minimal scaffold, compiled the embedded Silicon Light L0 behind JNI, produced an APK and passed DEX/APK verification.

Therefore:

```text
L0_SOURCE -> L0_OBJECT -> JNI_ADAPTER -> GENERATED_MINIMAL_APK = VERIFIED_LIMITED
PHYSICAL_DEVICE = TOKEN_VAZIO_PHYSICAL_DEVICE_REQUIRED
QEMU_GUEST = TOKEN_VAZIO_RUNTIME
RELEASE_ACCEPTANCE = TOKEN_VAZIO_RELEASE
```

## Current F3

- **F_ok:** one-way L3→L2→L1→L0 dependency architecture is implemented and CI-verified at the exact PR head.
- **F_gap:** domain-core migration coverage, property/fuzz equivalence, physical ABI execution and performance/energy measurements are not yet complete.
- **F_next:** inventory duplicate deterministic helpers and move only the smallest family whose exact semantics can be frozen and proven.
