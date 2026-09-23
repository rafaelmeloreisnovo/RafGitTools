# Receipt — RafCode Route Freestanding V1

Date: 2026-09-23  
Kind: `LOWLEVEL_ROUTE_CORE / APPEND_ONLY`  
Parent: Manifold Ω machine-readable V1 + ContextBroker V1  
claim_allowed: `false`

## Delta

A separate low-level route primitive was materialized without changing the stable `rafcode_federation_v1` 64-byte wire ABI.

```text
ROUTES_OMEGA_V1 trigger class
→ rafcode_route_v1
→ canonical route code
→ future ContextBroker adapter
```

Production source uses compiler-provided integer types only and does not import libc, CRT, heap, JSON, filesystem, network or Android APIs.

## Gates encoded

- source identity required;
- ambiguous input fails closed;
- unknown trigger/flags fail closed;
- reserved field must remain zero;
- zero-fold source identity rejected;
- V1 route mapping is versioned and one-to-one.

## Evidence state at write

```text
SOURCE = IMPLEMENTED
HOST_KAT = TOKEN_VAZIO_REMOTE_CI_PENDING
HOST_OBJECT_AUDIT = TOKEN_VAZIO_REMOTE_CI_PENDING
ARMV7_OBJECT = TOKEN_VAZIO_REMOTE_CI_PENDING
AARCH64_OBJECT = TOKEN_VAZIO_REMOTE_CI_PENDING
PHYSICAL_ARM32 = TOKEN_VAZIO_DEVICE
PHYSICAL_ARM64 = TOKEN_VAZIO_DEVICE
CONTEXTBROKER_ADAPTER = TOKEN_VAZIO_NOT_IMPLEMENTED
```

## R3

F_ok = minimal authorial freestanding route core isolated on a reversible branch; federation ABI preserved.  
F_gap = remote CI and workbench adapter.  
F_next = observe exact-head CI; only after PASS, add the thin ContextBundle route-binding adapter.
