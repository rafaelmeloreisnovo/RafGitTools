# Architecture — Silicon Light Low-Level V1

Status: `IMPLEMENTED_UNTESTED` until exact-branch CI runs.  
Authority: RafGitTools local source.  
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
