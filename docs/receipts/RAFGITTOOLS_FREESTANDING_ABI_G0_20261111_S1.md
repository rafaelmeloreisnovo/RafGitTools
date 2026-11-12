# μRECEIPT RAFAELIA Ω — ABI L0 G0 (2026-11-11)

**μID:** MU-RAFGITTOOLS-L0-ABI-G0-20261111-S1  
**Type:** RECEIPT / N0_LOCAL_EXECUTION / PASS_SCOPED  
**claim_allowed:** false  
**Producer:** rafaelmeloreisnovo/RafGitTools  
**Branch:** audit/freestanding-abi-five-boundary-gate-20261111  
**Base:** 164196721643a1e3ee736a29799884fb205e3f4b (main, snapshot)  
**Source:** native/raf_gate_l0_v1.c  
**Contract:** contracts/federated-freestanding-review-gate.v1.json, 1.0.2  
**Cross-repo status:** NOT_INTEGRATED / NOT_RUN.

## Executed local gate
- Command: sh scripts/verify_freestanding_gate_v1.sh
- Environment: GNU GCC 14.2.0, Clang 17.0.0, GNU Binutils 2.44; host Linux (not Motorola Android).
- Both hosted C11 tests passed on synthetic positive/negative vectors.
- ARMv7 armv7a-none-eabi, portable -mfloat-abi=soft -mfpu=none: ELF32 ARM ET_REL, .text 68 B, nm -u empty.
- AArch64 aarch64-none-elf: ELF64 AArch64 ET_REL, .text 72 B, nm -u empty.
- x86_64 x86_64-unknown-none-elf: ELF64 x86_64 ET_REL, .text 70 B, nm -u empty.
- Negative preflight: executing with PATH lacking clang returned code 127 and "gate=BLOCKED missing_host_tool=clang".
- Prior default ARMv7 compiler profile emitted NEON instructions and a .text section of 88 B. This motivated explicit portable target flags. Difference 20 B in section .text, **not** a throughput/latency benchmark.
- No library/server/device/boot/runtime/protocol execution is inferred. ET_REL!=EXEC and empty undefined symbol table!=absence of all platform dependencies.
- Source used in local build was compared to GitHub readback by Git blob identity.

## Byte identity
- C source SHA-256: b12dca84587a5b3c7eb7ae7df03dcda4e8c21defebc1ab01adb30bcd34c76452.
- C Git blob SHA-1: ebf0716f8e941f59dc41df187a3a92c4b99428f7.
- Host test SHA-256: 8f9bbe72583b6cfa1746fc69963a65537cd5a64b2705152e1a8bd3368d3a920d.
- Host test Git blob SHA-1: df67f0da342c90d482920f9e78c5aaef9fe8d5e8.
- Hosted gate script SHA-256: 32f0d79aaf75a4df6a21c418679abb41bcc103fcfca0d6d7430b2bcd6226a48a.
- Hosted gate Git blob SHA-1: 64c8e3a9d225a01a2e1e0fc043812e54f57234f1.

## Typed unresolved gates
G1 hosted GitHub CI exact branch HEAD=NOT_RUN; G2 linked ELF=NOT_RUN; G3 ARM32/AArch64 actual runtime=NOT_RUN; G4 Termux APT bootstrap and Android packaging=NOT_RUN; G5 Vectra VM provider+consumer transaction=NOT_RUN; G6 Firebird engine SQL=NOT_RUN; G7 Zabbix repository identifier=TOKEN_VAZIO; G8 external authorization/independent review=NOT_VERIFIED. Formal license proof of firebird_APK_sever_admin=TOKEN_VAZIO. None of these entries is a numeric zero.

## Security/data/privacy and licensing
Only bounded source/ref/status/hashes. No secrets, user logs, sensor data or private Firebird datasets copied. L0 source is licensed GPL-3.0-or-later under the RafGitTools repository; other repositories retain their license boundaries. No upstream code imported.

## Control, stop and rollback
Falsifiers: UNKNOWN/UNSET=0, FAIL=2, NOT_RUN=3, typed TOKEN_VAZIO=4 and invalid=999 all block, null pointer blocks. This is synthetic input testing, not evidence authentication. No PR or merge authorized. Stop at L0 local gate until provider, CI and consumer tests own their scopes; rollback is leaving the review branch unmerged and correction by successor commit.

R3=<F_ok:HOST_TEST+3_OBJECTS+MISSING_TOOL_NEGATIVE+GIT_BLOB_READBACK, F_gap:CI_LINK_DEVICE_LICENSE_PROVIDER_CONSUMER, F_next:provider exact-HEAD CI then independent repo-specific falsification>.
