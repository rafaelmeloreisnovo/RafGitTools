# RAFAELIA astrometry L0 — exact 2D/3-epoch invariants

Status: SOURCE_CANDIDATE; observed local gates are recorded separately. Never infer gravitational waves, lensing, orbital elements, or RLL from the algebra alone. This is an additive bounded kernel; `tools/multilayer_geometry` and `freestanding/lens` remain unchanged.

## Authority and units

Coordinates and epoch ticks are signed/unsigned 16-bit integers with **caller-supplied scale, unit, frame, and time standard**. Zero is data, `present=0` is absence, and a failed call leaves output invalid. No implicit conversion of missing, empty, null, whitespace, or ambiguous lexical tokens into numbers is allowed. The caller must validate those before invoking L0.

For epochs `t0<t1<t2`, let `d01=t1-t0`, `d12=t2-t1`, `d02=t2-t0`. The exact area and chords are `area2=det(P1-P0,P2-P0)` and `chord_ij_sq=||Pi-Pj||²`. If all three chords are nonzero and `area2≠0`, circle curvature is the **formal expression** `2|area2|/sqrt(chord01_sq*chord12_sq*chord02_sq)`. Report the formula, not a rounded decimal or a numeric product that overflows 64 bits. Three points do not identify a physical orbit.

Second divided difference: `acceleration = 2*((P2-P1)*d01-(P1-P0)*d12)/(d01*d12*d02)`. L0 stores exact signed numerators and positive **factored** denominator, so no enormous product is materialized. This is a kinematic finite-difference estimate, not a physical gravitational acceleration without a model.

Ray/conic intersection has the formal root `(-b±sqrt(b²-4ac))/(2a)`. L0 computes the discriminant exactly; no square root or ray ordering is claimed. `a=0` is not silently promoted to a quadratic; caller must route that linear case separately. Numeric comparisons need calibrated uncertainty and reference frame.

## Freestanding and ABI boundary

No libc, heap, JNI, NDK, JDK, SDK, Gradle, R8, syscall, OS or external includes in the **core**. The compiler/linker/toolchain and the hosted test runner remain external; this is not an OS, APK or standalone executable. Public API function names are ABI contracts; eliminating symbol names would make linkage and auditability worse. This code uses fixed-width primitives under compile-time width assertions.

## Minimal standalone hosted verification

```sh
cc -std=c11 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=all \
  freestanding/astrometry_l0/raf_astrometry.c \
  freestanding/astrometry_l0/test_host.c -o /tmp/raf_astrometry_test
/tmp/raf_astrometry_test
clang --target=armv7a-none-eabi -std=c11 -Os -ffreestanding -fno-builtin \
  -c freestanding/astrometry_l0/raf_astrometry.c -o /tmp/raf_astrometry_arm.o
nm -u /tmp/raf_astrometry_arm.o
```

A verified core cross-object is not proof of ARM-device execution. Test independently on ARMv7 and AArch64 with exact-HEAD receipts before claim promotion.

## Falsifiers / PIRC-2

- Three collinear distinct points → exact zero area and no circumcircle, not an inferred orbit.
- Equal/out-of-order epochs, absent observation, repeated coordinates → typed rejection and no output commit.
- Input extremes and 10,000 translated triangles → exact outputs without overflow or undefined behavior in declared range.
- `a=0`, discriminants negative/zero/positive → reject linear misuse; exact tangency classification.
- Physically degenerate data: two/three directions alone do not identify distance, lens model, gravitational-wave strain, or spin. Those remain `TOKEN_VAZIO` until calibration, many-source/time-series data and physical model selection.

Rollback: do not merge additive branch; no producer existing file is replaced. `SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM`; `claim_allowed=false`.

## Successor ABI hotfix (2026-11-18)

Elapsed-tick fields now use 32-bit `unsigned int` (checked by `_Static_assert`), not target-dependent `unsigned long`. This removes one data-model-size discrepancy across ARMv7/AArch64/x86-64 without changing the mathematical values. **Never serialize `struct raf_geo3` by raw memory copy across targets**: alignment/padding can still differ. A canonical byte-order field-by-field wire format and independent ABI receipts remain gates.
