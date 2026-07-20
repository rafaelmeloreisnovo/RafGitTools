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
| Test execution on runner | `TOKEN_VAZIO` |
| ARM32 compile evidence | `TOKEN_VAZIO` |
| ARM64 compile evidence | `TOKEN_VAZIO` |
| Android device runtime | `TOKEN_VAZIO` |

Machine-readable state is recorded in `configs/browserraf-csprng.phase2.json`.

## Next operational gate

The next gate is not to enable HTTPS. It is to establish reproducible evidence in this order:

1. compile the BrowserRaf source for ARM32 and ARM64;
2. run a minimal entropy smoke test on an Android device;
3. record binary hashes, kernel/API information and exact command lines;
4. prove failure behavior when the syscall is unavailable or rejected;
5. only then begin the X25519 and HKDF provider boundary.

## Canonical conclusion

```text
deterministic TLS random = removed
kernel entropy boundary  = implemented
cross-ABI proof          = TOKEN_VAZIO
runtime proof            = TOKEN_VAZIO
complete TLS             = false
```
