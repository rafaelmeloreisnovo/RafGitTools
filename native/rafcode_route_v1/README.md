# RafCode Route V1 — freestanding deterministic route core

State: `IMPLEMENTED_SOURCE / CI_PENDING`  
Owner: `rafaelmeloreisnovo/RafGitTools`  
Route authority: `rafaelmeloreisnovo/Mapa:data/manifold/routes_omega_v1.jsonl`  
Claim gate: `claim_allowed=false`

## Purpose

This module closes one specific gap between the machine-readable Manifold Ω and the workbench:

```text
normalized trigger class
→ freestanding route core
→ canonical R0001..R0010 route code
→ ContextBroker / WorkspaceSession adapter
```

It is deliberately **not** an NLP classifier. Text-to-trigger classification stays in the higher layer where ambiguity, provenance and user intent can be represented. The low-level core only accepts a bounded trigger class plus a source identity and either returns the canonical V1 route or fails closed.

## Why a separate module

The existing `native/rafcode_federation_v1` fixed-frame ABI is already stable. This route core does not repurpose federation state fields and does not change that 64-byte wire contract.

```text
federation ABI stability > convenience
new route primitive = separate versioned core
```

## V1 mapping

The current Mapa registry has ten deterministic seed routes and V1 binds them one-to-one:

| trigger | route |
|---|---|
| GENERAL | R0001 |
| MATHEMATICS_GEOMETRY | R0002 |
| MEMORY_CONVERSATIONS | R0003 |
| CODE_RUNTIME | R0004 |
| SCIENCE_CLAIMS | R0005 |
| NOVO | R0006 |
| ATLAS | R0007 |
| GAP | R0008 |
| EVID | R0009 |
| LEARN | R0010 |

Because the mapping is identity-coded in V1, a future change to ordering or semantics requires a new contract version rather than silent reinterpretation.

## Freestanding boundary

Production core:

- no libc headers;
- no libc/CRT calls;
- no allocator/heap;
- no filesystem/network;
- no JSON parser;
- no mutable global state;
- no external undefined symbols;
- no relocation dependency in the host production object;
- ARMv7 and AArch64 are object-compile gates.

The test harness is hosted and is not production evidence.

## Fail-closed inputs

The resolver rejects unknown triggers, missing source binding, ambiguous classification, unknown flag bits, zero folded source identity, non-zero reserved state, and wrong magic/version.

`EXPANSION_REQUIRED` is preserved as a safe informational flag. It never converts ambiguity into a route.

## Truth boundary

```text
TRIGGER_CLASSIFIED != ROUTE_RESOLVED != SOURCE_READ != EXECUTION != EVIDENCE != CLAIM
OBJECT_COMPILED != DEVICE_EXECUTED
CI_PASS != PHYSICAL_ARM32_PASS
```

The deterministic `route_tag` is a non-cryptographic structural tag, not an authentication proof.

## Build

```sh
make -C native/rafcode_route_v1 host-test
make -C native/rafcode_route_v1 audit
make -C native/rafcode_route_v1 armv7-object
make -C native/rafcode_route_v1 aarch64-object
```

Next integration step after CI is a thin adapter that binds the returned route code into ContextBundle V2 annotations without making the low-level core aware of Android, Gson, GitHub, Drive or model APIs.
