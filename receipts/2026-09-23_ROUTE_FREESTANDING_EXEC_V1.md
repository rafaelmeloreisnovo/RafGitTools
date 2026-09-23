# Receipt — RafCode Route Freestanding Executable V1

Date: 2026-09-23  
Kind: `FREESTANDING_EXECUTABLE / APPEND_ONLY`  
Parent: `rafcode_route_v1`  
claim_allowed: `false`

## Delta

A direct fixed-frame executable boundary was materialized for host, ARMv7 Android and AArch64 Android. It reuses the existing resolver core and adds only stack-local entry logic plus raw read/write/exit syscall veneers.

No JNI, libc, CRT, allocator, filesystem, network or provider dependency enters this executable.

## State at write

```text
SOURCE = IMPLEMENTED
HOST_SMOKE = TOKEN_VAZIO_CI_PENDING
HOST_ELF_AUDIT = TOKEN_VAZIO_CI_PENDING
ARMV7_ELF = TOKEN_VAZIO_CI_PENDING
AARCH64_ELF = TOKEN_VAZIO_CI_PENDING
PHYSICAL_TERMUX_ARM32 = TOKEN_VAZIO
PHYSICAL_TERMUX_ARM64 = TOKEN_VAZIO
```

R3=<F_ok: direct freestanding device-oriented path materialized; F_gap: exact-head build/audit and physical execution; F_next: run negative ELF gates for all ABIs, then use an actual Termux/ADB runner for device receipts>.


## Parent integration successor

Parent instrumentation repair `#488@9a0dd89e57d15d42491845352a890a66b6b1a658`
passed canonical START `35837104677` and was merged to `main` as
`ceb6ce0e3736ccc059aa2e398757083562e8be26`.

This route-executable PR is therefore retargeted to `main` with only its
own low-level executable/workflow/receipt delta.

```text
PARENT_ANDROIDTEST_COMPILE = PASS_CI_BOUNDED
PARENT_MAIN_INTEGRATION = OBSERVED
ROUTE_EXEC_EXACT_HEAD_CI = TOKEN_VAZIO_PENDING_SUCCESSOR
PHYSICAL_TERMUX_ARM32 = TOKEN_VAZIO
PHYSICAL_TERMUX_ARM64 = TOKEN_VAZIO
```
