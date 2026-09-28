# Silicon Light Core V1

State: `IMPLEMENTED_SOURCE / CI_PENDING`  
Owner: `rafaelmeloreisnovo/RafGitTools`  
Claim gate: `claim_allowed=false`

## Engineering meaning of “cidade de luz do silício”

The name is an architectural metaphor. The executable contract is concrete:

```text
L0 SILICON LIGHT CORE
  no libc/system headers
  no heap
  no syscall
  no filesystem/network
  no JNI/Android API
  no external runtime symbols
        ↓
L1 ABI ADAPTER
  JNI / native bridge / DEX boundary
        ↓
L2 ANDROID SHELL
  ART / SDK / package / permissions / storage / UI
        ↓
L3 ORCHESTRATION
  Gradle / CI / signing / QEMU / device receipts
```

Only L0 is claimed to be runtime-independent. Android cannot be truthfully packaged or executed without Android platform contracts. The method therefore isolates those contracts instead of pretending they do not exist.

## L0 primitives

V1 contains only bounded deterministic primitives:

- byte zero/copy/xor; `raf_sl_copy` has memcpy-style non-overlap semantics;
- constant-work equality loop for equal-length buffers;
- non-cryptographic 32-bit structural tag;
- saturating Q16.16 multiplication;
- fixed-size deterministic state initialization and step.

The structural tag is **not** a cryptographic hash, signature, MAC or authenticity proof.

## Gates

```sh
make -C native/silicon_light_v1 clean
make -C native/silicon_light_v1 host-test audit
make -C native/silicon_light_v1 armv7-object audit-armv7
make -C native/silicon_light_v1 aarch64-object audit-aarch64
```

The production source is compiled with `-nostdinc -ffreestanding -fno-builtin`. The hosted test harness may use libc; it is test-only and never part of L0.

ARMv7 may contain local `.ARM.exidx` section relocations emitted by the compiler. Those are not external runtime symbols. The cross-ABI hard gate is therefore **undefined external symbols = 0**; host L0 additionally requires relocation count = 0.

## Boundary

```text
SOURCE != OBJECT != LINKED ADAPTER != APK != DEVICE EXECUTION != CLAIM
```

This module does not replace `native/rafcode_route_v1`. It is a generic low-level substrate that future domain-specific cores may consume only after their own equivalence/regression gates.


## C semantics hardening

V1 deliberately avoids relational ordering of unrelated pointers and avoids signed right-shift for negative Q16 values. These are treated as portability shadows even when a specific compiler would produce the expected machine instruction.
