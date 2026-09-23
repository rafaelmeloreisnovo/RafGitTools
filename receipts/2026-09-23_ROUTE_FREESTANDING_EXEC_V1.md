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
