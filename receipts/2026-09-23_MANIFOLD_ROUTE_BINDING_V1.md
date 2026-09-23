# Receipt — Manifold Route Binding V1

Date: 2026-09-23  
Parent: RafCode Route Freestanding V1  
Kind: `WORKBENCH_ADAPTER / APPEND_ONLY`  
claim_allowed: `false`

## Delta

A typed Kotlin adapter now carries an already resolved `rafcode_route_v1` result into ContextBundle V2 annotations.

The adapter does not reimplement the C resolver. It enforces the V1 one-to-one mapping, validates the route tag shape, binds the Mapa authority reference, and reserves the route annotation namespace against caller spoofing.

## Evidence state at write

```text
FREESTANDING_ROUTE_CI = PASS run 35821489998
KOTLIN_ADAPTER_SOURCE = IMPLEMENTED
KOTLIN_ADAPTER_TEST = TOKEN_VAZIO_CI_PENDING
JNI_NATIVE_INVOCATION = TOKEN_VAZIO_NOT_IMPLEMENTED
DEVICE = TOKEN_VAZIO
```

## R3

F_ok = low-level resolver CI passed; thin typed bundle adapter materialized.  
F_gap = exact-head Android unit test / START pipeline and native invocation boundary.  
F_next = observe exact-head CI; then design the smallest JNI/process boundary without importing provider dependencies into the C core.
