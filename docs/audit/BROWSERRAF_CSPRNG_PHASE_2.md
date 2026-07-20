# BrowserRaf CSPRNG — Phase 2

**Repository:** `rafaelmeloreisnovo/RafGitTools`  
**Base commit:** `ff33313f87fafd6ff4cf89a7adc367ffa60094d1`  
**Branch:** `codex/browserraf-csprng-fail-closed-phase-2`  
**Date:** 2026-07-20

## Objective

Close the source-level blocker `TLS-002` without claiming that BrowserRaf already provides a complete TLS handshake, operational HTTPS or external certification.

The previous implementation filled the TLS `ClientHello.random` field with a deterministic fixed-seed LFSR. That behavior was unsuitable for a cryptographic protocol and remained explicitly blocked in the Phase 1 runtime map.

## Implemented boundary

The new `BrowserRaf/internal/br_entropy.h` provides a freestanding Linux `getrandom` boundary for:

| Target | Syscall number |
|---|---:|
| ARM32 / `armeabi-v7a` | 384 |
| ARM64 / `arm64-v8a` | 278 |
| x86-64 | 318 |

The contract is deliberately strict:

```text
short read   -> continue until the destination is full
EINTR        -> retry
zero return  -> clear the complete destination and fail
other error  -> clear the complete destination and fail
fallback     -> none
```

`TLS_INIT` now returns a status. It calls `BR_RANDOM_FILL` and records `TLS_ALERT_INTERNAL_ERROR` when the kernel cannot provide the requested bytes.

## Reproducible compile gate

`tools/verify_browserraf_entropy_cross_abi.py` compiles a minimal probe against the repository headers for all three supported targets. It preserves `-Wall`, `-Wextra` and `-Werror`, suppressing only `unused-function` because `br_sys.h` intentionally contains UI helpers that are unrelated to the entropy probe.

The verifier checks:

- compiler return code;
- ELF magic;
- 32-bit versus 64-bit ELF class;
- little-endian encoding;
- expected `e_machine` for ARM, AArch64 and x86-64;
- SHA-256 of every generated relocatable object;
- SHA-256 of the three source headers used by the proof.

An independent connector-materialized execution produced relocatable ELF objects for the three targets. This is classified as `PASS_LIMITED`: it proves source-level cross-target compilation, not canonical CI execution or Android runtime behavior.

## Claims that remain forbidden

This phase does **not** provide:

- X25519 key exchange;
- HKDF or transcript hashing;
- certificate-chain validation;
- hostname validation;
- Finished verification;
- AEAD record protection;
- a TLS 1.2 profile;
- HTTPS runtime enablement;
- TLS certification.

The canonical state remains:

```text
https = FAIL_CLOSED
claim_allowed = false
certified = false
```

## Evidence ledger

| Evidence | State |
|---|---|
| Entropy source boundary | `IMPLEMENTED` |
| TLS consumer integration | `IMPLEMENTED` |
| Regression test contract | `PRESENT` |
| Cross-ABI verifier | `PRESENT` |
| Independent host contract logic | `PASS_LIMITED` |
| Independent ARM32/ARM64/x86-64 compile | `PASS_LIMITED` |
| Canonical runner execution | `TOKEN_VAZIO` |
| Android device runtime | `TOKEN_VAZIO` |

Machine-readable state is recorded in `configs/browserraf-csprng.phase2.json`.

## Next operational gate

The next gate is not to enable HTTPS. It is to establish canonical and device evidence in this order:

1. execute `tools/verify_browserraf_entropy_cross_abi.py` from an actual repository checkout;
2. persist the generated manifest and source hashes;
3. run a minimal entropy smoke test on an Android device;
4. record kernel/API information, exact commands and binary hashes;
5. prove failure behavior when the syscall is unavailable or rejected;
6. only then begin the X25519 and HKDF provider boundary.

## Canonical conclusion

```text
deterministic TLS random = removed
kernel entropy boundary  = implemented
cross-ABI compile        = PASS_LIMITED
canonical runner proof   = TOKEN_VAZIO
Android runtime proof    = TOKEN_VAZIO
complete TLS             = false
```
