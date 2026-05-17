# VECTRA_OS.md — Zero-Abstraction Assembly Contract

This repository uses a low-level bootstrap and kernel style for RafaelOS/VECTRA assembly modules.
Read this file before changing `.S` sources.

## Scope

- Applies to assembly sources in this repository unless a narrower contract is documented in the source file itself.
- Bootstrap entry modules may declare their own register contract for process setup and termination.
- Kernel/hot-path modules must preserve the fixed VECTRA register contract below.

## Fixed VECTRA Register Contract

- `x0` / `r0`: state vector base pointer.
- `x1` / `r1`: coherence `C` in Q16.16 fixed-point.
- `x2` / `r2`: entropy `H` in Q16.16 fixed-point.
- `x3` / `r3`: phase counter modulo 42.
- `x4` / `r4`: attractor index, range `0..41`.

Do not use `x0..x4` freely outside designated modules. If a bootstrap or ABI boundary must use one of these registers, document the exact use at the use site.

## Assembly Rules

- Prefer local macros for known transition points; do not branch/link to undocumented external symbols.
- Do not add heap allocation or libc dependencies to assembly modules.
- Keep loops bounded and justify termination by an invariant such as `gcd(Δr, R) = 1` when traversal is toroidal.
- Keep `COLLAPSE_STEP` as a macro expansion, not a function call.
- Preserve `|A| = 42` and `period(BitOmega) = 42` invariants.

## AArch64 Bootstrap Exception

A file may designate its `_start` block as a bootstrap module. In that block only:

- `x29`, `x30`, and `sp` may be normalized before transferring control to the program entry.
- `x0` may be written only as the immediate `exit_group(status)` argument after the program entry returns.
- `x8` may hold the Linux/Android syscall number for `exit_group`.
- `x1..x4` remain untouched unless the file explicitly documents a stronger bootstrap ABI.

## VOID Paradox

Attractor `#22` is the VOID paradox and must be flagged, not silently patched.
