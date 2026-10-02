# Knowledge Campus Low-Level Execution Profile V1

State: IMPLEMENTED_UNTESTED
claim_allowed: false

## Boundary

Kotlin/Java on Android are not freestanding runtimes.

Even with no third-party dependencies they execute through ART/JVM and the Java/Android runtime model. Therefore this profile separates:

### L0 — FREESTANDING_REAL

Authority: existing Silicon Light L0.

Required properties:
- C11/ASM freestanding core;
- no libc/system headers;
- no malloc/calloc/realloc/free;
- no JNI/Android API inside the L0 core;
- no syscalls inside the declared pure-compute boundary;
- no undefined external runtime symbols;
- fixed-width/fixed-capacity state where applicable;
- deterministic validation and exact binary audit.

### L1 — KOTLIN_JAVA_RESTRICTED

This is a restricted managed-runtime shell, not freestanding.

Allowed:
- language primitives;
- primitive arrays where practical;
- Java/Android platform APIs only when explicitly bound to a surface contract.

Forbidden by default:
- AndroidX;
- third-party libraries;
- JNI/native method invocation;
- reflection/dynamic class loading;
- coroutines/hidden schedulers;
- implicit network;
- hidden persistence;
- automatic privilege escalation;
- semantic claims that managed runtime is heap-free or GC-free.

Invariant:

KOTLIN_JAVA_RESTRICTED != FREESTANDING_REAL.

A future implementation can reduce allocation and dependency surfaces, but cannot claim zero heap/zero GC at the whole Kotlin/Java runtime level without independent runtime evidence.

## Campus rule

Knowledge extraction, routing and ontology can have both implementations:

- L0 primitive kernels for hashing, bounded token scanning, fixed-capacity relation IDs, deterministic state transitions;
- L1 UI/orchestration adapters for Android interaction.

L1 may call L0 only through an explicit audited bridge. The bridge itself is not part of the L0 freestanding claim.

## Anti-promotion

IMPLEMENTED_UNTESTED != PASS.
SOURCE_LEVEL_RESTRICTION != RUNTIME_ZERO_ALLOCATION.
NO_EXTERNAL_LIBRARY != NO_RUNTIME_DEPENDENCY.
