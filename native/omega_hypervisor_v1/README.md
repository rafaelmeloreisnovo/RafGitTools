# Omega Hypervisor L0 V1

State: `IMPLEMENTED_UNTESTED`  
Federated authority: `Mapa PR#717@f23304cc5a2b77805df85ab1a0586c8521b87756`  
Manifest blob: `d223400c4bd57d6e4c31cd2de0db4c882b55a4bb`  
Claim gate: `claim_allowed=false`

## Meaning

“Hypervisor” here means a **federated state/custody control layer**, analogous to a hypervisor because it mounts isolated authority surfaces and schedules transitions. It is not a CPU virtualization implementation and does not replace QEMU/KVM/Android/Linux.

No inherited Vectra or PCR implementation is copied into this L0.

Vectra is a structural reference. PCR contributes the custody-cycle pattern. The executable state core below is a new clean implementation scaffold, and its legal authorship is deliberately not promoted by this receipt.

## State machine

```text
C01_INTENT
  -> C02_AUTHORSHIP
  -> C03_GAPS
  -> C04_MOUNT
  -> C05_EXECUTE
  -> C06_VERIFY
  -> C07_CUSTODY
  -> C08_OMEGA
  -> C01(next cycle)
```

Each transition has a non-compensatory gate mask. A failed transition remains in the same state and records the missing mask. At C08, provider revision becomes mandatory only when a provider was used; a human checkpoint becomes mandatory only when promotion is requested.

## L0 boundary

The production core is:

- freestanding C11;
- no libc/system headers;
- no heap;
- no syscall;
- no filesystem/network;
- no floating point;
- no external runtime symbols.

The hosted test harness is test-only.

## Proven authorial payload identity

V1 does not copy the payload into the state engine. It binds one proven path-level artifact by identity:

```text
RAF_BL0_V0
Git blob = 132f948d199f6679fcae4f33912e0a3ad69691a3
local    = rafaelia/raf_bl0.c
```

The Mapa federation records its independent introduction evidence in Vectra PR #1149 and Termux-app PR #470 plus four-repository byte identity.

## Build and gates

```sh
make -C native/omega_hypervisor_v1 clean
make -C native/omega_hypervisor_v1 host-test audit HOST_CC=clang NM=nm
make -C native/omega_hypervisor_v1 audit-armv7 ARMV7_CC=clang NM=nm
make -C native/omega_hypervisor_v1 audit-aarch64 AARCH64_CC=clang NM=nm
python3 -m unittest -v tests.test_authorial_omega_hypervisor_executor
python3 scripts/validate_authorial_omega_hypervisor_executor.py
```

## Separation of authority

```text
Mapa        = federated topology / relation / authority / gap state
RafGitTools = executor / branch / test / receipt
RafPolimata = assurance / benchmarks / falsifiers
Private     = private payload / provenance / authorship records
Drive       = documentary index / current state / receipts
Producer    = implementation truth for its own domain
```

Provider systems and standard primitives remain external authorities. Uncertain inherited code is reference-only until path-level provenance proves otherwise.

## R3

- **F_ok:** executable eight-state L0 exists with dynamic C08 gate semantics and exact Mapa/RAF_BL0 identity pins.
- **F_gap:** exact-head START CI, physical runtime, provider bindings and legal authorship classification of newly generated glue remain open.
- **F_next:** run canonical START; promote only the structural/CI scope that actually passes.
